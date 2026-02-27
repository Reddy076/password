package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.AutofillSuggestionRequest;
import com.revature.passwordmanager.dto.request.AutofillUsageRequest;
import com.revature.passwordmanager.dto.response.AutofillSuggestionResponse;
import com.revature.passwordmanager.dto.response.MessageResponse;
import com.revature.passwordmanager.service.autofill.AutofillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feature 36 – Smart Password Autofill (Backend API).
 *
 * <p>Base URL: {@code /api/autofill}</p>
 *
 * <p>All endpoints require authentication. This API is designed to be called
 * by a browser extension that stores the user's JWT token securely.
 * <strong>Passwords are NEVER returned</strong> — only vault entry metadata.</p>
 */
@RestController
@RequestMapping("/api/autofill")
@RequiredArgsConstructor
@Tag(name = "Autofill", description = "Feature 36 – Smart Password Autofill backend API for browser extensions")
public class AutofillController {

    private final AutofillService autofillService;

    @Operation(
        summary = "Get autofill suggestions for a URL",
        description = "Returns vault entries that match the domain of the given URL. " +
                      "Results are sorted by match quality: EXACT > SUBDOMAIN > PARTIAL. " +
                      "Highly sensitive entries are excluded. " +
                      "PASSWORDS ARE NOT INCLUDED — use POST /api/vault/entries/{id}/view-password to fetch the password."
    )
    @PostMapping("/suggestions")
    public ResponseEntity<AutofillSuggestionResponse> getSuggestions(
            @Valid @RequestBody AutofillSuggestionRequest request) {
        return ResponseEntity.ok(autofillService.getSuggestions(getCurrentUsername(), request));
    }

    @Operation(
        summary = "Get trusted domains",
        description = "Returns the list of domains the user has previously used autofill on, " +
                      "ordered by most recently used. Used by the extension to show quick-access sites."
    )
    @GetMapping("/trusted-domains")
    public ResponseEntity<List<String>> getTrustedDomains() {
        return ResponseEntity.ok(autofillService.getTrustedDomains(getCurrentUsername()));
    }

    @Operation(
        summary = "Log autofill usage",
        description = "Records that autofill was used (or suggested) for a domain. " +
                      "Called by the extension after the user selects a credential or dismisses the suggestion."
    )
    @PostMapping("/log-usage")
    public ResponseEntity<MessageResponse> logUsage(@Valid @RequestBody AutofillUsageRequest request) {
        autofillService.logUsage(
                getCurrentUsername(),
                request.getUrl(),
                request.getVaultEntryId(),
                request.isApplied());
        return ResponseEntity.ok(new MessageResponse("Usage logged successfully"));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
