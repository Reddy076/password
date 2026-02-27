package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.AccountDeletionRequest;
import com.revature.passwordmanager.service.user.AccountDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final AccountDeletionService accountDeletionService;

  @DeleteMapping("/account")
  public ResponseEntity<String> deleteAccount(@Valid @RequestBody AccountDeletionRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    accountDeletionService.scheduleAccountDeletion(username, request);
    return ResponseEntity.ok(
        "Account scheduled for deletion in 30 days. You can cancel this action by logging in and using the cancel endpoint.");
  }

  @PostMapping("/account/cancel-deletion")
  public ResponseEntity<String> cancelDeletion() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    accountDeletionService.cancelAccountDeletion(username);
    return ResponseEntity.ok("Account deletion cancelled.");
  }
}
