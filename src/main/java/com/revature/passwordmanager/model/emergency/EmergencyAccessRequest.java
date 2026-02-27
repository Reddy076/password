package com.revature.passwordmanager.model.emergency;

import com.revature.passwordmanager.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Represents a request by an emergency contact to access the vault owner's vault.
 * The lifecycle is:</p>
 * <ol>
 *   <li>Contact submits request → status = PENDING, owner is notified by email</li>
 *   <li>Owner can DENY at any time → status = DENIED</li>
 *   <li>If owner does not deny within {@code waitingPeriodHours} → scheduler sets status = APPROVED</li>
 *   <li>Contact uses {@code accessToken} to read the vault → status remains APPROVED until expiry</li>
 *   <li>After {@code expiresAt} → status = EXPIRED</li>
 * </ol>
 */
@Entity
@Table(name = "emergency_access_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The emergency contact who made this request. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id", nullable = false)
    @lombok.ToString.Exclude
    private EmergencyContact contact;

    /** The vault owner whose vault is being requested. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @lombok.ToString.Exclude
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AccessStatus status = AccessStatus.PENDING;

    @CreationTimestamp
    @Column(name = "requested_at", updatable = false)
    private LocalDateTime requestedAt;

    /** Copied from the contact's waitingPeriodHours at request time. */
    @Column(name = "waiting_period_hours", nullable = false)
    @Builder.Default
    private Integer waitingPeriodHours = 48;

    /** When the waiting period expires and access is auto-granted (if not denied). */
    @Column(name = "waiting_period_ends_at")
    private LocalDateTime waitingPeriodEndsAt;

    /** When the owner approved or denied the request (null if auto-approved by scheduler). */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** One-time access token issued when status becomes APPROVED. */
    @Column(name = "access_token", unique = true, length = 512)
    private String accessToken;

    /** When the access token expires (48 hours after approval by default). */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** Optional message from the contact explaining the reason for the request. */
    @Column(name = "request_message", length = 1000)
    private String requestMessage;

    public enum AccessStatus {
        /** Request submitted, waiting period running. */
        PENDING,
        /** Waiting period elapsed — access granted automatically. */
        APPROVED,
        /** Owner explicitly denied the request. */
        DENIED,
        /** Access token has expired. */
        EXPIRED
    }
}
