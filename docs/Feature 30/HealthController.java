package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.response.HealthResponse;
import com.revature.passwordmanager.service.system.HealthService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

  private final HealthService healthService;

  @GetMapping
  public ResponseEntity<HealthResponse> getHealth() {
    return ResponseEntity.ok(healthService.getHealthStatus());
  }
}
