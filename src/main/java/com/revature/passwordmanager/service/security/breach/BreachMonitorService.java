package com.revature.passwordmanager.service.security.breach;

import com.revature.passwordmanager.dto.response.BreachHistoryResponse;
import com.revature.passwordmanager.dto.response.BreachScanResponse;
import com.revature.passwordmanager.dto.response.BreachStatusResponse;
import com.revature.passwordmanager.dto.response.CompromisedCredentialResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.security.breach.BreachScanRecord;
import com.revature.passwordmanager.model.security.breach.BreachScanRecord.TriggerType;
import com.revature.passwordmanager.model.security.breach.CompromisedCredential;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.model.security.AuditLog.AuditAction;
import com.revature.passwordmanager.repository.BreachScanRecordRepository;
import com.revature.passwordmanager.repository.CompromisedCredentialRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.security.AuditLogService;
import com.revature.passwordmanager.service.security.EncryptionService;
import com.revature.passwordmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BreachMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(BreachMonitorService.class);

    private final UserRepository userRepository;
    private final VaultEntryRepository vaultEntryRepository;
    private final CompromisedCredentialRepository compromisedCredentialRepository;
    private final BreachScanRecordRepository breachScanRecordRepository;
    private final EncryptionService encryptionService;
    private final EncryptionUtil encryptionUtil;
    private final HaveIBeenPwnedClient hibpClient;
    private final BreachNotificationService notificationService;
    private final AuditLogService auditLogService;

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Triggers a manual breach scan for the given user's entire vault.
     * Decrypts each entry, checks HIBP, persists results, sends notifications.
     */
    @Transactional
    public BreachScanResponse runScan(String username, TriggerType triggerType) {
        User user = userRepository.findByUsernameOrThrow(username);
        List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());

        BreachScanRecord scanRecord = BreachScanRecord.builder()
                .user(user)
                .triggerType(triggerType)
                .entriesScanned(entries.size())
                .compromisedFound(0)
                .status(BreachScanRecord.ScanStatus.IN_PROGRESS)
                .build();
        scanRecord = breachScanRecordRepository.save(scanRecord);

        List<CompromisedCredentialResponse> newlyCompromised = new ArrayList<>();
        int compromisedCount = 0;

        try {
            SecretKey key = encryptionUtil.deriveKey(
                    user.getMasterPasswordHash(), user.getSalt());

            for (VaultEntry entry : entries) {
                try {
                    String plainPassword = encryptionService.decrypt(entry.getPassword(), key);
                    long pwnedCount = hibpClient.checkPassword(plainPassword);

                    if (pwnedCount > 0) {
                        compromisedCount++;
                        boolean alreadyKnown = compromisedCredentialRepository
                                .existsByUserIdAndVaultEntryIdAndIsResolvedFalse(user.getId(), entry.getId());

                        if (!alreadyKnown) {
                            CompromisedCredential credential = CompromisedCredential.builder()
                                    .user(user)
                                    .vaultEntry(entry)
                                    .pwnedCount(pwnedCount)
                                    .hashPrefix(hibpClient.getHashPrefix(plainPassword))
                                    .isResolved(false)
                                    .build();
                            compromisedCredentialRepository.save(credential);
                            notificationService.notifyBreach(username, entry, pwnedCount);
                            // Gap 8 fix: audit log each new breach detection
                            auditLogService.logAction(username, AuditAction.BREACH_DETECTED,
                                    String.format("Password breached: entry='%s' pwnedCount=%d",
                                            entry.getTitle(), pwnedCount));
                            newlyCompromised.add(mapToCredentialResponse(credential, entry));
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Skipping entry {} during breach scan: {}", entry.getId(), e.getMessage());
                }
            }

            scanRecord.setCompromisedFound(compromisedCount);
            scanRecord.setStatus(BreachScanRecord.ScanStatus.COMPLETED);
            breachScanRecordRepository.save(scanRecord);

            notificationService.notifyScanComplete(username, compromisedCount, entries.size());
            // Gap 8 fix: audit log the scan run
            auditLogService.logAction(username, AuditAction.BREACH_SCAN_RUN,
                    String.format("Breach scan completed: scanned=%d compromised=%d trigger=%s",
                            entries.size(), compromisedCount, triggerType.name()));
            logger.info("Breach scan complete: user={} scanned={} compromised={}",
                    username, entries.size(), compromisedCount);

        } catch (Exception e) {
            logger.error("Breach scan failed for user {}: {}", username, e.getMessage());
            scanRecord.setStatus(BreachScanRecord.ScanStatus.FAILED);
            scanRecord.setErrorMessage(e.getMessage());
            breachScanRecordRepository.save(scanRecord);
        }

        return BreachScanResponse.builder()
                .scanId(scanRecord.getId())
                .entriesScanned(entries.size())
                .compromisedFound(compromisedCount)
                .status(scanRecord.getStatus().name())
                .triggerType(triggerType.name())
                .scannedAt(scanRecord.getScannedAt())
                .newlyCompromised(newlyCompromised)
                .build();
    }

    /**
     * Returns the current breach status summary for a user.
     */
    @Transactional(readOnly = true)
    public BreachStatusResponse getStatus(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        long compromisedCount = compromisedCredentialRepository
                .countByUserIdAndIsResolvedFalse(user.getId());
        int totalEntries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId()).size();

        Optional<BreachScanRecord> lastScan = breachScanRecordRepository
                .findTopByUserIdOrderByScannedAtDesc(user.getId());

        String overallStatus;
        String recommendation;

        if (compromisedCount == 0) {
            overallStatus = "SAFE";
            recommendation = lastScan.isPresent()
                    ? "No compromised passwords found. Run regular scans to stay safe."
                    : "No scan has been run yet. Run a breach scan to check your vault.";
        } else if (compromisedCount <= 3) {
            overallStatus = "AT_RISK";
            recommendation = String.format(
                    "%d password(s) have been found in data breaches. Update them immediately.", compromisedCount);
        } else {
            overallStatus = "COMPROMISED";
            recommendation = String.format(
                    "%d password(s) are compromised. This is a critical security situation — update all affected passwords now.", compromisedCount);
        }

        return BreachStatusResponse.builder()
                .overallStatus(overallStatus)
                .totalCompromised((int) compromisedCount)
                .totalVaultEntries(totalEntries)
                .lastScanAt(lastScan.map(BreachScanRecord::getScannedAt).orElse(null))
                .scanInProgress(false)
                .recommendation(recommendation)
                .build();
    }

    /**
     * Returns all unresolved (active) compromised credentials for the user.
     */
    @Transactional(readOnly = true)
    public List<CompromisedCredentialResponse> getCompromisedCredentials(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        return compromisedCredentialRepository
                .findActiveByUserIdOrderByPwnedCountDesc(user.getId())
                .stream()
                .map(c -> mapToCredentialResponse(c, c.getVaultEntry()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the scan history for the user.
     */
    @Transactional(readOnly = true)
    public BreachHistoryResponse getBreachHistory(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        List<BreachScanRecord> records = breachScanRecordRepository
                .findByUserIdOrderByScannedAtDesc(user.getId());

        List<BreachHistoryResponse.ScanHistoryEntry> entries = records.stream()
                .map(r -> BreachHistoryResponse.ScanHistoryEntry.builder()
                        .scanId(r.getId())
                        .triggerType(r.getTriggerType().name())
                        .entriesScanned(r.getEntriesScanned())
                        .compromisedFound(r.getCompromisedFound())
                        .status(r.getStatus().name())
                        .scannedAt(r.getScannedAt())
                        .build())
                .collect(Collectors.toList());

        int totalCompromised = records.stream()
                .mapToInt(BreachScanRecord::getCompromisedFound).sum();

        return BreachHistoryResponse.builder()
                .scanHistory(entries)
                .totalScans(records.size())
                .totalCompromisedFound(totalCompromised)
                .build();
    }

    /**
     * Marks a specific compromised credential as resolved (password updated).
     */
    @Transactional
    public CompromisedCredentialResponse resolveCredential(String username, Long credentialId) {
        User user = userRepository.findByUsernameOrThrow(username);
        CompromisedCredential credential = compromisedCredentialRepository.findById(credentialId)
                .orElseThrow(() -> new ResourceNotFoundException("Compromised credential not found"));

        if (!credential.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Credential does not belong to this user");
        }

        credential.setResolved(true);
        credential.setResolvedAt(LocalDateTime.now());
        compromisedCredentialRepository.save(credential);
        // Gap 8 fix: audit log manual breach resolution
        auditLogService.logAction(username, AuditAction.BREACH_RESOLVED,
                String.format("Breach credential resolved: entry='%s'",
                        credential.getVaultEntry().getTitle()));

        return mapToCredentialResponse(credential, credential.getVaultEntry());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private CompromisedCredentialResponse mapToCredentialResponse(
            CompromisedCredential credential, VaultEntry entry) {
        return CompromisedCredentialResponse.builder()
                .id(credential.getId())
                .vaultEntryId(entry.getId())
                .vaultEntryTitle(entry.getTitle())
                .username(entry.getUsername())
                .websiteUrl(entry.getWebsiteUrl())
                .pwnedCount(credential.getPwnedCount())
                .resolved(credential.isResolved())
                .detectedAt(credential.getDetectedAt())
                .resolvedAt(credential.getResolvedAt())
                .build();
    }
}
