package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.AutofillSuggestionRequest;
import com.revature.passwordmanager.dto.request.AutofillUsageRequest;
import com.revature.passwordmanager.dto.response.AutofillSuggestionResponse;
import com.revature.passwordmanager.dto.response.AutofillSuggestionResponse.AutofillEntry;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.autofill.AutofillService;
import com.revature.passwordmanager.service.security.RateLimitService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Full controller-layer tests for {@link AutofillController}.
 * Each test verifies both the HTTP status and the JSON response body fields.</p>
 */
@WebMvcTest(controllers = AutofillController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class AutofillControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AutofillService autofillService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RateLimitService rateLimitService;

    private AutofillSuggestionResponse suggestionResponse;

    @BeforeEach
    void setUp() {
        Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(100);

        AutofillEntry githubEntry = AutofillEntry.builder()
                .entryId(1L)
                .title("GitHub")
                .username("enc_user")
                .websiteUrl("https://github.com")
                .matchType("EXACT")
                .isFavorite(true)
                .build();

        AutofillEntry appEntry = AutofillEntry.builder()
                .entryId(2L)
                .title("GitHub App")
                .username("enc_user2")
                .websiteUrl("https://app.github.com")
                .matchType("SUBDOMAIN")
                .isFavorite(false)
                .build();

        suggestionResponse = AutofillSuggestionResponse.builder()
                .domain("github.com")
                .suggestions(List.of(githubEntry, appEntry))
                .totalCount(2)
                .build();
    }

    // ── POST /api/autofill/suggestions ────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getSuggestions_ValidUrl_ShouldReturnMatchingEntries() throws Exception {
        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://github.com/login")
                .build();

        when(autofillService.getSuggestions(eq("testuser"), any(AutofillSuggestionRequest.class)))
                .thenReturn(suggestionResponse);

        mockMvc.perform(post("/api/autofill/suggestions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.domain").value("github.com"))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.suggestions[0].entryId").value(1))
                .andExpect(jsonPath("$.suggestions[0].title").value("GitHub"))
                .andExpect(jsonPath("$.suggestions[0].username").value("enc_user"))
                .andExpect(jsonPath("$.suggestions[0].websiteUrl").value("https://github.com"))
                .andExpect(jsonPath("$.suggestions[0].matchType").value("EXACT"))
                .andExpect(jsonPath("$.suggestions[0].isFavorite").value(true))
                .andExpect(jsonPath("$.suggestions[1].entryId").value(2))
                .andExpect(jsonPath("$.suggestions[1].matchType").value("SUBDOMAIN"))
                .andExpect(jsonPath("$.suggestions[1].isFavorite").value(false));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getSuggestions_NoMatches_ShouldReturnEmptyList() throws Exception {
        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://unknown-site.com")
                .build();

        AutofillSuggestionResponse emptyResponse = AutofillSuggestionResponse.builder()
                .domain("unknown-site.com")
                .suggestions(List.of())
                .totalCount(0)
                .build();

        when(autofillService.getSuggestions(eq("testuser"), any(AutofillSuggestionRequest.class)))
                .thenReturn(emptyResponse);

        mockMvc.perform(post("/api/autofill/suggestions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.domain").value("unknown-site.com"))
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.suggestions").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getSuggestions_BlankUrl_ShouldReturn400() throws Exception {
        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("")
                .build();

        mockMvc.perform(post("/api/autofill/suggestions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSuggestions_Unauthenticated_ShouldReturn401() throws Exception {
        AutofillSuggestionRequest request = AutofillSuggestionRequest.builder()
                .url("https://github.com")
                .build();

        mockMvc.perform(post("/api/autofill/suggestions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/autofill/trusted-domains ─────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getTrustedDomains_ShouldReturnDomainList() throws Exception {
        when(autofillService.getTrustedDomains("testuser"))
                .thenReturn(List.of("github.com", "google.com", "bank.com"));

        mockMvc.perform(get("/api/autofill/trusted-domains"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0]").value("github.com"))
                .andExpect(jsonPath("$[1]").value("google.com"))
                .andExpect(jsonPath("$[2]").value("bank.com"))
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getTrustedDomains_NoPreviousUsage_ShouldReturnEmptyArray() throws Exception {
        when(autofillService.getTrustedDomains("testuser")).thenReturn(List.of());

        mockMvc.perform(get("/api/autofill/trusted-domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getTrustedDomains_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/autofill/trusted-domains"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/autofill/log-usage ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void logUsage_Applied_ShouldReturn200WithMessage() throws Exception {
        AutofillUsageRequest request = AutofillUsageRequest.builder()
                .url("https://github.com/login")
                .vaultEntryId(1L)
                .applied(true)
                .build();

        mockMvc.perform(post("/api/autofill/log-usage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usage logged successfully"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void logUsage_NotApplied_ShouldReturn200() throws Exception {
        AutofillUsageRequest request = AutofillUsageRequest.builder()
                .url("https://github.com")
                .vaultEntryId(null)
                .applied(false)
                .build();

        mockMvc.perform(post("/api/autofill/log-usage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Usage logged successfully"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void logUsage_BlankUrl_ShouldReturn400() throws Exception {
        AutofillUsageRequest request = AutofillUsageRequest.builder()
                .url("")
                .applied(true)
                .build();

        mockMvc.perform(post("/api/autofill/log-usage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logUsage_Unauthenticated_ShouldReturn401() throws Exception {
        AutofillUsageRequest request = AutofillUsageRequest.builder()
                .url("https://github.com")
                .applied(true)
                .build();

        mockMvc.perform(post("/api/autofill/log-usage")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── Content-Type assertions ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void allEndpoints_ShouldReturnJsonContentType() throws Exception {
        AutofillSuggestionRequest suggReq = AutofillSuggestionRequest.builder()
                .url("https://github.com").build();

        when(autofillService.getSuggestions(eq("testuser"), any()))
                .thenReturn(AutofillSuggestionResponse.builder()
                        .domain("github.com").suggestions(List.of()).totalCount(0).build());
        when(autofillService.getTrustedDomains("testuser")).thenReturn(List.of());

        mockMvc.perform(post("/api/autofill/suggestions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(suggReq)))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/autofill/trusted-domains"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
