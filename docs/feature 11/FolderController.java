package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.FolderDTO;
import com.revature.passwordmanager.service.vault.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

  private final FolderService folderService;

  @GetMapping
  public ResponseEntity<List<FolderDTO>> getFolders() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.getFolders(username));
  }

  @GetMapping("/{id}")
  public ResponseEntity<FolderDTO> getFolderById(@PathVariable Long id) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.getFolderById(id, username));
  }

  @PostMapping
  public ResponseEntity<FolderDTO> createFolder(
      @RequestParam String name,
      @RequestParam(required = false) Long parentFolderId) {
    String username = getCurrentUsername();
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(folderService.createFolder(name, parentFolderId, username));
  }

  @PutMapping("/{id}")
  public ResponseEntity<FolderDTO> updateFolder(
      @PathVariable Long id,
      @RequestParam String name) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.updateFolder(id, name, username));
  }

  @PutMapping("/{id}/move")
  public ResponseEntity<FolderDTO> moveFolder(
      @PathVariable Long id,
      @RequestParam(required = false) Long parentId) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(folderService.moveFolder(id, parentId, username));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
    String username = getCurrentUsername();
    folderService.deleteFolder(id, username);
    return ResponseEntity.noContent().build();
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
