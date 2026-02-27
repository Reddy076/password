package com.revature.passwordmanager.service.files;

import com.revature.passwordmanager.dto.request.CreateFolderRequest;
import com.revature.passwordmanager.dto.response.FileUploadResponse;
import com.revature.passwordmanager.dto.response.SecureFileListResponse;
import com.revature.passwordmanager.dto.response.SecureFileListResponse.FileItem;
import com.revature.passwordmanager.dto.response.SecureFileListResponse.FolderItem;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.files.FileAccessLog;
import com.revature.passwordmanager.model.files.FileAccessLog.FileAction;
import com.revature.passwordmanager.model.files.FileFolder;
import com.revature.passwordmanager.model.files.SecureFile;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.FileAccessLogRepository;
import com.revature.passwordmanager.repository.FileFolderRepository;
import com.revature.passwordmanager.repository.SecureFileRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.service.files.FileEncryptionService.EncryptedFile;
import com.revature.passwordmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Orchestrates file upload, download, listing, deletion, and search.
 * Files are encrypted with AES-256-GCM using the user's derived key before
 * being written to the filesystem.</p>
 *
 * <p>Storage limit: 100 MB per user (configurable). Files larger than 50 MB
 * are rejected to prevent memory exhaustion during encryption.</p>
 */
@Service
@RequiredArgsConstructor
public class SecureFileService {

    /** Maximum individual file size: 50 MB. */
    public static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;

    /** Maximum total storage per user: 100 MB. */
    public static final long MAX_TOTAL_STORAGE_BYTES = 100L * 1024 * 1024;

    private final UserRepository userRepository;
    private final SecureFileRepository fileRepository;
    private final FileFolderRepository folderRepository;
    private final FileAccessLogRepository accessLogRepository;
    private final FileEncryptionService fileEncryptionService;
    private final FileStorageService fileStorageService;
    private final EncryptionUtil encryptionUtil;

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Encrypts and stores an uploaded file.
     *
     * @param username the authenticated user
     * @param file     the multipart file from the HTTP request
     * @param folderId optional folder id (null = root)
     * @param ipAddress the client IP for audit logging
     * @return metadata of the stored file
     */
    @Transactional
    public FileUploadResponse uploadFile(String username, MultipartFile file,
                                          Long folderId, String ipAddress) {
        User user = userRepository.findByUsernameOrThrow(username);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File size exceeds the maximum allowed size of 50 MB");
        }

        // Check total storage quota
        long usedBytes = fileRepository.sumFileSizeByUserId(user.getId());
        if (usedBytes + file.getSize() > MAX_TOTAL_STORAGE_BYTES) {
            throw new IllegalStateException(
                    "Storage quota exceeded. Maximum total storage is 100 MB per user.");
        }

        FileFolder folder = null;
        if (folderId != null) {
            folder = folderRepository.findByIdAndUserId(folderId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
        }

        try {
            byte[] plainBytes = file.getBytes();
            String checksum = fileEncryptionService.computeChecksum(plainBytes);

            // Derive key from user's master password hash + salt
            SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());
            EncryptedFile encrypted = fileEncryptionService.encrypt(plainBytes, key);

            // Store encrypted bytes on disk
            String storagePath = fileStorageService.store(user.getId(), encrypted.cipherBytes());

            String originalFilename = file.getOriginalFilename() != null
                    ? file.getOriginalFilename() : "unknown";
            String encryptedFilename = storagePath.substring(storagePath.lastIndexOf('/') + 1);

            SecureFile secureFile = SecureFile.builder()
                    .user(user)
                    .folder(folder)
                    .originalFilename(originalFilename)
                    .encryptedFilename(encryptedFilename)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .encryptionIv(encrypted.ivBase64())
                    .storagePath(storagePath)
                    .checksum(checksum)
                    .build();

            secureFile = fileRepository.save(secureFile);

            // Audit log
            logAccess(user, secureFile, FileAction.UPLOAD, ipAddress);

            return mapToUploadResponse(secureFile);

        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }

