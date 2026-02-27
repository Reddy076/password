package com.revature.passwordmanager.service.backup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.ImportRequest;
import com.revature.passwordmanager.dto.response.ImportResult;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.security.EncryptionService;
import com.revature.passwordmanager.service.vault.VaultService;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImportService {

  private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

  private final VaultEntryRepository vaultEntryRepository;
  private final UserRepository userRepository;
  private final EncryptionService encryptionService;
  private final VaultService vaultService;

  @Transactional
  public ImportResult importVault(String username, ImportRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    SecretKey key = encryptionService.decodeKey(user.getSalt());

    switch (request.getFormat().toUpperCase()) {
      case "JSON":
        return importJson(user, request.getData(), key);
      case "CSV":
        return importCsv(user, request.getData(), key);
      default:
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Unsupported format: " + request.getFormat())
            .build();
    }
  }

  @Transactional(readOnly = true)
  public ImportResult validateImport(String username, ImportRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    SecretKey key = encryptionService.decodeKey(user.getSalt());

    switch (request.getFormat().toUpperCase()) {
      case "JSON":
        return validateJson(user, request.getData(), key);
      case "CSV":
        return validateCsv(user, request.getData(), key);
      default:
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Unsupported format: " + request.getFormat())
            .build();
    }
  }

  private ImportResult validateJson(User user, String data, SecretKey key) {
    int success = 0; 
    int fail = 0;
    try {
      ObjectMapper mapper = new ObjectMapper();
      List<Map<String, Object>> entries = mapper.readValue(data, new TypeReference<>() {
      });
      for (Map<String, Object> entryMap : entries) {
        try {
          if (getString(entryMap, "title").isEmpty() && getString(entryMap, "websiteUrl").isEmpty()) {
            fail++;
            continue;
          }
          success++;
        } catch (Exception e) {
          fail++;
        }
      }
    } catch (Exception e) {
      return ImportResult.builder().totalProcessed(0).successCount(0).failCount(1).message("Invalid JSON format")
          .build();
    }
    return ImportResult.builder().totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Validation passed").build();
  }

  private ImportResult validateCsv(User user, String data, SecretKey key) {
    int success = 0, fail = 0;
    try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
      String headerLine = reader.readLine();
      if (headerLine == null)
        return ImportResult.builder().totalProcessed(0).successCount(0).failCount(0).message("Empty CSV").build();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.trim().isEmpty()) {
          continue;
        }
        success++;
      }
    } catch (Exception e) {
      return ImportResult.builder().totalProcessed(0).successCount(0).failCount(1).message("CSV parsing error").build();
    }
    return ImportResult.builder().totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Validation passed").build();
  }

  private ImportResult importJson(User user, String data, SecretKey key) {
    int success = 0, fail = 0;
    java.util.List<VaultEntry> entriesToSave = new java.util.ArrayList<>();
    try {
      ObjectMapper mapper = new ObjectMapper();
      List<Map<String, Object>> entries = mapper.readValue(data, new TypeReference<>() {
      });
      for (Map<String, Object> entryMap : entries) {
        try {
          String password = entryMap.get("password") != null ? entryMap.get("password").toString() : "";
          VaultEntry entry = VaultEntry.builder()
              .user(user)
              .title(getString(entryMap, "title"))
              .username(getString(entryMap, "username"))
              .password(encryptionService.encrypt(password, key))
              .websiteUrl(getString(entryMap, "websiteUrl"))
              .notes(getString(entryMap, "notes"))
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();
          entriesToSave.add(entry);
          success++;
        } catch (Exception e) {
          logger.warn("Failed to import entry: {}", e.getMessage());
          fail++;
        }
      }
      vaultService.bulkInsert(user.getUsername(), entriesToSave);
    } catch (Exception e) {
      logger.error("Failed to parse JSON import: {}", e.getMessage());
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(1)
          .message("Invalid JSON format")
          .build();
    }
    return ImportResult.builder()
        .totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Import completed")
        .build();
  }

  private ImportResult importCsv(User user, String data, SecretKey key) {
    int success = 0, fail = 0;
    java.util.List<VaultEntry> entriesToSave = new java.util.ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(data))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Empty CSV").build();
      }
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          String[] parts = line.split(",", -1);
          VaultEntry entry = VaultEntry.builder()
              .user(user)
              .title(parts.length > 0 ? parts[0].trim() : "")
              .username(parts.length > 1 ? parts[1].trim() : "")
              .password(parts.length > 2 ? encryptionService.encrypt(parts[2].trim(), key) : "")
              .websiteUrl(parts.length > 3 ? parts[3].trim() : "")
              .notes(parts.length > 4 ? parts[4].trim() : "")
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();
          entriesToSave.add(entry);
          success++;
        } catch (Exception e) {
          logger.warn("Failed to import CSV line: {}", e.getMessage());
          fail++;
        }
      }
      vaultService.bulkInsert(user.getUsername(), entriesToSave);
    } catch (Exception e) {
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(1)
          .message("CSV parsing error").build();
    }
    return ImportResult.builder()
        .totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Import completed").build();
  }

  private String getString(Map<String, Object> map, String key) {
    Object value = map.get(key);
    return value != null ? value.toString() : "";
  }
}
