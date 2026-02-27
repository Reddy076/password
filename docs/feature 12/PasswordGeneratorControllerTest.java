package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.request.PasswordGeneratorRequest;
import com.revature.passwordmanager.dto.response.PasswordStrengthResponse;
import com.revature.passwordmanager.service.security.PasswordGeneratorService;
import com.revature.passwordmanager.service.security.PasswordStrengthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(PasswordGeneratorController.class)
@AutoConfigureMockMvc(addFilters = false) // Disable security filters for this unit test
public class PasswordGeneratorControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private PasswordGeneratorService generatorService;

  @MockBean
  private PasswordStrengthService strengthService;

  @MockBean
  private com.revature.passwordmanager.security.JwtTokenProvider jwtTokenProvider;

  @MockBean
  private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

  @MockBean
  private com.revature.passwordmanager.service.auth.SessionService sessionService;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void generatePassword_WithBody_ShouldReturnGeneratedPassword() throws Exception {
    PasswordGeneratorRequest request = new PasswordGeneratorRequest();
    request.setLength(12);

    when(generatorService.generatePassword(any(PasswordGeneratorRequest.class))).thenReturn("generatedPass");

    mockMvc.perform(post("/api/generator/generate")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.password").value("generatedPass"));
  }

  @Test
  void generatePassword_NoBody_ShouldUseDefaultsAndReturnPassword() throws Exception {
    // When no body is provided, controller creates default request
    when(generatorService.generatePassword(any(PasswordGeneratorRequest.class))).thenReturn("defaultPass");

    mockMvc.perform(post("/api/generator/generate")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.password").value("defaultPass"));
  }

  @Test
  void checkStrength_ShouldReturnScore() throws Exception {
    PasswordStrengthResponse response = new PasswordStrengthResponse(80, "Good", List.of());

    when(strengthService.analyzePassword(any())).thenReturn(response);

    Map<String, String> request = Map.of("password", "strongPass");

    mockMvc.perform(post("/api/generator/strength")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.score").value(80))
        .andExpect(jsonPath("$.label").value("Good"));
  }
}