    // ── Download ──────────────────────────────────────────────────────────────

    /**
     * Decrypts and returns the file bytes for download.
     *
     * @param username  the authenticated user
     * @param fileId    the file to download
     * @param ipAddress the client IP for audit logging
     * @return the decrypted file bytes
     */
    @Transactional
    public byte[] downloadFile(String username, Long fileId, String ipAddress) {
        User user = userRepository.findByUsernameOrThrow(username);
        SecureFile secureFile = fileRepository.findByIdAndUserId(fileId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        SecretKey key = encryptionUtil.deriveKey(user.getMasterPasswordHash(), user.getSalt());
        byte[] cipherBytes = fileStorageService.load(secureFile.getStoragePath());
        byte[] plainBytes = fileEncryptionService.decrypt(cipherBytes, key, secureFile.getEncryptionIv());

        // Update last accessed timestamp
        secureFile.setLastAccessedAt(LocalDateTime.now());
        fileRepository.save(secureFile);

        // Audit log
        logAccess(user, secureFile, FileAction.DOWNLOAD, ipAddress);

        return plainBytes;
    }

    /**
     * Returns the {@link SecureFile} metadata for a download (used to set Content-Type and filename headers).
     */
    @Transactional(readOnly = true)
    public SecureFile getFileMetadata(String username, Long fileId) {
        User user = userRepository.findByUsernameOrThrow(username);
        return fileRepository.findByIdAndUserId(fileId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));
    }

    // ── List ──────────────────────────────────────────────────────────────────

    /**
     * Lists files and folders at the specified level.
     *
     * @param username the authenticated user
     * @param folderId the folder to list (null = root)
     * @return files and folders at the given level, plus storage summary
     */
    @Transactional(readOnly = true)
    public SecureFileListResponse listFiles(String username, Long folderId) {
        User user = userRepository.findByUsernameOrThrow(username);

        List<FileFolder> folders;
        List<SecureFile> files;

        if (folderId == null) {
            folders = folderRepository.findByUserIdAndParentIsNullOrderByNameAsc(user.getId());
            files = fileRepository.findByUserIdAndFolderIsNullOrderByOriginalFilenameAsc(user.getId());
        } else {
            // Verify folder belongs to user
            folderRepository.findByIdAndUserId(folderId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));
            folders = folderRepository.findByUserIdAndParentIdOrderByNameAsc(user.getId(), folderId);
            files = fileRepository.findByUserIdAndFolderIdOrderByOriginalFilenameAsc(user.getId(), folderId);
        }

        long totalFiles = fileRepository.countByUserId(user.getId());
        long totalStorage = fileRepository.sumFileSizeByUserId(user.getId());

