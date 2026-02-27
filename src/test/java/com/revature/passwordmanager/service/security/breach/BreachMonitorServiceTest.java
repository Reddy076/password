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
import com.revature.passwordmanager.repository.BreachScanRecordRepository;
import com.revature.passwordmanager.repository.CompromisedCredentialRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.security.AuditLogService;
import com.revature.passwordmanager.service.security.EncryptionService;
import com.revature.passwordmanager.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BreachMonitorServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VaultEntryRepository vaultEntryRepository;
    @Mock private CompromisedCredentialRepository compromisedCredentialRepository;
    @Mock private BreachScanRecordRepository breachScanRecordRepository;
    @Mock private EncryptionService encryptionService;
    @Mock private EncryptionUtil encryptionUtil;
    @Mock private HaveIBeenPwnedClient hibpClient;
    @Mock private BreachNotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private BreachMonitorService service;

    private User user;
    private SecretKey mockKey;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).username("testuser").email("test@example.com")
                .masterPasswordHash("hash").salt("salt").build();
        mockKey = mock(SecretKey.class);
    }

    // ── runScan ───────────────────────────────────────────────────────────────

    @Test
    void runScan_NoEntries_ShouldReturnZeroCompromised() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());
        when(breachScanRecordRepository.save(any())).thenAnswer(inv -> {
            BreachScanRecord r = inv.getArgument(0);
            r = BreachScanRecord.builder().id(1L).user(r.getUser())
                    .triggerType(r.getTriggerType()).entriesScanned(r.getEntriesScanned())
                    .compromisedFound(r.getCompromisedFound()).status(r.getStatus())
                    .scannedAt(LocalDateTime.now()).build();
            return r;
        });

        BreachScanResponse result = service.runScan("testuser", TriggerType.MANUAL);

        assertNotNull(result);
        assertEquals(0, result.getEntriesScanned());
        assertEquals(0, result.getCompromisedFound());
        assertEquals("COMPLETED", result.getStatus());
        verifyNoInteractions(hibpClient);
    }

    @Test
    void runScan_OneBreachedEntry_ShouldDetectAndPersist() {
        VaultEntry entry = buildEntry(10L, "Gmail");
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(entry));
        when(encryptionUtil.deriveKey(anyString(), anyString())).thenReturn(mockKey);
        when(encryptionService.decrypt(anyString(), eq(mockKey))).thenReturn("password");
        when(hibpClient.checkPassword("password")).thenReturn(3861493L);
        when(hibpClient.getHashPrefix("password")).thenReturn("5BAA6");
        when(compromisedCredentialRepository.existsByUserIdAndVaultEntryIdAndIsResolvedFalse(1L, 10L))
                .thenReturn(false);
        when(compromisedCredentialRepository.save(any())).thenAnswer(inv -> {
            CompromisedCredential c = inv.getArgument(0);
            c = CompromisedCredential.builder().id(1L).user(c.getUser())
                    .vaultEntry(c.getVaultEntry()).pwnedCount(c.getPwnedCount())
                    .hashPrefix(c.getHashPrefix()).isResolved(false)
                    .detectedAt(LocalDateTime.now()).build();
            return c;
        });
        when(breachScanRecordRepository.save(any())).thenAnswer(inv -> {
            BreachScanRecord r = inv.getArgument(0);
            if (r.getId() == null) {
                r = BreachScanRecord.builder().id(1L).user(r.getUser())
                        .triggerType(r.getTriggerType()).entriesScanned(r.getEntriesScanned())
                        .compromisedFound(r.getCompromisedFound()).status(r.getStatus())
                        .scannedAt(LocalDateTime.now()).build();
            }
            return r;
        });

        BreachScanResponse result = service.runScan("testuser", TriggerType.MANUAL);

        assertNotNull(result);
        assertEquals(1, result.getEntriesScanned());
        assertEquals(1, result.getCompromisedFound());
        assertEquals(1, result.getNewlyCompromised().size());
        assertEquals("Gmail", result.getNewlyCompromised().get(0).getVaultEntryTitle());
        verify(compromisedCredentialRepository).save(any(CompromisedCredential.class));
        verify(notificationService).notifyBreach(eq("testuser"), eq(entry), eq(3861493L));
    }

    @Test
    void runScan_AlreadyKnownBreach_ShouldNotDuplicate() {
        VaultEntry entry = buildEntry(10L, "Gmail");
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(entry));
        when(encryptionUtil.deriveKey(anyString(), anyString())).thenReturn(mockKey);
        when(encryptionService.decrypt(anyString(), eq(mockKey))).thenReturn("password");
        when(hibpClient.checkPassword("password")).thenReturn(5000L);
        when(compromisedCredentialRepository.existsByUserIdAndVaultEntryIdAndIsResolvedFalse(1L, 10L))
                .thenReturn(true); // already known
        when(breachScanRecordRepository.save(any())).thenAnswer(inv -> {
            BreachScanRecord r = inv.getArgument(0);
            if (r.getId() == null) r = BreachScanRecord.builder().id(1L).user(r.getUser())
                    .triggerType(r.getTriggerType()).entriesScanned(1)
                    .compromisedFound(1).status(BreachScanRecord.ScanStatus.IN_PROGRESS)
                    .scannedAt(LocalDateTime.now()).build();
            return r;
        });

        BreachScanResponse result = service.runScan("testuser", TriggerType.MANUAL);

        assertEquals(1, result.getCompromisedFound());
        assertEquals(0, result.getNewlyCompromised().size()); // no new — already tracked
        verify(compromisedCredentialRepository, never()).save(any(CompromisedCredential.class));
    }

    @Test
    void runScan_DecryptionFails_ShouldSkipEntryAndContinue() {
        VaultEntry goodEntry = buildEntry(10L, "Gmail");
        VaultEntry badEntry = buildEntry(11L, "Broken");
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L))
                .thenReturn(List.of(goodEntry, badEntry));
        when(encryptionUtil.deriveKey(anyString(), anyString())).thenReturn(mockKey);
        when(encryptionService.decrypt(anyString(), eq(mockKey)))
                .thenReturn("safepassword123!")
                .thenThrow(new RuntimeException("Decryption failed"));
        when(hibpClient.checkPassword("safepassword123!")).thenReturn(0L);
        when(breachScanRecordRepository.save(any())).thenAnswer(inv -> {
            BreachScanRecord r = inv.getArgument(0);
            if (r.getId() == null) r = BreachScanRecord.builder().id(1L).user(r.getUser())
                    .triggerType(r.getTriggerType()).entriesScanned(2)
                    .compromisedFound(0).status(BreachScanRecord.ScanStatus.IN_PROGRESS)
                    .scannedAt(LocalDateTime.now()).build();
            return r;
        });

        BreachScanResponse result = service.runScan("testuser", TriggerType.MANUAL);

        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(0, result.getCompromisedFound());
    }

    @Test
    void runScan_UserNotFound_ShouldThrow() {
        when(userRepository.findByUsernameOrThrow("unknown"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        assertThrows(ResourceNotFoundException.class,
                () -> service.runScan("unknown", TriggerType.MANUAL));
    }

    // ── getStatus ─────────────────────────────────────────────────────────────

    @Test
    void getStatus_NoCompromised_ShouldReturnSafe() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.countByUserIdAndIsResolvedFalse(1L)).thenReturn(0L);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(buildEntry(1L, "e1")));
        when(breachScanRecordRepository.findTopByUserIdOrderByScannedAtDesc(1L))
                .thenReturn(Optional.of(buildScanRecord(1)));

        BreachStatusResponse result = service.getStatus("testuser");

        assertEquals("SAFE", result.getOverallStatus());
        assertEquals(0, result.getTotalCompromised());
        assertNotNull(result.getLastScanAt());
    }

    @Test
    void getStatus_FewCompromised_ShouldReturnAtRisk() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.countByUserIdAndIsResolvedFalse(1L)).thenReturn(2L);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(buildEntry(1L, "e1")));
        when(breachScanRecordRepository.findTopByUserIdOrderByScannedAtDesc(1L))
                .thenReturn(Optional.of(buildScanRecord(1)));

        BreachStatusResponse result = service.getStatus("testuser");

        assertEquals("AT_RISK", result.getOverallStatus());
        assertEquals(2, result.getTotalCompromised());
    }

    @Test
    void getStatus_ManyCompromised_ShouldReturnCompromised() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.countByUserIdAndIsResolvedFalse(1L)).thenReturn(5L);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(buildEntry(1L, "e1")));
        when(breachScanRecordRepository.findTopByUserIdOrderByScannedAtDesc(1L))
                .thenReturn(Optional.of(buildScanRecord(1)));

        BreachStatusResponse result = service.getStatus("testuser");

        assertEquals("COMPROMISED", result.getOverallStatus());
        assertEquals(5, result.getTotalCompromised());
    }

    @Test
    void getStatus_NoScanYet_ShouldIndicateNoScan() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.countByUserIdAndIsResolvedFalse(1L)).thenReturn(0L);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(Collections.emptyList());
        when(breachScanRecordRepository.findTopByUserIdOrderByScannedAtDesc(1L))
                .thenReturn(Optional.empty());

        BreachStatusResponse result = service.getStatus("testuser");

        assertEquals("SAFE", result.getOverallStatus());
        assertNull(result.getLastScanAt());
        assertTrue(result.getRecommendation().contains("No scan"));
    }

    // ── getCompromisedCredentials ─────────────────────────────────────────────

    @Test
    void getCompromisedCredentials_ShouldReturnActiveList() {
        VaultEntry entry = buildEntry(10L, "Gmail");
        CompromisedCredential cred = CompromisedCredential.builder()
                .id(1L).user(user).vaultEntry(entry).pwnedCount(5000L)
                .hashPrefix("5BAA6").isResolved(false).detectedAt(LocalDateTime.now()).build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.findActiveByUserIdOrderByPwnedCountDesc(1L))
                .thenReturn(List.of(cred));

        List<CompromisedCredentialResponse> result = service.getCompromisedCredentials("testuser");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Gmail", result.get(0).getVaultEntryTitle());
        assertEquals(5000L, result.get(0).getPwnedCount());
        assertFalse(result.get(0).isResolved());
    }

    @Test
    void getCompromisedCredentials_Empty_ShouldReturnEmptyList() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.findActiveByUserIdOrderByPwnedCountDesc(1L))
                .thenReturn(Collections.emptyList());

        List<CompromisedCredentialResponse> result = service.getCompromisedCredentials("testuser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ── getBreachHistory ──────────────────────────────────────────────────────

    @Test
    void getBreachHistory_ShouldReturnHistory() {
        BreachScanRecord record = buildScanRecord(2);
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(breachScanRecordRepository.findByUserIdOrderByScannedAtDesc(1L))
                .thenReturn(List.of(record));

        BreachHistoryResponse result = service.getBreachHistory("testuser");

        assertNotNull(result);
        assertEquals(1, result.getTotalScans());
        assertEquals(2, result.getTotalCompromisedFound());
        assertEquals(1, result.getScanHistory().size());
        assertEquals("MANUAL", result.getScanHistory().get(0).getTriggerType());
    }

    @Test
    void getBreachHistory_NoHistory_ShouldReturnEmpty() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(breachScanRecordRepository.findByUserIdOrderByScannedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        BreachHistoryResponse result = service.getBreachHistory("testuser");

        assertEquals(0, result.getTotalScans());
        assertEquals(0, result.getTotalCompromisedFound());
        assertTrue(result.getScanHistory().isEmpty());
    }

    // ── resolveCredential ─────────────────────────────────────────────────────

    @Test
    void resolveCredential_ShouldMarkResolved() {
        VaultEntry entry = buildEntry(10L, "Gmail");
        CompromisedCredential cred = CompromisedCredential.builder()
                .id(1L).user(user).vaultEntry(entry).pwnedCount(5000L)
                .isResolved(false).detectedAt(LocalDateTime.now()).build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.findById(1L)).thenReturn(Optional.of(cred));
        when(compromisedCredentialRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CompromisedCredentialResponse result = service.resolveCredential("testuser", 1L);

        assertTrue(result.isResolved());
        assertNotNull(result.getResolvedAt());

        ArgumentCaptor<CompromisedCredential> captor = ArgumentCaptor.forClass(CompromisedCredential.class);
        verify(compromisedCredentialRepository).save(captor.capture());
        assertTrue(captor.getValue().isResolved());
        assertNotNull(captor.getValue().getResolvedAt());
    }

    @Test
    void resolveCredential_NotFound_ShouldThrow() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.resolveCredential("testuser", 99L));
    }

    @Test
    void resolveCredential_WrongUser_ShouldThrow() {
        User otherUser = User.builder().id(99L).username("other").masterPasswordHash("h").salt("s").build();
        VaultEntry entry = VaultEntry.builder().id(10L).user(otherUser).title("Gmail")
                .username("u").password("p").build();
        CompromisedCredential cred = CompromisedCredential.builder()
                .id(1L).user(otherUser).vaultEntry(entry).pwnedCount(100L)
                .isResolved(false).detectedAt(LocalDateTime.now()).build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(compromisedCredentialRepository.findById(1L)).thenReturn(Optional.of(cred));

        assertThrows(IllegalArgumentException.class,
                () -> service.resolveCredential("testuser", 1L));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private VaultEntry buildEntry(Long id, String title) {
        return VaultEntry.builder()
                .id(id).title(title).username("encUser").password("encPass")
                .websiteUrl("https://example.com").user(user)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .isDeleted(false).isFavorite(false).isHighlySensitive(false).build();
    }

    private BreachScanRecord buildScanRecord(int compromisedFound) {
        return BreachScanRecord.builder()
                .id(1L).user(user).triggerType(TriggerType.MANUAL)
                .entriesScanned(10).compromisedFound(compromisedFound)
                .status(BreachScanRecord.ScanStatus.COMPLETED)
                .scannedAt(LocalDateTime.now().minusHours(1)).build();
    }
}
