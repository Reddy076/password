package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.TwoFactorSetupResponse;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.service.auth.TwoFactorService;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.exception.AuthenticationException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/2fa")
@RequiredArgsConstructor
@Tag(name = "Two-Factor Auth Controller", description = "Endpoints for managing 2FA")
public class TwoFactorController {

  private final TwoFactorService twoFactorService;
  private final SessionService sessionService;
  private final UserRepository userRepository;

  @GetMapping("/status")
  @Operation(summary = "Get 2FA status")
  public ResponseEntity<Map<String, Boolean>> getStatus(HttpServletRequest request) {
    User user = getUserBySession(request);
    return ResponseEntity.ok(Map.of("enabled", user.is2faEnabled()));
  }

  @PostMapping("/setup")
  @Operation(summary = "Initialize 2FA setup (get QR code)")
  public ResponseEntity<TwoFactorSetupResponse> setup2FA(HttpServletRequest request) {
    User user = getUserBySession(request);
    TwoFactorSetupResponse response = twoFactorService.setup2FA(user);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/verify-setup")
  @Operation(summary = "Verify and enable 2FA")
  public ResponseEntity<Map<String, Object>> verifySetup(
      @RequestParam String code,
      HttpServletRequest request) {
    User user = getUserBySession(request);
    twoFactorService.verifySetup(user, code);
    return ResponseEntity.ok(Map.of(
        "success", true,
        "message", "2FA enabled successfully",
        "backupCodes", twoFactorService.getRecoveryCodes(user)));
  }

  @PostMapping("/disable")
  @Operation(summary = "Disable 2FA")
  public ResponseEntity<Map<String, String>> disable2FA(HttpServletRequest request) {
    User user = getUserBySession(request);
    twoFactorService.disable2FA(user);
    return ResponseEntity.ok(Map.of("message", "2FA disabled successfully"));
  }

  @GetMapping("/backup-codes")
  @Operation(summary = "Get backup recovery codes")
  public ResponseEntity<List<String>> getBackupCodes(HttpServletRequest request) {
    User user = getUserBySession(request);
    return ResponseEntity.ok(twoFactorService.getRecoveryCodes(user));
  }

  private User getUserBySession(HttpServletRequest request) {
    // Extract token from header
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new AuthenticationException("Missing or invalid Authorization header");
    }
    String token = authHeader.substring(7);

    // We need to get the user from the session service or token
    // SessionService doesn't expose getUser directly by token easily in v1?
    // Let's use the repository and username from token if needed,
    // but ideally SessionService should handle this.
    // Assuming SessionService has a method or we can rely on security context if
    // integrated.
    // For now, let's assume we can get it from the session service or similar.
    // UPDATE: SessionService in this project seems to handle session validation.

    // Let's rely on SessionService to get the session, then the user.
    // But SessionService methods return void or SessionResponse.
    // Let's verify SessionService capabilities.

    // Placeholder: assuming we can get the Principal from SecurityContextHolder
    // which is set by the JwtAuthenticationFilter (if it exists and is working).
    // If not, we might need to manually validate.

    // Let's use SecurityContextHolder since we are using Spring Security
    org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication();

    if (authentication == null || !authentication.isAuthenticated()) {
      throw new AuthenticationException("User not authenticated");
    }

    Object principal = authentication.getPrincipal();
    String username;
    if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
      username = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
    } else {
      username = principal.toString();
    }

    String finalUsername = username;
    return userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(finalUsername))
        .orElseThrow(() -> new AuthenticationException("User not found"));
  }
}
