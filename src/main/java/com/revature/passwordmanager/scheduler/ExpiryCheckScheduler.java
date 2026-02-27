package com.revature.passwordmanager.scheduler;

import com.revature.passwordmanager.model.expiry.ExpiryPolicy;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import com.revature.passwordmanager.repository.ExpiryPolicyRepository;
import com.revature.passwordmanager.repository.PasswordExpiryStatusRepository;
import com.revature.passwordmanager.service.expiry.ExpiryNotificationService;
import com.revature.passwordmanager.service.expiry.ExpiryPolicyEngine;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Daily scheduled task that:</p>
 * <ol>
 *   <li>Recomputes the {@link ExpiryState} for every active vault entry.</li>
 *   <li>Sends reminder notifications for entries that are EXPIRING_SOON or EXPIRED
 *       and haven't had a reminder sent yet.</li>
 * </ol>
 *
 * <p>Runs daily at 08:00 UTC to catch users at the start of their working day.</p>
 */
@Component
@RequiredArgsConstructor
public class ExpiryCheckScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExpiryCheckScheduler.class);

    private final PasswordExpiryStatusRepository expiryStatusRepository;
    private final ExpiryPolicyRepository expiryPolicyRepository;
    private final ExpiryPolicyEngine policyEngine;
    private final ExpiryNotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * ?") // Daily at 08:00 UTC
    @Transactional
    public void runDailyExpiryCheck() {
        logger.info("Starting daily password expiry check...");

        LocalDateTime now = LocalDateTime.now();
        int updatedCount = 0;
        int reminderCount = 0;
        int errorCount = 0;

        // Load all non-deleted expiry status records (single JPQL query, no N+1)
        List<PasswordExpiryStatus> allStatuses = expiryStatusRepository.findAllNonDeleted();

        for (PasswordExpiryStatus status : allStatuses) {
            try {
                // Look up the user's policy (or use defaults if none set)
                ExpiryPolicy policy = expiryPolicyRepository
                        .findByUserId(status.getVaultEntry().getUser().getId())
                        .orElse(ExpiryPolicy.builder()
                                .defaultExpiryDays(90)
                                .criticalExpiryDays(180)
                                .reminderDaysBefore(7)
                                .enabled(true)
                                .build());

                // Skip if expiry tracking is disabled for this user
                if (!Boolean.TRUE.equals(policy.getEnabled())) {
                    continue;
                }

                // Recompute expiry date and state
                LocalDateTime expiresAt = policyEngine.computeExpiresAt(status.getLastChangedAt(), policy);
                ExpiryState newState = policyEngine.computeState(
                        status.getLastChangedAt(), expiresAt, policy, now);

                boolean stateChanged = newState != status.getStatus();
                status.setExpiresAt(expiresAt);
                status.setStatus(newState);

                // Reset reminder flag when state improves (e.g., after password change)
                if (newState == ExpiryState.FRESH || newState == ExpiryState.AGING) {
                    status.setReminderSent(false);
                }

                expiryStatusRepository.save(status);
                updatedCount++;

                // Send reminder if due
                if (policyEngine.shouldSendReminder(status, now)) {
                    notificationService.sendExpiryReminder(status);
                    status.setReminderSent(true);
                    expiryStatusRepository.save(status);
                    reminderCount++;
                }

            } catch (Exception e) {
                errorCount++;
                logger.error("Expiry check failed for vault entry id={}: {}",
                        status.getVaultEntry().getId(), e.getMessage());
            }
        }

        logger.info("Daily expiry check complete. updated={} reminders={} errors={} total={}",
                updatedCount, reminderCount, errorCount, allStatuses.size());
    }
}
