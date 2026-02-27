package com.revature.passwordmanager.service.security;

import com.revature.passwordmanager.dto.response.LoginHistoryResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.security.LoginAttempt;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.LoginAttemptRepository;
import com.revature.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

  private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);

  private final LoginAttemptRepository loginAttemptRepository;
  private final UserRepository userRepository;

  @Transactional
  public void recordLoginAttempt(String username, boolean successful, String failureReason,
      String ipAddress, String deviceInfo) {
    try {
      User user = userRepository.findByUsername(username)
          .or(() -> userRepository.findByEmail(username))
          .orElse(null);

      LoginAttempt attempt = LoginAttempt.builder()
          .user(user)
          .usernameAttempted(username)
          .ipAddress(ipAddress)
          .deviceInfo(deviceInfo)
          .successful(successful)
          .failureReason(failureReason)
          .timestamp(LocalDateTime.now())
          .build();

      loginAttemptRepository.save(attempt);
      logger.debug("Login attempt recorded: {} - {}", username, successful ? "SUCCESS" : "FAILED");
    } catch (Exception e) {
      logger.error("Failed to record login attempt: {}", e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public List<LoginHistoryResponse> getLoginHistory(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<LoginAttempt> attempts = loginAttemptRepository
        .findByUserIdOrderByTimestampDesc(user.getId());

    return attempts.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public long getRecentFailedAttempts(String username, int minutes) {
    LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
    return loginAttemptRepository
        .countByUsernameAttemptedAndSuccessfulFalseAndTimestampAfter(username, since);
  }

  private LoginHistoryResponse mapToResponse(LoginAttempt attempt) {
    return LoginHistoryResponse.builder()
        .id(attempt.getId())
        .ipAddress(attempt.getIpAddress())
        .deviceInfo(attempt.getDeviceInfo())
        .location(attempt.getLocation())
        .successful(attempt.isSuccessful())
        .failureReason(attempt.getFailureReason())
        .timestamp(attempt.getTimestamp())
        .build();
  }
}
