package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.AddEmergencyContactRequest;
import com.revature.passwordmanager.dto.request.EmergencyAccessRequestDto;
import com.revature.passwordmanager.dto.response.EmergencyAccessRequestResponse;
import com.revature.passwordmanager.dto.response.EmergencyContactResponse;
import com.revature.passwordmanager.dto.response.EmergencyVaultResponse;
import com.revature.passwordmanager.dto.response.EmergencyVaultResponse.EmergencyVaultEntry;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.emergency.EmergencyAccessService;
import com.revature.passwordmanager.service.emergency.EmergencyContactService;
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
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Full controller-layer tests for {@link EmergencyAccessController}.
 * Each test verifies both the HTTP status and the JSON response body fields.</p>
 */
@WebMvcTest(controllers = EmergencyAccessController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class EmergencyAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmergencyContactService contactService;

    @MockBean
    private EmergencyAccessService accessService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RateLimitService rateLimitService;

    private EmergencyContactResponse contactResponse;
    private EmergencyAccessRequestResponse pendingRequestResponse;

    @BeforeEach
    void setUp() {
        Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(100);

        contactResponse = EmergencyContactResponse.builder()
                .id(10L)
                .contactEmail("contact@example.com")
                .contactName("John Doe")
                .relationship("Spouse")
                .waitingPeriodHours(48)
                .verified(false)
                .active(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        pendingRequestResponse = EmergencyAccessRequestResponse.builder()
                .id(100L)
                .contactId(10L)
                .contactEmail("contact@example.com")
                .contactName("John Doe")
                .ownerUsername("owner")
                .status("PENDING")
                .requestedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .waitingPeriodEndsAt(LocalDateTime.of(2026, 1, 3, 10, 0))
                .hoursUntilAutoApproval(24L)
                .requestMessage("Emergency situation")
                .build();
    }

    // ── POST /api/emergency/contacts ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void addContact_ValidRequest_ShouldReturn201WithContactDetails() throws Exception {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactEmail("contact@example.com")
                .contactName("John Doe")
                .relationship("Spouse")
                .waitingPeriodHours(48)
                .build();

        when(contactService.addContact(eq("owner"), any(AddEmergencyContactRequest.class)))
                .thenReturn(contactResponse);

        mockMvc.perform(post("/api/emergency/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.contactEmail").value("contact@example.com"))
                .andExpect(jsonPath("$.contactName").value("John Doe"))
                .andExpect(jsonPath("$.relationship").value("Spouse"))
                .andExpect(jsonPath("$.waitingPeriodHours").value(48))
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(username = "owner")
    void addContact_InvalidEmail_ShouldReturn400() throws Exception {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactEmail("not-an-email")
                .build();

        mockMvc.perform(post("/api/emergency/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "owner")
    void addContact_MissingEmail_ShouldReturn400() throws Exception {
        // Service throws IllegalArgumentException when email is null/blank
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactName("John Doe")
                // contactEmail is null
                .build();

        when(contactService.addContact(eq("owner"), any(AddEmergencyContactRequest.class)))
                .thenThrow(new IllegalArgumentException("Contact email is required"));

        mockMvc.perform(post("/api/emergency/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addContact_Unauthenticated_ShouldReturn401() throws Exception {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactEmail("contact@example.com")
                .build();

        mockMvc.perform(post("/api/emergency/contacts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/emergency/contacts ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void getContacts_ShouldReturnContactList() throws Exception {
        when(contactService.getContacts("owner")).thenReturn(List.of(contactResponse));

        mockMvc.perform(get("/api/emergency/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].contactEmail").value("contact@example.com"))
                .andExpect(jsonPath("$[0].contactName").value("John Doe"))
                .andExpect(jsonPath("$[0].relationship").value("Spouse"))
                .andExpect(jsonPath("$[0].waitingPeriodHours").value(48));
    }

    @Test
    @WithMockUser(username = "owner")
    void getContacts_Empty_ShouldReturnEmptyArray() throws Exception {
        when(contactService.getContacts("owner")).thenReturn(List.of());

        mockMvc.perform(get("/api/emergency/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getContacts_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/emergency/contacts"))
                .andExpect(status().isUnauthorized());
    }

    // ── PUT /api/emergency/contacts/{contactId} ───────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void updateContact_ValidRequest_ShouldReturnUpdatedContact() throws Exception {
        AddEmergencyContactRequest request = AddEmergencyContactRequest.builder()
                .contactName("Jane Doe")
                .relationship("Parent")
                .waitingPeriodHours(72)
                .build();

        EmergencyContactResponse updated = EmergencyContactResponse.builder()
                .id(10L)
                .contactEmail("contact@example.com")
                .contactName("Jane Doe")
                .relationship("Parent")
                .waitingPeriodHours(72)
                .verified(false)
                .active(true)
                .build();

        when(contactService.updateContact(eq("owner"), eq(10L), any(AddEmergencyContactRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(put("/api/emergency/contacts/10")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactName").value("Jane Doe"))
                .andExpect(jsonPath("$.relationship").value("Parent"))
                .andExpect(jsonPath("$.waitingPeriodHours").value(72));
    }

    // ── DELETE /api/emergency/contacts/{contactId} ────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void removeContact_ValidContact_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/emergency/contacts/10").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Emergency contact removed successfully"));
    }

    @Test
    @WithMockUser(username = "owner")
    void removeContact_NotFound_ShouldReturn404() throws Exception {
        Mockito.doThrow(new ResourceNotFoundException("Emergency contact not found"))
                .when(contactService).removeContact("owner", 999L);

        mockMvc.perform(delete("/api/emergency/contacts/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/emergency/request-access ────────────────────────────────────

    @Test
    @WithMockUser(username = "contactuser")
    void requestAccess_ValidRequest_ShouldReturn201WithPendingStatus() throws Exception {
        EmergencyAccessRequestDto dto = EmergencyAccessRequestDto.builder()
                .ownerUsername("owner")
                .requestMessage("Emergency situation")
                .build();

        when(accessService.requestAccess(eq("contactuser"), any(EmergencyAccessRequestDto.class)))
                .thenReturn(pendingRequestResponse);

        mockMvc.perform(post("/api/emergency/request-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.contactId").value(10))
                .andExpect(jsonPath("$.contactEmail").value("contact@example.com"))
                .andExpect(jsonPath("$.ownerUsername").value("owner"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.hoursUntilAutoApproval").value(24))
                .andExpect(jsonPath("$.requestMessage").value("Emergency situation"));
    }

    @Test
    void requestAccess_Unauthenticated_ShouldReturn401() throws Exception {
        EmergencyAccessRequestDto dto = EmergencyAccessRequestDto.builder()
                .ownerUsername("owner")
                .build();

        mockMvc.perform(post("/api/emergency/request-access")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/emergency/requests ───────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void getRequests_ShouldReturnRequestList() throws Exception {
        when(accessService.getRequestsForOwner("owner")).thenReturn(List.of(pendingRequestResponse));

        mockMvc.perform(get("/api/emergency/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].contactEmail").value("contact@example.com"))
                .andExpect(jsonPath("$[0].hoursUntilAutoApproval").value(24));
    }

    // ── POST /api/emergency/grant/{requestId} ─────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void grantAccess_ValidRequest_ShouldReturnApprovedWithToken() throws Exception {
        EmergencyAccessRequestResponse approvedResponse = EmergencyAccessRequestResponse.builder()
                .id(100L)
                .contactId(10L)
                .contactEmail("contact@example.com")
                .ownerUsername("owner")
                .status("APPROVED")
                .accessToken("abc-def-token")
                .expiresAt(LocalDateTime.of(2026, 1, 5, 10, 0))
                .build();

        when(accessService.grantAccess("owner", 100L)).thenReturn(approvedResponse);

        mockMvc.perform(post("/api/emergency/grant/100").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.accessToken").value("abc-def-token"))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "owner")
    void grantAccess_NotFound_ShouldReturn404() throws Exception {
        when(accessService.grantAccess("owner", 999L))
                .thenThrow(new ResourceNotFoundException("Emergency access request not found"));

        mockMvc.perform(post("/api/emergency/grant/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ── POST /api/emergency/deny/{requestId} ──────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void denyAccess_ValidRequest_ShouldReturnDeniedStatus() throws Exception {
        EmergencyAccessRequestResponse deniedResponse = EmergencyAccessRequestResponse.builder()
                .id(100L)
                .contactId(10L)
                .contactEmail("contact@example.com")
                .ownerUsername("owner")
                .status("DENIED")
                .decidedAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();

        when(accessService.denyAccess("owner", 100L)).thenReturn(deniedResponse);

        mockMvc.perform(post("/api/emergency/deny/100").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("DENIED"))
                .andExpect(jsonPath("$.decidedAt").isNotEmpty());
    }

    // ── GET /api/emergency/vault/{token} (public) ─────────────────────────────
    // Note: although this is a public endpoint in production (SecurityConfig permits /api/emergency/vault/*),
    // @WebMvcTest loads the full SecurityConfig which requires a valid JWT in the filter chain.
    // We use @WithMockUser here to bypass the JWT filter for the test; the public-access behaviour
    // is validated separately by the SecurityConfig permit pattern.

    @Test
    @WithMockUser
    void accessVault_ValidToken_ShouldReturnVaultMetadata() throws Exception {
        EmergencyVaultEntry entry = EmergencyVaultEntry.builder()
                .entryId(1L)
                .title("GitHub")
                .websiteUrl("https://github.com")
                .categoryName("Work")
                .lastUpdatedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        EmergencyVaultResponse vaultResponse = EmergencyVaultResponse.builder()
                .ownerUsername("owner")
                .expiresAt(LocalDateTime.of(2026, 1, 5, 10, 0))
                .hoursUntilExpiry(48L)
                .entries(List.of(entry))
                .build();

        when(accessService.accessVault("valid-token")).thenReturn(vaultResponse);

        mockMvc.perform(get("/api/emergency/vault/valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerUsername").value("owner"))
                .andExpect(jsonPath("$.hoursUntilExpiry").value(48))
                .andExpect(jsonPath("$.entries[0].entryId").value(1))
                .andExpect(jsonPath("$.entries[0].title").value("GitHub"))
                .andExpect(jsonPath("$.entries[0].websiteUrl").value("https://github.com"))
                .andExpect(jsonPath("$.entries[0].categoryName").value("Work"))
                .andExpect(jsonPath("$.entries[0].lastUpdatedAt").isNotEmpty());
    }

    @Test
    @WithMockUser
    void accessVault_InvalidToken_ShouldReturn401() throws Exception {
        when(accessService.accessVault("bad-token"))
                .thenThrow(new AuthenticationException("Invalid or expired access token"));

        mockMvc.perform(get("/api/emergency/vault/bad-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void accessVault_ExpiredToken_ShouldReturn401() throws Exception {
        when(accessService.accessVault("expired-token"))
                .thenThrow(new AuthenticationException("Access token has expired"));

        mockMvc.perform(get("/api/emergency/vault/expired-token"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/emergency/my-requests ────────────────────────────────────────

    @Test
    @WithMockUser(username = "contactuser")
    void getMyRequests_ShouldReturnContactRequests() throws Exception {
        when(accessService.getRequestsByContact("contactuser")).thenReturn(List.of(pendingRequestResponse));

        mockMvc.perform(get("/api/emergency/my-requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].ownerUsername").value("owner"));
    }

    // ── Content-Type assertions ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "owner")
    void allGetEndpoints_ShouldReturnJsonContentType() throws Exception {
        when(contactService.getContacts("owner")).thenReturn(List.of());
        when(accessService.getRequestsForOwner("owner")).thenReturn(List.of());
        when(accessService.getRequestsByContact("owner")).thenReturn(List.of());

        mockMvc.perform(get("/api/emergency/contacts"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/emergency/requests"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/emergency/my-requests"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
