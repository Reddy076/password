package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.CreateFolderRequest;
import com.revature.passwordmanager.dto.response.FileUploadResponse;
import com.revature.passwordmanager.dto.response.SecureFileListResponse;
import com.revature.passwordmanager.dto.response.SecureFileListResponse.FileItem;
import com.revature.passwordmanager.dto.response.SecureFileListResponse.FolderItem;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.files.SecureFile;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.files.SecureFileService;
import com.revature.passwordmanager.service.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Full controller-layer tests for {@link SecureFileController}.
 * Each test verifies both the HTTP status and the JSON response body fields.</p>
 */
@WebMvcTest(controllers = SecureFileController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class SecureFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SecureFileService fileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RateLimitService rateLimitService;

    private FileUploadResponse uploadResponse;
    private SecureFileListResponse listResponse;

    @BeforeEach
    void setUp() {
        Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(100);

        uploadResponse = FileUploadResponse.builder()
                .id(100L)
                .originalFilename("passport.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .checksum("sha256hash")
                .folderId(10L)
                .folderName("Documents")
                .uploadedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        FolderItem folderItem = FolderItem.builder()
                .id(10L)
                .name("Documents")
                .parentId(null)
                .createdAt(LocalDateTime.of(2026, 1, 1, 9, 0))
                .build();

        FileItem fileItem = FileItem.builder()
                .id(100L)
                .originalFilename("passport.pdf")
                .fileSize(1024L)
                .mimeType("application/pdf")
                .folderId(10L)
                .folderName("Documents")
                .uploadedAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        listResponse = SecureFileListResponse.builder()
                .folders(List.of(folderItem))
                .files(List.of(fileItem))
                .totalFiles(1L)
                .totalStorageBytes(1024L)
                .build();
    }

    // ── POST /api/files/upload ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void uploadFile_ValidFile_ShouldReturn201WithFileMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "passport.pdf", "application/pdf", "PDF content".getBytes());

        when(fileService.uploadFile(eq("testuser"), any(), isNull(), any()))
                .thenReturn(uploadResponse);

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.originalFilename").value("passport.pdf"))
                .andExpect(jsonPath("$.fileSize").value(1024))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.checksum").value("sha256hash"))
                .andExpect(jsonPath("$.folderId").value(10))
                .andExpect(jsonPath("$.folderName").value("Documents"))
                .andExpect(jsonPath("$.uploadedAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void uploadFile_WithFolderId_ShouldPassFolderIdToService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        when(fileService.uploadFile(eq("testuser"), any(), eq(10L), any()))
                .thenReturn(uploadResponse);

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .param("folderId", "10")
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").value(10));
    }

    @Test
    @WithMockUser(username = "testuser")
    void uploadFile_FileTooLarge_ShouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", "content".getBytes());

        when(fileService.uploadFile(eq("testuser"), any(), isNull(), any()))
                .thenThrow(new IllegalArgumentException("File size exceeds the maximum allowed size of 50 MB"));

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadFile_Unauthenticated_ShouldReturn401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "content".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/files ────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void listFiles_RootLevel_ShouldReturnFoldersAndFiles() throws Exception {
        when(fileService.listFiles("testuser", null)).thenReturn(listResponse);

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.folders[0].id").value(10))
                .andExpect(jsonPath("$.folders[0].name").value("Documents"))
                .andExpect(jsonPath("$.folders[0].parentId").doesNotExist())
                .andExpect(jsonPath("$.files[0].id").value(100))
                .andExpect(jsonPath("$.files[0].originalFilename").value("passport.pdf"))
                .andExpect(jsonPath("$.files[0].fileSize").value(1024))
                .andExpect(jsonPath("$.files[0].mimeType").value("application/pdf"))
                .andExpect(jsonPath("$.files[0].folderId").value(10))
                .andExpect(jsonPath("$.files[0].folderName").value("Documents"))
                .andExpect(jsonPath("$.totalFiles").value(1))
                .andExpect(jsonPath("$.totalStorageBytes").value(1024));
    }

    @Test
    @WithMockUser(username = "testuser")
    void listFiles_WithFolderId_ShouldPassFolderIdToService() throws Exception {
        when(fileService.listFiles("testuser", 10L)).thenReturn(listResponse);

        mockMvc.perform(get("/api/files").param("folderId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].id").value(100));
    }

    @Test
    @WithMockUser(username = "testuser")
    void listFiles_EmptyVault_ShouldReturnEmptyLists() throws Exception {
        SecureFileListResponse empty = SecureFileListResponse.builder()
                .folders(List.of())
                .files(List.of())
                .totalFiles(0L)
                .totalStorageBytes(0L)
                .build();

        when(fileService.listFiles("testuser", null)).thenReturn(empty);

        mockMvc.perform(get("/api/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folders").isArray())
                .andExpect(jsonPath("$.folders").isEmpty())
                .andExpect(jsonPath("$.files").isArray())
                .andExpect(jsonPath("$.files").isEmpty())
                .andExpect(jsonPath("$.totalFiles").value(0))
                .andExpect(jsonPath("$.totalStorageBytes").value(0));
    }

    @Test
    void listFiles_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/files/search ─────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void searchFiles_MatchingKeyword_ShouldReturnResults() throws Exception {
        when(fileService.searchFiles("testuser", "passport")).thenReturn(listResponse);

        mockMvc.perform(get("/api/files/search").param("keyword", "passport"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files[0].originalFilename").value("passport.pdf"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void searchFiles_NoResults_ShouldReturnEmptyList() throws Exception {
        SecureFileListResponse empty = SecureFileListResponse.builder()
                .folders(List.of())
                .files(List.of())
                .totalFiles(0L)
                .totalStorageBytes(0L)
                .build();

        when(fileService.searchFiles("testuser", "xyz")).thenReturn(empty);

        mockMvc.perform(get("/api/files/search").param("keyword", "xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.files").isEmpty());
    }

    // ── GET /api/files/{id}/download ──────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void downloadFile_ValidFile_ShouldReturnFileBytes() throws Exception {
        byte[] fileBytes = "PDF content".getBytes();
        User user = User.builder().id(1L).username("testuser").email("t@t.com")
                .masterPasswordHash("h").salt("s").build();
        SecureFile metadata = SecureFile.builder()
                .id(100L).user(user).originalFilename("passport.pdf")
                .mimeType("application/pdf").encryptionIv("iv").storagePath("1/abc.enc")
                .fileSize(11L).encryptedFilename("abc.enc").checksum("hash").build();

        when(fileService.getFileMetadata("testuser", 100L)).thenReturn(metadata);
        when(fileService.downloadFile(eq("testuser"), eq(100L), any())).thenReturn(fileBytes);

        mockMvc.perform(get("/api/files/100/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"passport.pdf\""))
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(fileBytes));
    }

    @Test
    @WithMockUser(username = "testuser")
    void downloadFile_NotFound_ShouldReturn404() throws Exception {
        when(fileService.getFileMetadata("testuser", 999L))
                .thenThrow(new ResourceNotFoundException("File not found"));

        mockMvc.perform(get("/api/files/999/download"))
                .andExpect(status().isNotFound());
    }

    @Test
    void downloadFile_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/files/100/download"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/files/{id} ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void deleteFile_ValidFile_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/files/100").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("File deleted successfully"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteFile_NotFound_ShouldReturn404() throws Exception {
        Mockito.doThrow(new ResourceNotFoundException("File not found"))
                .when(fileService).deleteFile("testuser", 999L);

        mockMvc.perform(delete("/api/files/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFile_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/files/100").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/files/folders ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void createFolder_ValidRequest_ShouldReturn201WithFolderDetails() throws Exception {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("NewFolder")
                .build();

        FolderItem folderItem = FolderItem.builder()
                .id(20L)
                .name("NewFolder")
                .parentId(null)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();

        when(fileService.createFolder(eq("testuser"), any(CreateFolderRequest.class)))
                .thenReturn(folderItem);

        mockMvc.perform(post("/api/files/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("NewFolder"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void createFolder_BlankName_ShouldReturn400() throws Exception {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("")
                .build();

        mockMvc.perform(post("/api/files/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void createFolder_DuplicateName_ShouldReturn400() throws Exception {
        CreateFolderRequest request = CreateFolderRequest.builder()
                .name("Documents")
                .build();

        when(fileService.createFolder(eq("testuser"), any(CreateFolderRequest.class)))
                .thenThrow(new IllegalArgumentException("A folder named 'Documents' already exists at this level"));

        mockMvc.perform(post("/api/files/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createFolder_Unauthenticated_ShouldReturn401() throws Exception {
        CreateFolderRequest request = CreateFolderRequest.builder().name("Test").build();

        mockMvc.perform(post("/api/files/folders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/files/folders/{folderId} ──────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void deleteFolder_ValidFolder_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/files/folders/10").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Folder deleted successfully"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void deleteFolder_NotFound_ShouldReturn404() throws Exception {
        Mockito.doThrow(new ResourceNotFoundException("Folder not found"))
                .when(fileService).deleteFolder("testuser", 999L);

        mockMvc.perform(delete("/api/files/folders/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    // ── Content-Type assertions ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void allGetEndpoints_ShouldReturnJsonContentType() throws Exception {
        SecureFileListResponse empty = SecureFileListResponse.builder()
                .folders(List.of()).files(List.of()).totalFiles(0L).totalStorageBytes(0L).build();

        when(fileService.listFiles("testuser", null)).thenReturn(empty);
        when(fileService.searchFiles("testuser", "test")).thenReturn(empty);

        mockMvc.perform(get("/api/files"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/files/search").param("keyword", "test"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
