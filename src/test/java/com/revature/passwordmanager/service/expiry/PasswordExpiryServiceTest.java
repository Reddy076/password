package com.revature.passwordmanager.service.expiry;

import com.revature.passwordmanager.dto.request.ExpiryPolicyRequest;
import com.revature.passwordmanager.dto.response.ExpiryPolicyResponse;
import com.revature.passwordmanager.dto.response.ExpiryStatusResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Feature 38 – Password Expiration Tracker.
 */
@ExtendWith(MockitoExtension.class)
class PasswordExpiryServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private VaultEntryRepository vaultEntryRepository;
    @Mock private ExpiryPolicyRepository expiryPolicyRepository;
    @Mock private PasswordExpiryStatusRepository expiryStatusRepository;
    @Mock private ExpiryPolicyEngine policyEngine;

    @InjectMocks
    private PasswordExpiryService expiryService;

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

    // ── getPolicy ─────────────────────────────────────────────────────────────

    @Test
    void getPolicy_ExistingPolicy_ShouldReturnMappedResponse() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));

        ExpiryPolicyResponse response = expiryService.getPolicy("testuser");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getDefaultExpiryDays()).isEqualTo(90);
        assertThat(response.getCriticalExpiryDays()).isEqualTo(180);
        assertThat(response.getReminderDaysBefore()).isEqualTo(7);
        assertThat(response.getEnabled()).isTrue();
    }

    @Test
    void getPolicy_NoPolicyExists_ShouldCreateDefaultAndReturn() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(expiryPolicyRepository.save(any(ExpiryPolicy.class))).thenAnswer(inv -> {
            ExpiryPolicy p = inv.getArgument(0);
            p = ExpiryPolicy.builder()
                    .id(99L)
                    .user(user)
                    .defaultExpiryDays(p.getDefaultExpiryDays())
                    .criticalExpiryDays(p.getCriticalExpiryDays())
                    .reminderDaysBefore(p.getReminderDaysBefore())
                    .enabled(p.getEnabled())
                    .build();
            return p;
        });

        ExpiryPolicyResponse response = expiryService.getPolicy("testuser");

        assertThat(response.getDefaultExpiryDays()).isEqualTo(90);
        assertThat(response.getReminderDaysBefore()).isEqualTo(7);
        assertThat(response.getEnabled()).isTrue();
        verify(expiryPolicyRepository).save(any(ExpiryPolicy.class));
    }

    // ── updatePolicy ──────────────────────────────────────────────────────────

    @Test
    void updatePolicy_AllFields_ShouldUpdateAndRecompute() {
        ExpiryPolicyRequest request = ExpiryPolicyRequest.builder()
                .defaultExpiryDays(60)
                .criticalExpiryDays(120)
                .reminderDaysBefore(5)
                .enabled(false)
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));
        when(expiryPolicyRepository.save(any(ExpiryPolicy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expiryStatusRepository.findAllByUserId(1L)).thenReturn(List.of());

        ExpiryPolicyResponse response = expiryService.updatePolicy("testuser", request);

        assertThat(response.getDefaultExpiryDays()).isEqualTo(60);
        assertThat(response.getCriticalExpiryDays()).isEqualTo(120);
        assertThat(response.getReminderDaysBefore()).isEqualTo(5);
        assertThat(response.getEnabled()).isFalse();
    }

    @Test
    void updatePolicy_NullFields_ShouldNotOverwriteExistingValues() {
        ExpiryPolicyRequest request = ExpiryPolicyRequest.builder()
                .defaultExpiryDays(60)
                // criticalExpiryDays, reminderDaysBefore, enabled are null
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));
        when(expiryPolicyRepository.save(any(ExpiryPolicy.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expiryStatusRepository.findAllByUserId(1L)).thenReturn(List.of());

        ExpiryPolicyResponse response = expiryService.updatePolicy("testuser", request);

        assertThat(response.getDefaultExpiryDays()).isEqualTo(60);
        assertThat(response.getCriticalExpiryDays()).isEqualTo(180); // unchanged
        assertThat(response.getReminderDaysBefore()).isEqualTo(7);   // unchanged
        assertThat(response.getEnabled()).isTrue();                   // unchanged
    }

    // ── getAllStatuses ────────────────────────────────────────────────────────

    @Test
    void getAllStatuses_WithEntries_ShouldReturnCorrectCounts() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(5);

        PasswordExpiryStatus freshStatus = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.FRESH)
                .lastChangedAt(now.minusDays(10))
                .expiresAt(now.plusDays(80))
                .reminderSent(false)
                .build();

        PasswordExpiryStatus expiringSoonStatus = PasswordExpiryStatus.builder()
                .vaultEntry(VaultEntry.builder().id(11L).user(user).title("Gmail")
                        .username("u").password("p").isDeleted(false).build())
                .status(ExpiryState.EXPIRING_SOON)
                .lastChangedAt(now.minusDays(85))
                .expiresAt(expiresAt)
                .reminderSent(false)
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryStatusRepository.findAllByUserId(1L))
                .thenReturn(List.of(freshStatus, expiringSoonStatus));
        when(policyEngine.daysUntilExpiry(any(), any())).thenReturn(80L, 5L);

        ExpiryStatusResponse response = expiryService.getAllStatuses("testuser");

        assertThat(response.getTotalEntries()).isEqualTo(2);
        assertThat(response.getFreshCount()).isEqualTo(1);
        assertThat(response.getExpiringSoonCount()).isEqualTo(1);
        assertThat(response.getExpiredCount()).isEqualTo(0);
        assertThat(response.getEntries()).hasSize(2);
    }

    @Test
    void getAllStatuses_EmptyVault_ShouldReturnZeroCounts() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryStatusRepository.findAllByUserId(1L)).thenReturn(List.of());

        ExpiryStatusResponse response = expiryService.getAllStatuses("testuser");

        assertThat(response.getTotalEntries()).isEqualTo(0);
        assertThat(response.getEntries()).isEmpty();
    }

    // ── getExpiringSoon ───────────────────────────────────────────────────────

    @Test
    void getExpiringSoon_DefaultDays_ShouldUse7DayWindow() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryStatusRepository.findExpiringSoon(eq(1L), any(), any())).thenReturn(List.of());

        expiryService.getExpiringSoon("testuser", null);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(expiryStatusRepository).findExpiringSoon(eq(1L), nowCaptor.capture(), cutoffCaptor.capture());

        // cutoff should be ~7 days from now
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                nowCaptor.getValue(), cutoffCaptor.getValue());
        assertThat(daysBetween).isEqualTo(7L);
    }

    @Test
    void getExpiringSoon_CustomDays_ShouldUseProvidedWindow() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(expiryStatusRepository.findExpiringSoon(eq(1L), any(), any())).thenReturn(List.of());

        expiryService.getExpiringSoon("testuser", 30);

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(expiryStatusRepository).findExpiringSoon(eq(1L), nowCaptor.capture(), cutoffCaptor.capture());

        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
                nowCaptor.getValue(), cutoffCaptor.getValue());
        assertThat(daysBetween).isEqualTo(30L);
    }

    // ── snoozeReminder ────────────────────────────────────────────────────────

    @Test
    void snoozeReminder_ValidEntry_ShouldSetSnoozedUntil() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRING_SOON)
                .lastChangedAt(LocalDateTime.now().minusDays(85))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .reminderSent(false)
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entry));
        when(expiryStatusRepository.findByVaultEntryId(10L)).thenReturn(Optional.of(status));
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(policyEngine.daysUntilExpiry(any(), any())).thenReturn(5L);

        expiryService.snoozeReminder("testuser", 10L, 7);

        ArgumentCaptor<PasswordExpiryStatus> captor = ArgumentCaptor.forClass(PasswordExpiryStatus.class);
        verify(expiryStatusRepository).save(captor.capture());
        assertThat(captor.getValue().getSnoozedUntil()).isNotNull();
        assertThat(captor.getValue().getSnoozedUntil()).isAfter(LocalDateTime.now());
    }

    @Test
    void snoozeReminder_EntryNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expiryService.snoozeReminder("testuser", 99L, 7))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void snoozeReminder_StatusNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(entry));
        when(expiryStatusRepository.findByVaultEntryId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expiryService.snoozeReminder("testuser", 10L, 7))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── onPasswordChanged ─────────────────────────────────────────────────────

    @Test
    void onPasswordChanged_NewEntry_ShouldCreateExpiryStatus() {
        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));
        when(expiryStatusRepository.findByVaultEntryId(10L)).thenReturn(Optional.empty());
        when(policyEngine.computeExpiresAt(any(), eq(policy))).thenReturn(LocalDateTime.now().plusDays(90));
        when(policyEngine.computeState(any(), any(), eq(policy), any())).thenReturn(ExpiryState.FRESH);
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        expiryService.onPasswordChanged(entry);

        ArgumentCaptor<PasswordExpiryStatus> captor = ArgumentCaptor.forClass(PasswordExpiryStatus.class);
        verify(expiryStatusRepository).save(captor.capture());
        PasswordExpiryStatus saved = captor.getValue();
        assertThat(saved.getVaultEntry()).isEqualTo(entry);
        assertThat(saved.getStatus()).isEqualTo(ExpiryState.FRESH);
        assertThat(saved.getReminderSent()).isFalse();
        assertThat(saved.getSnoozedUntil()).isNull();
    }

    @Test
    void onPasswordChanged_ExistingEntry_ShouldResetExpiryStatus() {
        PasswordExpiryStatus existingStatus = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRED)
                .lastChangedAt(LocalDateTime.now().minusDays(100))
                .expiresAt(LocalDateTime.now().minusDays(10))
                .reminderSent(true)
                .snoozedUntil(LocalDateTime.now().plusDays(3))
                .build();

        when(expiryPolicyRepository.findByUserId(1L)).thenReturn(Optional.of(policy));
        when(expiryStatusRepository.findByVaultEntryId(10L)).thenReturn(Optional.of(existingStatus));
        when(policyEngine.computeExpiresAt(any(), eq(policy))).thenReturn(LocalDateTime.now().plusDays(90));
        when(policyEngine.computeState(any(), any(), eq(policy), any())).thenReturn(ExpiryState.FRESH);
        when(expiryStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        expiryService.onPasswordChanged(entry);

        ArgumentCaptor<PasswordExpiryStatus> captor = ArgumentCaptor.forClass(PasswordExpiryStatus.class);
        verify(expiryStatusRepository).save(captor.capture());
        PasswordExpiryStatus saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ExpiryState.FRESH);
        assertThat(saved.getReminderSent()).isFalse();
        assertThat(saved.getSnoozedUntil()).isNull(); // snooze cleared
    }

    // ── onEntryDeleted ────────────────────────────────────────────────────────

    @Test
    void onEntryDeleted_ShouldDeleteExpiryStatusRecord() {
        expiryService.onEntryDeleted(10L);

        verify(expiryStatusRepository).deleteByVaultEntryId(10L);
    }
}
