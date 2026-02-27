package com.revature.passwordmanager.service.files;

import com.revature.passwordmanager.dto.request.CreateFolderRequest;
import com.revature.passwordmanager.dto.response.FileUploadResponse;
import com.revature.passwordmanager.dto.response.SecureFileListResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.files.FileAccessLog;
import com.revature.passwordmanager.model.files.FileFolder;
import com.revature.passwordmanager.model.files.SecureFile;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.FileAccessLogRepository;
import com.revature.passwordmanager.repository.FileFolderRepository;
import com.revature.passwordmanager.repository.SecureFileRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.service.files.FileEncryptionService.EncryptedFile;
import com.revature.passwordmanager.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Feature 40 – Secure File Storage Vault.
 */
@ExtendWith(MockitoExtension.class)
class SecureFileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SecureFileRepository fileRepository;
    @Mock private FileFolderRepository folderRepository;
    @Mock private FileAccessLogRepository accessLogRepository;
    @Mock private FileEncryptionService fileEncryptionService;
    @Mock private FileStorageService fileStorageService;
    @Mock private EncryptionUtil encryptionUtil;

    @InjectMocks
    private SecureFileService fileService;

    private User user;
    private SecretKey aesKey;
    private SecureFile secureFile;
    private FileFolder folder;

    @BeforeEach
    void setUp() throws Exception {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .masterPasswordHash("hash")
                .salt("salt")
                .build();

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        aesKey = keyGen.generateKey();

        folder = FileFolder.builder()
                .id(10L)
                .user(user)
                .name("Documents")
                .build();

        secureFile = SecureFile.builder()
                .id(100L)
                .user(user)
                .folder(folder)
                .originalFilename("passport.pdf")
                .encryptedFilename("abc123.enc")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .encryptionIv("base64iv==")
                .storagePath("1/abc123.enc")
                .checksum("sha256hash")
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    // ── uploadFile ────────────────────────────────────────────────────────────

    @Test
    void uploadFile_ValidFile_ShouldEncryptAndStore() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "passport.pdf", "application/pdf",
                "PDF content".getBytes());

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(folder));
        when(fileRepository.sumFileSizeByUserId(1L)).thenReturn(0L);
        when(encryptionUtil.deriveKey("hash", "salt")).thenReturn(aesKey);
        when(fileEncryptionService.computeChecksum(any())).thenReturn("sha256hash");
        when(fileEncryptionService.encrypt(any(), eq(aesKey)))
                .thenReturn(new EncryptedFile("encrypted".getBytes(), "base64iv=="));
        when(fileStorageService.store(1L, "encrypted".getBytes())).thenReturn("1/abc123.enc");
        when(fileRepository.save(any(SecureFile.class))).thenReturn(secureFile);
        when(accessLogRepository.save(any(FileAccessLog.class))).thenReturn(null);

        FileUploadResponse response = fileService.uploadFile("testuser", file, 10L, "127.0.0.1");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getOriginalFilename()).isEqualTo("passport.pdf");
        assertThat(response.getMimeType()).isEqualTo("application/pdf");
        assertThat(response.getFolderId()).isEqualTo(10L);
        assertThat(response.getFolderName()).isEqualTo("Documents");
        assertThat(response.getChecksum()).isEqualTo("sha256hash");
        verify(fileStorageService).store(eq(1L), any());
        verify(fileRepository).save(any(SecureFile.class));
    }

    @Test
    void uploadFile_EmptyFile_ShouldThrowIllegalArgumentException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.txt", "text/plain", new byte[0]);

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);

        assertThatThrownBy(() -> fileService.uploadFile("testuser", emptyFile, null, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void uploadFile_FileTooLarge_ShouldThrowIllegalArgumentException() {
        // Create a mock file that reports size > 50 MB
        MockMultipartFile largeFile = mock(MockMultipartFile.class);
        when(largeFile.isEmpty()).thenReturn(false);
        when(largeFile.getSize()).thenReturn(SecureFileService.MAX_FILE_SIZE_BYTES + 1);

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);

        assertThatThrownBy(() -> fileService.uploadFile("testuser", largeFile, null, "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50 MB");
    }

    @Test
    void uploadFile_QuotaExceeded_ShouldThrowIllegalStateException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        // Already at 99.9 MB used, uploading 1 MB would exceed 100 MB quota
        when(fileRepository.sumFileSizeByUserId(1L))
                .thenReturn(SecureFileService.MAX_TOTAL_STORAGE_BYTES - 1);

        assertThatThrownBy(() -> fileService.uploadFile("testuser", file, null, "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quota exceeded");
    }

    @Test
    void uploadFile_FolderNotFound_ShouldThrowResourceNotFoundException() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(fileRepository.sumFileSizeByUserId(1L)).thenReturn(0L);
        when(folderRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.uploadFile("testuser", file, 999L, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── downloadFile ──────────────────────────────────────────────────────────

    @Test
    void downloadFile_ValidFile_ShouldDecryptAndReturn() {
        byte[] cipherBytes = "encrypted".getBytes();
        byte[] plainBytes = "PDF content".getBytes();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(fileRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(secureFile));
        when(encryptionUtil.deriveKey("hash", "salt")).thenReturn(aesKey);
        when(fileStorageService.load("1/abc123.enc")).thenReturn(cipherBytes);
        when(fileEncryptionService.decrypt(cipherBytes, aesKey, "base64iv==")).thenReturn(plainBytes);
        when(fileRepository.save(any())).thenReturn(secureFile);
        when(accessLogRepository.save(any())).thenReturn(null);

        byte[] result = fileService.downloadFile("testuser", 100L, "127.0.0.1");

        assertThat(result).isEqualTo(plainBytes);
        verify(fileStorageService).load("1/abc123.enc");
        verify(fileEncryptionService).decrypt(cipherBytes, aesKey, "base64iv==");
    }

    @Test
    void downloadFile_FileNotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(fileRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.downloadFile("testuser", 999L, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── listFiles ─────────────────────────────────────────────────────────────

    @Test
    void listFiles_RootLevel_ShouldReturnRootFoldersAndFiles() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.findByUserIdAndParentIsNullOrderByNameAsc(1L)).thenReturn(List.of(folder));
        when(fileRepository.findByUserIdAndFolderIsNullOrderByOriginalFilenameAsc(1L)).thenReturn(List.of());
        when(fileRepository.countByUserId(1L)).thenReturn(1L);
        when(fileRepository.sumFileSizeByUserId(1L)).thenReturn(1024L);

        SecureFileListResponse response = fileService.listFiles("testuser", null);

        assertThat(response.getFolders()).hasSize(1);
        assertThat(response.getFolders().get(0).getName()).isEqualTo("Documents");
        assertThat(response.getFiles()).isEmpty();
        assertThat(response.getTotalFiles()).isEqualTo(1L);
        assertThat(response.getTotalStorageBytes()).isEqualTo(1024L);
    }

    @Test
    void listFiles_InFolder_ShouldReturnFolderContents() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(folder));
        when(folderRepository.findByUserIdAndParentIdOrderByNameAsc(1L, 10L)).thenReturn(List.of());
        when(fileRepository.findByUserIdAndFolderIdOrderByOriginalFilenameAsc(1L, 10L))
                .thenReturn(List.of(secureFile));
        when(fileRepository.countByUserId(1L)).thenReturn(1L);
        when(fileRepository.sumFileSizeByUserId(1L)).thenReturn(1024L);

        SecureFileListResponse response = fileService.listFiles("testuser", 10L);

        assertThat(response.getFiles()).hasSize(1);
        assertThat(response.getFiles().get(0).getOriginalFilename()).isEqualTo("passport.pdf");
        assertThat(response.getFiles().get(0).getMimeType()).isEqualTo("application/pdf");
        assertThat(response.getFiles().get(0).getFolderId()).isEqualTo(10L);
        assertThat(response.getFiles().get(0).getFolderName()).isEqualTo("Documents");
    }

    // ── searchFiles ───────────────────────────────────────────────────────────

    @Test
    void searchFiles_MatchingKeyword_ShouldReturnMatchingFiles() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(fileRepository.searchByFilename(1L, "passport")).thenReturn(List.of(secureFile));
        when(fileRepository.countByUserId(1L)).thenReturn(1L);
        when(fileRepository.sumFileSizeByUserId(1L)).thenReturn(1024L);

        SecureFileListResponse response = fileService.searchFiles("testuser", "passport");

        assertThat(response.getFiles()).hasSize(1);
        assertThat(response.getFiles().get(0).getOriginalFilename()).isEqualTo("passport.pdf");
    }

    @Test
    void searchFiles_NoMatch_ShouldReturnEmptyList() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(fileRepository.searchByFilename(1L, "nonexistent")).thenReturn(List.of());
        when(fileRepository.countByUserId(1L)).thenReturn(0L);
        when(fileRepository.sumFileSizeByUserId(1L)).thenReturn(0L);

        SecureFileListResponse response = fileService.searchFiles("testuser", "nonexistent");

        assertThat(response.getFiles()).isEmpty();
    }

    // ── deleteFile ────────────────────────────────────────────────────────────

    @Test
    void deleteFile_ValidFile_ShouldDeleteFromDiskAndDatabase() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(fileRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(secureFile));

        fileService.deleteFile("testuser", 100L);

        verify(fileStorageService).delete("1/abc123.enc");
        verify(accessLogRepository).deleteByFileId(100L);
        verify(fileRepository).delete(secureFile);
    }

    @Test
    void deleteFile_NotFound_ShouldThrowResourceNotFoundException() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(fileRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.deleteFile("testuser", 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── createFolder ──────────────────────────────────────────────────────────

    @Test
    void createFolder_RootLevel_ShouldCreateFolder() {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("NewFolder")
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.existsByUserIdAndParentIsNullAndName(1L, "NewFolder")).thenReturn(false);
        when(folderRepository.save(any(FileFolder.class))).thenReturn(
                FileFolder.builder().id(20L).user(user).name("NewFolder").build());

        SecureFileListResponse.FolderItem result = fileService.createFolder("testuser", request);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getName()).isEqualTo("NewFolder");
        assertThat(result.getParentId()).isNull();
    }

    @Test
    void createFolder_DuplicateName_ShouldThrowIllegalArgumentException() {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Documents")
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.existsByUserIdAndParentIsNullAndName(1L, "Documents")).thenReturn(true);

        assertThatThrownBy(() -> fileService.createFolder("testuser", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createFolder_WithParent_ShouldCreateNestedFolder() {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("SubFolder")
                .parentId(10L)
                .build();

        FileFolder subFolder = FileFolder.builder()
                .id(20L).user(user).parent(folder).name("SubFolder").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(folder));
        when(folderRepository.existsByUserIdAndParentIdAndName(1L, 10L, "SubFolder")).thenReturn(false);
        when(folderRepository.save(any(FileFolder.class))).thenReturn(subFolder);

        SecureFileListResponse.FolderItem result = fileService.createFolder("testuser", request);

        assertThat(result.getId()).isEqualTo(20L);
        assertThat(result.getName()).isEqualTo("SubFolder");
        assertThat(result.getParentId()).isEqualTo(10L);
    }

    // ── deleteFolder ──────────────────────────────────────────────────────────

    @Test
    void deleteFolder_EmptyFolder_ShouldDeleteFolder() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(folder));
        when(fileRepository.findByUserIdAndFolderIdOrderByOriginalFilenameAsc(1L, 10L))
                .thenReturn(List.of());
        when(folderRepository.findByUserIdAndParentIdOrderByNameAsc(1L, 10L)).thenReturn(List.of());

        fileService.deleteFolder("testuser", 10L);

        verify(folderRepository).delete(folder);
    }

    @Test
    void deleteFolder_WithFiles_ShouldDeleteFilesAndFolder() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(folderRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(folder));
        when(fileRepository.findByUserIdAndFolderIdOrderByOriginalFilenameAsc(1L, 10L))
                .thenReturn(List.of(secureFile));
        when(folderRepository.findByUserIdAndParentIdOrderByNameAsc(1L, 10L)).thenReturn(List.of());

        fileService.deleteFolder("testuser", 10L);

        verify(fileStorageService).delete("1/abc123.enc");
        verify(accessLogRepository).deleteByFileId(100L);
        verify(fileRepository).deleteByFolderId(10L);
        verify(folderRepository).delete(folder);
    }
}
