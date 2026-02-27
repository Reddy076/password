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
  private final EncryptionUtil encryptionUtil;

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

    String decryptedPassword = encryptionService.decrypt(entry.getPassword(), key);
    String decryptedUsername = encryptionService.decrypt(entry.getUsername(), key);
    String decryptedNotes = entry.getNotes() != null ? encryptionService.decrypt(entry.getNotes(), key) : null;

    return mapToDetailResponse(entry, decryptedUsername, decryptedPassword, decryptedNotes);
  }

  @Transactional(readOnly = true)
  public List<VaultEntryResponse> getAllEntries(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<VaultEntry> entries = vaultEntryRepository.findByUserId(user.getId());
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
    vaultEntryRepository.delete(entry);
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
        .createdAt(entry.getCreatedAt())
        .updatedAt(entry.getUpdatedAt())
        .build();
  }
}
