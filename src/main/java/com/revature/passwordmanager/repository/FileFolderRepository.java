package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.files.FileFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Feature 40 – Secure File Storage Vault.
 */
@Repository
public interface FileFolderRepository extends JpaRepository<FileFolder, Long> {

    /** All root-level folders (no parent) for a user. */
    List<FileFolder> findByUserIdAndParentIsNullOrderByNameAsc(Long userId);

    /** All child folders of a given parent. */
    List<FileFolder> findByUserIdAndParentIdOrderByNameAsc(Long userId, Long parentId);

    /** All folders for a user (for tree building). */
    List<FileFolder> findByUserIdOrderByNameAsc(Long userId);

    /** Find by id and owner (for ownership checks). */
    Optional<FileFolder> findByIdAndUserId(Long id, Long userId);

    /** Check for duplicate folder name within the same parent. */
    boolean existsByUserIdAndParentIdAndName(Long userId, Long parentId, String name);

    /** Check for duplicate root folder name. */
    boolean existsByUserIdAndParentIsNullAndName(Long userId, String name);
}
