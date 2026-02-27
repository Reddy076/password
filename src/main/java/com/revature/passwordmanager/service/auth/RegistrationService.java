package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.service.email.EmailService;
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
  private final SecurityQuestionService securityQuestionService;
  private final OtpService otpService;
  private final EmailService emailService;

  @Transactional
  public UserResponse registerUser(RegistrationRequest request) {

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new AuthenticationException("Email already in use");
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new AuthenticationException("Username already in use");
    }

    if (request.getPasswordHint() != null && !request.getPasswordHint().trim().isEmpty()) {
      if (request.getPasswordHint().toLowerCase().contains(request.getMasterPassword().toLowerCase())) {
        throw new AuthenticationException("Password hint cannot contain the master password");
      }
    }

    User user = User.builder()
        .email(request.getEmail())
        .username(request.getUsername())
        .masterPasswordHash(passwordEncoder.encode(request.getMasterPassword()))
        .passwordHint(request.getPasswordHint())
        .salt(UUID.randomUUID().toString())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .is2faEnabled(false)
        .emailVerified(false)
        .build();

    User savedUser = userRepository.save(user);

    securityQuestionService.saveSecurityQuestions(savedUser, request.getSecurityQuestions());

    String otpCode = otpService.generateOtp(savedUser, "EMAIL_VERIFICATION");
    emailService.sendOtpEmail(savedUser.getEmail(), otpCode);

    return UserResponse.builder()
        .id(savedUser.getId())
        .email(savedUser.getEmail())
        .username(savedUser.getUsername())
        .is2faEnabled(savedUser.is2faEnabled())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }

  @Transactional
  public void verifyEmail(String username, String otpCode) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    if (Boolean.TRUE.equals(user.getEmailVerified())) {
      throw new AuthenticationException("Email is already verified");
    }

    otpService.validateOtp(user, otpCode, "EMAIL_VERIFICATION");

    user.setEmailVerified(true);
    userRepository.save(user);
  }

  @Transactional
  public void resendVerificationOtp(String username) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElseThrow(() -> new AuthenticationException("User not found"));

    if (Boolean.TRUE.equals(user.getEmailVerified())) {
      throw new AuthenticationException("Email is already verified");
    }

    String otpCode = otpService.generateOtp(user, "EMAIL_VERIFICATION");
    emailService.sendOtpEmail(user.getEmail(), otpCode);
  }
}
