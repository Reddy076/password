package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.ExpiryPolicyRequest;
import com.revature.passwordmanager.dto.response.ExpiryPolicyResponse;
import com.revature.passwordmanager.dto.response.ExpiryStatusResponse;
import com.revature.passwordmanager.dto.response.ExpiryStatusResponse.EntryExpiryDetail;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.expiry.PasswordExpiryService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Full controller-layer tests for {@link PasswordExpiryController}.
 * Each test verifies both the HTTP status and the JSON response body fields.</p>
 */
@WebMvcTest(controllers = PasswordExpiryController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class PasswordExpiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PasswordExpiryService expiryService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(100);
    }

    // ── GET /api/expiry/status ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getAllStatuses_ShouldReturnStatusSummaryAndEntries() throws Exception {
        EntryExpiryDetail detail = EntryExpiryDetail.builder()
                .entryId(1L)
                .title("GitHub")
                .username("user@github.com")
                .websiteUrl("https://github.com")
                .lastChangedAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .expiresAt(LocalDateTime.of(2026, 4, 1, 0, 0))
                .status("FRESH")
                .daysUntilExpiry(80L)
                .reminderSent(false)
                .snoozed(false)
                .build();

        ExpiryStatusResponse response = ExpiryStatusResponse.builder()
                .totalEntries(1)
                .freshCount(1)
                .agingCount(0)
                .expiringSoonCount(0)
                .expiredCount(0)
                .entries(List.of(detail))
                .build();

        when(expiryService.getAllStatuses("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/expiry/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalEntries").value(1))
                .andExpect(jsonPath("$.freshCount").value(1))
                .andExpect(jsonPath("$.agingCount").value(0))
                .andExpect(jsonPath("$.expiringSoonCount").value(0))
                .andExpect(jsonPath("$.expiredCount").value(0))
                .andExpect(jsonPath("$.entries[0].entryId").value(1))
                .andExpect(jsonPath("$.entries[0].title").value("GitHub"))
                .andExpect(jsonPath("$.entries[0].username").value("user@github.com"))
                .andExpect(jsonPath("$.entries[0].websiteUrl").value("https://github.com"))
                .andExpect(jsonPath("$.entries[0].status").value("FRESH"))
                .andExpect(jsonPath("$.entries[0].daysUntilExpiry").value(80))
                .andExpect(jsonPath("$.entries[0].reminderSent").value(false))
                .andExpect(jsonPath("$.entries[0].snoozed").value(false));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllStatuses_EmptyVault_ShouldReturnZeroCounts() throws Exception {
        ExpiryStatusResponse response = ExpiryStatusResponse.builder()
                .totalEntries(0)
                .freshCount(0)
                .agingCount(0)
                .expiringSoonCount(0)
                .expiredCount(0)
                .entries(List.of())
                .build();

        when(expiryService.getAllStatuses("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/expiry/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(0))
                .andExpect(jsonPath("$.entries").isArray())
                .andExpect(jsonPath("$.entries").isEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getAllStatuses_MixedStates_ShouldReturnCorrectCounts() throws Exception {
        ExpiryStatusResponse response = ExpiryStatusResponse.builder()
                .totalEntries(4)
                .freshCount(1)
                .agingCount(1)
                .expiringSoonCount(1)
                .expiredCount(1)
                .entries(List.of())
                .build();

        when(expiryService.getAllStatuses("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/expiry/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(4))
                .andExpect(jsonPath("$.freshCount").value(1))
                .andExpect(jsonPath("$.agingCount").value(1))
                .andExpect(jsonPath("$.expiringSoonCount").value(1))
                .andExpect(jsonPath("$.expiredCount").value(1));
    }

    @Test
    void getAllStatuses_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/expiry/status"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/expiry/expiring-soon ─────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getExpiringSoon_DefaultDays_ShouldReturnExpiringSoonEntries() throws Exception {
        EntryExpiryDetail detail = EntryExpiryDetail.builder()
                .entryId(2L)
                .title("Gmail")
                .username("user@gmail.com")
                .websiteUrl("https://gmail.com")
                .lastChangedAt(LocalDateTime.of(2025, 11, 1, 0, 0))
                .expiresAt(LocalDateTime.of(2026, 2, 28, 0, 0))
                .status("EXPIRING_SOON")
                .daysUntilExpiry(3L)
                .reminderSent(false)
                .snoozed(false)
                .build();

        ExpiryStatusResponse response = ExpiryStatusResponse.builder()
                .totalEntries(1)
                .expiringSoonCount(1)
                .entries(List.of(detail))
                .build();

        when(expiryService.getExpiringSoon("testuser", null)).thenReturn(response);

        mockMvc.perform(get("/api/expiry/expiring-soon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(1))
                .andExpect(jsonPath("$.expiringSoonCount").value(1))
                .andExpect(jsonPath("$.entries[0].entryId").value(2))
                .andExpect(jsonPath("$.entries[0].title").value("Gmail"))
                .andExpect(jsonPath("$.entries[0].status").value("EXPIRING_SOON"))
                .andExpect(jsonPath("$.entries[0].daysUntilExpiry").value(3));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getExpiringSoon_CustomDays_ShouldPassDaysParam() throws Exception {
        ExpiryStatusResponse response = ExpiryStatusResponse.builder()
                .totalEntries(0)
                .expiringSoonCount(0)
                .entries(List.of())
                .build();

        when(expiryService.getExpiringSoon("testuser", 30)).thenReturn(response);

        mockMvc.perform(get("/api/expiry/expiring-soon").param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEntries").value(0));
    }

    @Test
    void getExpiringSoon_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/expiry/expiring-soon"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/expiry/policy ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getPolicy_ShouldReturnCurrentPolicy() throws Exception {
        ExpiryPolicyResponse response = ExpiryPolicyResponse.builder()
                .id(1L)
                .defaultExpiryDays(90)
                .criticalExpiryDays(180)
                .reminderDaysBefore(7)
                .enabled(true)
                .build();

        when(expiryService.getPolicy("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/expiry/policy"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.defaultExpiryDays").value(90))
                .andExpect(jsonPath("$.criticalExpiryDays").value(180))
                .andExpect(jsonPath("$.reminderDaysBefore").value(7))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getPolicy_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/expiry/policy"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/expiry/policy ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void updatePolicy_ValidRequest_ShouldReturnUpdatedPolicy() throws Exception {
        ExpiryPolicyRequest request = ExpiryPolicyRequest.builder()
                .defaultExpiryDays(60)
                .criticalExpiryDays(120)
                .reminderDaysBefore(5)
                .enabled(true)
                .build();

        ExpiryPolicyResponse response = ExpiryPolicyResponse.builder()
                .id(1L)
                .defaultExpiryDays(60)
                .criticalExpiryDays(120)
                .reminderDaysBefore(5)
                .enabled(true)
                .build();

        when(expiryService.updatePolicy(eq("testuser"), any(ExpiryPolicyRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/expiry/policy")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.defaultExpiryDays").value(60))
                .andExpect(jsonPath("$.criticalExpiryDays").value(120))
                .andExpect(jsonPath("$.reminderDaysBefore").value(5))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updatePolicy_DisableTracking_ShouldReturnEnabledFalse() throws Exception {
        ExpiryPolicyRequest request = ExpiryPolicyRequest.builder()
                .enabled(false)
                .build();

        ExpiryPolicyResponse response = ExpiryPolicyResponse.builder()
                .id(1L)
                .defaultExpiryDays(90)
                .criticalExpiryDays(180)
                .reminderDaysBefore(7)
                .enabled(false)
                .build();

        when(expiryService.updatePolicy(eq("testuser"), any(ExpiryPolicyRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/expiry/policy")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    @WithMockUser(username = "testuser")
    void updatePolicy_InvalidDefaultExpiryDays_ShouldReturn400() throws Exception {
        // defaultExpiryDays = 0 violates @Min(1)
        ExpiryPolicyRequest request = ExpiryPolicyRequest.builder()
                .defaultExpiryDays(0)
                .build();

        mockMvc.perform(put("/api/expiry/policy")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePolicy_Unauthenticated_ShouldReturn401() throws Exception {
        ExpiryPolicyRequest request = ExpiryPolicyRequest.builder()
                .defaultExpiryDays(60)
                .build();

        mockMvc.perform(put("/api/expiry/policy")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/expiry/snooze/{entryId} ─────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void snoozeReminder_ValidEntry_ShouldReturnUpdatedDetail() throws Exception {
        EntryExpiryDetail detail = EntryExpiryDetail.builder()
                .entryId(1L)
                .title("GitHub")
                .username("user@github.com")
                .websiteUrl("https://github.com")
                .lastChangedAt(LocalDateTime.of(2025, 11, 1, 0, 0))
                .expiresAt(LocalDateTime.of(2026, 2, 28, 0, 0))
                .status("EXPIRING_SOON")
                .daysUntilExpiry(3L)
                .reminderSent(false)
                .snoozed(true)
                .snoozedUntil(LocalDateTime.now().plusDays(7))
                .build();

        when(expiryService.snoozeReminder("testuser", 1L, null)).thenReturn(detail);

        mockMvc.perform(post("/api/expiry/snooze/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value(1))
                .andExpect(jsonPath("$.title").value("GitHub"))
                .andExpect(jsonPath("$.status").value("EXPIRING_SOON"))
                .andExpect(jsonPath("$.snoozed").value(true))
                .andExpect(jsonPath("$.snoozedUntil").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void snoozeReminder_WithCustomDays_ShouldPassDaysParam() throws Exception {
        EntryExpiryDetail detail = EntryExpiryDetail.builder()
                .entryId(1L)
                .title("GitHub")
                .status("EXPIRING_SOON")
                .snoozed(true)
                .snoozedUntil(LocalDateTime.now().plusDays(14))
                .build();

        when(expiryService.snoozeReminder("testuser", 1L, 14)).thenReturn(detail);

        mockMvc.perform(post("/api/expiry/snooze/1").with(csrf()).param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snoozed").value(true));
    }

    @Test
    @WithMockUser(username = "testuser")
    void snoozeReminder_EntryNotFound_ShouldReturn404() throws Exception {
        when(expiryService.snoozeReminder("testuser", 999L, null))
                .thenThrow(new ResourceNotFoundException("Vault entry not found"));

        mockMvc.perform(post("/api/expiry/snooze/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void snoozeReminder_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/expiry/snooze/1").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── Content-Type assertions ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void allGetEndpoints_ShouldReturnJsonContentType() throws Exception {
        when(expiryService.getAllStatuses("testuser"))
                .thenReturn(ExpiryStatusResponse.builder().totalEntries(0).entries(List.of()).build());
        when(expiryService.getExpiringSoon("testuser", null))
                .thenReturn(ExpiryStatusResponse.builder().totalEntries(0).entries(List.of()).build());
        when(expiryService.getPolicy("testuser"))
                .thenReturn(ExpiryPolicyResponse.builder().id(1L).defaultExpiryDays(90)
                        .criticalExpiryDays(180).reminderDaysBefore(7).enabled(true).build());

        mockMvc.perform(get("/api/expiry/status"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/expiry/expiring-soon"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/expiry/policy"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    // ── Snoozed entry detail — all fields ────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getAllStatuses_SnoozedEntry_ShouldIncludeSnoozedFields() throws Exception {
        LocalDateTime snoozedUntil = LocalDateTime.of(2026, 3, 10, 0, 0);
        EntryExpiryDetail detail = EntryExpiryDetail.builder()
                .entryId(5L)
                .title("AWS")
                .username("admin@aws.com")
                .websiteUrl("https://aws.amazon.com")
                .lastChangedAt(LocalDateTime.of(2025, 11, 1, 0, 0))
                .expiresAt(LocalDateTime.of(2026, 2, 28, 0, 0))
                .status("EXPIRING_SOON")
                .daysUntilExpiry(2L)
                .reminderSent(true)
                .snoozed(true)
                .snoozedUntil(snoozedUntil)
                .build();

        ExpiryStatusResponse response = ExpiryStatusResponse.builder()
                .totalEntries(1)
                .expiringSoonCount(1)
                .entries(List.of(detail))
                .build();

        when(expiryService.getAllStatuses("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/expiry/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].entryId").value(5))
                .andExpect(jsonPath("$.entries[0].title").value("AWS"))
                .andExpect(jsonPath("$.entries[0].username").value("admin@aws.com"))
                .andExpect(jsonPath("$.entries[0].websiteUrl").value("https://aws.amazon.com"))
                .andExpect(jsonPath("$.entries[0].status").value("EXPIRING_SOON"))
                .andExpect(jsonPath("$.entries[0].daysUntilExpiry").value(2))
                .andExpect(jsonPath("$.entries[0].reminderSent").value(true))
                .andExpect(jsonPath("$.entries[0].snoozed").value(true))
                .andExpect(jsonPath("$.entries[0].snoozedUntil").isNotEmpty());
    }

    // ── Expired entry ─────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getAllStatuses_ExpiredEntry_ShouldShowNegativeDaysUntilExpiry() throws Exception {
        EntryExpiryDetail detail = EntryExpiryDetail.builder()
                .entryId(3L)
                .title("OldBank")
                .status("EXPIRED")
                .daysUntilExpiry(-15L)
                .reminderSent(true)
                .snoozed(false)
                .build();

        ExpiryStatusResponse response = ExpiryStatusResponse.builder()
                .totalEntries(1)
                .expiredCount(1)
                .entries(List.of(detail))
                .build();

        when(expiryService.getAllStatuses("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/expiry/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredCount").value(1))
                .andExpect(jsonPath("$.entries[0].status").value("EXPIRED"))
                .andExpect(jsonPath("$.entries[0].daysUntilExpiry").value(-15))
                .andExpect(jsonPath("$.entries[0].reminderSent").value(true));
    }
}
