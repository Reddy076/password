package com.revature.passwordmanager.service.backup;

import com.revature.passwordmanager.dto.request.ThirdPartyImportRequest;
import com.revature.passwordmanager.dto.response.ImportResult;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
public class ThirdPartyImportService {

  private static final Logger logger = LoggerFactory.getLogger(ThirdPartyImportService.class);

  private final VaultEntryRepository vaultEntryRepository;
  private final UserRepository userRepository;
  private final EncryptionService encryptionService;
  private final VaultService vaultService;

  @Transactional
  public ImportResult importFromThirdParty(String username, ThirdPartyImportRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    SecretKey key = encryptionService.decodeKey(user.getSalt());

    switch (request.getSource().toUpperCase()) {
      case "CHROME":
        return importFromChrome(user, request.getData(), key);
      case "LASTPASS":
        return importFromLastPass(user, request.getData(), key);
      default:
        return ImportResult.builder()
            .totalProcessed(0).successCount(0).failCount(0)
            .message("Unsupported source: " + request.getSource())
            .build();
    }
  }

  private ImportResult importFromChrome(User user, String csvData, SecretKey key) {
    // Chrome CSV format: name,url,username,password
    int success = 0, fail = 0;
    java.util.List<VaultEntry> entriesToSave = new java.util.ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
      reader.readLine(); // skip header
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          String[] parts = line.split(",", -1);
          VaultEntry entry = VaultEntry.builder()
              .user(user)
              .title(parts.length > 0 ? parts[0].trim() : "")
              .websiteUrl(parts.length > 1 ? parts[1].trim() : "")
              .username(parts.length > 2 ? parts[2].trim() : "")
              .password(parts.length > 3 ? encryptionService.encrypt(parts[3].trim(), key) : "")
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();
          entriesToSave.add(entry);
          success++;
        } catch (Exception e) {
          logger.warn("Failed to import Chrome entry: {}", e.getMessage());
          fail++;
        }
      }
      vaultService.bulkInsert(user.getUsername(), entriesToSave);
    } catch (Exception e) {
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(1)
          .message("Chrome CSV parsing error").build();
    }
    return ImportResult.builder()
        .totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("Chrome import completed").build();
  }

  private ImportResult importFromLastPass(User user, String csvData, SecretKey key) {
    // LastPass CSV format: url,username,password,totp,extra,name,grouping,fav
    int success = 0, fail = 0;
    java.util.List<VaultEntry> entriesToSave = new java.util.ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new StringReader(csvData))) {
      reader.readLine(); // skip header
      String line;
      while ((line = reader.readLine()) != null) {
        try {
          String[] parts = line.split(",", -1);
          VaultEntry entry = VaultEntry.builder()
              .user(user)
              .websiteUrl(parts.length > 0 ? parts[0].trim() : "")
              .username(parts.length > 1 ? parts[1].trim() : "")
              .password(parts.length > 2 ? encryptionService.encrypt(parts[2].trim(), key) : "")
              .notes(parts.length > 4 ? parts[4].trim() : "")
              .title(parts.length > 5 ? parts[5].trim() : "")
              .createdAt(LocalDateTime.now())
              .updatedAt(LocalDateTime.now())
              .build();
          entriesToSave.add(entry);
          success++;
        } catch (Exception e) {
          logger.warn("Failed to import LastPass entry: {}", e.getMessage());
          fail++;
        }
      }
      vaultService.bulkInsert(user.getUsername(), entriesToSave);
    } catch (Exception e) {
      return ImportResult.builder()
          .totalProcessed(0).successCount(0).failCount(1)
          .message("LastPass CSV parsing error").build();
    }
    return ImportResult.builder()
        .totalProcessed(success + fail).successCount(success).failCount(fail)
        .message("LastPass import completed").build();
  }
}
