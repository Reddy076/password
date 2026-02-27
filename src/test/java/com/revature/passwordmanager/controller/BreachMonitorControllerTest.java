package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.BreachHistoryResponse;
import com.revature.passwordmanager.dto.response.BreachScanResponse;
import com.revature.passwordmanager.dto.response.BreachStatusResponse;
import com.revature.passwordmanager.dto.response.CompromisedCredentialResponse;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.security.RateLimitService;
import com.revature.passwordmanager.service.security.breach.BreachMonitorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BreachMonitorController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class BreachMonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BreachMonitorService breachMonitorService;

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

    // ── POST /api/security/breach-scan ────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void triggerScan_ShouldReturnScanResult() throws Exception {
        BreachScanResponse response = BreachScanResponse.builder()
                .scanId(1L)
                .entriesScanned(10)
                .compromisedFound(2)
                .status("COMPLETED")
                .triggerType("MANUAL")
                .scannedAt(LocalDateTime.now())
                .newlyCompromised(List.of())
                .build();

        when(breachMonitorService.runScan(eq("testuser"), any())).thenReturn(response);

        mockMvc.perform(post("/api/security/breach-scan").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").value(1))
                .andExpect(jsonPath("$.entriesScanned").value(10))
                .andExpect(jsonPath("$.compromisedFound").value(2))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.triggerType").value("MANUAL"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void triggerScan_NothingCompromised_ShouldReturnZero() throws Exception {
        BreachScanResponse response = BreachScanResponse.builder()
                .scanId(2L)
                .entriesScanned(5)
                .compromisedFound(0)
                .status("COMPLETED")
                .triggerType("MANUAL")
                .scannedAt(LocalDateTime.now())
                .newlyCompromised(List.of())
                .build();

        when(breachMonitorService.runScan(eq("testuser"), any())).thenReturn(response);

        mockMvc.perform(post("/api/security/breach-scan").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.compromisedFound").value(0))
                .andExpect(jsonPath("$.newlyCompromised").isArray())
                .andExpect(jsonPath("$.newlyCompromised").isEmpty());
    }

    @Test
    void triggerScan_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/security/breach-scan").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/security/breach-status ──────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getBreachStatus_Safe_ShouldReturnSafeStatus() throws Exception {
        BreachStatusResponse response = BreachStatusResponse.builder()
                .overallStatus("SAFE")
                .totalCompromised(0)
                .totalVaultEntries(10)
                .lastScanAt(LocalDateTime.now().minusDays(1))
                .scanInProgress(false)
                .recommendation("No compromised passwords found.")
                .build();

        when(breachMonitorService.getStatus("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/security/breach-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("SAFE"))
                .andExpect(jsonPath("$.totalCompromised").value(0))
                .andExpect(jsonPath("$.totalVaultEntries").value(10))
                .andExpect(jsonPath("$.scanInProgress").value(false))
                .andExpect(jsonPath("$.recommendation").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void getBreachStatus_Compromised_ShouldReturnCriticalStatus() throws Exception {
        BreachStatusResponse response = BreachStatusResponse.builder()
                .overallStatus("COMPROMISED")
                .totalCompromised(5)
                .totalVaultEntries(10)
                .lastScanAt(LocalDateTime.now().minusHours(2))
                .scanInProgress(false)
                .recommendation("5 password(s) are compromised. Update immediately.")
                .build();

        when(breachMonitorService.getStatus("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/security/breach-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("COMPROMISED"))
                .andExpect(jsonPath("$.totalCompromised").value(5));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getBreachStatus_AtRisk_ShouldReturnAtRisk() throws Exception {
        BreachStatusResponse response = BreachStatusResponse.builder()
                .overallStatus("AT_RISK")
                .totalCompromised(2)
                .totalVaultEntries(10)
                .lastScanAt(LocalDateTime.now().minusHours(1))
                .scanInProgress(false)
                .recommendation("2 password(s) found in breaches. Update them.")
                .build();

        when(breachMonitorService.getStatus("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/security/breach-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("AT_RISK"))
                .andExpect(jsonPath("$.totalCompromised").value(2));
    }

    @Test
    void getBreachStatus_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/security/breach-status"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/security/compromised-credentials ─────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getCompromisedCredentials_ShouldReturnList() throws Exception {
        CompromisedCredentialResponse cred = CompromisedCredentialResponse.builder()
                .id(1L)
                .vaultEntryId(10L)
                .vaultEntryTitle("Gmail")
                .username("user@gmail.com")
                .websiteUrl("https://gmail.com")
                .pwnedCount(5432L)
                .resolved(false)
                .detectedAt(LocalDateTime.now().minusDays(2))
                .build();

        when(breachMonitorService.getCompromisedCredentials("testuser"))
                .thenReturn(List.of(cred));

        mockMvc.perform(get("/api/security/compromised-credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].vaultEntryTitle").value("Gmail"))
                .andExpect(jsonPath("$[0].pwnedCount").value(5432))
                .andExpect(jsonPath("$[0].resolved").value(false));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getCompromisedCredentials_NoneFound_ShouldReturnEmpty() throws Exception {
        when(breachMonitorService.getCompromisedCredentials("testuser"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/security/compromised-credentials"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getCompromisedCredentials_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/security/compromised-credentials"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/security/breach-history ─────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getBreachHistory_ShouldReturnHistory() throws Exception {
        BreachHistoryResponse.ScanHistoryEntry entry = BreachHistoryResponse.ScanHistoryEntry.builder()
                .scanId(1L)
                .triggerType("MANUAL")
                .entriesScanned(10)
                .compromisedFound(1)
                .status("COMPLETED")
                .scannedAt(LocalDateTime.now().minusDays(1))
                .build();

        BreachHistoryResponse response = BreachHistoryResponse.builder()
                .scanHistory(List.of(entry))
                .totalScans(1)
                .totalCompromisedFound(1)
                .build();

        when(breachMonitorService.getBreachHistory("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/security/breach-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScans").value(1))
                .andExpect(jsonPath("$.totalCompromisedFound").value(1))
                .andExpect(jsonPath("$.scanHistory[0].scanId").value(1))
                .andExpect(jsonPath("$.scanHistory[0].triggerType").value("MANUAL"))
                .andExpect(jsonPath("$.scanHistory[0].compromisedFound").value(1));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getBreachHistory_NoHistory_ShouldReturnEmpty() throws Exception {
        BreachHistoryResponse response = BreachHistoryResponse.builder()
                .scanHistory(List.of())
                .totalScans(0)
                .totalCompromisedFound(0)
                .build();

        when(breachMonitorService.getBreachHistory("testuser")).thenReturn(response);

        mockMvc.perform(get("/api/security/breach-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScans").value(0))
                .andExpect(jsonPath("$.scanHistory").isEmpty());
    }

    @Test
    void getBreachHistory_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/security/breach-history"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/security/compromised-credentials/{id}/resolve ───────────────

    @Test
    @WithMockUser(username = "testuser")
    void resolveCredential_ShouldReturnResolvedCredential() throws Exception {
        CompromisedCredentialResponse resolved = CompromisedCredentialResponse.builder()
                .id(1L)
                .vaultEntryId(10L)
                .vaultEntryTitle("Gmail")
                .pwnedCount(5432L)
                .resolved(true)
                .detectedAt(LocalDateTime.now().minusDays(2))
                .resolvedAt(LocalDateTime.now())
                .build();

        when(breachMonitorService.resolveCredential("testuser", 1L)).thenReturn(resolved);

        mockMvc.perform(put("/api/security/compromised-credentials/1/resolve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.resolved").value(true))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
    }

    @Test
    void resolveCredential_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/security/compromised-credentials/1/resolve").with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
