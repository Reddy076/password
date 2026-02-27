package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.ChatRequest;
import com.revature.passwordmanager.dto.response.AIInsightResponse;
import com.revature.passwordmanager.dto.response.AIInsightResponse.Insight;
import com.revature.passwordmanager.dto.response.ChatResponse;
import com.revature.passwordmanager.dto.response.ChatResponse.MessageDto;
import com.revature.passwordmanager.security.JwtTokenProvider;
import com.revature.passwordmanager.service.ai.AIAssistantService;
import com.revature.passwordmanager.service.auth.SessionService;
import com.revature.passwordmanager.service.security.RateLimitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Feature 41 – AI Password Assistant.
 *
 * <p>Full controller-layer tests for {@link AIAssistantController}.
 * Each test verifies both the HTTP status and the JSON response body fields.</p>
 */
@WebMvcTest(controllers = AIAssistantController.class,
        excludeAutoConfiguration = {UserDetailsServiceAutoConfiguration.class})
class AIAssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AIAssistantService assistantService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private RateLimitService rateLimitService;

    private ChatResponse generalChatResponse;
    private ChatResponse generatePasswordResponse;
    private AIInsightResponse insightResponse;

    @BeforeEach
    void setUp() {
        Mockito.when(rateLimitService.isAllowed(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(true);
        Mockito.when(rateLimitService.getRemainingRequests(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString())).thenReturn(100);

        MessageDto userMsg = MessageDto.builder()
                .role("user")
                .content("Hello, how can you help me?")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
        MessageDto assistantMsg = MessageDto.builder()
                .role("assistant")
                .content("I can help you with password security!")
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0, 1))
                .build();

        generalChatResponse = ChatResponse.builder()
                .reply("I can help you with password security!")
                .intent("GENERAL")
                .generatedPassword(null)
                .suggestions(List.of("Generate a strong password", "Check for breaches"))
                .timestamp(LocalDateTime.of(2026, 1, 1, 10, 0, 1))
                .aiPowered(false)
                .history(List.of(userMsg, assistantMsg))
                .build();

        generatePasswordResponse = ChatResponse.builder()
                .reply("Here's a strong password for you!")
                .intent("GENERATE_PASSWORD")
                .generatedPassword("Str0ng!Pass#2026")
                .suggestions(List.of("Save this password to your vault", "Generate another password"))
                .timestamp(LocalDateTime.of(2026, 1, 1, 10, 0, 2))
                .aiPowered(false)
                .history(List.of())
                .build();

        insightResponse = AIInsightResponse.builder()
                .overallAssessment("Your vault security is GOOD. Keep up the good work!")
                .securityScore(85)
                .aiPowered(false)
                .insights(List.of(
                        Insight.builder()
                                .severity("MEDIUM")
                                .title("Outdated Passwords")
                                .description("2 passwords haven't been changed in over 90 days.")
                                .affectedCount(2)
                                .build(),
                        Insight.builder()
                                .severity("INFO")
                                .title("Enable Breach Monitoring")
                                .description("Run a breach scan to check for compromised passwords.")
                                .affectedCount(5)
                                .build()))
                .build();
    }

    // ── POST /api/ai/chat ─────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void chat_ValidMessage_ShouldReturnChatResponse() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("Hello, how can you help me?")
                .build();

        when(assistantService.chat(eq("testuser"), any(ChatRequest.class)))
                .thenReturn(generalChatResponse);

        mockMvc.perform(post("/api/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reply").value("I can help you with password security!"))
                .andExpect(jsonPath("$.intent").value("GENERAL"))
                .andExpect(jsonPath("$.generatedPassword").doesNotExist())
                .andExpect(jsonPath("$.suggestions[0]").value("Generate a strong password"))
                .andExpect(jsonPath("$.suggestions[1]").value("Check for breaches"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.aiPowered").value(false))
                .andExpect(jsonPath("$.history[0].role").value("user"))
                .andExpect(jsonPath("$.history[0].content").value("Hello, how can you help me?"))
                .andExpect(jsonPath("$.history[1].role").value("assistant"))
                .andExpect(jsonPath("$.history[1].content").value("I can help you with password security!"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void chat_GeneratePasswordIntent_ShouldIncludePassword() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .message("Generate a strong password for me")
                .build();

        when(assistantService.chat(eq("testuser"), any(ChatRequest.class)))
                .thenReturn(generatePasswordResponse);

        mockMvc.perform(post("/api/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Here's a strong password for you!"))
                .andExpect(jsonPath("$.intent").value("GENERATE_PASSWORD"))
                .andExpect(jsonPath("$.generatedPassword").value("Str0ng!Pass#2026"))
                .andExpect(jsonPath("$.suggestions[0]").value("Save this password to your vault"));
    }

    @Test
    @WithMockUser(username = "testuser")
    void chat_BlankMessage_ShouldReturn400() throws Exception {
        ChatRequest request = ChatRequest.builder().message("").build();

        mockMvc.perform(post("/api/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "testuser")
    void chat_MessageTooLong_ShouldReturn400() throws Exception {
        String longMessage = "a".repeat(2001);
        ChatRequest request = ChatRequest.builder().message(longMessage).build();

        mockMvc.perform(post("/api/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_Unauthenticated_ShouldReturn401() throws Exception {
        ChatRequest request = ChatRequest.builder().message("Hello").build();

        mockMvc.perform(post("/api/ai/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/ai/suggestions ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getSuggestions_ShouldReturnSuggestionList() throws Exception {
        ChatResponse suggestionsResponse = ChatResponse.builder()
                .reply("Here are some suggestions based on your vault:")
                .intent("SUGGESTIONS")
                .suggestions(List.of(
                        "Update 2 password(s) older than 90 days",
                        "Generate a strong password",
                        "Check for breached passwords",
                        "Enable 2FA for better security"))
                .timestamp(LocalDateTime.of(2026, 1, 1, 10, 0))
                .aiPowered(false)
                .history(List.of())
                .build();

        when(assistantService.getSuggestions("testuser")).thenReturn(suggestionsResponse);

        mockMvc.perform(get("/api/ai/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Here are some suggestions based on your vault:"))
                .andExpect(jsonPath("$.intent").value("SUGGESTIONS"))
                .andExpect(jsonPath("$.suggestions[0]").value("Update 2 password(s) older than 90 days"))
                .andExpect(jsonPath("$.suggestions[1]").value("Generate a strong password"))
                .andExpect(jsonPath("$.suggestions[2]").value("Check for breached passwords"))
                .andExpect(jsonPath("$.suggestions[3]").value("Enable 2FA for better security"))
                .andExpect(jsonPath("$.aiPowered").value(false));
    }

    @Test
    void getSuggestions_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/ai/suggestions"))
                .andExpect(status().isUnauthorized());
    }

    // ── POST /api/ai/generate-password ────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void generatePassword_NoContext_ShouldReturnPassword() throws Exception {
        when(assistantService.generatePassword("testuser", null))
                .thenReturn(generatePasswordResponse);

        mockMvc.perform(post("/api/ai/generate-password").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedPassword").value("Str0ng!Pass#2026"))
                .andExpect(jsonPath("$.intent").value("GENERATE_PASSWORD"))
                .andExpect(jsonPath("$.reply").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "testuser")
    void generatePassword_WithContext_ShouldPassContextToService() throws Exception {
        when(assistantService.generatePassword("testuser", "for my bank account"))
                .thenReturn(generatePasswordResponse);

        mockMvc.perform(post("/api/ai/generate-password")
                        .with(csrf())
                        .param("context", "for my bank account"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generatedPassword").value("Str0ng!Pass#2026"));
    }

    @Test
    void generatePassword_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/ai/generate-password").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/ai/security-insights ────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void getSecurityInsights_ShouldReturnInsightsWithScore() throws Exception {
        when(assistantService.getSecurityInsights("testuser")).thenReturn(insightResponse);

        mockMvc.perform(get("/api/ai/security-insights"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.overallAssessment")
                        .value("Your vault security is GOOD. Keep up the good work!"))
                .andExpect(jsonPath("$.securityScore").value(85))
                .andExpect(jsonPath("$.aiPowered").value(false))
                .andExpect(jsonPath("$.insights[0].severity").value("MEDIUM"))
                .andExpect(jsonPath("$.insights[0].title").value("Outdated Passwords"))
                .andExpect(jsonPath("$.insights[0].description")
                        .value("2 passwords haven't been changed in over 90 days."))
                .andExpect(jsonPath("$.insights[0].affectedCount").value(2))
                .andExpect(jsonPath("$.insights[1].severity").value("INFO"))
                .andExpect(jsonPath("$.insights[1].title").value("Enable Breach Monitoring"))
                .andExpect(jsonPath("$.insights[1].affectedCount").value(5));
    }

    @Test
    @WithMockUser(username = "testuser")
    void getSecurityInsights_EmptyVault_ShouldReturnPerfectScore() throws Exception {
        AIInsightResponse emptyResponse = AIInsightResponse.builder()
                .overallAssessment("Your vault is empty. Start adding passwords to track your security.")
                .securityScore(100)
                .insights(List.of())
                .aiPowered(false)
                .build();

        when(assistantService.getSecurityInsights("testuser")).thenReturn(emptyResponse);

        mockMvc.perform(get("/api/ai/security-insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityScore").value(100))
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.insights").isEmpty());
    }

    @Test
    void getSecurityInsights_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/ai/security-insights"))
                .andExpect(status().isUnauthorized());
    }

    // ── DELETE /api/ai/session ────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void clearSession_ShouldReturn200WithMessage() throws Exception {
        mockMvc.perform(delete("/api/ai/session").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chat history cleared successfully"));
    }

    @Test
    void clearSession_Unauthenticated_ShouldReturn401() throws Exception {
        mockMvc.perform(delete("/api/ai/session").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // ── Content-Type assertions ───────────────────────────────────────────────

    @Test
    @WithMockUser(username = "testuser")
    void allGetEndpoints_ShouldReturnJsonContentType() throws Exception {
        ChatResponse suggestions = ChatResponse.builder()
                .reply("Suggestions").intent("SUGGESTIONS").suggestions(List.of())
                .timestamp(LocalDateTime.now()).aiPowered(false).history(List.of()).build();
        AIInsightResponse insights = AIInsightResponse.builder()
                .overallAssessment("Good").securityScore(90).insights(List.of()).aiPowered(false).build();

        when(assistantService.getSuggestions("testuser")).thenReturn(suggestions);
        when(assistantService.getSecurityInsights("testuser")).thenReturn(insights);

        mockMvc.perform(get("/api/ai/suggestions"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        mockMvc.perform(get("/api/ai/security-insights"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
