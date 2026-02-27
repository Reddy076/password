package com.revature.passwordmanager.service.expiry;

import com.revature.passwordmanager.model.expiry.ExpiryReminder;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import com.revature.passwordmanager.model.notification.Notification.NotificationType;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.ExpiryReminderRepository;
import com.revature.passwordmanager.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Sends in-app notifications for expiring/expired passwords and records
 * an {@link ExpiryReminder} audit entry for each dispatch.</p>
 */
@Service
@RequiredArgsConstructor
public class ExpiryNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(ExpiryNotificationService.class);

    private final NotificationService notificationService;
    private final ExpiryReminderRepository expiryReminderRepository;

    /**
     * Sends an in-app notification for a password that is expiring soon or has expired,
     * and records the reminder in the audit table.
     *
     * @param status the expiry status record for the vault entry
     */
    @Transactional
    public void sendExpiryReminder(PasswordExpiryStatus status) {
        VaultEntry entry = status.getVaultEntry();
        User user = entry.getUser();
        ExpiryState state = status.getStatus();

        String title;
        String message;

        if (state == ExpiryState.EXPIRED) {
            title = "Password Expired: " + entry.getTitle();
            message = String.format(
                    "The password for \"%s\" expired %d day(s) ago. Please update it immediately.",
                    entry.getTitle(),
                    Math.abs(ChronoUnit.DAYS.between(LocalDateTime.now(), status.getExpiresAt())));
        } else {
            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), status.getExpiresAt());
            title = "Password Expiring Soon: " + entry.getTitle();
            message = String.format(
                    "The password for \"%s\" will expire in %d day(s). Consider updating it now.",
                    entry.getTitle(), daysLeft);
        }

        notificationService.createNotification(user.getUsername(), NotificationType.PASSWORD_EXPIRY, title, message);

        // Record the reminder audit entry
        ExpiryReminder reminder = ExpiryReminder.builder()
                .user(user)
                .vaultEntry(entry)
                .triggerState(state)
                .build();
        expiryReminderRepository.save(reminder);

        logger.info("Expiry reminder sent for user={} entry='{}' state={}",
                user.getUsername(), entry.getTitle(), state);
    }
}
