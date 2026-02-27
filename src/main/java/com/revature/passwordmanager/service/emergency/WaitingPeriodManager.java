package com.revature.passwordmanager.service.emergency;

import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Pure computation component — no database access.
 * Calculates waiting period deadlines and determines whether a request
 * is ready for auto-approval.</p>
 */
@Component
public class WaitingPeriodManager {

    /** Default duration an approved access token is valid (hours). */
    public static final int ACCESS_TOKEN_VALIDITY_HOURS = 48;

    /**
     * Computes when the waiting period ends for a new request.
     *
     * @param requestedAt       when the request was submitted
     * @param waitingPeriodHours the contact's configured waiting period
     * @return the timestamp when the waiting period expires
     */
    public LocalDateTime computeWaitingPeriodEnd(LocalDateTime requestedAt, int waitingPeriodHours) {
        return requestedAt.plusHours(waitingPeriodHours);
    }

    /**
     * Computes when an approved access token expires.
     *
     * @param approvedAt when the request was approved
     * @return the token expiry timestamp
     */
    public LocalDateTime computeTokenExpiry(LocalDateTime approvedAt) {
        return approvedAt.plusHours(ACCESS_TOKEN_VALIDITY_HOURS);
    }

    /**
     * Returns {@code true} if the waiting period has elapsed and the request
     * should be auto-approved.
     *
     * @param request the pending request
     * @param now     current time
     * @return {@code true} if the waiting period has ended
     */
    public boolean isWaitingPeriodElapsed(EmergencyAccessRequest request, LocalDateTime now) {
        if (request.getWaitingPeriodEndsAt() == null) {
            return false;
        }
        return !now.isBefore(request.getWaitingPeriodEndsAt());
    }

    /**
     * Returns {@code true} if an approved access token is still valid.
     *
     * @param request the approved request
     * @param now     current time
     * @return {@code true} if the token has not expired
     */
    public boolean isTokenValid(EmergencyAccessRequest request, LocalDateTime now) {
        if (request.getExpiresAt() == null) {
            return false;
        }
        return now.isBefore(request.getExpiresAt());
    }

    /**
     * Returns the number of hours remaining in the waiting period.
     * Negative if the waiting period has already elapsed.
     *
     * @param request the pending request
     * @param now     current time
     * @return hours until auto-approval (may be negative)
     */
    public long hoursUntilAutoApproval(EmergencyAccessRequest request, LocalDateTime now) {
        if (request.getWaitingPeriodEndsAt() == null) {
            return Long.MAX_VALUE;
        }
        return ChronoUnit.HOURS.between(now, request.getWaitingPeriodEndsAt());
    }

    /**
     * Returns the number of hours remaining before the access token expires.
     * Negative if the token has already expired.
     *
     * @param request the approved request
     * @param now     current time
     * @return hours until token expiry (may be negative)
     */
    public long hoursUntilTokenExpiry(EmergencyAccessRequest request, LocalDateTime now) {
        if (request.getExpiresAt() == null) {
            return 0;
        }
        return ChronoUnit.HOURS.between(now, request.getExpiresAt());
    }
}
