package com.revature.passwordmanager.scheduler;

import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest;
import com.revature.passwordmanager.model.emergency.EmergencyAccessRequest.AccessStatus;
import com.revature.passwordmanager.model.emergency.EmergencyContact;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.EmergencyAccessRequestRepository;
import com.revature.passwordmanager.service.emergency.EmergencyAccessService;
import com.revature.passwordmanager.service.emergency.WaitingPeriodManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 */
@ExtendWith(MockitoExtension.class)
class EmergencyAccessSchedulerTest {

    @Mock private EmergencyAccessRequestRepository requestRepository;
    @Mock private EmergencyAccessService accessService;
    @Mock private WaitingPeriodManager waitingPeriodManager;

    @InjectMocks
    private EmergencyAccessScheduler scheduler;

    private User owner;
    private EmergencyContact contact;
    private EmergencyAccessRequest pendingRequest;
    private EmergencyAccessRequest approvedRequest;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .username("owner")
                .email("owner@example.com")
                .masterPasswordHash("hash")
                .salt("salt")
                .build();

        contact = EmergencyContact.builder()
                .id(10L)
                .user(owner)
                .contactEmail("contact@example.com")
                .contactName("John Doe")
                .waitingPeriodHours(48)
                .active(true)
                .build();

        pendingRequest = EmergencyAccessRequest.builder()
                .id(100L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.PENDING)
                .waitingPeriodHours(48)
                .waitingPeriodEndsAt(LocalDateTime.now().minusHours(1))
                .build();

        approvedRequest = EmergencyAccessRequest.builder()
                .id(101L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.APPROVED)
                .accessToken("some-token")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void processEmergencyAccessRequests_PendingReadyForApproval_ShouldAutoApprove() {
        when(requestRepository.findPendingRequestsReadyForApproval(any())).thenReturn(List.of(pendingRequest));
        when(requestRepository.findApprovedRequestsReadyForExpiry(any())).thenReturn(List.of());
        when(accessService.approveRequest(pendingRequest)).thenReturn(null);

        scheduler.processEmergencyAccessRequests();

        verify(accessService).approveRequest(pendingRequest);
    }

    @Test
    void processEmergencyAccessRequests_ApprovedExpired_ShouldMarkExpired() {
        when(requestRepository.findPendingRequestsReadyForApproval(any())).thenReturn(List.of());
        when(requestRepository.findApprovedRequestsReadyForExpiry(any())).thenReturn(List.of(approvedRequest));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.processEmergencyAccessRequests();

        verify(requestRepository).save(argThat(r -> r.getStatus() == AccessStatus.EXPIRED));
    }

    @Test
    void processEmergencyAccessRequests_NothingToDo_ShouldCompleteWithoutErrors() {
        when(requestRepository.findPendingRequestsReadyForApproval(any())).thenReturn(List.of());
        when(requestRepository.findApprovedRequestsReadyForExpiry(any())).thenReturn(List.of());

        scheduler.processEmergencyAccessRequests();

        verify(accessService, never()).approveRequest(any());
        verify(requestRepository, never()).save(any());
    }

    @Test
    void processEmergencyAccessRequests_ApprovalError_ShouldContinueProcessingOthers() {
        EmergencyAccessRequest request2 = EmergencyAccessRequest.builder()
                .id(102L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.PENDING)
                .waitingPeriodHours(48)
                .waitingPeriodEndsAt(LocalDateTime.now().minusHours(1))
                .build();

        when(requestRepository.findPendingRequestsReadyForApproval(any()))
                .thenReturn(List.of(pendingRequest, request2));
        when(requestRepository.findApprovedRequestsReadyForExpiry(any())).thenReturn(List.of());
        when(accessService.approveRequest(pendingRequest))
                .thenThrow(new RuntimeException("DB error"));
        when(accessService.approveRequest(request2)).thenReturn(null);

        // Should not throw
        scheduler.processEmergencyAccessRequests();

        // Second request should still be processed
        verify(accessService).approveRequest(request2);
    }

    @Test
    void processEmergencyAccessRequests_ExpiryError_ShouldContinueProcessingOthers() {
        EmergencyAccessRequest request2 = EmergencyAccessRequest.builder()
                .id(103L)
                .contact(contact)
                .user(owner)
                .status(AccessStatus.APPROVED)
                .accessToken("token2")
                .expiresAt(LocalDateTime.now().minusHours(1))
                .build();

        when(requestRepository.findPendingRequestsReadyForApproval(any())).thenReturn(List.of());
        when(requestRepository.findApprovedRequestsReadyForExpiry(any()))
                .thenReturn(List.of(approvedRequest, request2));
        when(requestRepository.save(approvedRequest))
                .thenThrow(new RuntimeException("DB error"));
        when(requestRepository.save(request2)).thenReturn(request2);

        // Should not throw
        scheduler.processEmergencyAccessRequests();

        // Second request should still be processed
        verify(requestRepository).save(request2);
    }

    @Test
    void processEmergencyAccessRequests_BothPendingAndExpired_ShouldProcessBoth() {
        when(requestRepository.findPendingRequestsReadyForApproval(any())).thenReturn(List.of(pendingRequest));
        when(requestRepository.findApprovedRequestsReadyForExpiry(any())).thenReturn(List.of(approvedRequest));
        when(accessService.approveRequest(pendingRequest)).thenReturn(null);
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        scheduler.processEmergencyAccessRequests();

        verify(accessService).approveRequest(pendingRequest);
        verify(requestRepository).save(argThat(r -> r.getStatus() == AccessStatus.EXPIRED));
    }
}
