package com.revature.passwordmanager.service.security;

import com.revature.passwordmanager.dto.response.SecurityAuditResponse;
import com.revature.passwordmanager.dto.response.SecurityAuditResponse.VaultEntrySummary;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.util.PasswordStrengthCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SecurityAuditService {

  private static final Logger logger = LoggerFactory.getLogger(SecurityAuditService.class);
  private static final int OLD_PASSWORD_DAYS = 90;

  private final VaultEntryRepository vaultEntryRepository;
  private final UserRepository userRepository;
  private final EncryptionService encryptionService;
  private final PasswordStrengthCalculator passwordStrengthCalculator;

  @Transactional(readOnly = true)
  public SecurityAuditResponse generateAuditReport(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());

    SecretKey key = encryptionService.decodeKey(user.getSalt());

    List<VaultEntrySummary> weakPasswords = findWeakPasswords(entries, key);
    List<VaultEntrySummary> reusedPasswords = findReusedPasswords(entries, key);
    List<VaultEntrySummary> oldPasswords = findOldPasswords(entries);

    int securityScore = calculateSecurityScore(entries.size(),
        weakPasswords.size(), reusedPasswords.size(), oldPasswords.size());

    List<String> recommendations = generateRecommendations(
        weakPasswords.size(), reusedPasswords.size(), oldPasswords.size());

    return SecurityAuditResponse.builder()
        .totalEntries(entries.size())
        .weakCount(weakPasswords.size())
        .reusedCount(reusedPasswords.size())
        .oldCount(oldPasswords.size())
        .securityScore(securityScore)
        .recommendations(recommendations)
        .weakPasswords(weakPasswords)
        .reusedPasswords(reusedPasswords)
        .oldPasswords(oldPasswords)
        .build();
  }

  private List<VaultEntrySummary> findWeakPasswords(List<VaultEntry> entries, SecretKey key) {
    List<VaultEntrySummary> weak = new ArrayList<>();
    for (VaultEntry entry : entries) {
      try {
        String decrypted = encryptionService.decrypt(entry.getPassword(), key);
        int score = passwordStrengthCalculator.calculateScore(decrypted);
        if (score < 60) {
          weak.add(VaultEntrySummary.builder()
              .id(entry.getId())
              .title(entry.getTitle())
              .websiteUrl(entry.getWebsiteUrl())
              .issue("Weak password (score: " + score + "/100)")
              .build());
        }
      } catch (Exception e) {
        logger.warn("Could not analyze password for entry {}: {}", entry.getId(), e.getMessage());
      }
    }
    return weak;
  }

  private List<VaultEntrySummary> findReusedPasswords(List<VaultEntry> entries, SecretKey key) {
    Map<String, List<VaultEntry>> passwordGroups = new HashMap<>();
    for (VaultEntry entry : entries) {
      try {
        String decrypted = encryptionService.decrypt(entry.getPassword(), key);
        passwordGroups.computeIfAbsent(decrypted, k -> new ArrayList<>()).add(entry);
      } catch (Exception e) {
        logger.warn("Could not decrypt password for entry {}: {}", entry.getId(), e.getMessage());
      }
    }

    List<VaultEntrySummary> reused = new ArrayList<>();
    for (Map.Entry<String, List<VaultEntry>> group : passwordGroups.entrySet()) {
      if (group.getValue().size() > 1) {
        for (VaultEntry entry : group.getValue()) {
          reused.add(VaultEntrySummary.builder()
              .id(entry.getId())
              .title(entry.getTitle())
              .websiteUrl(entry.getWebsiteUrl())
              .issue("Password reused across " + group.getValue().size() + " entries")
              .build());
        }
      }
    }
    return reused;
  }

  private List<VaultEntrySummary> findOldPasswords(List<VaultEntry> entries) {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(OLD_PASSWORD_DAYS);
    return entries.stream()
        .filter(e -> e.getUpdatedAt() != null && e.getUpdatedAt().isBefore(cutoff))
        .map(entry -> VaultEntrySummary.builder()
            .id(entry.getId())
            .title(entry.getTitle())
            .websiteUrl(entry.getWebsiteUrl())
            .issue("Password not updated in over " + OLD_PASSWORD_DAYS + " days")
            .build())
        .collect(Collectors.toList());
  }

  private int calculateSecurityScore(int total, int weak, int reused, int old) {
    if (total == 0)
      return 100;
    int issues = weak + reused + old;
    double ratio = 1.0 - ((double) issues / (total * 3));
    return Math.max(0, Math.min(100, (int) (ratio * 100)));
  }

  private List<String> generateRecommendations(int weak, int reused, int old) {
    List<String> recommendations = new ArrayList<>();
    if (weak > 0) {
      recommendations.add("Update " + weak + " weak password(s) with stronger alternatives");
    }
    if (reused > 0) {
      recommendations.add("Change " + reused + " reused password(s) to unique values");
    }
    if (old > 0) {
      recommendations
          .add("Rotate " + old + " password(s) that haven't been updated in " + OLD_PASSWORD_DAYS + "+ days");
    }
    if (recommendations.isEmpty()) {
      recommendations.add("Your vault security is excellent! Keep up the good practices.");
    }
    return recommendations;
  }
}
