package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.AuditLogResponse;
import com.revature.passwordmanager.dto.response.LoginHistoryResponse;
import com.revature.passwordmanager.service.security.AuditLogService;
import com.revature.passwordmanager.service.security.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

  private final AuditLogService auditLogService;
  private final LoginAttemptService loginAttemptService;

  @GetMapping("/audit-logs")
  public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(auditLogService.getAuditLogs(username));
  }

  @GetMapping("/login-history")
  public ResponseEntity<List<LoginHistoryResponse>> getLoginHistory() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(loginAttemptService.getLoginHistory(username));
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
