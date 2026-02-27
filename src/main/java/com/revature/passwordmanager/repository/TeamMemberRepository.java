package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.team.TeamMember;
import com.revature.passwordmanager.model.team.TeamMember.TeamRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Feature 42 – Team/Family Vault Sharing.
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    List<TeamMember> findByTeamIdOrderByJoinedAtAsc(Long teamId);

    Optional<TeamMember> findByTeamIdAndUserId(Long teamId, Long userId);

    boolean existsByTeamIdAndUserId(Long teamId, Long userId);

    void deleteByTeamIdAndUserId(Long teamId, Long userId);

    /** Count members with a specific role in a team. */
    long countByTeamIdAndRole(Long teamId, TeamRole role);
}
