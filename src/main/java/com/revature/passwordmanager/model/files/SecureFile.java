package com.revature.passwordmanager.model.files;

import com.revature.passwordmanager.model.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Metadata record for an encrypted file stored in the vault.
 * The actual file bytes are stored on the filesystem (or S3) at {@code storagePath}.
 * The file is encrypted with AES-256-GCM using the user's derived key before storage.</p>
 */
@Entity
@Table(name = "secure_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecureFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @lombok.ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    @lombok.ToString.Exclude
    private FileFolder folder;

    /** Original filename as provided by the user. */
    @Column(name = "original_filename", nullable = false, length = 500)
    private String originalFilename;

    /** Encrypted filename stored on disk (UUID-based to avoid collisions). */
    @Column(name = "encrypted_filename", nullable = false, length = 500)
    private String encryptedFilename;

    /** File size in bytes (of the original, unencrypted file). */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** MIME type of the original file (e.g., "application/pdf", "image/jpeg"). */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** Base64-encoded GCM IV used for encryption (stored alongside the file). */
    @Column(name = "encryption_iv", nullable = false, length = 255)
    private String encryptionIv;

    /** Absolute or relative path to the encrypted file on the storage backend. */
    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;

    /** SHA-256 checksum of the original (unencrypted) file bytes for integrity verification. */
    @Column(name = "checksum", length = 255)
    private String checksum;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
}
