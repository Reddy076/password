package com.revature.passwordmanager.service.security;

import com.revature.passwordmanager.dto.response.SecurityAuditResponse;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.util.PasswordStrengthCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceTest {

  @Mock
  private VaultEntryRepository vaultEntryRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private EncryptionService encryptionService;
  @Mock
  private PasswordStrengthCalculator passwordStrengthCalculator;

  @InjectMocks
  private SecurityAuditService securityAuditService;

  private User user;
  private SecretKey mockKey;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .id(1L)
        .username("testuser")
        .masterPasswordHash("hash")
        .salt("c2FsdA==")
        .build();
    mockKey = mock(SecretKey.class);
  }

  @Test
  void generateAuditReport_EmptyVault_ShouldReturnPerfectScore() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of());
    when(encryptionService.decodeKey("c2FsdA==")).thenReturn(mockKey);

    SecurityAuditResponse report = securityAuditService.generateAuditReport("testuser");

    assertEquals(0, report.getTotalEntries());
    assertEquals(100, report.getSecurityScore());
    assertEquals(0, report.getWeakCount());
  }

  @Test
  void generateAuditReport_WithWeakPassword_ShouldFlag() {
    VaultEntry entry = VaultEntry.builder()
        .id(1L).user(user).title("Test Site")
        .password("encrypted").websiteUrl("http://test.com")
        .updatedAt(LocalDateTime.now())
        .build();

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(entry));
    when(encryptionService.decodeKey("c2FsdA==")).thenReturn(mockKey);
    when(encryptionService.decrypt("encrypted", mockKey)).thenReturn("weak");
    when(passwordStrengthCalculator.calculateScore("weak")).thenReturn(30);

    SecurityAuditResponse report = securityAuditService.generateAuditReport("testuser");

    assertEquals(1, report.getTotalEntries());
    assertEquals(1, report.getWeakCount());
    assertFalse(report.getWeakPasswords().isEmpty());
  }

  @Test
  void generateAuditReport_WithOldPassword_ShouldFlag() {
    VaultEntry entry = VaultEntry.builder()
        .id(1L).user(user).title("Old Site")
        .password("encrypted").websiteUrl("http://old.com")
        .updatedAt(LocalDateTime.now().minusDays(100))
        .build();

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(entry));
    when(encryptionService.decodeKey("c2FsdA==")).thenReturn(mockKey);
    when(encryptionService.decrypt("encrypted", mockKey)).thenReturn("StrongP@ss123!");
    when(passwordStrengthCalculator.calculateScore("StrongP@ss123!")).thenReturn(90);

    SecurityAuditResponse report = securityAuditService.generateAuditReport("testuser");

    assertEquals(1, report.getOldCount());
    assertFalse(report.getOldPasswords().isEmpty());
  }

  @Test
  void generateAuditReport_WithReusedPasswords_ShouldFlag() {
    VaultEntry entry1 = VaultEntry.builder()
        .id(1L).user(user).title("Site A").password("enc1")
        .updatedAt(LocalDateTime.now()).build();
    VaultEntry entry2 = VaultEntry.builder()
        .id(2L).user(user).title("Site B").password("enc2")
        .updatedAt(LocalDateTime.now()).build();

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L))
        .thenReturn(List.of(entry1, entry2));
    when(encryptionService.decodeKey("c2FsdA==")).thenReturn(mockKey);
    when(encryptionService.decrypt("enc1", mockKey)).thenReturn("SamePassword!");
    when(encryptionService.decrypt("enc2", mockKey)).thenReturn("SamePassword!");
    when(passwordStrengthCalculator.calculateScore("SamePassword!")).thenReturn(80);

    SecurityAuditResponse report = securityAuditService.generateAuditReport("testuser");

    assertEquals(2, report.getReusedCount());
  }
}
