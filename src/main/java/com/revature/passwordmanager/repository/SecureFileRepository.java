package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.files.SecureFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Feature 40 – Secure File Storage Vault.
 */
@Repository
public interface SecureFileRepository extends JpaRepository<SecureFile, Long> {

    /** All files for a user (optionally in a specific folder; null = root). */
    List<SecureFile> findByUserIdAndFolderIdOrderByOriginalFilenameAsc(Long userId, Long folderId);

    /** All root-level files (no folder) for a user. */
    List<SecureFile> findByUserIdAndFolderIsNullOrderByOriginalFilenameAsc(Long userId);

    /** All files for a user across all folders. */
    List<SecureFile> findByUserIdOrderByUploadedAtDesc(Long userId);

    /** Find by id and owner (for ownership checks). */
    Optional<SecureFile> findByIdAndUserId(Long id, Long userId);

    /** Search files by original filename (case-insensitive). */
    @Query("SELECT f FROM SecureFile f WHERE f.user.id = :userId " +
           "AND LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY f.originalFilename ASC")
    List<SecureFile> searchByFilename(@Param("userId") Long userId, @Param("keyword") String keyword);

    /** Count total files for a user. */
    long countByUserId(Long userId);

    /** Sum of file sizes for a user (for storage quota). */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM SecureFile f WHERE f.user.id = :userId")
    long sumFileSizeByUserId(@Param("userId") Long userId);

    void deleteByFolderId(Long folderId);
}
