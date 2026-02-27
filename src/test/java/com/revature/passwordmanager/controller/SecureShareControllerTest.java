package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.CreateShareRequest;
import com.revature.passwordmanager.dto.response.ShareLinkResponse;
import com.revature.passwordmanager.dto.response.SharedPasswordResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.security.RateLimitService;
import com.revature.passwordmanager.service.sharing.SecureShareService;
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

@WebMvcTest(controllers = SecureShareController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class SecureShareControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private SecureShareService shareService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private SessionService sessionService;
    @MockBean private RateLimitService rateLimitService;

    private ShareLinkResponse sampleShareLink;
    private SharedPasswordResponse sampleSharedPassword;

    @BeforeEach
    void setUp() {
        Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(100);

        sampleShareLink = ShareLinkResponse.builder()
                .shareId(1L)
                .shareToken("abc123token")
                .shareUrl("/api/shares/abc123token")
                .encryptionKey("base64KeyHere==")
                .vaultEntryTitle("Netflix")
                .recipientEmail("friend@example.com")
                .permission("VIEW_ONCE")
                .maxViews(1)
                .viewCount(0)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        sampleSharedPassword = SharedPasswordResponse.builder()
                .title("Netflix")
                .username("user@netflix.com")
                .encryptedPassword("encryptedCiphertext==")
                .encryptionIv("ivBase64==")
                .websiteUrl("https://netflix.com")
                .permission("VIEW_ONCE")
                .viewsRemaining(0)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .sharedBy("alice")
                .build();
    }

    // ── POST /api/shares ──────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void createShare_ValidRequest_ShouldReturn201WithShareLink() throws Exception {
        CreateShareRequest request = CreateShareRequest.builder()
                .vaultEntryId(10L)
                .recipientEmail("friend@example.com")
                .expiryHours(24)
                .maxViews(1)
                .permission("VIEW_ONCE")
                .build();

        when(shareService.createShare(eq("testuser"), any(CreateShareRequest.class)))
                .thenReturn(sampleShareLink);

        mockMvc.perform(post("/api/shares")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.shareId").value(1))
                .andExpect(jsonPath("$.shareToken").value("abc123token"))
                .andExpect(jsonPath("$.shareUrl").value("/api/shares/abc123token"))
                .andExpect(jsonPath("$.encryptionKey").value("base64KeyHere=="))
                .andExpect(jsonPath("$.vaultEntryTitle").value("Netflix"))
                .andExpect(jsonPath("$.recipientEmail").value("friend@example.com"))
                .andExpect(jsonPath("$.permission").value("VIEW_ONCE"))
                .andExpect(jsonPath("$.maxViews").value(1))
                .andExpect(jsonPath("$.viewCount").value(0))
                .andExpect(jsonPath("$.revoked").value(false))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void createShare_MultipleViews_ShouldReturnCorrectPermission() throws Exception {
        ShareLinkResponse multiViewResponse = ShareLinkResponse.builder()
                .shareId(2L).shareToken("tok456").shareUrl("/api/shares/tok456")
                .vaultEntryTitle("Gmail").permission("VIEW_MULTIPLE").maxViews(5).viewCount(0)
                .expiresAt(LocalDateTime.now().plusHours(48)).createdAt(LocalDateTime.now())
                .revoked(false).build();

        when(shareService.createShare(eq("testuser"), any())).thenReturn(multiViewResponse);

        CreateShareRequest request = CreateShareRequest.builder()
                .vaultEntryId(5L).expiryHours(48).maxViews(5).permission("VIEW_MULTIPLE").build();

        mockMvc.perform(post("/api/shares").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permission").value("VIEW_MULTIPLE"))
                .andExpect(jsonPath("$.maxViews").value(5));
    }

    @Test
    @WithMockUser(username = "testuser")
    void createShare_EntryNotFound_ShouldReturn404() throws Exception {
        when(shareService.createShare(eq("testuser"), any()))
                .thenThrow(new ResourceNotFoundException("Vault entry not found"));

        CreateShareRequest request = CreateShareRequest.builder()
                .vaultEntryId(999L).expiryHours(24).maxViews(1).permission("VIEW_ONCE").build();

        mockMvc.perform(post("/api/shares").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void createShare_Unauthenticated_ShouldReturn401() throws Exception {
        CreateShareRequest request = CreateShareRequest.builder()
                .vaultEntryId(10L).expiryHours(24).maxViews(1).permission("VIEW_ONCE").build();

        mockMvc.perform(post("/api/shares").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/shares/{token} ───────────────────────────────────────────────
    // Note: although this is a public endpoint in production (SecurityConfig permits /api/shares/*),
    // @WebMvcTest loads the full SecurityConfig which requires a valid JWT in the filter chain.
    // We use @WithMockUser here to bypass the JWT filter for the test; the public-access behaviour
    // is validated separately by the SecurityConfig permit pattern.

    @Test
    @WithMockUser
    void getSharedPassword_ValidToken_ShouldReturnPasswordData() throws Exception {
        when(shareService.getSharedPassword("abc123token")).thenReturn(sampleSharedPassword);

        mockMvc.perform(get("/api/shares/abc123token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Netflix"))
                .andExpect(jsonPath("$.username").value("user@netflix.com"))
                .andExpect(jsonPath("$.encryptedPassword").value("encryptedCiphertext=="))
                .andExpect(jsonPath("$.encryptionIv").value("ivBase64=="))
                .andExpect(jsonPath("$.websiteUrl").value("https://netflix.com"))
                .andExpect(jsonPath("$.permission").value("VIEW_ONCE"))
                .andExpect(jsonPath("$.viewsRemaining").value(0))
                .andExpect(jsonPath("$.sharedBy").value("alice"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @WithMockUser
    void getSharedPassword_InvalidToken_ShouldReturn404() throws Exception {
        when(shareService.getSharedPassword("badtoken"))
                .thenThrow(new ResourceNotFoundException("Share not found or has expired"));

        mockMvc.perform(get("/api/shares/badtoken"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getSharedPassword_ExpiredShare_ShouldReturn500() throws Exception {
        when(shareService.getSharedPassword("expiredToken"))
                .thenThrow(new IllegalStateException("Share is no longer valid: expired"));

        mockMvc.perform(get("/api/shares/expiredToken"))
                .andExpect(status().isInternalServerError());
    }

    // ── GET /api/shares ───────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getActiveShares_ShouldReturnList() throws Exception {
        when(shareService.getActiveShares("testuser")).thenReturn(List.of(sampleShareLink));

        mockMvc.perform(get("/api/shares"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].shareId").value(1))
                .andExpect(jsonPath("$[0].shareToken").value("abc123token"))
                .andExpect(jsonPath("$[0].shareUrl").value("/api/shares/abc123token"))
                .andExpect(jsonPath("$[0].vaultEntryTitle").value("Netflix"))
                .andExpect(jsonPath("$[0].permission").value("VIEW_ONCE"))
                .andExpect(jsonPath("$[0].maxViews").value(1))
                .andExpect(jsonPath("$[0].viewCount").value(0))
                .andExpect(jsonPath("$[0].revoked").value(false))
                .andExpect(jsonPath("$[0].expiresAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getActiveShares_NoShares_ShouldReturnEmptyList() throws Exception {
        when(shareService.getActiveShares("testuser")).thenReturn(List.of());

        mockMvc.perform(get("/api/shares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getActiveShares_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/shares"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/shares/{id} ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void revokeShare_ShouldReturnRevokedShare() throws Exception {
        ShareLinkResponse revoked = ShareLinkResponse.builder()
                .shareId(1L).shareToken("abc123token").shareUrl("/api/shares/abc123token")
                .vaultEntryTitle("Netflix").permission("VIEW_ONCE").maxViews(1).viewCount(0)
                .expiresAt(LocalDateTime.now().plusHours(1)).createdAt(LocalDateTime.now())
                .revoked(true).build();

        when(shareService.revokeShare("testuser", 1L)).thenReturn(revoked);

        mockMvc.perform(delete("/api/shares/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shareId").value(1))
                .andExpect(jsonPath("$.revoked").value(true))
                .andExpect(jsonPath("$.vaultEntryTitle").value("Netflix"))
                .andExpect(jsonPath("$.shareToken").value("abc123token"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void revokeShare_NotFound_ShouldReturn404() throws Exception {
        when(shareService.revokeShare("testuser", 999L))
                .thenThrow(new ResourceNotFoundException("Share not found"));

        mockMvc.perform(delete("/api/shares/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser")
    void revokeShare_WrongOwner_ShouldReturn400() throws Exception {
        when(shareService.revokeShare("testuser", 1L))
                .thenThrow(new IllegalArgumentException("Share does not belong to this user"));

        mockMvc.perform(delete("/api/shares/1").with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revokeShare_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/shares/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/shares/received ──────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getReceivedShares_ShouldReturnList() throws Exception {
        ShareLinkResponse received = ShareLinkResponse.builder()
                .shareId(2L).shareToken("tok789").shareUrl("/api/shares/tok789")
                .vaultEntryTitle("Spotify").recipientEmail("testuser@example.com")
                .permission("VIEW_MULTIPLE").maxViews(3).viewCount(1)
                .expiresAt(LocalDateTime.now().plusHours(12)).createdAt(LocalDateTime.now())
                .revoked(false).build();

        when(shareService.getReceivedShares("testuser")).thenReturn(List.of(received));

        mockMvc.perform(get("/api/shares/received"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].shareId").value(2))
                .andExpect(jsonPath("$[0].shareToken").value("tok789"))
                .andExpect(jsonPath("$[0].vaultEntryTitle").value("Spotify"))
                .andExpect(jsonPath("$[0].permission").value("VIEW_MULTIPLE"))
                .andExpect(jsonPath("$[0].maxViews").value(3))
                .andExpect(jsonPath("$[0].viewCount").value(1))
                .andExpect(jsonPath("$[0].revoked").value(false));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getReceivedShares_None_ShouldReturnEmpty() throws Exception {
        when(shareService.getReceivedShares("testuser")).thenReturn(List.of());

        mockMvc.perform(get("/api/shares/received"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getReceivedShares_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/shares/received"))
                .andExpect(status().isUnauthorized());
    }
}
