package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.files.FileAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Feature 40 – Secure File Storage Vault.
 */
@Repository
public interface FileAccessLogRepository extends JpaRepository<FileAccessLog, Long> {

    List<FileAccessLog> findByUserIdOrderByAccessedAtDesc(Long userId);

    List<FileAccessLog> findByFileIdOrderByAccessedAtDesc(Long fileId);

    void deleteByFileId(Long fileId);
}
