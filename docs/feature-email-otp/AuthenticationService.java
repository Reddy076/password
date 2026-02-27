package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.config.JwtConfig;
import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
// UserResponse import removed as it is no longer used here
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final JwtConfig jwtConfig;
  private final UserRepository userRepository;
  private final SessionService sessionService;
  private final SecurityQuestionService securityQuestionService;
  private final TwoFactorService twoFactorService;
  private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  private final com.revature.passwordmanager.service.email.EmailService emailService;
  private final OtpService otpService;

  @org.springframework.transaction.annotation.Transactional
  public void sendOtp(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    // Generate OTP (Type: EMAIL_LOGIN or similar, or generic "EMAIL")
    String otpCode = otpService.generateOtp(user, "EMAIL");

    // Send Email
    emailService.sendOtpEmail(user.getEmail(), otpCode);
  }

  public AuthResponse login(LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getMasterPassword()));

      User user = userRepository.findByUsername(request.getUsername())
          .or(() -> userRepository.findByEmail(request.getUsername()))
          .orElseThrow(() -> new AuthenticationException("User not found"));

      // Check if 2FA is enabled
      if (user.is2faEnabled()) {
        return AuthResponse.builder()
            .requires2FA(true)
            .username(user.getUsername())
            .build();
      }

      String accessToken = jwtTokenProvider.generateAccessToken(authentication);
      String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

      // Create session
      sessionService.createSession(user, accessToken, httpRequest);

      return AuthResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .username(user.getUsername())
          .expiresIn(jwtConfig.getAccessTokenExpiration())
          .requires2FA(false)
          .build();

    } catch (org.springframework.security.core.AuthenticationException e) {
      throw new AuthenticationException("Invalid username or password");
    }
  }

  @org.springframework.transaction.annotation.Transactional
  public AuthResponse refreshToken(com.revature.passwordmanager.dto.request.RefreshTokenRequest request,
      jakarta.servlet.http.HttpServletRequest httpRequest) {
    String refreshToken = request.getRefreshToken();

    // 1. Validate the refresh token strictly
    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new AuthenticationException("Invalid or expired refresh token");
    }

    // 2. Check if the session associated with this token is active
    // NOTE: This assumes we stored the Refresh Token in the session, or the Access
    // Token?
    // Current SessionService implementation uses Access Token.
    // If we want to support Refresh Token Rotation, we must solve the binding
    // issue.
    // For Feature 6.5 MVP: We rely on the Refresh Token validity itself + stateless
    // check.
    // We cannot easily check "isSessionActive" for the Refresh Token unless we
    // change SessionService to store it.
    // Compromise: We skip session check for the Refresh Token for now,
    // OR we assume the client sends the *old* Access Token in the header? No,
    // endpoint is public usually.

    // Let's create a NEW session for the new Access Token.
    // The old session will expire naturally when its Access Token expires (15m).

    String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    // 3. Generate new tokens
    // We need an Authentication object. We can reconstruct it from the user.
    org.springframework.security.core.userdetails.UserDetails userDetails = new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getMasterPasswordHash(), java.util.Collections.emptyList());

    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
        userDetails.getAuthorities());

    String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    // 4. Create new Session
    sessionService.createSession(user, newAccessToken, httpRequest);

    return AuthResponse.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .username(user.getUsername())
        .expiresIn(jwtConfig.getAccessTokenExpiration())
        .requires2FA(user.is2faEnabled())
        .build();
  }

  public void logout(String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String accessToken = authHeader.substring(7);
      // Invalidate the session associated with this Access Token
      sessionService.terminateSessionByToken(accessToken);
    }
  }

  @org.springframework.transaction.annotation.Transactional(readOnly = true)
  public java.util.List<com.revature.passwordmanager.dto.SecurityQuestionDTO> getSecurityQuestions(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    return securityQuestionService
        .getSecurityQuestions(user).stream()
        .map(sq -> new com.revature.passwordmanager.dto.SecurityQuestionDTO(sq.getQuestionText(), ""))
        .collect(java.util.stream.Collectors.toList());
  }

  @org.springframework.transaction.annotation.Transactional
  public AuthResponse verifyOtp(String username, String code, jakarta.servlet.http.HttpServletRequest httpRequest) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    if (!user.is2faEnabled()) {
      throw new AuthenticationException("2FA is not enabled for this user");
    }

    if (!twoFactorService.verifyCode(user, code)) {
      throw new AuthenticationException("Invalid 2FA code");
    }

    // Generate tokens since 2FA passed
    org.springframework.security.core.userdetails.UserDetails userDetails = new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getMasterPasswordHash(), java.util.Collections.emptyList());

    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
        userDetails.getAuthorities());

    String accessToken = jwtTokenProvider.generateAccessToken(authentication);
    String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    // Create session
    sessionService.createSession(user, accessToken, httpRequest);

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .username(user.getUsername())
        .expiresIn(jwtConfig.getAccessTokenExpiration())
        .requires2FA(false)
        .build();
  }
}
