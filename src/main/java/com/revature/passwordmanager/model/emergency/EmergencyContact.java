package com.revature.passwordmanager.model.emergency;

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
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Represents a trusted emergency contact designated by a vault owner.
 * The contact is identified by email — they do not need to be a registered user
 * to be added, but they must be a registered user to actually request access.</p>
 */
@Entity
@Table(name = "emergency_contacts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "contact_email"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The vault owner who designated this contact. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @lombok.ToString.Exclude
    private User user;

    /** Email address of the emergency contact. */
    @Column(name = "contact_email", nullable = false, length = 255)
    private String contactEmail;

    /** Display name for the contact (optional). */
    @Column(name = "contact_name", length = 255)
    private String contactName;

    /** Relationship description (e.g., "Spouse", "Parent", "Lawyer"). */
    @Column(name = "relationship", length = 100)
    private String relationship;

    /**
     * Waiting period in hours before access is automatically granted
     * if the owner does not deny the request. Default 48 hours.
     */
    @Column(name = "waiting_period_hours", nullable = false)
    @Builder.Default
    private Integer waitingPeriodHours = 48;

    /** Whether this contact has been verified (email confirmation sent). */
    @Column(name = "verified", nullable = false)
    @Builder.Default
    private Boolean verified = false;

    /** Whether this contact is currently active (not removed). */
    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
