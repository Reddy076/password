package com.revature.passwordmanager.model.team;

import com.revature.passwordmanager.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Represents an email invitation to join a team. The invitee does not need
 * to be a registered user at the time of invitation. When they register and
 * accept, they become a team member with the specified role.</p>
 */
@Entity
@Table(name = "team_invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    @lombok.ToString.Exclude
    private Team team;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    @lombok.ToString.Exclude
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private TeamMember.TeamRole role = TeamMember.TeamRole.MEMBER;

    /** Unique token sent in the invitation email. */
    @Column(name = "token", unique = true, length = 255)
    private String token;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum InvitationStatus {
        PENDING, ACCEPTED, DECLINED, EXPIRED
    }
}
