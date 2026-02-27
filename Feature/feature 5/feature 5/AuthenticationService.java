package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;

  public AuthResponse login(LoginRequest request) {
    try {
      Authentication authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              request.getUsername(),
              request.getMasterPassword()));

      User user = userRepository.findByUsername(request.getUsername())
          .or(() -> userRepository.findByEmail(request.getUsername()))
          .orElseThrow(() -> new AuthenticationException("User not found"));

      String accessToken = jwtTokenProvider.generateAccessToken(authentication);
      String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

      UserResponse userResponse = UserResponse.builder()
          .id(user.getId())
          .email(user.getEmail())
          .username(user.getUsername())
          .is2faEnabled(user.is2faEnabled())
          .createdAt(user.getCreatedAt())
          .build();

      return AuthResponse.builder()
          .accessToken(accessToken)
          .refreshToken(refreshToken)
          .user(userResponse)
          .build();

    } catch (org.springframework.security.core.AuthenticationException e) {
      throw new AuthenticationException("Invalid username or password");
    }
  }
}
