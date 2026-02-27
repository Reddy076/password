package com.revature.passwordmanager.scheduler;

import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest;
import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest.AccessStatus;
import com.revature.passwordmanager.repository.EmergencyAccessRequestRepository;
import com.revature.passwordmanager.service.emergency.EmergencyAccessService;
import com.revature.passwordmanager.service.emergency.WaitingPeriodManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Hourly scheduled task that:</p>
 * <ol>
 *   <li>Auto-approves PENDING requests whose waiting period has elapsed.</li>
 *   <li>Marks APPROVED requests as EXPIRED when their access token has expired.</li>
 * </ol>
 *
 * <p>Runs every hour at :00 to ensure timely processing.</p>
 */
@Component
@RequiredArgsConstructor
public class EmergencyAccessScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyAccessScheduler.class);

    private final EmergencyAccessRequestRepository requestRepository;
    private final EmergencyAccessService accessService;
    private final WaitingPeriodManager waitingPeriodManager;

    @Scheduled(cron = "0 0 * * * ?") // Every hour at :00
    @Transactional
    public void processEmergencyAccessRequests() {
        logger.info("Running emergency access scheduler...");
        LocalDateTime now = LocalDateTime.now();

        int approvedCount = 0;
        int expiredCount = 0;
        int errorCount = 0;

        // 1. Auto-approve pending requests whose waiting period has elapsed
        List<EmergencyAccessRequest> pendingReady =
                requestRepository.findPendingRequestsReadyForApproval(now);

        for (EmergencyAccessRequest request : pendingReady) {
            try {
                accessService.approveRequest(request);
                approvedCount++;
                logger.info("Auto-approved emergency access request id={} for user={}",
                        request.getId(), request.getUser().getUsername());
            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to auto-approve emergency access request id={}: {}",
                        request.getId(), e.getMessage());
            }
        }

        // 2. Expire approved requests whose access token has expired
        List<EmergencyAccessRequest> approvedExpired =
                requestRepository.findApprovedRequestsReadyForExpiry(now);

        for (EmergencyAccessRequest request : approvedExpired) {
            try {
                request.setStatus(AccessStatus.EXPIRED);
                requestRepository.save(request);
                expiredCount++;
                logger.info("Expired emergency access token for request id={}", request.getId());
            } catch (Exception e) {
                errorCount++;
                logger.error("Failed to expire emergency access request id={}: {}",
                        request.getId(), e.getMessage());
            }
        }

        logger.info("Emergency access scheduler complete. approved={} expired={} errors={}",
                approvedCount, expiredCount, errorCount);
    }
}
