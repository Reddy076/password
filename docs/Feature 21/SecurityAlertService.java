package com.revature.passwordmanager.service.security;

import com.revature.passwordmanager.dto.SecurityAlertDTO;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.security.SecurityAlert;
import com.revature.passwordmanager.model.security.SecurityAlert.AlertType;
import com.revature.passwordmanager.model.security.SecurityAlert.Severity;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.SecurityAlertRepository;
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
public class SecurityAlertService {

  private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);

  private final SecurityAlertRepository securityAlertRepository;
  private final UserRepository userRepository;

  @Transactional
  public void createAlert(String username, AlertType alertType, String title,
      String message, Severity severity) {
    try {
      User user = userRepository.findByUsername(username).orElse(null);
      if (user == null) {
        logger.warn("Cannot create alert for unknown user: {}", username);
        return;
      }

      SecurityAlert alert = SecurityAlert.builder()
          .user(user)
          .alertType(alertType)
          .title(title)
          .message(message)
          .severity(severity)
          .isRead(false)
          .createdAt(LocalDateTime.now())
          .build();

      securityAlertRepository.save(alert);
      logger.debug("Security alert created: {} - {} for user {}", alertType, title, username);
    } catch (Exception e) {
      logger.error("Failed to create security alert: {}", e.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public List<SecurityAlertDTO> getAlerts(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    return securityAlertRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
        .stream()
        .map(this::mapToDTO)
        .collect(Collectors.toList());
  }

  @Transactional
  public void markAsRead(String username, Long alertId) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    SecurityAlert alert = securityAlertRepository.findById(alertId)
        .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

    if (!alert.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Alert does not belong to user");
    }

    alert.setRead(true);
    securityAlertRepository.save(alert);
  }

  @Transactional
  public void deleteAlert(String username, Long alertId) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    SecurityAlert alert = securityAlertRepository.findById(alertId)
        .orElseThrow(() -> new ResourceNotFoundException("Alert not found"));

    if (!alert.getUser().getId().equals(user.getId())) {
      throw new IllegalArgumentException("Alert does not belong to user");
    }

    securityAlertRepository.delete(alert);
  }

  private SecurityAlertDTO mapToDTO(SecurityAlert alert) {
    return SecurityAlertDTO.builder()
        .id(alert.getId())
        .alertType(alert.getAlertType().name())
        .title(alert.getTitle())
        .message(alert.getMessage())
        .severity(alert.getSeverity().name())
        .isRead(alert.isRead())
        .createdAt(alert.getCreatedAt())
        .build();
  }
}
