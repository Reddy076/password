package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.config.SecurityConfig;
import com.revature.passwordmanager.dto.SecurityQuestionDTO;
import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.request.RecoveryRequest;
import com.revature.passwordmanager.dto.request.RefreshTokenRequest;
import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.security.CustomUserDetailsService;
import com.revature.passwordmanager.security.JwtAuthenticationFilter;
import com.revature.passwordmanager.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.revature.passwordmanager.service.auth.AccountRecoveryService;
import com.revature.passwordmanager.service.auth.AuthenticationService;
import com.revature.passwordmanager.service.auth.RegistrationService;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.security.DuressService;
import com.revature.passwordmanager.service.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = {
                UserDetailsServiceAutoConfiguration.class
})
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class,
                com.revature.passwordmanager.security.RateLimitFilter.class })
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private RegistrationService registrationService;

        @MockBean
        private AuthenticationService authenticationService;

        @MockBean
        private JwtTokenProvider jwtTokenProvider;

        @MockBean
        private SessionService sessionService;

        @MockBean
        private RateLimitService rateLimitService;

        @MockBean
        private CustomUserDetailsService customUserDetailsService;

        @MockBean
        private AccountRecoveryService accountRecoveryService;

        @MockBean
        private DuressService duressService;

        @MockBean
        private UserRepository userRepository;

        @MockBean
        private com.revature.passwordmanager.service.security.CaptchaService captchaService;

        @MockBean
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(true);
                Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                                ArgumentMatchers.anyString())).thenReturn(100);
        }

        @Test
        @WithMockUser
        void testRegister() throws Exception {
                RegistrationRequest request = new RegistrationRequest();
                request.setUsername("testuser");
                request.setEmail("test@example.com");
                request.setMasterPassword("password123!");

                SecurityQuestionDTO q1 = new SecurityQuestionDTO(
                                "Q", "A");
                request.setSecurityQuestions(Arrays.asList(q1, q1, q1));

                UserResponse response = UserResponse.builder()
                                .id(1L)
                                .username("testuser")
                                .email("test@example.com")
                                .build();
                when(registrationService.registerUser(any(RegistrationRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser
        void testLogin() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setUsername("testuser");
                request.setMasterPassword("password");

                AuthResponse response = AuthResponse.builder().accessToken("token").build();
                when(authenticationService.login(any(LoginRequest.class), any())).thenReturn(response);

                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        void testRefreshToken() throws Exception {
                RefreshTokenRequest request = new RefreshTokenRequest();
                request.setRefreshToken("valid-refresh-token");

                AuthResponse response = AuthResponse.builder()
                                .accessToken("new-access-token")
                                .refreshToken("new-refresh-token")
                                .build();
                when(authenticationService.refreshToken(any(RefreshTokenRequest.class), any())).thenReturn(response);

                mockMvc.perform(post("/api/auth/refresh-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("new-access-token"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void testLogout() throws Exception {
                doNothing().when(authenticationService).logout(anyString(), anyString());

                mockMvc.perform(post("/api/auth/logout")
                                .header("Authorization", "Bearer test-token"))
                                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser
        void testGetSecurityQuestions() throws Exception {
                when(authenticationService.getSecurityQuestions("testuser")).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/api/auth/security-questions/testuser"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser
        void testResetPassword() throws Exception {
                RecoveryRequest request = new RecoveryRequest();
                request.setUsername("testuser");
                request.setNewMasterPassword("newPassword123!");
                // Security answers validation usually happens in Service, but List size
                // validation in DTO
                // DTO requires exactly 3 answers.
                SecurityQuestionDTO q1 = new SecurityQuestionDTO(
                                "Q", "A");
                request.setSecurityAnswers(Arrays.asList(q1, q1, q1));

                doNothing().when(accountRecoveryService)
                                .resetPassword(any(RecoveryRequest.class));

                mockMvc.perform(post("/api/auth/reset-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content()
                                                .string("Password changed successfully"));
        }

        @Test
        @WithMockUser
        void testVerifyOtp() throws Exception {
                AuthResponse response = AuthResponse.builder()
                                .accessToken("otp-verified-token")
                                .username("testuser")
                                .build();
                when(authenticationService.verifyOtp(anyString(), anyString(), any())).thenReturn(response);

                mockMvc.perform(post("/api/auth/verify-otp")
                                .param("username", "testuser")
                                .param("code", "123456"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("otp-verified-token"));
        }

        @Test
        @WithMockUser
        void testSendOtp() throws Exception {
                doNothing().when(authenticationService).sendOtp("testuser");

                mockMvc.perform(post("/api/auth/send-otp")
                                .param("username", "testuser"))
                                .andExpect(status().isOk())
                                .andExpect(content()
                                                .string("OTP sent to your email."));
        }

        @Test
        @WithMockUser
        void testDuressLogin_Success() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setUsername("testuser");
                request.setMasterPassword("duressPassword");

                AuthResponse response = AuthResponse.builder()
                                .accessToken("duressToken")
                                .username("testuser")
                                .build();
                when(authenticationService.duressLogin(any(LoginRequest.class), any())).thenReturn(response);

                mockMvc.perform(post("/api/auth/duress-login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("duressToken"))
                                .andExpect(jsonPath("$.username").value("testuser"));
        }

        @Test
        @WithMockUser
        void testDuressLogin_Unauthorized() throws Exception {
                LoginRequest request = new LoginRequest();
                request.setUsername("testuser");
                request.setMasterPassword("wrongPassword");

                when(authenticationService.duressLogin(any(LoginRequest.class), any()))
                                .thenThrow(new com.revature.passwordmanager.exception.AuthenticationException(
                                                "Invalid username or password"));

                mockMvc.perform(post("/api/auth/duress-login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(username = "testuser")
        void testSetDuressPassword() throws Exception {
                doNothing().when(duressService).setDuressPassword(anyString(), anyString());

                Map<String, String> request = Map.of("duressPassword", "myDuressPass");

                mockMvc.perform(post("/api/auth/set-duress-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Duress password set successfully"));
        }

        @Test
        @WithMockUser
        void testGetPasswordHint() throws Exception {
                User user = new User();
                user.setUsername("testuser");
                user.setPasswordHint("My first pet");

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                mockMvc.perform(get("/api/auth/password-hint/testuser"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.hint").value("My first pet"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void testSetPasswordHint_Success() throws Exception {
                User user = new User();
                user.setUsername("testuser");
                user.setMasterPasswordHash("hashedPassword");

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("masterPassword123", "hashedPassword")).thenReturn(true);
                when(userRepository.save(any(User.class))).thenReturn(user);

                Map<String, String> request = Map.of(
                                "hint", "My first pet",
                                "masterPassword", "masterPassword123");

                mockMvc.perform(put("/api/auth/password-hint")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Password hint updated"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void testSetPasswordHint_NoMasterPassword() throws Exception {
                User user = new User();
                user.setUsername("testuser");

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

                Map<String, String> request = Map.of("hint", "My first pet"); // Missing masterPassword

                mockMvc.perform(put("/api/auth/password-hint")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Master password is required to set a hint"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void testSetPasswordHint_HintContainsPassword() throws Exception {
                User user = new User();
                user.setUsername("testuser");
                user.setMasterPasswordHash("hashedPassword");

                when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
                when(passwordEncoder.matches("masterPassword123", "hashedPassword")).thenReturn(true);

                Map<String, String> request = Map.of(
                                "hint", "My MasterPassword123 is terrible",
                                "masterPassword", "masterPassword123");

                mockMvc.perform(put("/api/auth/password-hint")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(content().string("Password hint cannot contain the master password"));
        }
}
