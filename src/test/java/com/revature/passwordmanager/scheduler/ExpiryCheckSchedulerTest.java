package com.revature.passwordmanager.scheduler;

import com.revature.passwordmanager.model.expiry.ExpiryPolicy;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.ExpiryPolicyRepository;
import com.revature.passwordmanager.repository.PasswordExpiryStatusRepository;
import com.revature.passwordmanager.service.expiry.ExpiryNotificationService;
import com.revature.passwordmanager.service.expiry.ExpiryPolicyEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Feature 38 – Password Expiration Tracker.
 */
@ExtendWith(MockitoExtension.class)
class ExpiryCheckSchedulerTest {

    @Mock private PasswordExpiryStatusRepository expiryStatusRepository;
    @Mock private ExpiryPolicyRepository expiryPolicyRepository;
    @Mock private ExpiryPolicyEngine policyEngine;
    @Mock private ExpiryNotificationService notificationService;

    @InjectMocks
    private ExpiryCheckScheduler scheduler;

    private User user;
    private VaultEntry entry;
    private ExpiryPolicy policy;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .masterPasswordHash("hash")
                .salt("salt")
                .build();

        entry = VaultEntry.builder()
                .id(10L)
                .user(user)
                .title("GitHub")
                .username("enc_user")
                .password("enc_pass")
                .isDeleted(false)
                .build();

        policy = ExpiryPolicy.builder()
                .id(1L)
                .user(user)
                .defaultExpiryDays(90)
                .criticalExpiryDays(180)
                .reminderDaysBefore(7)
                .enabled(true)
                .build();
    }

    @Test
    void runDailyExpiryCheck_FreshEntry_ShouldUpdateStateAndNotSendReminder() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.FRESH)
                .lastChangedAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().plusDays(80))
                .reminderSent(false)
                .build();

        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of(status));
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));
        when(policyEngine.computeExpiresAt(any(), eq(policy))).thenReturn(LocalDateTime.now().plusDays(80));
        when(policyEngine.computeState(any(), any(), eq(policy), any())).thenReturn(ExpiryState.FRESH);
        when(policyEngine.shouldSendReminder(any(), any())).thenReturn(false);
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.runDailyExpiryCheck();

        verify(expiryStatusRepository, atLeastOnce()).save(any());
        verify(notificationService, never()).sendExpiryReminder(any());
    }

    @Test
    void runDailyExpiryCheck_ExpiringSoonEntry_ShouldSendReminderAndMarkSent() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRING_SOON)
                .lastChangedAt(LocalDateTime.now().minusDays(85))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .reminderSent(false)
                .build();

        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of(status));
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));
        when(policyEngine.computeExpiresAt(any(), eq(policy))).thenReturn(LocalDateTime.now().plusDays(5));
        when(policyEngine.computeState(any(), any(), eq(policy), any())).thenReturn(ExpiryState.EXPIRING_SOON);
        when(policyEngine.shouldSendReminder(any(), any())).thenReturn(true);
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.runDailyExpiryCheck();

        verify(notificationService).sendExpiryReminder(any());
        // Should save twice: once for state update, once for marking reminderSent=true
        verify(expiryStatusRepository, atLeast(2)).save(any());
    }

    @Test
    void runDailyExpiryCheck_ExpiredEntry_ShouldSendReminderIfNotSent() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRED)
                .lastChangedAt(LocalDateTime.now().minusDays(100))
                .expiresAt(LocalDateTime.now().minusDays(10))
                .reminderSent(false)
                .build();

        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of(status));
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));
        when(policyEngine.computeExpiresAt(any(), eq(policy))).thenReturn(LocalDateTime.now().minusDays(10));
        when(policyEngine.computeState(any(), any(), eq(policy), any())).thenReturn(ExpiryState.EXPIRED);
        when(policyEngine.shouldSendReminder(any(), any())).thenReturn(true);
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.runDailyExpiryCheck();

        verify(notificationService).sendExpiryReminder(any());
    }

    @Test
    void runDailyExpiryCheck_DisabledPolicy_ShouldSkipEntry() {
        ExpiryPolicy disabledPolicy = ExpiryPolicy.builder()
                .id(1L)
                .user(user)
                .defaultExpiryDays(90)
                .criticalExpiryDays(180)
                .reminderDaysBefore(7)
                .enabled(false) // disabled
                .build();

        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRING_SOON)
                .lastChangedAt(LocalDateTime.now().minusDays(85))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .reminderSent(false)
                .build();

        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of(status));
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(disabledPolicy));

        scheduler.runDailyExpiryCheck();

        verify(notificationService, never()).sendExpiryReminder(any());
        verify(expiryStatusRepository, never()).save(any());
    }

    @Test
    void runDailyExpiryCheck_DeletedEntry_ShouldBeSkipped() {
        // findAllNonDeleted() already filters deleted entries at the DB level,
        // so this test verifies the scheduler handles an empty list correctly
        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of());

        scheduler.runDailyExpiryCheck();

        verify(expiryPolicyRepository, never()).findByUserId(any());
        verify(notificationService, never()).sendExpiryReminder(any());
    }

    @Test
    void runDailyExpiryCheck_EmptyStatusList_ShouldCompleteWithoutErrors() {
        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of());

        scheduler.runDailyExpiryCheck();

        verify(notificationService, never()).sendExpiryReminder(any());
        verify(expiryStatusRepository, never()).save(any());
    }

    @Test
    void runDailyExpiryCheck_NoPolicyFound_ShouldUseDefaultPolicy() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.FRESH)
                .lastChangedAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().plusDays(80))
                .reminderSent(false)
                .build();

        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of(status));
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.empty());
        // Default policy has enabled=true, so processing should continue
        when(policyEngine.computeExpiresAt(any(), any())).thenReturn(LocalDateTime.now().plusDays(80));
        when(policyEngine.computeState(any(), any(), any(), any())).thenReturn(ExpiryState.FRESH);
        when(policyEngine.shouldSendReminder(any(), any())).thenReturn(false);
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.runDailyExpiryCheck();

        verify(expiryStatusRepository, atLeastOnce()).save(any());
    }

    @Test
    void runDailyExpiryCheck_ExceptionInOneEntry_ShouldContinueProcessingOthers() {
        VaultEntry entry2 = VaultEntry.builder()
                .id(11L)
                .user(user)
                .title("Gmail")
                .username("u")
                .password("p")
                .isDeleted(false)
                .build();

        PasswordExpiryStatus status1 = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.FRESH)
                .lastChangedAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().plusDays(80))
                .reminderSent(false)
                .build();

        PasswordExpiryStatus status2 = PasswordExpiryStatus.builder()
                .vaultEntry(entry2)
                .status(ExpiryState.FRESH)
                .lastChangedAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().plusDays(80))
                .reminderSent(false)
                .build();

        when(expiryStatusRepository.findAllNonDeleted()).thenReturn(List.of(status1, status2));
        when(expiryPolicyRepository.findByUserId(1L))
                .thenThrow(new RuntimeException("DB error"))  // first call fails
                .thenReturn(Optional.of(policy));             // second call succeeds
        when(policyEngine.computeExpiresAt(any(), eq(policy))).thenReturn(LocalDateTime.now().plusDays(80));
        when(policyEngine.computeState(any(), any(), eq(policy), any())).thenReturn(ExpiryState.FRESH);
        when(policyEngine.shouldSendReminder(any(), any())).thenReturn(false);
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Should not throw — errors are caught per-entry
        scheduler.runDailyExpiryCheck();

        // Second entry should still be processed
        verify(expiryStatusRepository, atLeastOnce()).save(any());
    }
}
