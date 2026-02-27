package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.PasswordGeneratorRequest;
import com.revature.passwordmanager.dto.response.PasswordStrengthResponse;
import com.revature.passwordmanager.service.security.PasswordGeneratorService;
import com.revature.passwordmanager.service.security.PasswordStrengthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/generator")
@RequiredArgsConstructor
public class PasswordGeneratorController {

  private final PasswordGeneratorService generatorService;
  private final PasswordStrengthService strengthService;

  @PostMapping("/generate")
  public ResponseEntity<Map<String, String>> generatePassword(@RequestBody PasswordGeneratorRequest request) {
    String password = generatorService.generatePassword(request);
    return ResponseEntity.ok(Map.of("password", password));
  }

  @PostMapping("/strength")
  public ResponseEntity<PasswordStrengthResponse> checkStrength(@RequestBody Map<String, String> request) {
    String password = request.get("password");
    PasswordStrengthResponse response = strengthService.analyzePassword(password);
    return ResponseEntity.ok(response);
  }
}
