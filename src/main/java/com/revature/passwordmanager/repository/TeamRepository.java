package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.team.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Feature 42 – Team/Family Vault Sharing.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    /** All teams created by a user. */
    List<Team> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    /** All teams a user is a member of (via TeamMember). */
    @Query("SELECT t FROM Team t JOIN TeamMember m ON m.team = t WHERE m.user.id = :userId ORDER BY t.createdAt DESC")
    List<Team> findTeamsByMemberId(@Param("userId") Long userId);
}
