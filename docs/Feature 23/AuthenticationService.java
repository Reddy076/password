package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.config.JwtConfig;
import com.revature.passwordmanager.dto.SecurityQuestionDTO;
import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.request.RefreshTokenRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.email.EmailService;
import com.revature.passwordmanager.service.security.AuditLogService;
import com.revature.passwordmanager.service.security.SecurityAlertService;
import com.revature.passwordmanager.model.security.SecurityAlert.AlertType;
import com.revature.passwordmanager.model.security.SecurityAlert.Severity;
import com.revature.passwordmanager.service.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.revature.passwordmanager.model.security.AuditLog.AuditAction.LOGIN;
import static com.revature.passwordmanager.model.security.AuditLog.AuditAction.LOGIN_FAILED;
import static com.revature.passwordmanager.model.security.AuditLog.AuditAction.LOGOUT;

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

  private final EmailService emailService;
  private final OtpService otpService;
  private final AuditLogService auditLogService;
  private final LoginAttemptService loginAttemptService;
  private final SecurityAlertService securityAlertService;

  @Transactional
  public void sendOtp(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    // Generate OTP (Type: EMAIL_LOGIN or similar, or generic "EMAIL")
    String otpCode = otpService.generateOtp(user, "EMAIL");

    // Send Email
    emailService.sendOtpEmail(user.getEmail(), otpCode);
  }

  public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
    // Check account lockout
    User userForLockCheck = userRepository.findByUsername(request.getUsername())
        .or(() -> userRepository.findByEmail(request.getUsername()))
        .orElse(null);
    if (userForLockCheck != null && userForLockCheck.getLockedUntil() != null
        && userForLockCheck.getLockedUntil().isAfter(LocalDateTime.now())) {
      String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
      String deviceInfo = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
      loginAttemptService.recordLoginAttempt(request.getUsername(), false, "Account locked", ip, deviceInfo);
      throw new AuthenticationException("Account is locked until " + userForLockCheck.getLockedUntil());
    }

    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getMasterPassword()));

      User user = userRepository.findByUsername(request.getUsername())
          .or(() -> userRepository.findByEmail(request.getUsername()))
          .orElseThrow(() -> new AuthenticationException("User not found"));

      // Reset failed attempts on successful login
      if (user.getFailedLoginAttempts() > 0) {
        userRepository.resetFailedLoginAttempts(user.getUsername());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
      }

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

      // Audit log: successful login
      String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
      String deviceInfo = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

      if (loginAttemptService.isNewDevice(user.getUsername(), deviceInfo)) {
        securityAlertService.createAlert(user.getUsername(),
            com.revature.passwordmanager.model.security.SecurityAlert.AlertType.NEW_DEVICE_LOGIN,
            "New Device Detected",
            "Login detected from a new device or browser: " + deviceInfo,
            com.revature.passwordmanager.model.security.SecurityAlert.Severity.MEDIUM);
      }

      auditLogService.logAction(user.getUsername(),
          LOGIN,
          "Successful login", ip);
      loginAttemptService.recordLoginAttempt(user.getUsername(), true, null, ip, deviceInfo);

      return AuthResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .username(user.getUsername())
          .expiresIn(jwtConfig.getAccessTokenExpiration())
          .requires2FA(false)
          .build();

    } catch (org.springframework.security.core.AuthenticationException e) {
      // Increment failed attempts and check for lockout
      if (userForLockCheck != null) {
        userRepository.incrementFailedLoginAttempts(userForLockCheck.getUsername());

        // Fetch user again to get true atomic count
        User updatedUser = userRepository.findById(userForLockCheck.getId()).orElse(userForLockCheck);
        int attempts = updatedUser.getFailedLoginAttempts();

        // Trigger alert on multiple failed logins
        if (attempts >= 3) {
          securityAlertService.createAlert(request.getUsername(),
              AlertType.MULTIPLE_FAILED_LOGINS,
              "Multiple Failed Login Attempts",
              attempts + " failed login attempts detected from IP: "
                  + (httpRequest != null ? httpRequest.getRemoteAddr() : "unknown"),
              Severity.HIGH);
        }

        if (attempts >= 5) {
          int lockoutCount = updatedUser.getLockoutCount() + 1;
          long lockMinutes = (long) Math.min(15 * Math.pow(2, lockoutCount - 1), 1440);
          updatedUser.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
          updatedUser.setLockoutCount(lockoutCount);
          updatedUser.setFailedLoginAttempts(0);

          // Trigger alert on account lockout
          securityAlertService.createAlert(request.getUsername(),
              AlertType.ACCOUNT_LOCKED,
              "Account Locked",
              "Account locked for " + lockMinutes + " minutes due to " + attempts + " failed login attempts",
              Severity.CRITICAL);

          userRepository.save(updatedUser);
        }
      }

      // Audit log: failed login
      String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
      String deviceInfo = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;
      auditLogService.logAction(request.getUsername(),
          LOGIN_FAILED,
          "Failed login attempt", ip);
      loginAttemptService.recordLoginAttempt(request.getUsername(), false, "Invalid credentials", ip, deviceInfo);
      throw new AuthenticationException("Invalid username or password");
    }
  }

  @Transactional
  public AuthResponse refreshToken(RefreshTokenRequest request,
      HttpServletRequest httpRequest) {
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
    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getMasterPasswordHash(), Collections.emptyList());

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

  public void logout(String authHeader, String username) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String accessToken = authHeader.substring(7);
      // Invalidate the session associated with this Access Token
      sessionService.terminateSessionByToken(accessToken);
    }
    // Audit log: logout
    if (username != null) {
      auditLogService.logAction(username,
          LOGOUT,
          "User logged out");
    }
  }

  @Transactional(readOnly = true)
  public List<SecurityQuestionDTO> getSecurityQuestions(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    return securityQuestionService
        .getSecurityQuestions(user).stream()
        .map(sq -> new SecurityQuestionDTO(sq.getQuestionText(), ""))
        .collect(Collectors.toList());
  }

  @Transactional
  public AuthResponse verifyOtp(String username, String code, HttpServletRequest httpRequest) {
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
    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
        user.getUsername(), user.getMasterPasswordHash(), Collections.emptyList());

    Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
        userDetails.getAuthorities());

    String accessToken = jwtTokenProvider.generateAccessToken(authentication);
    String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

    // Create session
    sessionService.createSession(user, accessToken, httpRequest);

    // Audit log: successful login via 2FA
    String ip = httpRequest != null ? httpRequest.getRemoteAddr() : null;
    String deviceInfo = httpRequest != null ? httpRequest.getHeader("User-Agent") : null;

    if (loginAttemptService.isNewDevice(user.getUsername(), deviceInfo)) {
      securityAlertService.createAlert(user.getUsername(),
          AlertType.NEW_DEVICE_LOGIN,
          "New Device Detected",
          "Login detected from a new device or browser: " + deviceInfo,
          Severity.MEDIUM);
    }

    auditLogService.logAction(user.getUsername(),
        LOGIN,
        "Successful login (2FA)", ip);
    loginAttemptService.recordLoginAttempt(user.getUsername(), true, null, ip, deviceInfo);

    return AuthResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .username(user.getUsername())
        .expiresIn(jwtConfig.getAccessTokenExpiration())
        .requires2FA(false)
        .build();
  }
}
