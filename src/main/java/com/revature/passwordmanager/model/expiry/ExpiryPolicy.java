package com.revature.passwordmanager.model.expiry;

import com.revature.passwordmanager.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Stores per-user expiration policy settings. One row per user (OneToOne).
 * Defaults mirror the proposal: 90-day standard expiry, 180-day critical,
 * 7-day advance reminder window.</p>
 */
@Entity
@Table(name = "expiry_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpiryPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @lombok.ToString.Exclude
    private User user;

    /** Number of days before a password is considered expired (default 90). */
    @Column(name = "default_expiry_days", nullable = false)
    @Builder.Default
    private Integer defaultExpiryDays = 90;

    /** Number of days before a password is considered critically old (default 180). */
    @Column(name = "critical_expiry_days", nullable = false)
    @Builder.Default
    private Integer criticalExpiryDays = 180;

    /** How many days before expiry to send a reminder notification (default 7). */
    @Column(name = "reminder_days_before", nullable = false)
    @Builder.Default
    private Integer reminderDaysBefore = 7;

    /** Whether expiry tracking is active for this user (default true). */
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
