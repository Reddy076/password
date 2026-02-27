package com.revature.passwordmanager.service.vault;

import com.revature.passwordmanager.dto.response.SnapshotResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.model.vault.VaultSnapshot;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.repository.VaultSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VaultSnapshotService {

  private static final Logger logger = LoggerFactory.getLogger(VaultSnapshotService.class);

  private final VaultSnapshotRepository vaultSnapshotRepository;
  private final VaultEntryRepository vaultEntryRepository;
  private final UserRepository userRepository;

  /**
   * Creates a snapshot of the current (old) password before it is overwritten.
   * The password stored is already encrypted.
   */
  @Transactional
  public void createSnapshot(VaultEntry entry) {
    VaultSnapshot snapshot = VaultSnapshot.builder()
        .vaultEntry(entry)
        .password(entry.getPassword()) // Already encrypted
        .changedAt(LocalDateTime.now())
        .build();

    vaultSnapshotRepository.save(snapshot);
    logger.info("Created password snapshot for vault entry {}", entry.getId());
  }

  /**
   * Returns the password change history for a vault entry, newest first.
   * Passwords are masked.
   */
  @Transactional(readOnly = true)
  public List<SnapshotResponse> getHistory(String username, Long entryId) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    // Verify entry exists and belongs to user
    vaultEntryRepository.findByIdAndUserId(entryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Vault entry not found"));

    List<VaultSnapshot> snapshots = vaultSnapshotRepository
        .findByVaultEntryIdOrderByChangedAtDesc(entryId);

    return snapshots.stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  private SnapshotResponse mapToResponse(VaultSnapshot snapshot) {
    return SnapshotResponse.builder()
        .id(snapshot.getId())
        .password("******") // Masked
        .changedAt(snapshot.getChangedAt())
        .build();
  }
}
