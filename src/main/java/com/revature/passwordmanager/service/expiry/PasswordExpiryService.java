package com.revature.passwordmanager.service.expiry;

import com.revature.passwordmanager.dto.request.ExpiryPolicyRequest;
import com.revature.passwordmanager.dto.response.ExpiryPolicyResponse;
import com.revature.passwordmanager.dto.response.ExpiryStatusResponse;
import com.revature.passwordmanager.dto.response.ExpiryStatusResponse.EntryExpiryDetail;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.expiry.ExpiryPolicy;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.ExpiryPolicyRepository;
import com.revature.passwordmanager.repository.PasswordExpiryStatusRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Core service for the expiry feature. Handles:</p>
 * <ul>
 *   <li>Policy CRUD (get / update)</li>
 *   <li>Expiry status queries (all entries, expiring-soon)</li>
 *   <li>Snooze management</li>
 *   <li>Lifecycle hooks called by {@link com.revature.passwordmanager.service.vault.VaultService}
 *       when a vault entry is created or its password is updated</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PasswordExpiryService {

    private final UserRepository userRepository;
    private final VaultEntryRepository vaultEntryRepository;
    private final ExpiryPolicyRepository expiryPolicyRepository;
    private final PasswordExpiryStatusRepository expiryStatusRepository;
    private final ExpiryPolicyEngine policyEngine;

    // ── Policy ────────────────────────────────────────────────────────────────

    /**
     * Returns the user's expiry policy, creating a default one if none exists.
     */
    @Transactional
    public ExpiryPolicyResponse getPolicy(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        ExpiryPolicy policy = getOrCreatePolicy(user);
        return mapPolicyToResponse(policy);
    }

    /**
     * Updates the user's expiry policy. Only non-null fields in the request are applied.
     * After updating, all existing expiry status records are recomputed.
     */
    @Transactional
    public ExpiryPolicyResponse updatePolicy(String username, ExpiryPolicyRequest request) {
        User user = userRepository.findByUsernameOrThrow(username);
        ExpiryPolicy policy = getOrCreatePolicy(user);

        if (request.getDefaultExpiryDays() != null) {
            policy.setDefaultExpiryDays(request.getDefaultExpiryDays());
        }
        if (request.getCriticalExpiryDays() != null) {
            policy.setCriticalExpiryDays(request.getCriticalExpiryDays());
        }
        if (request.getReminderDaysBefore() != null) {
            policy.setReminderDaysBefore(request.getReminderDaysBefore());
        }
        if (request.getEnabled() != null) {
            policy.setEnabled(request.getEnabled());
        }

        policy = expiryPolicyRepository.save(policy);

        // Recompute all expiry statuses with the new policy
        recomputeAllStatuses(user, policy);

        return mapPolicyToResponse(policy);
    }

    // ── Status queries ────────────────────────────────────────────────────────

    /**
     * Returns the full expiry status for all active vault entries belonging to the user.
     */
    @Transactional(readOnly = true)
    public ExpiryStatusResponse getAllStatuses(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        List<PasswordExpiryStatus> statuses = expiryStatusRepository.findAllByUserId(user.getId());

        LocalDateTime now = LocalDateTime.now();
        List<EntryExpiryDetail> details = statuses.stream()
                .map(s -> mapToDetail(s, now))
                .collect(Collectors.toList());

        long fresh = details.stream().filter(d -> "FRESH".equals(d.getStatus())).count();
        long aging = details.stream().filter(d -> "AGING".equals(d.getStatus())).count();
        long expiringSoon = details.stream().filter(d -> "EXPIRING_SOON".equals(d.getStatus())).count();
        long expired = details.stream().filter(d -> "EXPIRED".equals(d.getStatus())).count();

        return ExpiryStatusResponse.builder()
                .totalEntries(details.size())
                .freshCount((int) fresh)
                .agingCount((int) aging)
                .expiringSoonCount((int) expiringSoon)
                .expiredCount((int) expired)
                .entries(details)
                .build();
    }

    /**
     * Returns only the entries expiring within the next {@code days} days.
     *
     * @param username the authenticated user
     * @param days     look-ahead window in days (default 7 if null)
     */
    @Transactional(readOnly = true)
    public ExpiryStatusResponse getExpiringSoon(String username, Integer days) {
        User user = userRepository.findByUsernameOrThrow(username);
        int lookAhead = (days != null && days > 0) ? days : 7;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.plusDays(lookAhead);

        List<PasswordExpiryStatus> statuses =
                expiryStatusRepository.findExpiringSoon(user.getId(), now, cutoff);

        List<EntryExpiryDetail> details = statuses.stream()
                .map(s -> mapToDetail(s, now))
                .collect(Collectors.toList());

        return ExpiryStatusResponse.builder()
                .totalEntries(details.size())
                .expiringSoonCount(details.size())
                .entries(details)
                .build();
    }

    // ── Snooze ────────────────────────────────────────────────────────────────

    /**
     * Snoozes the expiry reminder for a vault entry for the specified number of days.
     * The reminder will not be re-sent until the snooze period expires.
     *
     * @param username the authenticated user
     * @param entryId  the vault entry id
     * @param days     number of days to snooze (default 7 if null)
     */
    @Transactional
    public EntryExpiryDetail snoozeReminder(String username, Long entryId, Integer days) {
        User user = userRepository.findByUsernameOrThrow(username);
        VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

        PasswordExpiryStatus status = expiryStatusRepository.findByVaultEntryId(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Expiry status not found for entry " + entryId));

        int snoozeDays = (days != null && days > 0) ? days : 7;
        status.setSnoozedUntil(LocalDateTime.now().plusDays(snoozeDays));
        expiryStatusRepository.save(status);

        return mapToDetail(status, LocalDateTime.now());
    }

    // ── Lifecycle hooks (called by VaultService) ──────────────────────────────

    /**
     * Creates or resets the expiry status record when a vault entry is created
     * or its password is updated.
     *
     * @param entry the vault entry that was created or updated
     */
    @Transactional
    public void onPasswordChanged(VaultEntry entry) {
        User user = entry.getUser();
        ExpiryPolicy policy = getOrCreatePolicy(user);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = policyEngine.computeExpiresAt(now, policy);
        ExpiryState state = policyEngine.computeState(now, expiresAt, policy, now);

        PasswordExpiryStatus status = expiryStatusRepository.findByVaultEntryId(entry.getId())
                .orElse(PasswordExpiryStatus.builder().vaultEntry(entry).build());

        status.setLastChangedAt(now);
        status.setExpiresAt(expiresAt);
        status.setStatus(state);
        status.setReminderSent(false);
        status.setSnoozedUntil(null);

        expiryStatusRepository.save(status);
    }

    /**
     * Removes the expiry status record when a vault entry is permanently deleted.
     *
     * @param entryId the vault entry id
     */
    @Transactional
    public void onEntryDeleted(Long entryId) {
        expiryStatusRepository.deleteByVaultEntryId(entryId);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private ExpiryPolicy getOrCreatePolicy(User user) {
        return expiryPolicyRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    ExpiryPolicy defaultPolicy = ExpiryPolicy.builder().user(user).build();
                    return expiryPolicyRepository.save(defaultPolicy);
                });
    }

    private void recomputeAllStatuses(User user, ExpiryPolicy policy) {
        List<PasswordExpiryStatus> statuses = expiryStatusRepository.findAllByUserId(user.getId());
        LocalDateTime now = LocalDateTime.now();

        for (PasswordExpiryStatus status : statuses) {
            LocalDateTime expiresAt = policyEngine.computeExpiresAt(status.getLastChangedAt(), policy);
            ExpiryState state = policyEngine.computeState(status.getLastChangedAt(), expiresAt, policy, now);
            status.setExpiresAt(expiresAt);
            status.setStatus(state);
            // Reset reminder flag so users get notified again if state changed
            if (state == ExpiryState.FRESH || state == ExpiryState.AGING) {
                status.setReminderSent(false);
            }
        }
        expiryStatusRepository.saveAll(statuses);
    }

    private EntryExpiryDetail mapToDetail(PasswordExpiryStatus status, LocalDateTime now) {
        VaultEntry entry = status.getVaultEntry();
        long daysUntilExpiry = status.getExpiresAt() != null
                ? policyEngine.daysUntilExpiry(status.getExpiresAt(), now)
                : Long.MAX_VALUE;

        boolean snoozed = status.getSnoozedUntil() != null && status.getSnoozedUntil().isAfter(now);

        return EntryExpiryDetail.builder()
                .entryId(entry.getId())
                .title(entry.getTitle())
                .username(entry.getUsername())
                .websiteUrl(entry.getWebsiteUrl())
                .lastChangedAt(status.getLastChangedAt())
                .expiresAt(status.getExpiresAt())
                .status(status.getStatus().name())
                .daysUntilExpiry(daysUntilExpiry)
                .reminderSent(Boolean.TRUE.equals(status.getReminderSent()))
                .snoozed(snoozed)
                .snoozedUntil(status.getSnoozedUntil())
                .build();
    }

    private ExpiryPolicyResponse mapPolicyToResponse(ExpiryPolicy policy) {
        return ExpiryPolicyResponse.builder()
                .id(policy.getId())
                .defaultExpiryDays(policy.getDefaultExpiryDays())
                .criticalExpiryDays(policy.getCriticalExpiryDays())
                .reminderDaysBefore(policy.getReminderDaysBefore())
                .enabled(policy.getEnabled())
                .build();
    }
}