        return SecureFileListResponse.builder()
                .folders(folders.stream().map(this::mapToFolderItem).collect(Collectors.toList()))
                .files(files.stream().map(this::mapToFileItem).collect(Collectors.toList()))
                .totalFiles(totalFiles)
                .totalStorageBytes(totalStorage)
                .build();
    }

    // ── Search ────────────────────────────────────────────────────────────────

    /**
     * Searches files by original filename (case-insensitive substring match).
     */
    @Transactional(readOnly = true)
    public SecureFileListResponse searchFiles(String username, String keyword) {
        User user = userRepository.findByUsernameOrThrow(username);
        List<SecureFile> files = fileRepository.searchByFilename(user.getId(), keyword);

        long totalFiles = fileRepository.countByUserId(user.getId());
        long totalStorage = fileRepository.sumFileSizeByUserId(user.getId());

        return SecureFileListResponse.builder()
                .folders(List.of())
                .files(files.stream().map(this::mapToFileItem).collect(Collectors.toList()))
                .totalFiles(totalFiles)
                .totalStorageBytes(totalStorage)
                .build();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Permanently deletes a file (both the database record and the encrypted bytes on disk).
     */
    @Transactional
    public void deleteFile(String username, Long fileId) {
        User user = userRepository.findByUsernameOrThrow(username);
        SecureFile secureFile = fileRepository.findByIdAndUserId(fileId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        // Delete from disk first
        fileStorageService.delete(secureFile.getStoragePath());

        // Delete audit logs and DB record
        accessLogRepository.deleteByFileId(fileId);
        fileRepository.delete(secureFile);
    }

    // ── Folder management ─────────────────────────────────────────────────────

    /**
     * Creates a new folder.
     */
    @Transactional
    public FolderItem createFolder(String username, CreateFolderRequest request) {
        User user = userRepository.findByUsernameOrThrow(username);

        FileFolder parent = null;
        if (request.getParentId() != null) {
            parent = folderRepository.findByIdAndUserId(request.getParentId(), user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent folder not found"));
        }

        // Check for duplicate name at the same level
        boolean duplicate = (parent == null)
                ? folderRepository.existsByUserIdAndParentIsNullAndName(user.getId(), request.getName())
                : folderRepository.existsByUserIdAndParentIdAndName(user.getId(), parent.getId(), request.getName());

        if (duplicate) {
            throw new IllegalArgumentException(
                    "A folder named '" + request.getName() + "' already exists at this level");
        }

        FileFolder folder = FileFolder.builder()
                .user(user)
                .parent(parent)
                .name(request.getName())
                .build();

        folder = folderRepository.save(folder);
        return mapToFolderItem(folder);
    }

    /**
     * Deletes a folder and all its contents (files + subfolders recursively).
     */
    @Transactional
    public void deleteFolder(String username, Long folderId) {
        User user = userRepository.findByUsernameOrThrow(username);
        FileFolder folder = folderRepository.findByIdAndUserId(folderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Folder not found"));

        deleteFolderRecursive(folder);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void deleteFolderRecursive(FileFolder folder) {
        // Delete all files in this folder
        List<SecureFile> files = fileRepository
                .findByUserIdAndFolderIdOrderByOriginalFilenameAsc(folder.getUser().getId(), folder.getId());
        for (SecureFile file : files) {
            fileStorageService.delete(file.getStoragePath());
            accessLogRepository.deleteByFileId(file.getId());
        }
        fileRepository.deleteByFolderId(folder.getId());

        // Recursively delete subfolders
        List<FileFolder> subfolders = folderRepository
                .findByUserIdAndParentIdOrderByNameAsc(folder.getUser().getId(), folder.getId());
        for (FileFolder sub : subfolders) {
            deleteFolderRecursive(sub);
        }

        folderRepository.delete(folder);
    }

    private void logAccess(User user, SecureFile file, FileAction action, String ipAddress) {
        FileAccessLog log = FileAccessLog.builder()
                .user(user)
                .file(file)
                .action(action)
                .ipAddress(ipAddress)
                .build();
        accessLogRepository.save(log);
    }

    private FileUploadResponse mapToUploadResponse(SecureFile file) {
        return FileUploadResponse.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .checksum(file.getChecksum())
                .folderId(file.getFolder() != null ? file.getFolder().getId() : null)
                .folderName(file.getFolder() != null ? file.getFolder().getName() : null)
                .uploadedAt(file.getUploadedAt())
                .build();
    }

    private FileItem mapToFileItem(SecureFile file) {
        return FileItem.builder()
                .id(file.getId())
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getFileSize())
                .mimeType(file.getMimeType())
                .folderId(file.getFolder() != null ? file.getFolder().getId() : null)
                .folderName(file.getFolder() != null ? file.getFolder().getName() : null)
                .uploadedAt(file.getUploadedAt())
                .lastAccessedAt(file.getLastAccessedAt())
                .build();
    }

    private FolderItem mapToFolderItem(FileFolder folder) {
        return FolderItem.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
