package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.VaultEntryRequest;
import com.revature.passwordmanager.dto.response.SnapshotResponse;
import com.revature.passwordmanager.dto.response.TrashEntryResponse;
import com.revature.passwordmanager.dto.response.VaultEntryDetailResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.service.vault.VaultService;
import com.revature.passwordmanager.service.vault.VaultSnapshotService;
import com.revature.passwordmanager.service.vault.VaultTrashService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vault")
@RequiredArgsConstructor
public class VaultController {

  private final VaultService vaultService;
  private final VaultTrashService vaultTrashService;
  private final VaultSnapshotService vaultSnapshotService;

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

  @GetMapping("/search")
  public ResponseEntity<List<VaultEntryResponse>> searchEntries(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long folderId,
      @RequestParam(required = false) Boolean isFavorite,
      @RequestParam(required = false) Boolean isHighlySensitive,
      @RequestParam(defaultValue = "title") String sortBy,
      @RequestParam(defaultValue = "asc") String sortDir) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.searchEntries(
        username, keyword, categoryId, folderId, isFavorite, isHighlySensitive, sortBy, sortDir));
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

  @PutMapping("/{id}/favorite")
  public ResponseEntity<VaultEntryResponse> toggleFavorite(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.toggleFavorite(username, id));
  }

  @GetMapping("/favorites")
  public ResponseEntity<List<VaultEntryResponse>> getFavorites() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.getFavorites(username));
  }

  @PostMapping("/entries/bulk-delete")
  public ResponseEntity<Void> bulkDelete(@RequestBody List<Long> ids) {
    String username = getCurrentUsername();
    vaultService.bulkDelete(username, ids);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/entries/{id}/view-password")
  public ResponseEntity<Map<String, String>> viewPassword(@PathVariable Long id) {
    String username = getCurrentUsername();
    String password = vaultService.getPassword(username, id);
    return ResponseEntity.ok(Map.of("password", password));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
    String username = getCurrentUsername();
    vaultService.deleteEntry(username, id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/sensitive-view")
  public ResponseEntity<VaultEntryDetailResponse> accessSensitiveEntry(
      @PathVariable Long id,
      @RequestBody @jakarta.validation.Valid com.revature.passwordmanager.dto.request.SensitiveAccessRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultService.accessSensitiveEntry(username, id, request));
  }

  // ==================== Snapshot / History Endpoints ====================

  @GetMapping("/entries/{id}/history")
  public ResponseEntity<List<SnapshotResponse>> getPasswordHistory(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultSnapshotService.getHistory(username, id));
  }

  // ==================== Trash Endpoints ====================

  @GetMapping("/trash")
  public ResponseEntity<List<TrashEntryResponse>> getTrashEntries() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultTrashService.getTrashEntries(username));
  }

  @GetMapping("/trash/count")
  public ResponseEntity<Map<String, Long>> getTrashCount() {
    String username = getCurrentUsername();
    long count = vaultTrashService.getTrashCount(username);
    return ResponseEntity.ok(Map.of("count", count));
  }

  @PostMapping("/trash/{id}/restore")
  public ResponseEntity<TrashEntryResponse> restoreEntry(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(vaultTrashService.restoreEntry(username, id));
  }

  @PostMapping("/trash/restore-all")
  public ResponseEntity<Void> restoreAll() {
    String username = getCurrentUsername();
    vaultTrashService.restoreAll(username);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/trash/{id}")
  public ResponseEntity<Void> permanentDelete(@PathVariable Long id) {
    String username = getCurrentUsername();
    vaultTrashService.permanentDelete(username, id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/trash/empty")
  public ResponseEntity<Void> emptyTrash() {
    String username = getCurrentUsername();
    vaultTrashService.emptyTrash(username);
    return ResponseEntity.noContent().build();
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
