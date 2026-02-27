package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.vault.VaultEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

  @Query("SELECT v FROM VaultEntry v WHERE v.user.id = :userId " +
      "AND (:keyword IS NULL OR LOWER(v.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
      "    OR LOWER(v.websiteUrl) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
      "AND (:categoryId IS NULL OR v.category.id = :categoryId) " +
      "AND (:folderId IS NULL OR v.folder.id = :folderId) " +
      "AND (:isFavorite IS NULL OR v.isFavorite = :isFavorite) " +
      "AND (:isHighlySensitive IS NULL OR v.isHighlySensitive = :isHighlySensitive)")
  List<VaultEntry> searchEntries(
      @Param("userId") Long userId,
      @Param("keyword") String keyword,
      @Param("categoryId") Long categoryId,
      @Param("folderId") Long folderId,
      @Param("isFavorite") Boolean isFavorite,
      @Param("isHighlySensitive") Boolean isHighlySensitive);
}
