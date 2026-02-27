package com.revature.passwordmanager.service.expiry;

import com.revature.passwordmanager.model.expiry.ExpiryPolicy;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Pure computation component — no database access. Calculates expiry dates
 * and determines the {@link ExpiryState} for a vault entry given a policy.</p>
 */
@Component
public class ExpiryPolicyEngine {

    /**
     * Computes the expiry timestamp for a password last changed at {@code lastChangedAt}
     * using the given policy's {@code defaultExpiryDays}.
     *
     * @param lastChangedAt when the password was last changed
     * @param policy        the user's expiry policy
     * @return computed expiry timestamp
     */
    public LocalDateTime computeExpiresAt(LocalDateTime lastChangedAt, ExpiryPolicy policy) {
        return lastChangedAt.plusDays(policy.getDefaultExpiryDays());
    }

    /**
     * Determines the {@link ExpiryState} for a vault entry.
     *
     * <p>State transitions (relative to {@code now}):</p>
     * <ul>
     *   <li>EXPIRED — expiresAt is in the past</li>
     *   <li>EXPIRING_SOON — within {@code reminderDaysBefore} days of expiry</li>
     *   <li>AGING — past the halfway point of the expiry window</li>
     *   <li>FRESH — within the first half of the expiry window</li>
     * </ul>
     *
     * @param lastChangedAt when the password was last changed
     * @param expiresAt     computed expiry timestamp
     * @param policy        the user's expiry policy
     * @param now           current time (injected for testability)
     * @return the computed {@link ExpiryState}
     */
    public ExpiryState computeState(LocalDateTime lastChangedAt,
                                    LocalDateTime expiresAt,
                                    ExpiryPolicy policy,
                                    LocalDateTime now) {
        if (now.isAfter(expiresAt) || now.isEqual(expiresAt)) {
            return ExpiryState.EXPIRED;
        }

        long daysUntilExpiry = ChronoUnit.DAYS.between(now, expiresAt);
        if (daysUntilExpiry <= policy.getReminderDaysBefore()) {
            return ExpiryState.EXPIRING_SOON;
        }

        long totalDays = policy.getDefaultExpiryDays();
        long daysElapsed = ChronoUnit.DAYS.between(lastChangedAt, now);
        if (daysElapsed > totalDays / 2) {
            return ExpiryState.AGING;
        }

        return ExpiryState.FRESH;
    }

    /**
     * Returns the number of days remaining until expiry (negative if already expired).
     *
     * @param expiresAt computed expiry timestamp
     * @param now       current time
     * @return days until expiry (may be negative)
     */
    public long daysUntilExpiry(LocalDateTime expiresAt, LocalDateTime now) {
        return ChronoUnit.DAYS.between(now, expiresAt);
    }

    /**
     * Returns {@code true} if a reminder should be sent for this entry.
     * A reminder is due when:
     * <ul>
     *   <li>The state is EXPIRING_SOON or EXPIRED</li>
     *   <li>No reminder has been sent yet ({@code reminderSent == false})</li>
     *   <li>The entry is not currently snoozed</li>
     * </ul>
     *
     * @param status the current expiry status record
     * @param now    current time
     * @return {@code true} if a reminder should be dispatched
     */
    public boolean shouldSendReminder(PasswordExpiryStatus status, LocalDateTime now) {
        if (Boolean.TRUE.equals(status.getReminderSent())) {
            return false;
        }
        if (status.getSnoozedUntil() != null && status.getSnoozedUntil().isAfter(now)) {
            return false;
        }
        ExpiryState state = status.getStatus();
        return state == ExpiryState.EXPIRING_SOON || state == ExpiryState.EXPIRED;
    }
}
