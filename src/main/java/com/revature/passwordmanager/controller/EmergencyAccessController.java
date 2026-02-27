package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.AddEmergencyContactRequest;
import com.revature.passwordmanager.dto.request.EmergencyAccessRequestDto;
import com.revature.passwordmanager.dto.response.EmergencyAccessRequestResponse;
import com.revature.passwordmanager.dto.response.EmergencyContactResponse;
import com.revature.passwordmanager.dto.response.EmergencyVaultResponse;
import com.revature.passwordmanager.dto.response.MessageResponse;
import com.revature.passwordmanager.service.emergency.EmergencyAccessService;
import com.revature.passwordmanager.service.emergency.EmergencyContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feature 39 – Emergency Access (Digital Legacy).
 *
 * <p>Base URL: {@code /api/emergency}</p>
 *
 * <p>All endpoints require authentication except {@code GET /api/emergency/vault/{token}}
 * which uses a one-time access token instead of a JWT.</p>
 */
@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
@Tag(name = "Emergency Access", description = "Feature 39 – Emergency Access (Digital Legacy): manage trusted contacts and vault access")
public class EmergencyAccessController {

    private final EmergencyContactService contactService;
    private final EmergencyAccessService accessService;

    // ── Contact management (owner) ────────────────────────────────────────────

    @Operation(
        summary = "Add an emergency contact",
        description = "Designates a trusted contact who can request emergency access to the vault. " +
                      "The contact is identified by email and does not need to be a registered user."
    )
    @PostMapping("/contacts")
    public ResponseEntity<EmergencyContactResponse> addContact(
            @Valid @RequestBody AddEmergencyContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contactService.addContact(getCurrentUsername(), request));
    }

    @Operation(summary = "List all active emergency contacts")
    @GetMapping("/contacts")
    public ResponseEntity<List<EmergencyContactResponse>> getContacts() {
        return ResponseEntity.ok(contactService.getContacts(getCurrentUsername()));
    }

    @Operation(summary = "Update an emergency contact's details")
    @PutMapping("/contacts/{contactId}")
    public ResponseEntity<EmergencyContactResponse> updateContact(
            @Parameter(description = "Emergency contact id")
            @PathVariable Long contactId,
            @Valid @RequestBody AddEmergencyContactRequest request) {
        return ResponseEntity.ok(contactService.updateContact(getCurrentUsername(), contactId, request));
    }

    @Operation(summary = "Remove an emergency contact")
    @DeleteMapping("/contacts/{contactId}")
    public ResponseEntity<MessageResponse> removeContact(
            @Parameter(description = "Emergency contact id")
            @PathVariable Long contactId) {
        contactService.removeContact(getCurrentUsername(), contactId);
        return ResponseEntity.ok(new MessageResponse("Emergency contact removed successfully"));
    }

    // ── Access requests (contact) ─────────────────────────────────────────────

    @Operation(
        summary = "Request emergency access to a vault owner's vault",
        description = "The authenticated user must be listed as an emergency contact for the specified owner. " +
                      "The owner will be notified and has the waiting period to deny the request."
    )
    @PostMapping("/request-access")
    public ResponseEntity<EmergencyAccessRequestResponse> requestAccess(
            @RequestBody EmergencyAccessRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accessService.requestAccess(getCurrentUsername(), request));
    }

    @Operation(summary = "List all emergency access requests made by the authenticated contact")
    @GetMapping("/my-requests")
    public ResponseEntity<List<EmergencyAccessRequestResponse>> getMyRequests() {
        return ResponseEntity.ok(accessService.getRequestsByContact(getCurrentUsername()));
    }

    // ── Request management (owner) ────────────────────────────────────────────

    @Operation(summary = "List all emergency access requests for the authenticated vault owner")
    @GetMapping("/requests")
    public ResponseEntity<List<EmergencyAccessRequestResponse>> getRequests() {
        return ResponseEntity.ok(accessService.getRequestsForOwner(getCurrentUsername()));
    }

    @Operation(
        summary = "Grant emergency access (owner explicitly approves before waiting period ends)",
        description = "Issues an access token immediately. The contact can then use it to view vault metadata."
    )
    @PostMapping("/grant/{requestId}")
    public ResponseEntity<EmergencyAccessRequestResponse> grantAccess(
            @Parameter(description = "Emergency access request id")
            @PathVariable Long requestId) {
        return ResponseEntity.ok(accessService.grantAccess(getCurrentUsername(), requestId));
    }

    @Operation(
        summary = "Deny an emergency access request",
        description = "Cancels the request. The contact will not be able to access the vault."
    )
    @PostMapping("/deny/{requestId}")
    public ResponseEntity<EmergencyAccessRequestResponse> denyAccess(
            @Parameter(description = "Emergency access request id")
            @PathVariable Long requestId) {
        return ResponseEntity.ok(accessService.denyAccess(getCurrentUsername(), requestId));
    }

    // ── Vault access (public — token-based) ───────────────────────────────────

    @Operation(
        summary = "Access vault using an emergency access token (no JWT required)",
        description = "Returns read-only vault metadata (entry titles, URLs, categories) for an approved request. " +
                      "Passwords are NOT included. The token expires 48 hours after approval."
    )
    @GetMapping("/vault/{token}")
    public ResponseEntity<EmergencyVaultResponse> accessVault(
            @Parameter(description = "Emergency access token from the approved request")
            @PathVariable String token) {
        return ResponseEntity.ok(accessService.accessVault(token));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
