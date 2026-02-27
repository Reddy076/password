package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.CreateTeamRequest;
import com.revature.passwordmanager.dto.request.InviteMemberRequest;
import com.revature.passwordmanager.dto.response.MessageResponse;
import com.revature.passwordmanager.dto.response.TeamMemberResponse;
import com.revature.passwordmanager.dto.response.TeamResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.service.team.TeamVaultService;
import com.revature.passwordmanager.service.team.TeamVaultService.TeamActivityItem;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Base URL: {@code /api/teams}</p>
 *
 * <p>All endpoints require authentication. Role-based access control is enforced
 * at the service layer: OWNER > ADMIN > MEMBER > VIEWER.</p>
 */
@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Team Vault", description = "Feature 42 – Team/Family Vault Sharing with RBAC")
public class TeamVaultController {

    private final TeamVaultService teamVaultService;

    // ── Team CRUD ─────────────────────────────────────────────────────────────

    @Operation(summary = "Create a new team", description = "Creates a team and makes the creator the OWNER.")
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamVaultService.createTeam(getCurrentUsername(), request));
    }

    @Operation(summary = "List all teams the authenticated user belongs to")
    @GetMapping
    public ResponseEntity<List<TeamResponse>> getMyTeams() {
        return ResponseEntity.ok(teamVaultService.getMyTeams(getCurrentUsername()));
    }

    @Operation(summary = "Get a specific team by id (user must be a member)")
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamResponse> getTeam(
            @Parameter(description = "Team id") @PathVariable Long teamId) {
        return ResponseEntity.ok(teamVaultService.getTeam(getCurrentUsername(), teamId));
    }

    @Operation(summary = "Delete a team (OWNER only)")
    @DeleteMapping("/{teamId}")
    public ResponseEntity<MessageResponse> deleteTeam(
            @Parameter(description = "Team id") @PathVariable Long teamId) {
        teamVaultService.deleteTeam(getCurrentUsername(), teamId);
        return ResponseEntity.ok(new MessageResponse("Team deleted successfully"));
    }

    // ── Member management ─────────────────────────────────────────────────────

    @Operation(summary = "List all members of a team")
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> getMembers(
            @Parameter(description = "Team id") @PathVariable Long teamId) {
        return ResponseEntity.ok(teamVaultService.getMembers(getCurrentUsername(), teamId));
    }

    @Operation(
        summary = "Invite a member to the team",
        description = "Invites a user by email. If they are already registered, they are added immediately. " +
                      "Otherwise, an invitation email is sent. Requires OWNER or ADMIN role."
    )
    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> inviteMember(
            @Parameter(description = "Team id") @PathVariable Long teamId,
            @Valid @RequestBody InviteMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamVaultService.inviteMember(getCurrentUsername(), teamId, request));
    }

    @Operation(
        summary = "Change a member's role (OWNER only)",
        description = "Changes the role of a team member. Cannot change the OWNER's role or assign OWNER role."
    )
    @PutMapping("/{teamId}/members/{userId}/role")
    public ResponseEntity<TeamMemberResponse> changeMemberRole(
            @Parameter(description = "Team id") @PathVariable Long teamId,
            @Parameter(description = "User id of the member to update") @PathVariable Long userId,
            @Parameter(description = "New role: ADMIN, MEMBER, or VIEWER") @RequestParam String role) {
        return ResponseEntity.ok(teamVaultService.changeMemberRole(getCurrentUsername(), teamId, userId, role));
    }

    @Operation(
        summary = "Remove a member from the team",
        description = "OWNER/ADMIN can remove MEMBER/VIEWER. Members can remove themselves (leave team)."
    )
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<MessageResponse> removeMember(
            @Parameter(description = "Team id") @PathVariable Long teamId,
            @Parameter(description = "User id of the member to remove") @PathVariable Long userId) {
        teamVaultService.removeMember(getCurrentUsername(), teamId, userId);
        return ResponseEntity.ok(new MessageResponse("Member removed from team"));
    }

    // ── Vault sharing ─────────────────────────────────────────────────────────

    @Operation(
        summary = "Share a vault entry with the team",
        description = "Shares one of the authenticated user's vault entries with the team. " +
                      "Highly sensitive entries cannot be shared. Requires MEMBER role or above."
    )
    @PostMapping("/{teamId}/share")
    public ResponseEntity<VaultEntryResponse> shareEntry(
            @Parameter(description = "Team id") @PathVariable Long teamId,
            @Parameter(description = "Vault entry id to share") @RequestParam Long vaultEntryId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(teamVaultService.shareEntry(getCurrentUsername(), teamId, vaultEntryId));
    }

    @Operation(summary = "Get all vault entries shared with the team")
    @GetMapping("/{teamId}/vault")
    public ResponseEntity<List<VaultEntryResponse>> getTeamVault(
            @Parameter(description = "Team id") @PathVariable Long teamId) {
        return ResponseEntity.ok(teamVaultService.getTeamVault(getCurrentUsername(), teamId));
    }

    @Operation(
        summary = "Remove a vault entry from the team vault",
        description = "The entry owner or OWNER/ADMIN can unshare an entry."
    )
    @DeleteMapping("/{teamId}/vault/{vaultEntryId}")
    public ResponseEntity<MessageResponse> unshareEntry(
            @Parameter(description = "Team id") @PathVariable Long teamId,
            @Parameter(description = "Vault entry id to unshare") @PathVariable Long vaultEntryId) {
        teamVaultService.unshareEntry(getCurrentUsername(), teamId, vaultEntryId);
        return ResponseEntity.ok(new MessageResponse("Entry removed from team vault"));
    }

    // ── Activity ──────────────────────────────────────────────────────────────

    @Operation(summary = "Get team activity log (recent sharing events)")
    @GetMapping("/{teamId}/activity")
    public ResponseEntity<List<TeamActivityItem>> getTeamActivity(
            @Parameter(description = "Team id") @PathVariable Long teamId) {
        return ResponseEntity.ok(teamVaultService.getTeamActivity(getCurrentUsername(), teamId));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
