package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.service.auth.AuthenticationService;
import com.revature.passwordmanager.service.auth.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final RegistrationService registrationService;
  private final AuthenticationService authenticationService;
  private final com.revature.passwordmanager.service.auth.AccountRecoveryService accountRecoveryService;

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
    UserResponse response = registrationService.registerUser(request);
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    AuthResponse response = authenticationService.login(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<AuthResponse> refreshToken(
      @Valid @RequestBody com.revature.passwordmanager.dto.request.RefreshTokenRequest request,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    AuthResponse response = authenticationService.refreshToken(request, httpRequest);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
    authenticationService.logout(authHeader);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/security-questions/{username}")
  public ResponseEntity<java.util.List<com.revature.passwordmanager.dto.SecurityQuestionDTO>> getSecurityQuestions(
      @PathVariable String username) {
    return ResponseEntity.ok(authenticationService.getSecurityQuestions(username));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<String> resetPassword(
      @Valid @RequestBody com.revature.passwordmanager.dto.request.RecoveryRequest request) {
    accountRecoveryService.resetPassword(request);
    return ResponseEntity.ok("Password changed successfully");
  }

  @PostMapping("/verify-otp")
  public ResponseEntity<AuthResponse> verifyOtp(
      @RequestParam String username,
      @RequestParam String code,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    return ResponseEntity.ok(authenticationService.verifyOtp(username, code, httpRequest));
  }

  @PostMapping("/send-otp")
  public ResponseEntity<String> sendOtp(@RequestParam String username) {
    authenticationService.sendOtp(username);
    return ResponseEntity.ok("OTP sent to your email.");
  }
}
