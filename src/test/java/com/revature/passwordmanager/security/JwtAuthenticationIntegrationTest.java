package com.revature.passwordmanager.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.ChangePasswordRequest;
import com.revature.passwordmanager.service.auth.AuthenticationService;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(JwtAuthenticationIntegrationTest.Mocks.class)
public class JwtAuthenticationIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private SessionService sessionService;

  @MockBean
  private CustomUserDetailsService customUserDetailsService;

  @TestConfiguration
  static class Mocks {
    @Bean
    @Primary
    public AuthenticationService authenticationService() {
      return Mockito.mock(AuthenticationService.class);
    }

    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
      return Mockito.mock(JavaMailSender.class);
    }
  }

  // We need to mock UserService because the controller calls it
  @MockBean
  private UserService userService;

  @Autowired
  private ObjectMapper objectMapper;

  private String validToken;

  @BeforeEach
  void setUp() {
    UserDetails userDetails = new org.springframework.security.core.userdetails.User(
        "testuser",
        "hashedPassword",
        new ArrayList<>());

    when(customUserDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

    Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    validToken = jwtTokenProvider.generateAccessToken(auth);

    when(sessionService.isSessionActive(validToken)).thenReturn(true);
  }

  @Test
  void testChangePassword_WithValidToken_ShouldSucceed() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setOldPassword("oldPassword");
    request.setNewPassword("newSafePassword123!");

    mockMvc.perform(put("/api/users/change-password")
        .header("Authorization", "Bearer " + validToken)
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message")
            .value("Password changed successfully"));
  }

  @Test
  void testChangePassword_WithInvalidToken_ShouldFail() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest();
    request.setOldPassword("oldPassword");
    request.setNewPassword("newSafePassword123!");

    when(sessionService.isSessionActive("invalid_token")).thenReturn(false);

    mockMvc.perform(put("/api/users/change-password")
        .header("Authorization", "Bearer invalid_token")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }
}
