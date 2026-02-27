package com.revature.passwordmanager.service.vault;

import com.revature.passwordmanager.dto.request.VaultEntryRequest;
import com.revature.passwordmanager.dto.response.VaultEntryDetailResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.Category;
import com.revature.passwordmanager.model.vault.Folder;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.CategoryRepository;
import com.revature.passwordmanager.repository.FolderRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.security.EncryptionService;
import com.revature.passwordmanager.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VaultServiceTest {

  @Mock
  private VaultEntryRepository vaultEntryRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private CategoryRepository categoryRepository;
  @Mock
  private FolderRepository folderRepository;
  @Mock
  private EncryptionService encryptionService;
  @Mock
  private com.revature.passwordmanager.service.auth.TwoFactorService twoFactorService; // Mock 2FA service
  @Mock
  private EncryptionUtil encryptionUtil;

  @InjectMocks
  private VaultService vaultService;

  private User user;
  private Category category;
  private Folder folder;
  private VaultEntry entry;
  private SecretKey mockKey;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .id(1L)
        .username("testuser")
        .masterPasswordHash("hash")
        .salt("salt")
        .build();

    category = Category.builder().id(1L).name("Login").build();
    folder = Folder.builder().id(1L).name("Work").build();

    mockKey = mock(SecretKey.class);

    entry = VaultEntry.builder()
        .id(1L)
        .user(user)
        .category(category)
        .folder(folder)
        .title("Test Entry")
        .username("encryptedUsername")
        .password("encryptedPassword")
        .isHighlySensitive(false)
        .build();
  }

  @Test
  void getEntry_ShouldReturnMaskedData_WhenHighlySensitive() {
    entry.setIsHighlySensitive(true);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
    when(encryptionUtil.deriveKey(anyString(), anyString())).thenReturn(mockKey);
    // decrypt is not called for masked fields

    VaultEntryDetailResponse response = vaultService.getEntry("testuser", 1L);

    assertNotNull(response);
    assertEquals("******", response.getPassword());
    assertEquals("******", response.getUsername());
    assertTrue(response.getRequiresSensitiveAuth());
  }

  @Test
  void accessSensitiveEntry_ShouldReturnDecrypted_WhenCredentialsValid() {
    entry.setIsHighlySensitive(true);
    com.revature.passwordmanager.dto.request.SensitiveAccessRequest request = new com.revature.passwordmanager.dto.request.SensitiveAccessRequest();
    request.setMasterPassword("password");

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
    when(encryptionUtil.deriveKey(eq("password"), anyString())).thenReturn(mockKey);
    when(encryptionService.decrypt(anyString(), any())).thenReturn("decrypted");

    VaultEntryDetailResponse response = vaultService.accessSensitiveEntry("testuser", 1L, request);

    assertNotNull(response);
    assertEquals("decrypted", response.getPassword());
    assertEquals("decrypted", response.getUsername());
  }

  @Test
  void accessSensitiveEntry_ShouldThrowException_WhenPasswordInvalid() {
    entry.setIsHighlySensitive(true);
    com.revature.passwordmanager.dto.request.SensitiveAccessRequest request = new com.revature.passwordmanager.dto.request.SensitiveAccessRequest();
    request.setMasterPassword("wrongpassword");

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
    when(encryptionUtil.deriveKey(eq("wrongpassword"), anyString())).thenThrow(new RuntimeException("Bad key"));

    assertThrows(com.revature.passwordmanager.exception.AuthenticationException.class,
        () -> vaultService.accessSensitiveEntry("testuser", 1L, request));
  }

  @Test
  void createEntry_ShouldReturnResponse_WhenValidRequest() {
    VaultEntryRequest request = new VaultEntryRequest();
    request.setTitle("Test Entry");
    request.setCategoryId(1L);
    request.setFolderId(1L);
    request.setPassword("password");
    request.setUsername("user");

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    when(folderRepository.findById(1L)).thenReturn(Optional.of(folder));
    when(encryptionUtil.deriveKey(anyString(), anyString())).thenReturn(mockKey);
    when(encryptionService.encrypt(anyString(), any())).thenReturn("encrypted");
    when(vaultEntryRepository.save(any(VaultEntry.class))).thenReturn(entry);

    VaultEntryResponse response = vaultService.createEntry("testuser", request);

    assertNotNull(response);
    assertEquals("Test Entry", response.getTitle());
    verify(vaultEntryRepository).save(any(VaultEntry.class));
  }

  @Test
  void getEntry_ShouldReturnDetailResponse_WhenFound() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
    when(encryptionUtil.deriveKey(anyString(), anyString())).thenReturn(mockKey);
    when(encryptionService.decrypt(anyString(), any())).thenReturn("decrypted");

    VaultEntryDetailResponse response = vaultService.getEntry("testuser", 1L);

    assertNotNull(response);
    assertEquals("decrypted", response.getPassword());
    assertEquals("decrypted", response.getUsername());
  }

  @Test
  void toggleFavorite_ShouldToggleStatus_WhenEntryExists() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(entry));
    when(vaultEntryRepository.save(any(VaultEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // Initial state is false (default)
    VaultEntryResponse response = vaultService.toggleFavorite("testuser", 1L);

    assertNotNull(response);
    assertTrue(response.getIsFavorite());
    verify(vaultEntryRepository).save(any(VaultEntry.class));
  }
}
