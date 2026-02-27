package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.ImportRequest;
import com.revature.passwordmanager.dto.request.ThirdPartyImportRequest;
import com.revature.passwordmanager.dto.response.ExportResponse;
import com.revature.passwordmanager.dto.response.ImportResult;
import com.revature.passwordmanager.service.backup.ExportService;
import com.revature.passwordmanager.service.backup.ImportService;
import com.revature.passwordmanager.service.backup.ThirdPartyImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
public class BackupController {

  private final ExportService exportService;
  private final ImportService importService;
  private final ThirdPartyImportService thirdPartyImportService;

  @GetMapping("/export")
  public ResponseEntity<ExportResponse> exportVault(
      @RequestParam(defaultValue = "JSON") String format) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(exportService.exportVault(username, format));
  }

  @PostMapping("/import")
  public ResponseEntity<ImportResult> importVault(@RequestBody ImportRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(importService.importVault(username, request));
  }

  @PostMapping("/import/external")
  public ResponseEntity<ImportResult> importFromExternal(@RequestBody ThirdPartyImportRequest request) {
    String username = getCurrentUsername();
    return ResponseEntity.ok(thirdPartyImportService.importFromThirdParty(username, request));
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
