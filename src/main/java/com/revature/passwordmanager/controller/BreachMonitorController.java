package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.BreachHistoryResponse;
import com.revature.passwordmanager.dto.response.BreachScanResponse;
import com.revature.passwordmanager.dto.response.BreachStatusResponse;
import com.revature.passwordmanager.dto.response.CompromisedCredentialResponse;
import com.revature.passwordmanager.model.security.breach.BreachScanRecord.TriggerType;
import com.revature.passwordmanager.service.security.breach.BreachMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
@Tag(name = "Breach Monitor", description = "Dark web and data breach monitoring for vault passwords")
public class BreachMonitorController {

    private final BreachMonitorService breachMonitorService;

    @Operation(summary = "Trigger a manual breach scan for the authenticated user's vault")
    @PostMapping("/breach-scan")
    public ResponseEntity<BreachScanResponse> triggerScan() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(breachMonitorService.runScan(username, TriggerType.MANUAL));
    }

    @Operation(summary = "Get the current breach status of the user's vault (SAFE / AT_RISK / COMPROMISED)")
    @GetMapping("/breach-status")
    public ResponseEntity<BreachStatusResponse> getBreachStatus() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(breachMonitorService.getStatus(username));
    }

    @Operation(summary = "List all currently compromised (unresolved) credentials")
    @GetMapping("/compromised-credentials")
    public ResponseEntity<List<CompromisedCredentialResponse>> getCompromisedCredentials() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(breachMonitorService.getCompromisedCredentials(username));
    }

    @Operation(summary = "Get historical breach scan records")
    @GetMapping("/breach-history")
    public ResponseEntity<BreachHistoryResponse> getBreachHistory() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(breachMonitorService.getBreachHistory(username));
    }

    @Operation(summary = "Mark a compromised credential as resolved (password updated)")
    @PutMapping("/compromised-credentials/{id}/resolve")
    public ResponseEntity<CompromisedCredentialResponse> resolveCredential(@PathVariable Long id) {
        String username = getCurrentUsername();
        return ResponseEntity.ok(breachMonitorService.resolveCredential(username, id));
    }

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
