package com.revature.passwordmanager.service.expiry;

import com.revature.passwordmanager.model.expiry.ExpiryReminder;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import com.revature.passwordmanager.model.notification.Notification.NotificationType;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.ExpiryReminderRepository;
import com.revature.passwordmanager.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Feature 38 – Password Expiration Tracker.
 */
@ExtendWith(MockitoExtension.class)
class ExpiryNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ExpiryReminderRepository expiryReminderRepository;

    @InjectMocks
    private ExpiryNotificationService expiryNotificationService;

    private User user;
    private VaultEntry entry;

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
                .build();
    }

    @Test
    void sendExpiryReminder_ExpiringSoon_ShouldSendNotificationAndSaveReminder() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRING_SOON)
                .lastChangedAt(LocalDateTime.now().minusDays(85))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .reminderSent(false)
                .build();

        when(expiryReminderRepository.save(any(ExpiryReminder.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        expiryNotificationService.sendExpiryReminder(status);

        // Verify notification was created with PASSWORD_EXPIRY type
        verify(notificationService).createNotification(
                eq("testuser"),
                eq(NotificationType.PASSWORD_EXPIRY),
                any(String.class),
                any(String.class));

        // Verify reminder audit record was saved
        ArgumentCaptor<ExpiryReminder> reminderCaptor = ArgumentCaptor.forClass(ExpiryReminder.class);
        verify(expiryReminderRepository).save(reminderCaptor.capture());
        ExpiryReminder savedReminder = reminderCaptor.getValue();
        assertThat(savedReminder.getUser()).isEqualTo(user);
        assertThat(savedReminder.getVaultEntry()).isEqualTo(entry);
        assertThat(savedReminder.getTriggerState()).isEqualTo(ExpiryState.EXPIRING_SOON);
    }

    @Test
    void sendExpiryReminder_Expired_ShouldSendExpiredNotification() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRED)
                .lastChangedAt(LocalDateTime.now().minusDays(100))
                .expiresAt(LocalDateTime.now().minusDays(10))
                .reminderSent(false)
                .build();

        when(expiryReminderRepository.save(any(ExpiryReminder.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        expiryNotificationService.sendExpiryReminder(status);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq("testuser"),
                eq(NotificationType.PASSWORD_EXPIRY),
                titleCaptor.capture(),
                any(String.class));

        // Title should mention "Expired" not "Expiring Soon"
        assertThat(titleCaptor.getValue()).contains("Expired");
    }

    @Test
    void sendExpiryReminder_ExpiringSoon_TitleShouldContainEntryTitle() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRING_SOON)
                .lastChangedAt(LocalDateTime.now().minusDays(85))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .reminderSent(false)
                .build();

        when(expiryReminderRepository.save(any(ExpiryReminder.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        expiryNotificationService.sendExpiryReminder(status);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).createNotification(
                eq("testuser"),
                eq(NotificationType.PASSWORD_EXPIRY),
                titleCaptor.capture(),
                any(String.class));

        assertThat(titleCaptor.getValue()).contains("GitHub");
    }

    @Test
    void sendExpiryReminder_ShouldSaveReminderWithCorrectTriggerState() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .vaultEntry(entry)
                .status(ExpiryState.EXPIRED)
                .lastChangedAt(LocalDateTime.now().minusDays(100))
                .expiresAt(LocalDateTime.now().minusDays(10))
                .reminderSent(false)
                .build();

        when(expiryReminderRepository.save(any(ExpiryReminder.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        expiryNotificationService.sendExpiryReminder(status);

        ArgumentCaptor<ExpiryReminder> captor = ArgumentCaptor.forClass(ExpiryReminder.class);
        verify(expiryReminderRepository).save(captor.capture());
        assertThat(captor.getValue().getTriggerState()).isEqualTo(ExpiryState.EXPIRED);
    }
}
