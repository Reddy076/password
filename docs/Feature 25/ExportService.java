package com.revature.passwordmanager.service.backup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.revature.passwordmanager.dto.response.ExportResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.backup.BackupExport;
import com.revature.passwordmanager.model.backup.BackupExport.ExportFormat;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.BackupExportRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExportService {

  private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

  private final VaultEntryRepository vaultEntryRepository;
  private final UserRepository userRepository;
  private final BackupExportRepository backupExportRepository;

  @Transactional
  public ExportResponse exportVault(String username, String format) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());
    ExportFormat exportFormat = ExportFormat.valueOf(format.toUpperCase());

    String data;
    switch (exportFormat) {
      case JSON:
        data = exportAsJson(entries);
        break;
      case CSV:
        data = exportAsCsv(entries);
        break;
      default:
        data = exportAsJson(entries);
    }

    BackupExport export = BackupExport.builder()
        .user(user)
        .exportFormat(exportFormat)
        .fileName("vault_export_" + System.currentTimeMillis() + "." + format.toLowerCase())
        .entryCount(entries.size())
        .isEncrypted(false)
        .createdAt(LocalDateTime.now())
        .build();
    backupExportRepository.save(export);

    return ExportResponse.builder()
        .fileName(export.getFileName())
        .format(format.toUpperCase())
        .entryCount(entries.size())
        .encrypted(false)
        .data(data)
        .exportedAt(export.getCreatedAt())
        .build();
  }

  private String exportAsJson(List<VaultEntry> entries) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      mapper.enable(SerializationFeature.INDENT_OUTPUT);
      List<Map<String, Object>> exportData = entries.stream()
          .map(this::entryToMap)
          .collect(Collectors.toList());
      return mapper.writeValueAsString(exportData);
    } catch (Exception e) {
      logger.error("Failed to export as JSON: {}", e.getMessage());
      throw new RuntimeException("Export failed", e);
    }
  }

  private String exportAsCsv(List<VaultEntry> entries) {
    StringBuilder sb = new StringBuilder();
    sb.append("title,username,website_url,notes,is_favorite\n");
    for (VaultEntry entry : entries) {
      sb.append(escapeCsv(entry.getTitle())).append(",");
      sb.append(escapeCsv(entry.getUsername())).append(",");
      sb.append(escapeCsv(entry.getWebsiteUrl())).append(",");
      sb.append(escapeCsv(entry.getNotes())).append(",");
      sb.append(entry.getIsFavorite()).append("\n");
    }
    return sb.toString();
  }

  private Map<String, Object> entryToMap(VaultEntry entry) {
    Map<String, Object> map = new HashMap<>();
    map.put("title", entry.getTitle());
    map.put("username", entry.getUsername());
    map.put("websiteUrl", entry.getWebsiteUrl());
    map.put("notes", entry.getNotes());
    map.put("isFavorite", entry.getIsFavorite());
    map.put("createdAt", entry.getCreatedAt() != null ? entry.getCreatedAt().toString() : null);
    return map;
  }

  private String escapeCsv(String value) {
    if (value == null)
      return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
