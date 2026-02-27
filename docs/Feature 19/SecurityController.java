package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.AuditLogResponse;
import com.revature.passwordmanager.service.security.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

  private final AuditLogService auditLogService;

  @GetMapping("/audit-logs")
  public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(auditLogService.getAuditLogs(username));
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
