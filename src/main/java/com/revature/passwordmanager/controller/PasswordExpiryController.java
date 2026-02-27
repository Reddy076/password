package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.ExpiryPolicyRequest;
import com.revature.passwordmanager.dto.response.ExpiryPolicyResponse;
import com.revature.passwordmanager.dto.response.ExpiryStatusResponse;
import com.revature.passwordmanager.dto.response.ExpiryStatusResponse.EntryExpiryDetail;
import com.revature.passwordmanager.service.expiry.PasswordExpiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Feature 38 – Password Expiration Tracker.
 *
 * <p>Base URL: {@code /api/expiry}</p>
 *
 * <p>All endpoints require authentication. The authenticated user's vault is
 * always scoped — no cross-user access is possible.</p>
 */
@RestController
@RequestMapping("/api/expiry")
@RequiredArgsConstructor
@Tag(name = "Password Expiry", description = "Feature 38 – Track password age and manage expiration policies")
public class PasswordExpiryController {

    private final PasswordExpiryService expiryService;

    // ── Status endpoints ──────────────────────────────────────────────────────

    @Operation(
        summary = "Get all password expiry statuses",
        description = "Returns the expiry state (FRESH / AGING / EXPIRING_SOON / EXPIRED) " +
                      "for every active vault entry, along with summary counts."
    )
    @GetMapping("/status")
    public ResponseEntity<ExpiryStatusResponse> getAllStatuses() {
        return ResponseEntity.ok(expiryService.getAllStatuses(getCurrentUsername()));
    }

    @Operation(
        summary = "Get passwords expiring soon",
        description = "Returns only vault entries whose passwords will expire within the next N days " +
                      "(default 7 if ?days is omitted). Ordered by expiry date ascending."
    )
    @GetMapping("/expiring-soon")
    public ResponseEntity<ExpiryStatusResponse> getExpiringSoon(
            @Parameter(description = "Look-ahead window in days (default 7)")
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(expiryService.getExpiringSoon(getCurrentUsername(), days));
    }

    // ── Policy endpoints ──────────────────────────────────────────────────────

    @Operation(
        summary = "Get current expiry policy",
        description = "Returns the user's expiry policy settings. A default policy " +
                      "(90-day expiry, 7-day reminder) is created automatically if none exists."
    )
    @GetMapping("/policy")
    public ResponseEntity<ExpiryPolicyResponse> getPolicy() {
        return ResponseEntity.ok(expiryService.getPolicy(getCurrentUsername()));
    }

    @Operation(
        summary = "Update expiry policy",
        description = "Updates the user's expiry policy. Only non-null fields are applied. " +
                      "All existing expiry statuses are recomputed immediately after the update."
    )
    @PutMapping("/policy")
    public ResponseEntity<ExpiryPolicyResponse> updatePolicy(
            @Valid @RequestBody ExpiryPolicyRequest request) {
        return ResponseEntity.ok(expiryService.updatePolicy(getCurrentUsername(), request));
    }

    // ── Snooze endpoint ───────────────────────────────────────────────────────

    @Operation(
        summary = "Snooze expiry reminder for a vault entry",
        description = "Suppresses the expiry reminder for the specified vault entry for N days " +
                      "(default 7). The reminder will resume after the snooze period expires."
    )
    @PostMapping("/snooze/{entryId}")
    public ResponseEntity<EntryExpiryDetail> snoozeReminder(
            @Parameter(description = "Vault entry id")
            @PathVariable Long entryId,
            @Parameter(description = "Number of days to snooze (default 7)")
            @RequestParam(required = false) Integer days) {
        return ResponseEntity.ok(expiryService.snoozeReminder(getCurrentUsername(), entryId, days));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
