package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.config.JwtConfig;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private AuthenticationManager authenticationManager;

  @Mock
  private JwtTokenProvider tokenProvider;

  @Mock
  private JwtConfig jwtConfig;

  @Mock
  private com.revature.passwordmanager.service.auth.SessionService sessionService;

  @Mock
  private com.revature.passwordmanager.service.auth.SecurityQuestionService securityQuestionService;

  @Mock
  private com.revature.passwordmanager.service.email.EmailService emailService;

  @Mock
  private com.revature.passwordmanager.service.auth.OtpService otpService;

  @InjectMocks
  private AuthenticationService authenticationService;

  private LoginRequest loginRequest;
  private User user;

  @BeforeEach
  void setUp() {
    loginRequest = new LoginRequest();
    loginRequest.setUsername("testuser");
    loginRequest.setMasterPassword("password123");

    user = new User();
    user.setUsername("testuser");
    user.setEmail("test@example.com");
    user.setMasterPasswordHash("hashedPassword");
  }

  @Test
  void testLogin_Success() {
    when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(user));
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(mock(Authentication.class));
    when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("accessToken");
    when(tokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("refreshToken");
    when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);

    jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
    AuthResponse response = authenticationService.login(loginRequest, mockRequest);

    assertNotNull(response);
    assertEquals("accessToken", response.getAccessToken());
    assertEquals("refreshToken", response.getRefreshToken());
    assertEquals("testuser", response.getUsername());
    assertEquals(900000L, response.getExpiresIn());
    assertFalse(response.isRequires2FA());

    verify(sessionService).createSession(eq(user), eq("accessToken"), eq(mockRequest));
  }

  @Test
  void testLogin_UserNotFound() {
    when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.empty());
    when(userRepository.findByEmail(loginRequest.getUsername())).thenReturn(Optional.empty());

    jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
    assertThrows(AuthenticationException.class, () -> authenticationService.login(loginRequest, mockRequest));
  }

  @Test
  void testRefreshToken_Success() {
    com.revature.passwordmanager.dto.request.RefreshTokenRequest refreshRequest = new com.revature.passwordmanager.dto.request.RefreshTokenRequest();
    refreshRequest.setRefreshToken("validRefreshToken");

    when(tokenProvider.validateToken("validRefreshToken")).thenReturn(true);
    when(tokenProvider.getUsernameFromToken("validRefreshToken")).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

    when(tokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("newAccessToken");
    when(tokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("newRefreshToken");
    when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);

    jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
    AuthResponse response = authenticationService.refreshToken(refreshRequest, mockRequest);

    assertNotNull(response);
    assertEquals("newAccessToken", response.getAccessToken());
    assertEquals("newRefreshToken", response.getRefreshToken());
    assertEquals("testuser", response.getUsername());
    assertEquals(900000L, response.getExpiresIn());

    verify(sessionService).createSession(eq(user), eq("newAccessToken"), eq(mockRequest));
  }

  @Test
  void testRefreshToken_Invalid() {
    com.revature.passwordmanager.dto.request.RefreshTokenRequest refreshRequest = new com.revature.passwordmanager.dto.request.RefreshTokenRequest();
    refreshRequest.setRefreshToken("invalidToken");

    when(tokenProvider.validateToken("invalidToken")).thenReturn(false);

    jakarta.servlet.http.HttpServletRequest mockRequest = mock(jakarta.servlet.http.HttpServletRequest.class);
    assertThrows(AuthenticationException.class, () -> authenticationService.refreshToken(refreshRequest, mockRequest));
  }

  @Test
  void testLogout() {
    String authHeader = "Bearer validAccessToken";
    authenticationService.logout(authHeader);
    verify(sessionService).terminateSessionByToken("validAccessToken");
  }

  @Test
  void testLogout_InvalidHeader() {
    authenticationService.logout(null);
    verify(sessionService, never()).terminateSessionByToken(anyString());

    authenticationService.logout("InvalidHeader");
    verify(sessionService, never()).terminateSessionByToken(anyString());
  }

  @Test
  void testGetSecurityQuestions_Success() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    com.revature.passwordmanager.model.user.SecurityQuestion sq = new com.revature.passwordmanager.model.user.SecurityQuestion();
    sq.setQuestionText("Q1");
    when(securityQuestionService.getSecurityQuestions(user)).thenReturn(java.util.Collections.singletonList(sq));

    java.util.List<com.revature.passwordmanager.dto.SecurityQuestionDTO> result = authenticationService
        .getSecurityQuestions("testuser");

    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Q1", result.get(0).getQuestion());
  }

  @Test
  void testSendOtp_Success() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(otpService.generateOtp(user, "EMAIL")).thenReturn("123456");

    authenticationService.sendOtp("testuser");

    verify(emailService).sendOtpEmail("test@example.com", "123456");
  }

}
