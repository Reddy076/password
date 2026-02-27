package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.team.TeamInvitation;
import com.revature.passwordmanager.model.team.TeamInvitation.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Feature 42 – Team/Family Vault Sharing.
 */
@Repository
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {

    List<TeamInvitation> findByTeamIdOrderByCreatedAtDesc(Long teamId);

    Optional<TeamInvitation> findByToken(String token);

    Optional<TeamInvitation> findByTeamIdAndEmail(Long teamId, String email);

    List<TeamInvitation> findByEmailAndStatus(String email, InvitationStatus status);

    /** Find expired pending invitations for cleanup. */
    List<TeamInvitation> findByStatusAndExpiresAtBefore(InvitationStatus status, LocalDateTime now);
}
