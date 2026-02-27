package com.revature.passwordmanager.service.emergency;

import com.revature.passwordmanager.dto.request.EmergencyAccessRequestDto;
import com.revature.passwordmanager.dto.response.EmergencyAccessRequestResponse;
import com.revature.passwordmanager.dto.response.EmergencyVaultResponse;
import com.revature.passwordmanager.dto.response.EmergencyVaultResponse.EmergencyVaultEntry;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Handles the emergency access request lifecycle:
 * request → waiting period → approve/deny → vault access.</p>
 */
@Service
@RequiredArgsConstructor
public class EmergencyAccessService {

    private final UserRepository userRepository;
    private final EmergencyContactRepository contactRepository;
    private final EmergencyAccessRequestRepository requestRepository;
    private final VaultEntryRepository vaultEntryRepository;
    private final WaitingPeriodManager waitingPeriodManager;
    private final EmergencyNotificationService notificationService;

    // ── Request access ────────────────────────────────────────────────────────

    /**
     * Submits an emergency access request. The authenticated user must be listed
     * as an emergency contact for the specified vault owner.
     *
     * @param contactUsername the authenticated emergency contact's username
     * @param request         the request details (owner username + optional message)
     * @return the created request response
     */
    @Transactional
    public EmergencyAccessRequestResponse requestAccess(String contactUsername,
                                                         EmergencyAccessRequestDto request) {
        User contactUser = userRepository.findByUsernameOrThrow(contactUsername);
        User owner = userRepository.findByUsername(request.getOwnerUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Find the emergency contact record
        List<EmergencyContact> contacts = contactRepository
                .findByContactEmailAndActiveTrue(contactUser.getEmail());

        EmergencyContact contact = contacts.stream()
                .filter(c -> c.getUser().getId().equals(owner.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "You are not listed as an emergency contact for user '" +
                        request.getOwnerUsername() + "'"));

        // Prevent duplicate pending requests
        if (requestRepository.existsByContactIdAndUserIdAndStatus(
                contact.getId(), owner.getId(), AccessStatus.PENDING)) {
            throw new IllegalStateException(
                    "A pending emergency access request already exists for this user");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime waitingPeriodEndsAt = waitingPeriodManager
                .computeWaitingPeriodEnd(now, contact.getWaitingPeriodHours());

        EmergencyAccessRequest accessRequest = EmergencyAccessRequest.builder()
                .contact(contact)
                .user(owner)
                .status(AccessStatus.PENDING)
                .waitingPeriodHours(contact.getWaitingPeriodHours())
                .waitingPeriodEndsAt(waitingPeriodEndsAt)
                .requestMessage(request.getRequestMessage())
                .build();

        accessRequest = requestRepository.save(accessRequest);

        // Notify the vault owner
        notificationService.notifyOwnerOfAccessRequest(accessRequest);

        return mapToResponse(accessRequest, now);
    }

    // ── Owner: view pending requests ──────────────────────────────────────────

    /**
     * Returns all emergency access requests for the vault owner (all statuses).
     */
    @Transactional(readOnly = true)
    public List<EmergencyAccessRequestResponse> getRequestsForOwner(String ownerUsername) {
        User owner = userRepository.findByUsernameOrThrow(ownerUsername);
        LocalDateTime now = LocalDateTime.now();
        return requestRepository.findByUserIdOrderByRequestedAtDesc(owner.getId())
                .stream()
                .map(r -> mapToResponse(r, now))
                .collect(Collectors.toList());
    }

    // ── Owner: grant access ───────────────────────────────────────────────────

    /**
     * Owner explicitly grants access before the waiting period ends.
     *
     * @param ownerUsername the authenticated vault owner
     * @param requestId     the request to approve
     * @return the updated request response (with access token)
     */
    @Transactional
    public EmergencyAccessRequestResponse grantAccess(String ownerUsername, Long requestId) {
        User owner = userRepository.findByUsernameOrThrow(ownerUsername);
        EmergencyAccessRequest request = requestRepository.findByIdAndUserId(requestId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Emergency access request not found"));

        if (request.getStatus() != AccessStatus.PENDING) {
            throw new IllegalStateException(
                    "Request is not in PENDING state (current: " + request.getStatus() + ")");
        }

        return approveRequest(request);
    }

    // ── Owner: deny access ────────────────────────────────────────────────────

    /**
     * Owner denies an emergency access request.
     *
     * @param ownerUsername the authenticated vault owner
     * @param requestId     the request to deny
     * @return the updated request response
     */
    @Transactional
    public EmergencyAccessRequestResponse denyAccess(String ownerUsername, Long requestId) {
        User owner = userRepository.findByUsernameOrThrow(ownerUsername);
        EmergencyAccessRequest request = requestRepository.findByIdAndUserId(requestId, owner.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Emergency access request not found"));

        if (request.getStatus() != AccessStatus.PENDING) {
            throw new IllegalStateException(
                    "Request is not in PENDING state (current: " + request.getStatus() + ")");
        }

        request.setStatus(AccessStatus.DENIED);
        request.setDecidedAt(LocalDateTime.now());
        request = requestRepository.save(request);

        notificationService.notifyOwnerOfDenial(request);

        return mapToResponse(request, LocalDateTime.now());
    }

    // ── Contact: view own requests ────────────────────────────────────────────

    /**
     * Returns all requests made by the authenticated contact.
     */
    @Transactional(readOnly = true)
    public List<EmergencyAccessRequestResponse> getRequestsByContact(String contactUsername) {
        User contactUser = userRepository.findByUsernameOrThrow(contactUsername);
        List<EmergencyContact> contacts = contactRepository
                .findByContactEmailAndActiveTrue(contactUser.getEmail());

        LocalDateTime now = LocalDateTime.now();
        return contacts.stream()
                .flatMap(c -> requestRepository.findByContactIdOrderByRequestedAtDesc(c.getId()).stream())
                .map(r -> mapToResponse(r, now))
                .collect(Collectors.toList());
    }

    // ── Access vault ──────────────────────────────────────────────────────────

    /**
     * Returns the vault owner's vault metadata (no passwords) using a valid access token.
     * This endpoint is public — no JWT required, only the access token.
     *
     * @param accessToken the token from the approved request
     * @return read-only vault metadata
     */
    @Transactional(readOnly = true)
    public EmergencyVaultResponse accessVault(String accessToken) {
        EmergencyAccessRequest request = requestRepository.findByAccessToken(accessToken)
                .orElseThrow(() -> new AuthenticationException("Invalid or expired access token"));

        if (request.getStatus() != AccessStatus.APPROVED) {
            throw new AuthenticationException("Access has not been approved");
        }

        LocalDateTime now = LocalDateTime.now();
        if (!waitingPeriodManager.isTokenValid(request, now)) {
            throw new AuthenticationException("Access token has expired");
        }

        User owner = request.getUser();
        List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(owner.getId());

        List<EmergencyVaultEntry> vaultEntries = entries.stream()
                .map(e -> EmergencyVaultEntry.builder()
                        .entryId(e.getId())
                        .title(e.getTitle())
                        .websiteUrl(e.getWebsiteUrl())
                        .categoryName(e.getCategory() != null ? e.getCategory().getName() : null)
                        .folderName(e.getFolder() != null ? e.getFolder().getName() : null)
                        .lastUpdatedAt(e.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());

        return EmergencyVaultResponse.builder()
                .ownerUsername(owner.getUsername())
                .expiresAt(request.getExpiresAt())
                .hoursUntilExpiry(waitingPeriodManager.hoursUntilTokenExpiry(request, now))
                .entries(vaultEntries)
                .build();
    }

    // ── Internal: approve a request ───────────────────────────────────────────

    /**
     * Approves a request (called by owner grant or scheduler auto-approval).
     */
    @Transactional
    public EmergencyAccessRequestResponse approveRequest(EmergencyAccessRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        LocalDateTime expiresAt = waitingPeriodManager.computeTokenExpiry(now);

        request.setStatus(AccessStatus.APPROVED);
        request.setDecidedAt(now);
        request.setAccessToken(token);
        request.setExpiresAt(expiresAt);
        request = requestRepository.save(request);

        notificationService.notifyOwnerOfAutoApproval(request);

        return mapToResponse(request, now);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private EmergencyAccessRequestResponse mapToResponse(EmergencyAccessRequest request, LocalDateTime now) {
        EmergencyContact contact = request.getContact();
        long hoursUntilAutoApproval = waitingPeriodManager.hoursUntilAutoApproval(request, now);

        return EmergencyAccessRequestResponse.builder()
                .id(request.getId())
                .contactId(contact.getId())
                .contactEmail(contact.getContactEmail())
                .contactName(contact.getContactName())
                .ownerUsername(request.getUser().getUsername())
                .status(request.getStatus().name())
                .requestedAt(request.getRequestedAt())
                .waitingPeriodEndsAt(request.getWaitingPeriodEndsAt())
                .hoursUntilAutoApproval(hoursUntilAutoApproval)
                .decidedAt(request.getDecidedAt())
                .accessToken(request.getStatus() == AccessStatus.APPROVED ? request.getAccessToken() : null)
                .expiresAt(request.getExpiresAt())
                .requestMessage(request.getRequestMessage())
                .build();
    }
}
