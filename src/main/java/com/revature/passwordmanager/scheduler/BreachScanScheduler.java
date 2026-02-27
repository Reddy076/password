package com.revature.passwordmanager.scheduler;

import com.revature.passwordmanager.model.security.breach.BreachScanRecord.TriggerType;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.service.security.breach.BreachMonitorService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Runs a nightly automated breach scan for all active users.
 * Fires daily at 02:00 UTC to avoid peak load hours.
 */
@Component
@RequiredArgsConstructor
public class BreachScanScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BreachScanScheduler.class);

    private final BreachMonitorService breachMonitorService;
    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyBreachScan() {
        logger.info("Starting scheduled daily breach scan for all users...");

        // Gap 6 fix: only scan active users — exclude accounts pending deletion
        // to avoid key-derivation failures on partially-deleted users and wasted
        // HIBP API quota.
        List<User> users = userRepository.findByDeletionScheduledAtIsNull();
        int successCount = 0;
        int failCount = 0;

        for (User user : users) {
            try {
                breachMonitorService.runScan(user.getUsername(), TriggerType.SCHEDULED);
                successCount++;
            } catch (Exception e) {
                failCount++;
                logger.error("Scheduled breach scan failed for user {}: {}",
                        user.getUsername(), e.getMessage());
            }
        }

        logger.info("Daily breach scan complete. success={} failed={} total={}",
                successCount, failCount, users.size());
    }
}
