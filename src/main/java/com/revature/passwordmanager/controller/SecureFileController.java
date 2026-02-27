package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.CreateFolderRequest;
import com.revature.passwordmanager.dto.response.FileUploadResponse;
import com.revature.passwordmanager.dto.response.MessageResponse;
import com.revature.passwordmanager.dto.response.SecureFileListResponse;
import com.revature.passwordmanager.dto.response.SecureFileListResponse.FolderItem;
import com.revature.passwordmanager.model.files.SecureFile;
import com.revature.passwordmanager.service.files.SecureFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Base URL: {@code /api/files}</p>
 *
 * <p>All endpoints require authentication. Files are encrypted with AES-256-GCM
 * using the user's derived key before storage. Passwords are never stored in
 * plaintext on disk.</p>
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "Secure File Vault", description = "Feature 40 – Encrypted file storage vault")
public class SecureFileController {

    private final SecureFileService fileService;

    // ── Upload ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Upload an encrypted file",
        description = "Encrypts the file with AES-256-GCM using the user's derived key and stores it. " +
                      "Maximum file size: 50 MB. Maximum total storage per user: 100 MB."
    )
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "The file to upload")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional folder id to place the file in (null = root)")
            @RequestParam(value = "folderId", required = false) Long folderId,
            HttpServletRequest request) {
        String username = getCurrentUsername();
        String ipAddress = request.getRemoteAddr();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.uploadFile(username, file, folderId, ipAddress));
    }

    // ── List ──────────────────────────────────────────────────────────────────

    @Operation(
        summary = "List files and folders",
        description = "Returns files and folders at the specified level. " +
                      "Omit folderId to list root-level items."
    )
    @GetMapping
    public ResponseEntity<SecureFileListResponse> listFiles(
            @Parameter(description = "Folder id to list (omit for root)")
            @RequestParam(required = false) Long folderId) {
        return ResponseEntity.ok(fileService.listFiles(getCurrentUsername(), folderId));
    }

    // ── Search ────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Search files by filename",
        description = "Case-insensitive substring search across all files for the authenticated user."
    )
    @GetMapping("/search")
    public ResponseEntity<SecureFileListResponse> searchFiles(
            @Parameter(description = "Search keyword")
            @RequestParam String keyword) {
        return ResponseEntity.ok(fileService.searchFiles(getCurrentUsername(), keyword));
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @Operation(
        summary = "Download a decrypted file",
        description = "Decrypts the file using the user's derived key and returns the original bytes. " +
                      "The response Content-Type and Content-Disposition headers are set from the stored metadata."
    )
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "File id")
            @PathVariable Long id,
            HttpServletRequest request) {
        String username = getCurrentUsername();
        String ipAddress = request.getRemoteAddr();

        SecureFile metadata = fileService.getFileMetadata(username, id);
        byte[] fileBytes = fileService.downloadFile(username, id, ipAddress);

        String contentType = metadata.getMimeType() != null
                ? metadata.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + metadata.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(fileBytes.length)
                .body(fileBytes);
    }

    // ── Delete file ───────────────────────────────────────────────────────────

    @Operation(
        summary = "Delete a file",
        description = "Permanently deletes the file from both the database and the encrypted storage."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteFile(
            @Parameter(description = "File id")
            @PathVariable Long id) {
        fileService.deleteFile(getCurrentUsername(), id);
        return ResponseEntity.ok(new MessageResponse("File deleted successfully"));
    }

    // ── Folder management ─────────────────────────────────────────────────────

    @Operation(
        summary = "Create a folder",
        description = "Creates a new folder. Optionally specify a parentId for nested folders."
    )
    @PostMapping("/folders")
    public ResponseEntity<FolderItem> createFolder(
            @Valid @RequestBody CreateFolderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(fileService.createFolder(getCurrentUsername(), request));
    }

    @Operation(
        summary = "Delete a folder",
        description = "Permanently deletes a folder and all its contents (files and subfolders) recursively."
    )
    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<MessageResponse> deleteFolder(
            @Parameter(description = "Folder id")
            @PathVariable Long folderId) {
        fileService.deleteFolder(getCurrentUsername(), folderId);
        return ResponseEntity.ok(new MessageResponse("Folder deleted successfully"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getCurrentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
