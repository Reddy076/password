package com.revature.passwordmanager.service.expiry;

import com.revature.passwordmanager.model.expiry.ExpiryPolicy;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus;
import com.revature.passwordmanager.model.expiry.PasswordExpiryStatus.ExpiryState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Pure unit tests for {@link ExpiryPolicyEngine} — no Spring context needed.</p>
 */
class ExpiryPolicyEngineTest {

    private ExpiryPolicyEngine engine;
    private ExpiryPolicy defaultPolicy;

    @BeforeEach
    void setUp() {
        engine = new ExpiryPolicyEngine();
        defaultPolicy = ExpiryPolicy.builder()
                .defaultExpiryDays(90)
                .criticalExpiryDays(180)
                .reminderDaysBefore(7)
                .enabled(true)
                .build();
    }

    // ── computeExpiresAt ──────────────────────────────────────────────────────

    @Test
    void computeExpiresAt_ShouldAddDefaultExpiryDays() {
        LocalDateTime lastChanged = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime expected = LocalDateTime.of(2026, 4, 1, 0, 0); // +90 days

        LocalDateTime result = engine.computeExpiresAt(lastChanged, defaultPolicy);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void computeExpiresAt_CustomPolicy_ShouldUseCustomDays() {
        ExpiryPolicy policy = ExpiryPolicy.builder()
                .defaultExpiryDays(30)
                .criticalExpiryDays(60)
                .reminderDaysBefore(3)
                .enabled(true)
                .build();
        LocalDateTime lastChanged = LocalDateTime.of(2026, 1, 1, 0, 0);

        LocalDateTime result = engine.computeExpiresAt(lastChanged, policy);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 1, 31, 0, 0));
    }

    // ── computeState ─────────────────────────────────────────────────────────

    @Test
    void computeState_JustChanged_ShouldBeFresh() {
        LocalDateTime lastChanged = LocalDateTime.now();
        LocalDateTime expiresAt = engine.computeExpiresAt(lastChanged, defaultPolicy);

        ExpiryState state = engine.computeState(lastChanged, expiresAt, defaultPolicy, LocalDateTime.now());

        assertThat(state).isEqualTo(ExpiryState.FRESH);
    }

    @Test
    void computeState_PastHalfway_ShouldBeAging() {
        LocalDateTime lastChanged = LocalDateTime.now().minusDays(50); // past 45-day halfway
        LocalDateTime expiresAt = engine.computeExpiresAt(lastChanged, defaultPolicy);
        LocalDateTime now = LocalDateTime.now();

        ExpiryState state = engine.computeState(lastChanged, expiresAt, defaultPolicy, now);

        assertThat(state).isEqualTo(ExpiryState.AGING);
    }

    @Test
    void computeState_WithinReminderWindow_ShouldBeExpiringSoon() {
        LocalDateTime lastChanged = LocalDateTime.now().minusDays(85); // 5 days left (< 7-day reminder)
        LocalDateTime expiresAt = engine.computeExpiresAt(lastChanged, defaultPolicy);
        LocalDateTime now = LocalDateTime.now();

        ExpiryState state = engine.computeState(lastChanged, expiresAt, defaultPolicy, now);

        assertThat(state).isEqualTo(ExpiryState.EXPIRING_SOON);
    }

    @Test
    void computeState_PastExpiry_ShouldBeExpired() {
        LocalDateTime lastChanged = LocalDateTime.now().minusDays(100); // 10 days past expiry
        LocalDateTime expiresAt = engine.computeExpiresAt(lastChanged, defaultPolicy);
        LocalDateTime now = LocalDateTime.now();

        ExpiryState state = engine.computeState(lastChanged, expiresAt, defaultPolicy, now);

        assertThat(state).isEqualTo(ExpiryState.EXPIRED);
    }

    @Test
    void computeState_ExactlyAtExpiry_ShouldBeExpired() {
        LocalDateTime lastChanged = LocalDateTime.now().minusDays(90);
        LocalDateTime expiresAt = engine.computeExpiresAt(lastChanged, defaultPolicy);
        // now == expiresAt
        ExpiryState state = engine.computeState(lastChanged, expiresAt, defaultPolicy, expiresAt);

        assertThat(state).isEqualTo(ExpiryState.EXPIRED);
    }

    @Test
    void computeState_ExactlyAtReminderBoundary_ShouldBeExpiringSoon() {
        // 7 days left = exactly at reminder boundary
        LocalDateTime lastChanged = LocalDateTime.now().minusDays(83);
        LocalDateTime expiresAt = engine.computeExpiresAt(lastChanged, defaultPolicy);
        LocalDateTime now = LocalDateTime.now();

        ExpiryState state = engine.computeState(lastChanged, expiresAt, defaultPolicy, now);

        assertThat(state).isEqualTo(ExpiryState.EXPIRING_SOON);
    }

    // ── daysUntilExpiry ───────────────────────────────────────────────────────

    @Test
    void daysUntilExpiry_FutureExpiry_ShouldBePositive() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
        LocalDateTime expiresAt = base.plusDays(10);
        long days = engine.daysUntilExpiry(expiresAt, base);

        assertThat(days).isEqualTo(10L);
    }

    @Test
    void daysUntilExpiry_PastExpiry_ShouldBeNegative() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 11, 12, 0, 0);
        LocalDateTime expiresAt = base.minusDays(5);
        long days = engine.daysUntilExpiry(expiresAt, base);

        assertThat(days).isEqualTo(-5L);
    }

    @Test
    void daysUntilExpiry_SameDay_ShouldBeZero() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 12, 0, 0);
        long days = engine.daysUntilExpiry(now, now);

        assertThat(days).isEqualTo(0L);
    }

    // ── shouldSendReminder ────────────────────────────────────────────────────

    @Test
    void shouldSendReminder_ExpiringSoonNoReminderSent_ShouldReturnTrue() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .status(ExpiryState.EXPIRING_SOON)
                .reminderSent(false)
                .build();

        assertThat(engine.shouldSendReminder(status, LocalDateTime.now())).isTrue();
    }

    @Test
    void shouldSendReminder_ExpiredNoReminderSent_ShouldReturnTrue() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .status(ExpiryState.EXPIRED)
                .reminderSent(false)
                .build();

        assertThat(engine.shouldSendReminder(status, LocalDateTime.now())).isTrue();
    }

    @Test
    void shouldSendReminder_ReminderAlreadySent_ShouldReturnFalse() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .status(ExpiryState.EXPIRING_SOON)
                .reminderSent(true)
                .build();

        assertThat(engine.shouldSendReminder(status, LocalDateTime.now())).isFalse();
    }

    @Test
    void shouldSendReminder_FreshState_ShouldReturnFalse() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .status(ExpiryState.FRESH)
                .reminderSent(false)
                .build();

        assertThat(engine.shouldSendReminder(status, LocalDateTime.now())).isFalse();
    }

    @Test
    void shouldSendReminder_AgingState_ShouldReturnFalse() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .status(ExpiryState.AGING)
                .reminderSent(false)
                .build();

        assertThat(engine.shouldSendReminder(status, LocalDateTime.now())).isFalse();
    }

    @Test
    void shouldSendReminder_SnoozedUntilFuture_ShouldReturnFalse() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .status(ExpiryState.EXPIRING_SOON)
                .reminderSent(false)
                .snoozedUntil(LocalDateTime.now().plusDays(3))
                .build();

        assertThat(engine.shouldSendReminder(status, LocalDateTime.now())).isFalse();
    }

    @Test
    void shouldSendReminder_SnoozedUntilPast_ShouldReturnTrue() {
        PasswordExpiryStatus status = PasswordExpiryStatus.builder()
                .status(ExpiryState.EXPIRING_SOON)
                .reminderSent(false)
                .snoozedUntil(LocalDateTime.now().minusDays(1))
                .build();

        assertThat(engine.shouldSendReminder(status, LocalDateTime.now())).isTrue();
    }
}
