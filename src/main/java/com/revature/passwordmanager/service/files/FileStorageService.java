package com.revature.passwordmanager.service.files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Handles raw file I/O on the local filesystem. Files are stored under
 * {@code secure.files.storage-path} (default: {@code ./secure-files}).
 * Each user gets their own subdirectory to avoid filename collisions.</p>
 *
 * <p>The bytes written to disk are already encrypted by {@link FileEncryptionService}
 * before this service is called — this service is purely responsible for persistence.</p>
 */
@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    private final Path baseStoragePath;

    public FileStorageService(
            @Value("${secure.files.storage-path:./secure-files}") String storagePath) {
        this.baseStoragePath = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.baseStoragePath);
        } catch (IOException e) {
            logger.warn("Could not create base storage directory {}: {}", this.baseStoragePath, e.getMessage());
        }
    }

    /**
     * Stores encrypted file bytes on disk.
     *
     * @param userId       the owner's user id (used as subdirectory)
     * @param encryptedBytes the encrypted file bytes to persist
     * @return the relative storage path (stored in the database)
     * @throws RuntimeException if the write fails
     */
    public String store(Long userId, byte[] encryptedBytes) {
        try {
            Path userDir = baseStoragePath.resolve(String.valueOf(userId));
            Files.createDirectories(userDir);

            String filename = UUID.randomUUID().toString() + ".enc";
            Path filePath = userDir.resolve(filename);
            Files.write(filePath, encryptedBytes);

            // Return relative path for portability
            return userId + "/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Reads encrypted file bytes from disk.
     *
     * @param storagePath the relative path stored in the database
     * @return the encrypted file bytes
     * @throws RuntimeException if the read fails
     */
    public byte[] load(String storagePath) {
        try {
            Path filePath = baseStoragePath.resolve(storagePath).normalize();
            if (!filePath.startsWith(baseStoragePath)) {
                throw new SecurityException("Path traversal attempt detected");
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load file from storage: " + storagePath, e);
        }
    }

    /**
     * Deletes a file from disk.
     *
     * @param storagePath the relative path stored in the database
     */
    public void delete(String storagePath) {
        try {
            Path filePath = baseStoragePath.resolve(storagePath).normalize();
            if (!filePath.startsWith(baseStoragePath)) {
                throw new SecurityException("Path traversal attempt detected");
            }
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.warn("Failed to delete file from storage {}: {}", storagePath, e.getMessage());
        }
    }
}
