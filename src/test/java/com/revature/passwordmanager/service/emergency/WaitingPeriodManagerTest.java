package com.revature.passwordmanager.service.emergency;

import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest;
import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest.AccessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Pure unit tests for {@link WaitingPeriodManager} — no Spring context needed.</p>
 */
class WaitingPeriodManagerTest {

    private WaitingPeriodManager manager;

    @BeforeEach
    void setUp() {
        manager = new WaitingPeriodManager();
    }

    // ── computeWaitingPeriodEnd ───────────────────────────────────────────────

    @Test
    void computeWaitingPeriodEnd_ShouldAddHours() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime result = manager.computeWaitingPeriodEnd(requestedAt, 48);
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 1, 3, 10, 0));
    }

    @Test
    void computeWaitingPeriodEnd_1Hour_ShouldAdd1Hour() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime result = manager.computeWaitingPeriodEnd(requestedAt, 1);
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 1, 1, 11, 0));
    }

    // ── computeTokenExpiry ────────────────────────────────────────────────────

    @Test
    void computeTokenExpiry_ShouldAdd48Hours() {
        LocalDateTime approvedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime result = manager.computeTokenExpiry(approvedAt);
        assertThat(result).isEqualTo(LocalDateTime.of(2026, 1, 3, 10, 0));
    }

    // ── isWaitingPeriodElapsed ────────────────────────────────────────────────

    @Test
    void isWaitingPeriodElapsed_BeforeEnd_ShouldReturnFalse() {
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .status(AccessStatus.PENDING)
                .waitingPeriodEndsAt(LocalDateTime.now().plusHours(10))
                .build();

        assertThat(manager.isWaitingPeriodElapsed(request, LocalDateTime.now())).isFalse();
    }

    @Test
    void isWaitingPeriodElapsed_AfterEnd_ShouldReturnTrue() {
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .status(AccessStatus.PENDING)
                .waitingPeriodEndsAt(LocalDateTime.now().minusHours(1))
                .build();

        assertThat(manager.isWaitingPeriodElapsed(request, LocalDateTime.now())).isTrue();
    }

    @Test
    void isWaitingPeriodElapsed_NullEndsAt_ShouldReturnFalse() {
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .status(AccessStatus.PENDING)
                .waitingPeriodEndsAt(null)
                .build();

        assertThat(manager.isWaitingPeriodElapsed(request, LocalDateTime.now())).isFalse();
    }

    @Test
    void isWaitingPeriodElapsed_ExactlyAtEnd_ShouldReturnTrue() {
        LocalDateTime end = LocalDateTime.of(2026, 1, 3, 10, 0);
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .status(AccessStatus.PENDING)
                .waitingPeriodEndsAt(end)
                .build();

        assertThat(manager.isWaitingPeriodElapsed(request, end)).isTrue();
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Test
    void isTokenValid_BeforeExpiry_ShouldReturnTrue() {
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .status(AccessStatus.APPROVED)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        assertThat(manager.isTokenValid(request, LocalDateTime.now())).isTrue();
    }

    @Test
    void isTokenValid_AfterExpiry_ShouldReturnFalse() {
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .status(AccessStatus.APPROVED)
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        assertThat(manager.isTokenValid(request, LocalDateTime.now())).isFalse();
    }

    @Test
    void isTokenValid_NullExpiry_ShouldReturnFalse() {
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .status(AccessStatus.APPROVED)
                .expiresAt(null)
                .build();

        assertThat(manager.isTokenValid(request, LocalDateTime.now())).isFalse();
    }

    // ── hoursUntilAutoApproval ────────────────────────────────────────────────

    @Test
    void hoursUntilAutoApproval_FutureEnd_ShouldBePositive() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime end = base.plusHours(24);
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .waitingPeriodEndsAt(end)
                .build();

        long hours = manager.hoursUntilAutoApproval(request, base);
        assertThat(hours).isEqualTo(24L);
    }

    @Test
    void hoursUntilAutoApproval_PastEnd_ShouldBeNegative() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 3, 10, 0);
        LocalDateTime end = base.minusHours(5);
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .waitingPeriodEndsAt(end)
                .build();

        long hours = manager.hoursUntilAutoApproval(request, base);
        assertThat(hours).isEqualTo(-5L);
    }

    @Test
    void hoursUntilAutoApproval_NullEnd_ShouldReturnMaxValue() {
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .waitingPeriodEndsAt(null)
                .build();

        long hours = manager.hoursUntilAutoApproval(request, LocalDateTime.now());
        assertThat(hours).isEqualTo(Long.MAX_VALUE);
    }

    // ── hoursUntilTokenExpiry ─────────────────────────────────────────────────

    @Test
    void hoursUntilTokenExpiry_FutureExpiry_ShouldBePositive() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime expiry = base.plusHours(48);
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .expiresAt(expiry)
                .build();

        long hours = manager.hoursUntilTokenExpiry(request, base);
        assertThat(hours).isEqualTo(48L);
    }

    @Test
    void hoursUntilTokenExpiry_PastExpiry_ShouldBeNegative() {
        LocalDateTime base = LocalDateTime.of(2026, 1, 3, 10, 0);
        LocalDateTime expiry = base.minusHours(10);
        EmergencyAccessRequest request = EmergencyAccessRequest.builder()
                .expiresAt(expiry)
                .build();

        long hours = manager.hoursUntilTokenExpiry(request, base);
        assertThat(hours).isEqualTo(-10L);
    }
}
