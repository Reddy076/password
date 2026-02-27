package com.revature.passwordmanager.service.vault;

import com.revature.passwordmanager.dto.request.VaultEntryRequest;
import com.revature.passwordmanager.dto.response.VaultEntryDetailResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.Category;
import com.revature.passwordmanager.model.vault.Folder;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.CategoryRepository;
import com.revature.passwordmanager.repository.FolderRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.security.EncryptionService;
import com.revature.passwordmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultService {

  private final VaultEntryRepository vaultEntryRepository;
  private final UserRepository userRepository;
  private final CategoryRepository categoryRepository;
  private final FolderRepository folderRepository;
  private final EncryptionService encryptionService;
  // private final EncryptionUtil encryptionUtil; // Already there? No, wait.
  // The file has:
  // private final EncryptionService encryptionService;
  // private final EncryptionService encryptionService; // DUPLICATE
  // private final EncryptionUtil encryptionUtil;
  // private final com.revature.passwordmanager.service.auth.TwoFactorService
  // twoFactorService;

  // I will replace the whole block of fields and constructor generation is by
  // Lombok.

  private final EncryptionUtil encryptionUtil;
  private final com.revature.passwordmanager.service.auth.TwoFactorService twoFactorService;
  private final VaultSnapshotService vaultSnapshotService;

  @Transactional
  public VaultEntryResponse createEntry(String username, VaultEntryRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Category category = categoryRepository.findById(request.getCategoryId())
        .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

    Folder folder = null;
    if (request.getFolderId() != null) {
      folder = folderRepository.findById(request.getFolderId())
          .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
    }

    // Derive key for encryption
    SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    // Encrypt sensitive fields
    String encryptedPassword = encryptionService.encrypt(request.getPassword(), key);
    String encryptedUsername = encryptionService.encrypt(request.getUsername(), key);
    String encryptedNotes = request.getNotes() != null ? encryptionService.encrypt(request.getNotes(), key) : null;

    VaultEntry entry = VaultEntry.builder()
        .user(user)
        .category(category)
        .folder(folder)
        .title(request.getTitle())
        .username(encryptedUsername)
        .password(encryptedPassword)
        .websiteUrl(request.getWebsiteUrl())
        .notes(encryptedNotes)
        .isFavorite(request.getIsFavorite() != null ? request.getIsFavorite() : false)
        .isHighlySensitive(request.getIsHighlySensitive() != null ? request.getIsHighlySensitive() : false)
        .build();

    VaultEntry savedEntry = vaultEntryRepository.save(entry);
    return mapToResponse(savedEntry);
  }

  @Transactional(readOnly = true)
  public VaultEntryDetailResponse getEntry(String username, Long entryId) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    String decryptedPassword;
    String decryptedUsername;
    String decryptedNotes;

    if (Boolean.TRUE.equals(entry.getIsHighlySensitive())) {
      decryptedPassword = "******";
      decryptedUsername = "******";
      decryptedNotes = "******";
    } else {
      decryptedPassword = encryptionService.decrypt(entry.getPassword(), key);
      decryptedUsername = encryptionService.decrypt(entry.getUsername(), key);
      decryptedNotes = entry.getNotes() != null ? encryptionService.decrypt(entry.getNotes(), key) : null;
    }

    return mapToDetailResponse(entry, decryptedUsername, decryptedPassword, decryptedNotes);
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getAllEntries(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());
    return entries.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public VaultEntryResponse updateEntry(String username, Long entryId, VaultEntryRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());

    if (request.getTitle() != null)
      entry.setTitle(request.getTitle());
    if (request.getWebsiteUrl() != null)
      entry.setWebsiteUrl(request.getWebsiteUrl());
    if (request.getIsFavorite() != null)
      entry.setIsFavorite(request.getIsFavorite());
    if (request.getIsHighlySensitive() != null)
      entry.setIsHighlySensitive(request.getIsHighlySensitive());

    if (request.getCategoryId() != null) {
      Category category = categoryRepository.findById(request.getCategoryId())
          .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
      entry.setCategory(category);
    }

    if (request.getFolderId() != null) {
      Folder folder = folderRepository.findById(request.getFolderId())
          .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
      entry.setFolder(folder);
    }

    // Encrypt sensitive fields if provided
    if (request.getPassword() != null) {
      // Save snapshot of old password before changing
      vaultSnapshotService.createSnapshot(entry);
      entry.setPassword(encryptionService.encrypt(request.getPassword(), key));
    }
    if (request.getUsername() != null) {
      entry.setUsername(encryptionService.encrypt(request.getUsername(), key));
    }
    if (request.getNotes() != null) {
      entry.setNotes(encryptionService.encrypt(request.getNotes(), key));
    }

    VaultEntry savedEntry = vaultEntryRepository.save(entry);
    return mapToResponse(savedEntry);
  }

  @Transactional
  public void deleteEntry(String username, Long entryId) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    // Soft delete — move to trash
    entry.setIsDeleted(true);
    entry.setDeletedAt(LocalDateTime.now());
    vaultEntryRepository.save(entry);
  }

  @Transactional
  public VaultEntryResponse toggleFavorite(String username, Long entryId) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    entry.setIsFavorite(!entry.getIsFavorite());
    VaultEntry savedEntry = vaultEntryRepository.save(entry);
    return mapToResponse(savedEntry);
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getFavorites(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsFavoriteTrueAndIsDeletedFalse(user.getId());
    return entries.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getEntriesByFolder(String username, Long folderId) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndFolderIdAndIsDeletedFalse(user.getId(), folderId);
    return entries.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> searchEntries(String username, String keyword, Long categoryId,
      Long folderId, Boolean isFavorite, Boolean isHighlySensitive, String sortBy, String sortDir) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.searchEntries(
        user.getId(), keyword, categoryId, folderId, isFavorite, isHighlySensitive);

    // Sort in-memory
    Comparator<VaultEntry> comparator = switch (sortBy != null ? sortBy : "title") {
      case "createdAt" ->
        Comparator.comparing(VaultEntry::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
      case "updatedAt" ->
        Comparator.comparing(VaultEntry::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
      default -> Comparator.comparing(VaultEntry::getTitle, String.CASE_INSENSITIVE_ORDER);
    };

    if ("desc".equalsIgnoreCase(sortDir)) {
      comparator = comparator.reversed();
    }

    return entries.stream()
        .sorted(comparator)
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  @Transactional
  public void bulkDelete(String username, List<Long> ids) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.findAllById(ids);
    // Verify ownership
    entries.stream()
        .filter(e -> !e.getUser().getId().equals(user.getId()))
        .findAny()
        .ifPresent(e -> {
          throw new ResourceNotFoundException("Vault entry not found or access denied");
        });

    // Soft delete — move to trash
    LocalDateTime now = LocalDateTime.now();
    entries.forEach(e -> {
      e.setIsDeleted(true);
      e.setDeletedAt(now);
    });
    vaultEntryRepository.saveAll(entries);
  }

  @Transactional(readOnly = true)
  public String getPassword(String username, Long id) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(id, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    if (Boolean.TRUE.equals(entry.getIsHighlySensitive())) {
      throw new com.revature.passwordmanager.exception.AuthenticationException(
          "Sensitive entry requires authentication");
    }

    return entry.getPassword();
  }

  @Transactional(readOnly = true)
  public VaultEntryDetailResponse accessSensitiveEntry(String username, Long entryId,
      com.revature.passwordmanager.dto.request.SensitiveAccessRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    VaultEntry entry = vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    // 1. Verify Master Password
    SecretKey key;
    try {
      key = encryptionUtil.deriveKey(request.getMasterPassword(), user.getSalt());
    } catch (Exception e) {
      throw new com.revature.passwordmanager.exception.AuthenticationException("Invalid master password");
    }

    // 2. Verify OTP if enabled
    if (user.is2faEnabled()) {
      if (request.getOtpToken() == null || request.getOtpToken().isBlank()) {
        throw new com.revature.passwordmanager.exception.AuthenticationException("OTP is required");
      }
      if (!twoFactorService.verifyCode(user, request.getOtpToken())) {
        throw new com.revature.passwordmanager.exception.AuthenticationException("Invalid OTP");
      }
    }

    String decryptedPassword;
    String decryptedUsername;
    String decryptedNotes;

    try {
      decryptedPassword = encryptionService.decrypt(entry.getPassword(), key);
      decryptedUsername = encryptionService.decrypt(entry.getUsername(), key);
      decryptedNotes = entry.getNotes() != null ? encryptionService.decrypt(entry.getNotes(), key) : null;
    } catch (Exception e) {
      throw new com.revature.passwordmanager.exception.AuthenticationException(
          "Invalid master password or encryption error");
    }

    return mapToDetailResponse(entry, decryptedUsername, decryptedPassword, decryptedNotes);
  }

  // Helper methods
  private VaultEntryResponse mapToResponse(VaultEntry entry) {
    return VaultEntryResponse.builder()
        .id(entry.getId())
        .title(entry.getTitle())
        .username("******") // Masked
        .websiteUrl(entry.getWebsiteUrl())
        .categoryId(entry.getCategory().getId())
        .categoryName(entry.getCategory().getName())
        .folderId(entry.getFolder() != null ? entry.getFolder().getId() : null)
        .folderName(entry.getFolder() != null ? entry.getFolder().getName() : null)
        .isFavorite(entry.getIsFavorite())
        // .isHighlySensitive(entry.getIsHighlySensitive()) // VaultEntryResponse
        // doesn't have this field
        .createdAt(entry.getCreatedAt())
        .updatedAt(entry.getUpdatedAt())
        .build();
  }

  private VaultEntryDetailResponse mapToDetailResponse(VaultEntry entry, String username, String password,
      String notes) {
    return VaultEntryDetailResponse.builder()
        .id(entry.getId())
        .title(entry.getTitle())
        .username(username)
        .password(password)
        .websiteUrl(entry.getWebsiteUrl())
        .notes(notes)
        .categoryId(entry.getCategory().getId())
        .categoryName(entry.getCategory().getName())
        .folderId(entry.getFolder() != null ? entry.getFolder().getId() : null)
        .folderName(entry.getFolder() != null ? entry.getFolder().getName() : null)
        .isFavorite(entry.getIsFavorite())
        .isHighlySensitive(entry.getIsHighlySensitive())
        .requiresSensitiveAuth(
            Boolean.TRUE.equals(entry.getIsHighlySensitive()) && (password == null || password.equals("******")))
        .createdAt(entry.getCreatedAt())
        .updatedAt(entry.getUpdatedAt())
        .build();
  }
}
