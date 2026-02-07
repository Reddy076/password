package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.security.MasterPasswordValidator;
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
  private final MasterPasswordValidator masterPasswordValidator;

  @Transactional
  public UserResponse registerUser(RegistrationRequest request) {
    // 1. Check if user already exists
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new AuthenticationException("Email is already in use");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new AuthenticationException("Username is already taken");
    }

    // 2. Validate master password strength
    if (!masterPasswordValidator.isValid(request.getMasterPassword())) {
      throw new AuthenticationException("Weak master password: " + masterPasswordValidator.getRequirementsMessage());
    }

    // 3. Generate Salt (Random UUID for simplicity in this context, or could be
    // CSPRNG bytes)
    String salt = UUID.randomUUID().toString();

    // 4. Create User Entity
    User newUser = User.builder()
        .email(request.getEmail())
        .username(request.getUsername())
        .masterPasswordHash(passwordEncoder.encode(request.getMasterPassword())) // Store BCrypt hash for auth
        .salt(salt)
        .is2faEnabled(false)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();

    // 5. Save to DB
    User savedUser = userRepository.save(newUser);

    // 6. Return Response
    return UserResponse.builder()
        .id(savedUser.getId())
        .email(savedUser.getEmail())
        .username(savedUser.getUsername())
        .is2faEnabled(savedUser.is2faEnabled())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }
}
