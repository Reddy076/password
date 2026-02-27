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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Feature 42 – Team/Family Vault Sharing.
 */
@ExtendWith(MockitoExtension.class)
class TeamVaultServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository memberRepository;
    @Mock private TeamInvitationRepository invitationRepository;
    @Mock private SharedVaultEntryRepository sharedEntryRepository;
    @Mock private VaultEntryRepository vaultEntryRepository;
    @Mock private EmailService emailService;

    @InjectMocks
    private TeamVaultService teamVaultService;

    private User owner;
    private User member;
    private Team team;
    private TeamMember ownerMembership;
    private TeamMember memberMembership;
    private VaultEntry vaultEntry;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@test.com")
                .masterPasswordHash("h").salt("s").build();
        member = User.builder().id(2L).username("member").email("member@test.com")
                .masterPasswordHash("h").salt("s").build();

        team = Team.builder().id(10L).name("Dev Team").description("Dev team")
                .createdBy(owner).createdAt(LocalDateTime.now()).build();

        ownerMembership = TeamMember.builder().id(100L).team(team).user(owner)
                .role(TeamRole.OWNER).joinedAt(LocalDateTime.now()).build();
        memberMembership = TeamMember.builder().id(101L).team(team).user(member)
                .role(TeamRole.MEMBER).joinedAt(LocalDateTime.now()).build();

        vaultEntry = VaultEntry.builder().id(200L).user(owner).title("GitHub")
                .username("u").password("p").isDeleted(false).isHighlySensitive(false)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    // ── createTeam ────────────────────────────────────────────────────────────

    @Test
    void createTeam_ValidRequest_ShouldCreateTeamAndAddOwner() {
        CreateTeamRequest request = CreateTeamRequest.builder()
                .name("Dev Team").description("Dev team").build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(memberRepository.save(any(TeamMember.class))).thenReturn(ownerMembership);
        when(sharedEntryRepository.findByTeamIdOrderBySharedAtDesc(10L)).thenReturn(List.of());

        TeamResponse response = teamVaultService.createTeam("owner", request);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Dev Team");
        assertThat(response.getCreatedByUsername()).isEqualTo("owner");
        assertThat(response.getMemberCount()).isEqualTo(1);
        assertThat(response.getCurrentUserRole()).isEqualTo("OWNER");
        verify(memberRepository).save(argThat(m -> m.getRole() == TeamRole.OWNER));
    }

    // ── getMyTeams ────────────────────────────────────────────────────────────

    @Test
    void getMyTeams_ShouldReturnAllTeamsUserBelongsTo() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findTeamsByMemberId(1L)).thenReturn(List.of(team));
        when(memberRepository.findByTeamIdOrderByJoinedAtAsc(10L)).thenReturn(List.of(ownerMembership));
        when(sharedEntryRepository.findByTeamIdOrderBySharedAtDesc(10L)).thenReturn(List.of());

        List<TeamResponse> responses = teamVaultService.getMyTeams("owner");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Dev Team");
    }

    // ── getTeam ───────────────────────────────────────────────────────────────

    @Test
    void getTeam_ValidMember_ShouldReturnTeamWithMembers() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.existsByTeamIdAndUserId(10L, 1L)).thenReturn(true);
        when(memberRepository.findByTeamIdOrderByJoinedAtAsc(10L))
                .thenReturn(List.of(ownerMembership, memberMembership));
        when(sharedEntryRepository.findByTeamIdOrderBySharedAtDesc(10L)).thenReturn(List.of());

        TeamResponse response = teamVaultService.getTeam("owner", 10L);

        assertThat(response.getMemberCount()).isEqualTo(2);
        assertThat(response.getMembers()).hasSize(2);
    }

    @Test
    void getTeam_NotMember_ShouldThrowAuthenticationException() {
        when(userRepository.findByUsernameOrThrow("outsider")).thenReturn(
                User.builder().id(99L).username("outsider").email("o@t.com")
                        .masterPasswordHash("h").salt("s").build());
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.existsByTeamIdAndUserId(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> teamVaultService.getTeam("outsider", 10L))
                .isInstanceOf(AuthenticationException.class);
    }

    // ── inviteMember ──────────────────────────────────────────────────────────

    @Test
    void inviteMember_RegisteredUser_ShouldAddDirectly() {
        InviteMemberRequest request = InviteMemberRequest.builder()
                .email("member@test.com").role("MEMBER").build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(memberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(false);
        when(invitationRepository.findByTeamIdAndEmail(10L, "member@test.com")).thenReturn(Optional.empty());
        when(invitationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepository.save(any(TeamMember.class))).thenReturn(memberMembership);

        TeamMemberResponse response = teamVaultService.inviteMember("owner", 10L, request);

        assertThat(response.getRole()).isEqualTo("MEMBER");
        verify(memberRepository).save(any(TeamMember.class));
    }

    @Test
    void inviteMember_AlreadyMember_ShouldThrowIllegalArgumentException() {
        InviteMemberRequest request = InviteMemberRequest.builder()
                .email("member@test.com").role("MEMBER").build();

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(memberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> teamVaultService.inviteMember("owner", 10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already a member");
    }

    // ── changeMemberRole ──────────────────────────────────────────────────────

    @Test
    void changeMemberRole_ValidChange_ShouldUpdateRole() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(memberRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(memberMembership));
        when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TeamMemberResponse response = teamVaultService.changeMemberRole("owner", 10L, 2L, "ADMIN");

        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void changeMemberRole_ChangeOwnerRole_ShouldThrowIllegalArgumentException() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> teamVaultService.changeMemberRole("owner", 10L, 1L, "MEMBER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── shareEntry ────────────────────────────────────────────────────────────

    @Test
    void shareEntry_ValidEntry_ShouldShareWithTeam() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(vaultEntryRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(vaultEntry));
        when(sharedEntryRepository.existsByTeamIdAndVaultEntryId(10L, 200L)).thenReturn(false);
        when(sharedEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VaultEntryResponse response = teamVaultService.shareEntry("owner", 10L, 200L);

        assertThat(response.getId()).isEqualTo(200L);
        assertThat(response.getTitle()).isEqualTo("GitHub");
        verify(sharedEntryRepository).save(any(SharedVaultEntry.class));
    }

    @Test
    void shareEntry_HighlySensitiveEntry_ShouldThrowIllegalArgumentException() {
        vaultEntry.setIsHighlySensitive(true);

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(vaultEntryRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(vaultEntry));

        assertThatThrownBy(() -> teamVaultService.shareEntry("owner", 10L, 200L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sensitive");
    }

    @Test
    void shareEntry_AlreadyShared_ShouldThrowIllegalStateException() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(vaultEntryRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.of(vaultEntry));
        when(sharedEntryRepository.existsByTeamIdAndVaultEntryId(10L, 200L)).thenReturn(true);

        assertThatThrownBy(() -> teamVaultService.shareEntry("owner", 10L, 200L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already shared");
    }

    // ── getTeamVault ──────────────────────────────────────────────────────────

    @Test
    void getTeamVault_ShouldReturnSharedEntries() {
        SharedVaultEntry shared = SharedVaultEntry.builder()
                .id(1L).team(team).vaultEntry(vaultEntry).sharedBy(owner)
                .sharedAt(LocalDateTime.now()).build();

        when(userRepository.findByUsernameOrThrow("member")).thenReturn(member);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.existsByTeamIdAndUserId(10L, 2L)).thenReturn(true);
        when(sharedEntryRepository.findByTeamIdOrderBySharedAtDesc(10L)).thenReturn(List.of(shared));

        List<VaultEntryResponse> responses = teamVaultService.getTeamVault("member", 10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getTitle()).isEqualTo("GitHub");
    }

    // ── removeMember ──────────────────────────────────────────────────────────

    @Test
    void removeMember_SelfRemoval_ShouldSucceed() {
        when(userRepository.findByUsernameOrThrow("member")).thenReturn(member);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 2L)).thenReturn(Optional.of(memberMembership));

        teamVaultService.removeMember("member", 10L, 2L);

        verify(memberRepository).delete(memberMembership);
    }

    @Test
    void removeMember_RemoveOwner_ShouldThrowIllegalArgumentException() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(memberRepository.findByTeamIdAndUserId(10L, 1L)).thenReturn(Optional.of(ownerMembership));

        assertThatThrownBy(() -> teamVaultService.removeMember("owner", 10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot remove the team OWNER");
    }
}
