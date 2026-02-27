package com.revature.passwordmanager.service.team;

import com.revature.passwordmanager.dto.request.CreateTeamRequest;
import com.revature.passwordmanager.dto.request.InviteMemberRequest;
import com.revature.passwordmanager.dto.response.TeamMemberResponse;
import com.revature.passwordmanager.dto.response.TeamResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.team.SharedVaultEntry;
import com.revature.passwordmanager.model.team.Team;
import com.revature.passwordmanager.model.team.TeamInvitation;
import com.revature.passwordmanager.model.team.TeamInvitation.InvitationStatus;
import com.revature.passwordmanager.model.team.TeamMember;
import com.revature.passwordmanager.model.team.TeamMember.TeamRole;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.SharedVaultEntryRepository;
import com.revature.passwordmanager.repository.TeamInvitationRepository;
import com.revature.passwordmanager.repository.TeamMemberRepository;
import com.revature.passwordmanager.repository.TeamRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Handles team lifecycle, member management, vault entry sharing, and activity.</p>
 */
@Service
@RequiredArgsConstructor
public class TeamVaultService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository memberRepository;
    private final TeamInvitationRepository invitationRepository;
    private final SharedVaultEntryRepository sharedEntryRepository;
    private final VaultEntryRepository vaultEntryRepository;
    private final EmailService emailService;

    // ── Team CRUD ─────────────────────────────────────────────────────────────

    /**
     * Creates a new team. The creator automatically becomes the OWNER.
     */
    @Transactional
    public TeamResponse createTeam(String username, CreateTeamRequest request) {
        User creator = userRepository.findByUsernameOrThrow(username);

        Team team = Team.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(creator)
                .build();
        team = teamRepository.save(team);

        // Add creator as OWNER
        TeamMember ownerMember = TeamMember.builder()
                .team(team)
                .user(creator)
                .role(TeamRole.OWNER)
                .build();
        memberRepository.save(ownerMember);

        return mapToTeamResponse(team, creator, List.of(ownerMember));
    }

    /**
     * Returns all teams the authenticated user belongs to.
     */
    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        List<Team> teams = teamRepository.findTeamsByMemberId(user.getId());
        return teams.stream()
                .map(t -> {
                    List<TeamMember> members = memberRepository.findByTeamIdOrderByJoinedAtAsc(t.getId());
                    return mapToTeamResponse(t, user, members);
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns a single team by id. The user must be a member.
     */
    @Transactional(readOnly = true)
    public TeamResponse getTeam(String username, Long teamId) {
        User user = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireMembership(team, user);

        List<TeamMember> members = memberRepository.findByTeamIdOrderByJoinedAtAsc(teamId);
        return mapToTeamResponse(team, user, members);
    }

    /**
     * Deletes a team. Only the OWNER can delete.
     */
    @Transactional
    public void deleteTeam(String username, Long teamId) {
        User user = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireRole(team, user, TeamRole.OWNER);
        teamRepository.delete(team);
    }

    // ── Member management ─────────────────────────────────────────────────────

    /**
     * Returns all members of a team. The user must be a member.
     */
    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getMembers(String username, Long teamId) {
        User user = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireMembership(team, user);

        return memberRepository.findByTeamIdOrderByJoinedAtAsc(teamId)
                .stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());
    }

    /**
     * Invites a user to the team by email. Sends an invitation email.
     * Only OWNER or ADMIN can invite.
     */
    @Transactional
    public TeamMemberResponse inviteMember(String username, Long teamId, InviteMemberRequest request) {
        User inviter = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireRoleAtLeast(team, inviter, TeamRole.ADMIN);

        TeamRole role = parseRole(request.getRole(), TeamRole.MEMBER);

        // Check if already a member
        userRepository.findByEmail(request.getEmail()).ifPresent(existingUser -> {
            if (memberRepository.existsByTeamIdAndUserId(teamId, existingUser.getId())) {
                throw new IllegalArgumentException("User is already a member of this team");
            }
        });

        // Create or update invitation
        TeamInvitation invitation = invitationRepository
                .findByTeamIdAndEmail(teamId, request.getEmail())
                .orElse(TeamInvitation.builder()
                        .team(team)
                        .email(request.getEmail())
                        .invitedBy(inviter)
                        .build());

        invitation.setRole(role);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setAcceptedAt(null);
        invitation = invitationRepository.save(invitation);

        // If the user is already registered, add them directly
        User invitee = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (invitee != null) {
            TeamMember member = TeamMember.builder()
                    .team(team)
                    .user(invitee)
                    .role(role)
                    .build();
            member = memberRepository.save(member);
            invitation.setStatus(InvitationStatus.ACCEPTED);
            invitation.setAcceptedAt(LocalDateTime.now());
            invitationRepository.save(invitation);
            return mapToMemberResponse(member);
        }

        // Send invitation email
        try {
            emailService.sendSimpleEmail(
                    request.getEmail(),
                    "You've been invited to join team '" + team.getName() + "'",
                    "You've been invited to join the team '" + team.getName() + "' as " + role.name() + ". " +
                    "Register at the app and use invitation token: " + invitation.getToken());
        } catch (Exception e) {
            // Non-fatal
        }

        // Return a placeholder response for the pending invitation
        return TeamMemberResponse.builder()
                .memberId(invitation.getId())
                .email(request.getEmail())
                .role(role.name())
                .build();
    }

    /**
     * Changes a member's role. Only OWNER can change roles; OWNER role cannot be changed.
     */
    @Transactional
    public TeamMemberResponse changeMemberRole(String username, Long teamId, Long targetUserId, String newRole) {
        User requester = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireRole(team, requester, TeamRole.OWNER);

        TeamMember targetMember = memberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team"));

        if (targetMember.getRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException("Cannot change the OWNER's role");
        }
        if (targetUserId.equals(userRepository.findByUsernameOrThrow(username).getId())) {
            throw new IllegalArgumentException("Cannot change your own role");
        }

        TeamRole role = parseRole(newRole, TeamRole.MEMBER);
        if (role == TeamRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role via this endpoint");
        }

        targetMember.setRole(role);
        targetMember = memberRepository.save(targetMember);
        return mapToMemberResponse(targetMember);
    }

    /**
     * Removes a member from the team. OWNER cannot be removed.
     * OWNER or ADMIN can remove MEMBER/VIEWER. Members can remove themselves.
     */
    @Transactional
    public void removeMember(String username, Long teamId, Long targetUserId) {
        User requester = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        TeamMember targetMember = memberRepository.findByTeamIdAndUserId(teamId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in team"));

        if (targetMember.getRole() == TeamRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the team OWNER");
        }

        // Allow self-removal or OWNER/ADMIN removal
        boolean isSelf = requester.getId().equals(targetUserId);
        if (!isSelf) {
            requireRoleAtLeast(team, requester, TeamRole.ADMIN);
        }

        memberRepository.delete(targetMember);
    }

    // ── Vault sharing ─────────────────────────────────────────────────────────

    /**
     * Shares a vault entry with the team. The entry must belong to the requester.
     * Only OWNER, ADMIN, or MEMBER can share entries.
     */
    @Transactional
    public VaultEntryResponse shareEntry(String username, Long teamId, Long vaultEntryId) {
        User user = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireRoleAtLeast(team, user, TeamRole.MEMBER);

        VaultEntry entry = vaultEntryRepository.findByIdAndUserId(vaultEntryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

        if (Boolean.TRUE.equals(entry.getIsHighlySensitive())) {
            throw new IllegalArgumentException("Highly sensitive entries cannot be shared with teams");
        }

        if (sharedEntryRepository.existsByTeamIdAndVaultEntryId(teamId, vaultEntryId)) {
            throw new IllegalStateException("Entry is already shared with this team");
        }

        SharedVaultEntry shared = SharedVaultEntry.builder()
                .team(team)
                .vaultEntry(entry)
                .sharedBy(user)
                .build();
        sharedEntryRepository.save(shared);

        return mapToVaultEntryResponse(entry);
    }

    /**
     * Returns all vault entries shared with the team.
     * All team members can view shared entries.
     */
    @Transactional(readOnly = true)
    public List<VaultEntryResponse> getTeamVault(String username, Long teamId) {
        User user = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireMembership(team, user);

        return sharedEntryRepository.findByTeamIdOrderBySharedAtDesc(teamId)
                .stream()
                .map(s -> mapToVaultEntryResponse(s.getVaultEntry()))
                .collect(Collectors.toList());
    }

    /**
     * Removes a vault entry from the team vault.
     * The entry owner or OWNER/ADMIN can unshare.
     */
    @Transactional
    public void unshareEntry(String username, Long teamId, Long vaultEntryId) {
        User user = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));

        SharedVaultEntry shared = sharedEntryRepository.findByTeamIdAndVaultEntryId(teamId, vaultEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Shared entry not found"));

        boolean isEntryOwner = shared.getVaultEntry().getUser().getId().equals(user.getId());
        if (!isEntryOwner) {
            requireRoleAtLeast(team, user, TeamRole.ADMIN);
        }

        sharedEntryRepository.delete(shared);
    }

    // ── Activity ──────────────────────────────────────────────────────────────

    /**
     * Returns recent team activity (shared entries with timestamps).
     */
    @Transactional(readOnly = true)
    public List<TeamActivityItem> getTeamActivity(String username, Long teamId) {
        User user = userRepository.findByUsernameOrThrow(username);
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Team not found"));
        requireMembership(team, user);

        return sharedEntryRepository.findByTeamIdOrderBySharedAtDesc(teamId)
                .stream()
                .map(s -> new TeamActivityItem(
                        "ENTRY_SHARED",
                        s.getSharedBy().getUsername(),
                        "Shared '" + s.getVaultEntry().getTitle() + "' with the team",
                        s.getSharedAt()))
                .collect(Collectors.toList());
    }

    /** Simple activity item record. */
    public record TeamActivityItem(String action, String actorUsername, String description, LocalDateTime timestamp) {}

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void requireMembership(Team team, User user) {
        if (!memberRepository.existsByTeamIdAndUserId(team.getId(), user.getId())) {
            throw new AuthenticationException("You are not a member of this team");
        }
    }

    private void requireRole(Team team, User user, TeamRole requiredRole) {
        TeamMember member = memberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .orElseThrow(() -> new AuthenticationException("You are not a member of this team"));
        if (member.getRole() != requiredRole) {
            throw new AuthenticationException("Insufficient permissions. Required role: " + requiredRole);
        }
    }

    private void requireRoleAtLeast(Team team, User user, TeamRole minimumRole) {
        TeamMember member = memberRepository.findByTeamIdAndUserId(team.getId(), user.getId())
                .orElseThrow(() -> new AuthenticationException("You are not a member of this team"));
        if (roleOrdinal(member.getRole()) < roleOrdinal(minimumRole)) {
            throw new AuthenticationException("Insufficient permissions. Minimum required role: " + minimumRole);
        }
    }

    private int roleOrdinal(TeamRole role) {
        return switch (role) {
            case VIEWER -> 0;
            case MEMBER -> 1;
            case ADMIN -> 2;
            case OWNER -> 3;
        };
    }

    private TeamRole parseRole(String roleStr, TeamRole defaultRole) {
        if (roleStr == null || roleStr.isBlank()) return defaultRole;
        try {
            return TeamRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + roleStr + ". Valid roles: ADMIN, MEMBER, VIEWER");
        }
    }

    private TeamResponse mapToTeamResponse(Team team, User currentUser, List<TeamMember> members) {
        TeamMember currentMember = members.stream()
                .filter(m -> m.getUser().getId().equals(currentUser.getId()))
                .findFirst().orElse(null);

        List<TeamMemberResponse> memberResponses = members.stream()
                .map(this::mapToMemberResponse)
                .collect(Collectors.toList());

        long sharedCount = sharedEntryRepository.findByTeamIdOrderBySharedAtDesc(team.getId()).size();

        return TeamResponse.builder()
                .id(team.getId())
                .name(team.getName())
                .description(team.getDescription())
                .createdByUsername(team.getCreatedBy().getUsername())
                .createdAt(team.getCreatedAt())
                .memberCount(members.size())
                .sharedEntryCount((int) sharedCount)
                .currentUserRole(currentMember != null ? currentMember.getRole().name() : null)
                .members(memberResponses)
                .build();
    }

    private TeamMemberResponse mapToMemberResponse(TeamMember member) {
        return TeamMemberResponse.builder()
                .memberId(member.getId())
                .userId(member.getUser().getId())
                .username(member.getUser().getUsername())
                .email(member.getUser().getEmail())
                .role(member.getRole().name())
                .joinedAt(member.getJoinedAt())
                .build();
    }

    private VaultEntryResponse mapToVaultEntryResponse(VaultEntry entry) {
        return VaultEntryResponse.builder()
                .id(entry.getId())
                .title(entry.getTitle())
                .websiteUrl(entry.getWebsiteUrl())
                .isFavorite(entry.getIsFavorite())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }
}
