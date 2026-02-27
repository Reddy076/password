package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.SecurityQuestionDTO;
import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.request.RecoveryRequest;
import com.revature.passwordmanager.dto.request.RefreshTokenRequest;
import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.service.auth.AccountRecoveryService;
import com.revature.passwordmanager.service.auth.AuthenticationService;
import com.revature.passwordmanager.service.auth.RegistrationService;
import com.revature.passwordmanager.service.security.CaptchaService;
import com.revature.passwordmanager.service.security.DuressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final RegistrationService registrationService;
  private final AuthenticationService authenticationService;
  private final AccountRecoveryService accountRecoveryService;
  private final DuressService duressService;
  private final CaptchaService captchaService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
    UserResponse response = registrationService.registerUser(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {

    if (captchaService.isCaptchaRequired(request.getUsername())) {
      if (request.getCaptchaToken() == null || request.getCaptchaToken().isBlank()) {
        return ResponseEntity.badRequest().body(
            AuthResponse.builder().message("CAPTCHA verification required").build());
      }
      if (!captchaService.verifyCaptcha(request.getCaptchaToken())) {
        return ResponseEntity.badRequest().body(
            AuthResponse.builder().message("CAPTCHA verification failed").build());
      }
    }

    AuthResponse response = authenticationService.login(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<AuthResponse> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
    AuthResponse response = authenticationService.refreshToken(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    authenticationService.logout(authHeader, username);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/security-questions/{username}")
  public ResponseEntity<List<SecurityQuestionDTO>> getSecurityQuestions(
      @PathVariable String username) {
    return ResponseEntity.ok(authenticationService.getSecurityQuestions(username));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<String> resetPassword(
      @Valid @RequestBody RecoveryRequest request) {
    accountRecoveryService.resetPassword(request);
    return ResponseEntity.ok("Password changed successfully");
  }

  @PostMapping("/verify-otp")
  public ResponseEntity<AuthResponse> verifyOtp(
      @RequestParam String username,
      @RequestParam String code,
      HttpServletRequest httpRequest) {
    return ResponseEntity.ok(authenticationService.verifyOtp(username, code, httpRequest));
  }

  @PostMapping("/send-otp")
  public ResponseEntity<String> sendOtp(@RequestParam String username) {
    authenticationService.sendOtp(username);
    return ResponseEntity.ok("OTP sent to your email.");
  }

  @PostMapping("/duress-login")
  public ResponseEntity<AuthResponse> duressLogin(@Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest) {
    AuthResponse response = authenticationService.duressLogin(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/set-duress-password")
  public ResponseEntity<String> setDuressPassword(@RequestBody Map<String, String> request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    duressService.setDuressPassword(username, request.get("duressPassword"));
    return ResponseEntity.ok("Duress password set successfully");
  }

  @GetMapping("/password-hint/{username}")
  public ResponseEntity<Map<String, String>> getPasswordHint(@PathVariable String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    String hint = user.getPasswordHint() != null ? user.getPasswordHint() : "No hint set";
    return ResponseEntity.ok(Map.of("hint", hint));
  }

  @PutMapping("/password-hint")
  public ResponseEntity<?> setPasswordHint(@RequestBody Map<String, String> request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    String newHint = request.get("hint");
    String masterPassword = request.get("masterPassword");

    if (masterPassword == null || masterPassword.isBlank()) {
      return ResponseEntity.badRequest().body("Master password is required to set a hint");
    }

    if (!passwordEncoder.matches(masterPassword, user.getMasterPasswordHash())) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid master password");
    }

    if (newHint != null && !newHint.trim().isEmpty()) {
      if (newHint.toLowerCase().contains(masterPassword.toLowerCase())) {
        return ResponseEntity.badRequest().body("Password hint cannot contain the master password");
      }
    }

    user.setPasswordHint(newHint);
    userRepository.save(user);
    return ResponseEntity.ok("Password hint updated");
  }
}
