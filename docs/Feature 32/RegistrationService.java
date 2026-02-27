package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  // JwtTokenProvider removed
  // JwtConfig removed
  // UserSettingsService removed as it is not yet implemented
  private final SecurityQuestionService securityQuestionService;

  @Transactional
  public UserResponse registerUser(RegistrationRequest request) {
    // 1. Check if user exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new AuthenticationException("Email already in use");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new AuthenticationException("Username already in use");
    }

    // 2. Validate password hint
    if (request.getPasswordHint() != null && !request.getPasswordHint().trim().isEmpty()) {
      if (request.getPasswordHint().toLowerCase().contains(request.getMasterPassword().toLowerCase())) {
        throw new AuthenticationException("Password hint cannot contain the master password");
      }
    }

    // 3. Create User
    User user = User.builder()
        .email(request.getEmail())
        .username(request.getUsername())
        .masterPasswordHash(passwordEncoder.encode(request.getMasterPassword()))
        .passwordHint(request.getPasswordHint())
        .salt(UUID.randomUUID().toString())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .is2faEnabled(false)
        .build();

    // 4. Save to DB
    User savedUser = userRepository.save(user);

    // 5. Save Security Questions
    securityQuestionService.saveSecurityQuestions(savedUser, request.getSecurityQuestions());

    // 6. Return Response (No tokens generated for registration as per requirements)
    return UserResponse.builder()
        .id(savedUser.getId())
        .email(savedUser.getEmail())
        .username(savedUser.getUsername())
        .is2faEnabled(savedUser.is2faEnabled())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }
}
