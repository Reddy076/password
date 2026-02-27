package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.config.SecurityConfig;
import com.revature.passwordmanager.dto.request.LoginRequest;
import com.revature.passwordmanager.dto.request.RegistrationRequest;
import com.revature.passwordmanager.dto.response.AuthResponse;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.AuthenticationService;
import com.revature.passwordmanager.service.auth.RegistrationService;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, com.revature.passwordmanager.security.JwtAuthenticationFilter.class })
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
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private com.revature.passwordmanager.service.auth.AccountRecoveryService accountRecoveryService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    void testRegister() throws Exception {
        RegistrationRequest request = new RegistrationRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setMasterPassword("password123!");

        com.revature.passwordmanager.dto.SecurityQuestionDTO q1 = new com.revature.passwordmanager.dto.SecurityQuestionDTO(
                "Q", "A");
        request.setSecurityQuestions(java.util.Arrays.asList(q1, q1, q1));

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
    void testGetSecurityQuestions() throws Exception {
        when(authenticationService.getSecurityQuestions("testuser")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/auth/security-questions/testuser"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void testResetPassword() throws Exception {
        com.revature.passwordmanager.dto.request.RecoveryRequest request = new com.revature.passwordmanager.dto.request.RecoveryRequest();
        request.setUsername("testuser");
        request.setNewMasterPassword("newPassword123!");
        // Security answers validation usually happens in Service, but List size
        // validation in DTO
        // DTO requires exactly 3 answers.
        com.revature.passwordmanager.dto.SecurityQuestionDTO q1 = new com.revature.passwordmanager.dto.SecurityQuestionDTO(
                "Q", "A");
        request.setSecurityAnswers(java.util.Arrays.asList(q1, q1, q1));

        doNothing().when(accountRecoveryService)
                .resetPassword(any(com.revature.passwordmanager.dto.request.RecoveryRequest.class));

        mockMvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("Password changed successfully"));
    }

    @Test
    @WithMockUser
    void testSendOtp() throws Exception {
        doNothing().when(authenticationService).sendOtp("testuser");

        mockMvc.perform(post("/api/auth/send-otp")
                .param("username", "testuser"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string("OTP sent to your email."));
    }
}
