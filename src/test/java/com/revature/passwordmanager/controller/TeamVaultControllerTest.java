package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.CreateTeamRequest;
import com.revature.passwordmanager.dto.request.InviteMemberRequest;
import com.revature.passwordmanager.dto.response.TeamMemberResponse;
import com.revature.passwordmanager.dto.response.TeamResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.security.RateLimitService;
import com.revature.passwordmanager.service.team.TeamVaultService;
import com.revature.passwordmanager.service.team.TeamVaultService.TeamActivityItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Feature 42 – Team/Family Vault Sharing.
 *
 * <p>Full controller-layer tests for {@link TeamVaultController}.
 * Each test verifies both the HTTP status and the JSON response body fields.</p>
 */
@WebMvcTest(controllers = TeamVaultController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class TeamVaultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TeamVaultService teamVaultService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RateLimitService rateLimitService;

    private TeamResponse teamResponse;
    private TeamMemberResponse ownerMemberResponse;
    private TeamMemberResponse memberResponse;

    @BeforeEach
    void setUp() {
        Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(100);

        ownerMemberResponse = TeamMemberResponse.builder()
                .memberId(100L).userId(1L).username("owner").email("owner@test.com")
                .role("OWNER").joinedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        memberResponse = TeamMemberResponse.builder()
                .memberId(101L).userId(2L).username("member").email("member@test.com")
                .role("MEMBER").joinedAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();

        teamResponse = TeamResponse.builder()
                .id(10L).name("Dev Team").description("Development team")
                .createdByUsername("owner").createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .memberCount(2).sharedEntryCount(1).currentUserRole("OWNER")
                .members(List.of(ownerMemberResponse, memberResponse))
                .build();
    }

    // ── POST /api/teams ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void createTeam_ValidRequest_ShouldReturn201WithTeamDetails() throws Exception {
        CreateTeamRequest request = CreateTeamRequest.builder()
                .name("Dev Team").description("Development team").build();

        when(teamVaultService.createTeam(eq("owner"), any(CreateTeamRequest.class)))
                .thenReturn(teamResponse);

        mockMvc.perform(post("/api/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Dev Team"))
                .andExpect(jsonPath("$.description").value("Development team"))
                .andExpect(jsonPath("$.createdByUsername").value("owner"))
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.sharedEntryCount").value(1))
                .andExpect(jsonPath("$.currentUserRole").value("OWNER"))
                .andExpect(jsonPath("$.members[0].username").value("owner"))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"))
                .andExpect(jsonPath("$.members[1].username").value("member"))
                .andExpect(jsonPath("$.members[1].role").value("MEMBER"));
    }

    @Test
    @WithMockUser(username = "owner")
    void createTeam_BlankName_ShouldReturn400() throws Exception {
        CreateTeamRequest request = CreateTeamRequest.builder().name("").build();

        mockMvc.perform(post("/api/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createTeam_Unauthenticated_ShouldReturn401() throws Exception {
        CreateTeamRequest request = CreateTeamRequest.builder().name("Team").build();

        mockMvc.perform(post("/api/teams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/teams ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void getMyTeams_ShouldReturnTeamList() throws Exception {
        when(teamVaultService.getMyTeams("owner")).thenReturn(List.of(teamResponse));

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Dev Team"))
                .andExpect(jsonPath("$[0].memberCount").value(2))
                .andExpect(jsonPath("$[0].currentUserRole").value("OWNER"));
    }

    @Test
    @WithMockUser(username = "owner")
    void getMyTeams_NoTeams_ShouldReturnEmptyArray() throws Exception {
        when(teamVaultService.getMyTeams("owner")).thenReturn(List.of());

        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── GET /api/teams/{teamId} ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void getTeam_ValidMember_ShouldReturnTeamWithMembers() throws Exception {
        when(teamVaultService.getTeam("owner", 10L)).thenReturn(teamResponse);

        mockMvc.perform(get("/api/teams/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Dev Team"))
                .andExpect(jsonPath("$.members").isArray())
                .andExpect(jsonPath("$.members.length()").value(2))
                .andExpect(jsonPath("$.members[0].memberId").value(100))
                .andExpect(jsonPath("$.members[0].userId").value(1))
                .andExpect(jsonPath("$.members[0].username").value("owner"))
                .andExpect(jsonPath("$.members[0].email").value("owner@test.com"))
                .andExpect(jsonPath("$.members[0].role").value("OWNER"))
                .andExpect(jsonPath("$.members[0].joinedAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "outsider")
    void getTeam_NotMember_ShouldReturn401() throws Exception {
        when(teamVaultService.getTeam("outsider", 10L))
                .thenThrow(new AuthenticationException("You are not a member of this team"));

        mockMvc.perform(get("/api/teams/10"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/teams/{teamId} ────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void deleteTeam_Owner_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/teams/10").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Team deleted successfully"));
    }

    @Test
    @WithMockUser(username = "member")
    void deleteTeam_NotOwner_ShouldReturn401() throws Exception {
        Mockito.doThrow(new AuthenticationException("Insufficient permissions. Required role: OWNER"))
                .when(teamVaultService).deleteTeam("member", 10L);

        mockMvc.perform(delete("/api/teams/10").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/teams/{teamId}/members ───────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void getMembers_ShouldReturnMemberList() throws Exception {
        when(teamVaultService.getMembers("owner", 10L))
                .thenReturn(List.of(ownerMemberResponse, memberResponse));

        mockMvc.perform(get("/api/teams/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].memberId").value(100))
                .andExpect(jsonPath("$[0].username").value("owner"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[1].memberId").value(101))
                .andExpect(jsonPath("$[1].username").value("member"))
                .andExpect(jsonPath("$[1].role").value("MEMBER"));
    }

    // ── POST /api/teams/{teamId}/members ──────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void inviteMember_ValidRequest_ShouldReturn201WithMemberDetails() throws Exception {
        InviteMemberRequest request = InviteMemberRequest.builder()
                .email("member@test.com").role("MEMBER").build();

        when(teamVaultService.inviteMember(eq("owner"), eq(10L), any(InviteMemberRequest.class)))
                .thenReturn(memberResponse);

        mockMvc.perform(post("/api/teams/10/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.memberId").value(101))
                .andExpect(jsonPath("$.username").value("member"))
                .andExpect(jsonPath("$.email").value("member@test.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    @WithMockUser(username = "owner")
    void inviteMember_InvalidEmail_ShouldReturn400() throws Exception {
        InviteMemberRequest request = InviteMemberRequest.builder()
                .email("not-an-email").role("MEMBER").build();

        mockMvc.perform(post("/api/teams/10/members")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── PUT /api/teams/{teamId}/members/{userId}/role ─────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void changeMemberRole_ValidChange_ShouldReturnUpdatedMember() throws Exception {
        TeamMemberResponse adminResponse = TeamMemberResponse.builder()
                .memberId(101L).userId(2L).username("member").email("member@test.com")
                .role("ADMIN").joinedAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();

        when(teamVaultService.changeMemberRole("owner", 10L, 2L, "ADMIN"))
                .thenReturn(adminResponse);

        mockMvc.perform(put("/api/teams/10/members/2/role")
                        .with(csrf())
                        .param("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(101))
                .andExpect(jsonPath("$.username").value("member"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    // ── DELETE /api/teams/{teamId}/members/{userId} ───────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void removeMember_ValidRemoval_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/teams/10/members/2").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Member removed from team"));
    }

    // ── POST /api/teams/{teamId}/share ────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void shareEntry_ValidEntry_ShouldReturn201WithEntryDetails() throws Exception {
        VaultEntryResponse entryResponse = VaultEntryResponse.builder()
                .id(200L).title("GitHub").websiteUrl("https://github.com")
                .isFavorite(false).createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        when(teamVaultService.shareEntry("owner", 10L, 200L)).thenReturn(entryResponse);

        mockMvc.perform(post("/api/teams/10/share")
                        .with(csrf())
                        .param("vaultEntryId", "200"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(200))
                .andExpect(jsonPath("$.title").value("GitHub"))
                .andExpect(jsonPath("$.websiteUrl").value("https://github.com"));
    }

    @Test
    @WithMockUser(username = "owner")
    void shareEntry_HighlySensitive_ShouldReturn400() throws Exception {
        when(teamVaultService.shareEntry("owner", 10L, 200L))
                .thenThrow(new IllegalArgumentException("Highly sensitive entries cannot be shared with teams"));

        mockMvc.perform(post("/api/teams/10/share")
                        .with(csrf())
                        .param("vaultEntryId", "200"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /api/teams/{teamId}/vault ─────────────────────────────────────────

    @Test
    @WithMockUser(username = "member")
    void getTeamVault_ShouldReturnSharedEntries() throws Exception {
        VaultEntryResponse entryResponse = VaultEntryResponse.builder()
                .id(200L).title("GitHub").websiteUrl("https://github.com")
                .isFavorite(false).createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        when(teamVaultService.getTeamVault("member", 10L)).thenReturn(List.of(entryResponse));

        mockMvc.perform(get("/api/teams/10/vault"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(200))
                .andExpect(jsonPath("$[0].title").value("GitHub"))
                .andExpect(jsonPath("$[0].websiteUrl").value("https://github.com"));
    }

    // ── DELETE /api/teams/{teamId}/vault/{vaultEntryId} ───────────────────────

    @Test
    @WithMockUser(username = "owner")
    void unshareEntry_ValidEntry_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/teams/10/vault/200").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Entry removed from team vault"));
    }

    // ── GET /api/teams/{teamId}/activity ──────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void getTeamActivity_ShouldReturnActivityList() throws Exception {
        TeamActivityItem activity = new TeamActivityItem(
                "ENTRY_SHARED", "owner",
                "Shared 'GitHub' with the team",
                LocalDateTime.of(2026, 1, 1, 10, 0));

        when(teamVaultService.getTeamActivity("owner", 10L)).thenReturn(List.of(activity));

        mockMvc.perform(get("/api/teams/10/activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("ENTRY_SHARED"))
                .andExpect(jsonPath("$[0].actorUsername").value("owner"))
                .andExpect(jsonPath("$[0].description").value("Shared 'GitHub' with the team"))
                .andExpect(jsonPath("$[0].timestamp").isNotEmpty());
    }

    // ── Content-Type assertions ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void allGetEndpoints_ShouldReturnJsonContentType() throws Exception {
        when(teamVaultService.getMyTeams("owner")).thenReturn(List.of());
        when(teamVaultService.getTeam("owner", 10L)).thenReturn(teamResponse);
        when(teamVaultService.getMembers("owner", 10L)).thenReturn(List.of());
        when(teamVaultService.getTeamVault("owner", 10L)).thenReturn(List.of());
        when(teamVaultService.getTeamActivity("owner", 10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/teams"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/teams/10"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/teams/10/members"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/teams/10/vault"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/teams/10/activity"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
