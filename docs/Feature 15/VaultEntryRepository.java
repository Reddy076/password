package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.vault.VaultEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VaultEntryRepository extends JpaRepository<VaultEntry, Long> {
  List<VaultEntry> findByUserId(Long userId);

  List<VaultEntry> findByUserIdAndFolderId(Long userId, Long folderId);

  List<VaultEntry> findByUserIdAndCategoryId(Long userId, Long categoryId);

  Optional<VaultEntry> findByIdAndUserId(Long id, Long userId);

  List<VaultEntry> findByUserIdAndIsFavoriteTrue(Long userId);
}
