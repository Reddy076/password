package com.revature.passwordmanager.service.emergency;

import com.revature.passwordmanager.dto.request.EmergencyAccessRequestDto;
import com.revature.passwordmanager.dto.response.EmergencyAccessRequestResponse;
import com.revature.passwordmanager.dto.response.EmergencyVaultResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest;
import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest.AccessStatus;
import com.revature.passwordmanager.model.emergency.EmergencyContact;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.EmergencyAccessRequestRepository;
import com.revature.passwordmanager.repository.EmergencyContactRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 */
@ExtendWith(MockitoExtension.class)
class EmergencyAccessServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmergencyContactRepository contactRepository;
    @Mock private EmergencyAccessRequestRepository requestRepository;
    @Mock private VaultEntryRepository vaultEntryRepository;
    @Mock private WaitingPeriodManager waitingPeriodManager;
    @Mock private EmergencyNotificationService notificationService;

    @InjectMocks
    private EmergencyAccessService accessService;

    private User owner;
    private User contactUser;
    private EmergencyContact contact;
    private EmergencyAccessRequest pendingRequest;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .username("owner")
                .email("owner@example.com")
                .masterPasswordHash("hash")
                .salt("salt")
                .build();

        contactUser = User.builder()
                .id(2L)
                .username("contactuser")
                .email("contact@example.com")
                .masterPasswordHash("hash2")
                .salt("salt2")
                .build();

        contact = EmergencyContact.builder()
                .id(10L)
                .user(owner)
                .contactEmail("contact@example.com")
                .contactName("John Doe")
                .relationship("Spouse")
                .waitingPeriodHours(48)
                .verified(false)
                .active(true)
                .build();

        pendingRequest = EmergencyAccessRequest.builder()
                .id(100L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.PENDING)
                .waitingPeriodHours(48)
                .waitingPeriodEndsAt(LocalDateTime.now().plusHours(24))
                .requestedAt(LocalDateTime.now().minusHours(24))
                .build();
    }

    // ── requestAccess ─────────────────────────────────────────────────────────

    @Test
    void requestAccess_ValidContact_ShouldCreateRequest() {
        EmergencyAccessRequestDto dto = EmergencyAccessRequestDto.builder()
                .ownerUsername("owner")
                .requestMessage("Emergency situation")
                .build();

        when(userRepository.findByUsernameOrThrow("contactuser")).thenReturn(contactUser);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(contactRepository.findByContactEmailAndActiveTrue("contact@example.com"))
                .thenReturn(List.of(contact));
        when(requestRepository.existsByContactIdAndUserIdAndStatus(10L, 1L, AccessStatus.PENDING))
                .thenReturn(false);
        when(waitingPeriodManager.computeWaitingPeriodEnd(any(), eq(48)))
                .thenReturn(LocalDateTime.now().plusHours(48));
        when(requestRepository.save(any(EmergencyAccessRequest.class))).thenReturn(pendingRequest);
        when(waitingPeriodManager.hoursUntilAutoApproval(any(), any())).thenReturn(24L);

        EmergencyAccessRequestResponse response = accessService.requestAccess("contactuser", dto);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getContactEmail()).isEqualTo("contact@example.com");
        assertThat(response.getOwnerUsername()).isEqualTo("owner");
        verify(notificationService).notifyOwnerOfAccessRequest(any());
    }

    @Test
    void requestAccess_NotAContact_ShouldThrowIllegalArgumentException() {
        EmergencyAccessRequestDto dto = EmergencyAccessRequestDto.builder()
                .ownerUsername("owner")
                .build();

        when(userRepository.findByUsernameOrThrow("contactuser")).thenReturn(contactUser);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(contactRepository.findByContactEmailAndActiveTrue("contact@example.com"))
                .thenReturn(List.of()); // not a contact

        assertThatThrownBy(() -> accessService.requestAccess("contactuser", dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not listed as an emergency contact");
    }

    @Test
    void requestAccess_DuplicatePending_ShouldThrowIllegalStateException() {
        EmergencyAccessRequestDto dto = EmergencyAccessRequestDto.builder()
                .ownerUsername("owner")
                .build();

        when(userRepository.findByUsernameOrThrow("contactuser")).thenReturn(contactUser);
        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(contactRepository.findByContactEmailAndActiveTrue("contact@example.com"))
                .thenReturn(List.of(contact));
        when(requestRepository.existsByContactIdAndUserIdAndStatus(10L, 1L, AccessStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> accessService.requestAccess("contactuser", dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending emergency access request already exists");
    }

    // ── grantAccess ───────────────────────────────────────────────────────────

    @Test
    void grantAccess_PendingRequest_ShouldApproveAndReturnToken() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(requestRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(pendingRequest));
        when(waitingPeriodManager.computeTokenExpiry(any())).thenReturn(LocalDateTime.now().plusHours(48));
        when(requestRepository.save(any())).thenAnswer(inv -> {
            EmergencyAccessRequest r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });
        when(waitingPeriodManager.hoursUntilAutoApproval(any(), any())).thenReturn(0L);

        EmergencyAccessRequestResponse response = accessService.grantAccess("owner", 100L);

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getAccessToken()).isNotNull();
        verify(notificationService).notifyOwnerOfAutoApproval(any());
    }

    @Test
    void grantAccess_NotPending_ShouldThrowIllegalStateException() {
        pendingRequest.setStatus(AccessStatus.DENIED);

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(requestRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> accessService.grantAccess("owner", 100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in PENDING state");
    }

    @Test
    void grantAccess_NotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(requestRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.grantAccess("owner", 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── denyAccess ────────────────────────────────────────────────────────────

    @Test
    void denyAccess_PendingRequest_ShouldDenyAndNotify() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(requestRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(pendingRequest));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(waitingPeriodManager.hoursUntilAutoApproval(any(), any())).thenReturn(24L);

        EmergencyAccessRequestResponse response = accessService.denyAccess("owner", 100L);

        assertThat(response.getStatus()).isEqualTo("DENIED");
        verify(notificationService).notifyOwnerOfDenial(any());
    }

    @Test
    void denyAccess_NotPending_ShouldThrowIllegalStateException() {
        pendingRequest.setStatus(AccessStatus.APPROVED);

        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(requestRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> accessService.denyAccess("owner", 100L))
                .isInstanceOf(IllegalStateException.class);
    }

    // ── accessVault ───────────────────────────────────────────────────────────

    @Test
    void accessVault_ValidToken_ShouldReturnVaultMetadata() {
        EmergencyAccessRequest approvedRequest = EmergencyAccessRequest.builder()
                .id(100L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.APPROVED)
                .accessToken("valid-token")
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        VaultEntry entry = VaultEntry.builder()
                .id(1L)
                .user(owner)
                .title("GitHub")
                .username("enc_user")
                .password("enc_pass")
                .websiteUrl("https://github.com")
                .isDeleted(false)
                .updatedAt(LocalDateTime.now())
                .build();

        when(requestRepository.findByAccessToken("valid-token")).thenReturn(Optional.of(approvedRequest));
        when(waitingPeriodManager.isTokenValid(eq(approvedRequest), any())).thenReturn(true);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(entry));
        when(waitingPeriodManager.hoursUntilTokenExpiry(eq(approvedRequest), any())).thenReturn(24L);

        EmergencyVaultResponse response = accessService.accessVault("valid-token");

        assertThat(response.getOwnerUsername()).isEqualTo("owner");
        assertThat(response.getHoursUntilExpiry()).isEqualTo(24L);
        assertThat(response.getEntries()).hasSize(1);
        assertThat(response.getEntries().get(0).getTitle()).isEqualTo("GitHub");
        assertThat(response.getEntries().get(0).getWebsiteUrl()).isEqualTo("https://github.com");
    }

    @Test
    void accessVault_InvalidToken_ShouldThrowAuthenticationException() {
        when(requestRepository.findByAccessToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.accessVault("bad-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid or expired access token");
    }

    @Test
    void accessVault_NotApproved_ShouldThrowAuthenticationException() {
        EmergencyAccessRequest pendingReq = EmergencyAccessRequest.builder()
                .id(100L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.PENDING)
                .accessToken("pending-token")
                .build();

        when(requestRepository.findByAccessToken("pending-token")).thenReturn(Optional.of(pendingReq));

        assertThatThrownBy(() -> accessService.accessVault("pending-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Access has not been approved");
    }

    @Test
    void accessVault_ExpiredToken_ShouldThrowAuthenticationException() {
        EmergencyAccessRequest approvedRequest = EmergencyAccessRequest.builder()
                .id(100L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.APPROVED)
                .accessToken("expired-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(requestRepository.findByAccessToken("expired-token")).thenReturn(Optional.of(approvedRequest));
        when(waitingPeriodManager.isTokenValid(eq(approvedRequest), any())).thenReturn(false);

        assertThatThrownBy(() -> accessService.accessVault("expired-token"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Access token has expired");
    }

    // ── getRequestsForOwner ───────────────────────────────────────────────────

    @Test
    void getRequestsForOwner_ShouldReturnAllRequests() {
        when(userRepository.findByUsernameOrThrow("owner")).thenReturn(owner);
        when(requestRepository.findByUserIdOrderByRequestedAtDesc(1L)).thenReturn(List.of(pendingRequest));
        when(waitingPeriodManager.hoursUntilAutoApproval(any(), any())).thenReturn(24L);

        List<EmergencyAccessRequestResponse> responses = accessService.getRequestsForOwner("owner");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus()).isEqualTo("PENDING");
    }
}
