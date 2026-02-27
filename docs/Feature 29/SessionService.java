package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.response.SessionResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.user.UserSession;
import com.revature.passwordmanager.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.revature.passwordmanager.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

  private final UserSessionRepository userSessionRepository;
  private final UserRepository userRepository;

  @Transactional
  public void createSession(User user, String token, HttpServletRequest request, String location,
      String deviceFingerprint) {
    log.info("Creating new session for user: {}", user.getUsername());

    String ipAddress = request.getRemoteAddr();
    String userAgent = request.getHeader("User-Agent");

    UserSession session = UserSession.builder()
        .user(user)
        .token(token)
        .ipAddress(ipAddress)
        .deviceInfo(userAgent != null ? userAgent : "Unknown Device")
        .deviceFingerprint(deviceFingerprint)
        .location(location)
        .isActive(true)
        .lastAccessedAt(LocalDateTime.now())
        // Set expiry to match JWT expiry or longer - let's say 7 days for refresh
        // reference
        .expiresAt(LocalDateTime.now().plusDays(7))
        .build();

    userSessionRepository.save(session);
    log.info("Session created successfully for user: {}", user.getUsername());
  }

  @Transactional(readOnly = true)
  public boolean isSessionActive(String token) {
    return userSessionRepository.findByToken(token)
        .map(UserSession::isActive)
        .orElse(false);
  }

  @Transactional
  public void terminateSession(Long sessionId, String username) {
    log.info("Terminating session {} for user {}", sessionId, username);
    UserSession session = userSessionRepository.findById(sessionId)
        .orElseThrow(() -> new ResourceNotFoundException("Session", "id", sessionId));

    if (!session.getUser().getUsername().equals(username)) {
      log.warn("User {} attempted to terminate session {} belonging to another user", username, sessionId);
      throw new ResourceNotFoundException("Session", "id", sessionId); // Don't reveal existence
    }

    session.setActive(false);
    userSessionRepository.save(session);
    log.info("Session {} terminated successfully", sessionId);
  }

  @Transactional
  public void terminateSessionByToken(String token) {
    userSessionRepository.findByToken(token).ifPresent(session -> {
      session.setActive(false);
      userSessionRepository.save(session);
      log.info("Session terminated by token for user {}", session.getUser().getUsername());
    });
  }

  @Transactional
  public void terminateAllUserSessions(String username) {
    log.info("Terminating all sessions for user {}", username);
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    List<UserSession> activeSessions = userSessionRepository.findByUserIdAndIsActiveTrue(user.getId());
    for (UserSession session : activeSessions) {
      session.setActive(false);
    }
    userSessionRepository.saveAll(activeSessions);
    log.info("Terminated {} sessions for user {}", activeSessions.size(), username);
  }

  @Transactional(readOnly = true)
  public List<SessionResponse> getUserSessions(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    return userSessionRepository.findByUserIdAndIsActiveTrue(user.getId()).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public SessionResponse getCurrentSession(String token) {
    return userSessionRepository.findByToken(token)
        .map(this::mapToResponse)
        .orElseThrow(() -> new ResourceNotFoundException("Session", "token", "current"));
  }

  private SessionResponse mapToResponse(UserSession session) {
    return SessionResponse.builder()
        .id(session.getId())
        .ipAddress(session.getIpAddress())
        .deviceInfo(session.getDeviceInfo())
        .location(session.getLocation())
        .isActive(session.isActive())
        .createdAt(session.getCreatedAt())
        .lastAccessedAt(session.getLastAccessedAt())
        .expiresAt(session.getExpiresAt())
        .build();
  }
}
