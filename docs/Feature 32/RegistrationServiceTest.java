package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.service.auth.RegistrationService;
import com.revature.passwordmanager.service.auth.SecurityQuestionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  // JwtTokenProvider mock removed
  // JwtConfig mock removed

  @Mock
  private SecurityQuestionService securityQuestionService;

  @InjectMocks
  private RegistrationService registrationService;

  private RegistrationRequest request;

  @BeforeEach
  void setUp() {
    request = new RegistrationRequest();
    request.setUsername("testuser");
    request.setEmail("test@example.com");
    request.setMasterPassword("StrongPassword123!");
    // Security questions are not strictly validated in Service unless we add
    // validation there,
    // but DTO validation handles it in Controller.
  }

  @Test
  void testRegisterUser_Success() {
    request.setPasswordHint("My favorite color");

    when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
    when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
    // masterPasswordValidator was removed from Service in recent edits (replaced by
    // DTO validation?)
    // Let's check RegistrationService code. I removed MasterPasswordValidator.

    when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
      User user = invocation.getArgument(0);
      assertNotNull(user.getSalt(), "Salt must not be null");
      assertEquals("My favorite color", user.getPasswordHint());
      user.setId(1L);
      return user;
    });

    // Token generation mocks removed

    UserResponse response = registrationService.registerUser(request);

    assertNotNull(response);
    assertEquals(1L, response.getId());
    assertEquals(request.getUsername(), response.getUsername());
    assertEquals(request.getEmail(), response.getEmail());
    assertFalse(response.is2faEnabled());
    assertNotNull(response.getCreatedAt());

    verify(userRepository).save(any(User.class));
    verify(securityQuestionService).saveSecurityQuestions(any(User.class), any());
  }

  @Test
  void testRegisterUser_DuplicateUsername() {
    when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);
    when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

    assertThrows(AuthenticationException.class, () -> registrationService.registerUser(request));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void testRegisterUser_DuplicateEmail() {
    when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

    assertThrows(AuthenticationException.class, () -> registrationService.registerUser(request));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void testRegisterUser_HintContainsPassword() {
    request.setPasswordHint("My password is StrongPassword123!");

    when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
    when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);

    AuthenticationException exception = assertThrows(AuthenticationException.class,
        () -> registrationService.registerUser(request));
    assertEquals("Password hint cannot contain the master password", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
  }
}
