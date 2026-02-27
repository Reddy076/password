package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.config.SecurityConfig;
import com.revature.passwordmanager.dto.response.HealthResponse;
import com.revature.passwordmanager.security.CustomUserDetailsService;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.security.RateLimitService;
import com.revature.passwordmanager.service.system.HealthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HealthController.class, excludeAutoConfiguration = {
    org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
@Import(SecurityConfig.class)
public class HealthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private HealthService healthService;

  @MockBean
  private CustomUserDetailsService customUserDetailsService;

  @MockBean
  private JwtTokenProvider jwtTokenProvider;

  @MockBean
  private SessionService sessionService;

  @MockBean
  private RateLimitService rateLimitService;

  @BeforeEach
  void setUp() {
    when(rateLimitService.isAllowed(anyString(), anyString())).thenReturn(true);
    when(rateLimitService.getRemainingRequests(anyString(), anyString())).thenReturn(100);
  }

  @Test
  @WithMockUser(username = "testuser")
  void getHealth_ShouldReturnHealthStatus() throws Exception {
    HealthResponse.ComponentHealth dbHealth = HealthResponse.ComponentHealth.builder()
        .status("UP")
        .details("Database is reachable")
        .build();

    HealthResponse response = HealthResponse.builder()
        .status("UP")
        .timestamp(LocalDateTime.now())
        .components(Map.of("database", dbHealth))
        .build();

    when(healthService.getHealthStatus()).thenReturn(response);

    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.components.database.status").value("UP"))
        .andExpect(jsonPath("$.components.database.details").value("Database is reachable"));
  }

  @Test
  void getHealth_WithoutAuth_ShouldReturnHealthStatus() throws Exception {
    HealthResponse healthResponse = HealthResponse.builder()
        .status("UP")
        .version("1.0.0") // Assuming HealthResponse has a version field or it's part of the map
        .timestamp(LocalDateTime.now())
        .build();
    when(healthService.getHealthStatus()).thenReturn(healthResponse);

    mockMvc.perform(get("/api/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.version").value("1.0.0"));
  }

  @Test
  @WithMockUser(username = "testuser")
  void getDbHealth_ShouldReturnDbHealth() throws Exception {
    HealthResponse.ComponentHealth dbHealth = HealthResponse.ComponentHealth.builder()
        .status("UP")
        .details("Database is reachable")
        .build();

    when(healthService.getDbHealth()).thenReturn(dbHealth);

    mockMvc.perform(get("/api/health/db"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.details").value("Database is reachable"));
  }

  @Test
  @WithMockUser(username = "testuser")
  void getServicesHealth_ShouldReturnServicesHealth() throws Exception {
    HealthResponse.ComponentHealth svcHealth = HealthResponse.ComponentHealth.builder()
        .status("UP")
        .details("External services are reachable")
        .build();

    when(healthService.getServicesHealth()).thenReturn(svcHealth);

    mockMvc.perform(get("/api/health/services"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"))
        .andExpect(jsonPath("$.details").value("External services are reachable"));
  }
}
