package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.SessionResponse;
import com.revature.passwordmanager.service.auth.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

  private final SessionService sessionService;

  @GetMapping
  public ResponseEntity<List<SessionResponse>> getActiveSessions(@AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(sessionService.getUserSessions(userDetails.getUsername()));
  }

  @GetMapping("/current")
  public ResponseEntity<SessionResponse> getCurrentSession(@RequestHeader("Authorization") String authHeader) {
    if (authHeader != null && authHeader.startsWith("Bearer ")) {
      String token = authHeader.substring(7);
      return ResponseEntity.ok(sessionService.getCurrentSession(token));
    }
    return ResponseEntity.notFound().build();
  }

  @DeleteMapping("/{sessionId}")
  public ResponseEntity<Void> terminateSession(
      @PathVariable Long sessionId,
      @AuthenticationPrincipal UserDetails userDetails) {
    sessionService.terminateSession(sessionId, userDetails.getUsername());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> terminateAllSessions(@AuthenticationPrincipal UserDetails userDetails) {
    sessionService.terminateAllUserSessions(userDetails.getUsername());
    // Note: This will also kill the current session, so the user will be logged out
    // immediately
    return ResponseEntity.noContent().build();
  }
}
