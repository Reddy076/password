package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.VaultEntryRequest;
import com.revature.passwordmanager.dto.response.VaultEntryDetailResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.service.vault.VaultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

  private final VaultService vaultService;

  @PostMapping
  public ResponseEntity<VaultEntryResponse> createEntry(@RequestBody VaultEntryRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(vaultService.createEntry(username, request));
  }

  @GetMapping
  public ResponseEntity<List<VaultEntryResponse>> getAllEntries() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getAllEntries(username));
  }

  @GetMapping("/{id}")
  public ResponseEntity<VaultEntryDetailResponse> getEntry(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getEntry(username, id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<VaultEntryResponse> updateEntry(
      @PathVariable Long id,
      @RequestBody VaultEntryRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.updateEntry(username, id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
    String username = getCurrentUsername();
    vaultService.deleteEntry(username, id);
    return ResponseEntity.noContent().build();
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
