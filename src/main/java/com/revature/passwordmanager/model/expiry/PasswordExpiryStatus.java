package com.revature.passwordmanager.model.expiry;

import com.revature.passwordmanager.model.vault.VaultEntry;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Tracks the expiry state of a single vault entry. One row per vault entry.
 * Updated whenever the vault entry's password changes or the scheduler runs.</p>
 */
@Entity
@Table(name = "password_expiry_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordExpiryStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vault_entry_id", nullable = false, unique = true)
    @lombok.ToString.Exclude
    private VaultEntry vaultEntry;

    /** Timestamp when the password was last changed (mirrors vault entry updatedAt on creation). */
    @Column(name = "last_changed_at", nullable = false)
    private LocalDateTime lastChangedAt;

    /** Computed expiry timestamp = lastChangedAt + policy.defaultExpiryDays. */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Current expiry state, recomputed by the scheduler. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExpiryState status = ExpiryState.FRESH;

    /** Whether a reminder notification has already been sent for the current expiry cycle. */
    @Column(name = "reminder_sent", nullable = false)
    @Builder.Default
    private Boolean reminderSent = false;

    /** Whether the user has snoozed the reminder (snooze resets on next password change). */
    @Column(name = "snoozed_until")
    private LocalDateTime snoozedUntil;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum ExpiryState {
        /** Password changed recently — within the first half of the expiry window. */
        FRESH,
        /** Password is aging — past half the expiry window but not yet in reminder range. */
        AGING,
        /** Password will expire within the reminder window (e.g., 7 days). */
        EXPIRING_SOON,
        /** Password has passed the expiry date. */
        EXPIRED
    }
}
