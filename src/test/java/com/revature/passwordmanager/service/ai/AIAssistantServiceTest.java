package com.revature.passwordmanager.service.ai;

import com.revature.passwordmanager.dto.request.ChatRequest;
import com.revature.passwordmanager.dto.response.AIInsightResponse;
import com.revature.passwordmanager.dto.response.ChatResponse;
import com.revature.passwordmanager.model.ai.ChatMessage;
import com.revature.passwordmanager.model.ai.ChatMessage.MessageRole;
import com.revature.passwordmanager.model.ai.ChatSession;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.ChatSessionRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.security.PasswordGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Feature 41 – AI Password Assistant.
 */
@ExtendWith(MockitoExtension.class)
class AIAssistantServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ChatSessionRepository sessionRepository;
    @Mock private VaultEntryRepository vaultEntryRepository;
    @Mock private ContextBuilder contextBuilder;
    @Mock private OpenAIClient openAIClient;
    @Mock private PasswordGeneratorService passwordGeneratorService;

    @InjectMocks
    private AIAssistantService assistantService;

    private User user;
    private ChatSession session;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .masterPasswordHash("hash")
                .salt("salt")
                .build();

        session = ChatSession.builder()
                .id(10L)
                .user(user)
                .messages(new ArrayList<>())
                .build();
    }

    // ── chat ──────────────────────────────────────────────────────────────────

    @Test
    void chat_GeneralMessage_ShouldReturnReply() {
        ChatRequest request = ChatRequest.builder().message("Hello, how can you help me?").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findByUserId(1L)).thenReturn(Optional.of(session));
        when(contextBuilder.buildVaultContext(1L)).thenReturn("Vault summary: 5 entries.");
        when(contextBuilder.buildSystemPrompt(anyString())).thenReturn("System prompt");
        when(openAIClient.detectIntent("Hello, how can you help me?")).thenReturn("GENERAL");
        when(openAIClient.chat(anyString(), anyList(), anyString()))
                .thenReturn("I can help you with password security!");
        when(openAIClient.isConfigured()).thenReturn(false);
        when(sessionRepository.save(any())).thenReturn(session);

        ChatResponse response = assistantService.chat("testuser", request);

        assertThat(response.getReply()).isEqualTo("I can help you with password security!");
        assertThat(response.getIntent()).isEqualTo("GENERAL");
        assertThat(response.getGeneratedPassword()).isNull();
        assertThat(response.isAiPowered()).isFalse();
        assertThat(response.getSuggestions()).isNotEmpty();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void chat_GeneratePasswordIntent_ShouldIncludeGeneratedPassword() {
        ChatRequest request = ChatRequest.builder().message("Generate a strong password for me").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findByUserId(1L)).thenReturn(Optional.of(session));
        when(contextBuilder.buildVaultContext(1L)).thenReturn("Vault summary: 5 entries.");
        when(contextBuilder.buildSystemPrompt(anyString())).thenReturn("System prompt");
        when(openAIClient.detectIntent("Generate a strong password for me")).thenReturn("GENERATE_PASSWORD");
        when(openAIClient.chat(anyString(), anyList(), anyString()))
                .thenReturn("Here's a strong password for you!");
        when(openAIClient.isConfigured()).thenReturn(false);
        when(passwordGeneratorService.generatePassword(any())).thenReturn("Str0ng!Pass#2026");
        when(sessionRepository.save(any())).thenReturn(session);

        ChatResponse response = assistantService.chat("testuser", request);

        assertThat(response.getGeneratedPassword()).isEqualTo("Str0ng!Pass#2026");
        assertThat(response.getIntent()).isEqualTo("GENERATE_PASSWORD");
        verify(passwordGeneratorService).generatePassword(any());
    }

    @Test
    void chat_NewSession_ShouldCreateSession() {
        ChatRequest request = ChatRequest.builder().message("Hello").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(contextBuilder.buildVaultContext(1L)).thenReturn("Empty vault.");
        when(contextBuilder.buildSystemPrompt(anyString())).thenReturn("System prompt");
        when(openAIClient.detectIntent("Hello")).thenReturn("GENERAL");
        when(openAIClient.chat(anyString(), anyList(), anyString())).thenReturn("Hello!");
        when(openAIClient.isConfigured()).thenReturn(false);

        ChatResponse response = assistantService.chat("testuser", request);

        assertThat(response.getReply()).isEqualTo("Hello!");
        // Session was created (save called at least once for new session)
        verify(sessionRepository, atLeast(1)).save(any(ChatSession.class));
    }

    @Test
    void chat_WithHistory_ShouldPassHistoryToClient() {
        ChatMessage priorMsg = ChatMessage.builder()
                .id(1L)
                .session(session)
                .role(MessageRole.USER)
                .content("Prior message")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();
        session.getMessages().add(priorMsg);

        ChatRequest request = ChatRequest.builder().message("Follow-up question").build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findByUserId(1L)).thenReturn(Optional.of(session));
        when(contextBuilder.buildVaultContext(1L)).thenReturn("Vault context");
        when(contextBuilder.buildSystemPrompt(anyString())).thenReturn("System prompt");
        when(openAIClient.detectIntent("Follow-up question")).thenReturn("GENERAL");
        when(openAIClient.chat(anyString(), anyList(), anyString())).thenReturn("Follow-up answer");
        when(openAIClient.isConfigured()).thenReturn(false);
        when(sessionRepository.save(any())).thenReturn(session);

        ChatResponse response = assistantService.chat("testuser", request);

        // History should include the prior message + new messages
        assertThat(response.getHistory()).isNotEmpty();
    }

    // ── getSuggestions ────────────────────────────────────────────────────────

    @Test
    void getSuggestions_EmptyVault_ShouldSuggestAddingPasswords() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of());

        ChatResponse response = assistantService.getSuggestions("testuser");

        assertThat(response.getSuggestions()).isNotEmpty();
        assertThat(response.getSuggestions().stream()
                .anyMatch(s -> s.contains("first password") || s.contains("Import")))
                .isTrue();
    }

    @Test
    void getSuggestions_WithOldPasswords_ShouldSuggestUpdate() {
        VaultEntry oldEntry = VaultEntry.builder()
                .id(1L).user(user).title("OldBank")
                .username("u").password("p").isDeleted(false)
                .updatedAt(LocalDateTime.now().minusDays(100))
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(oldEntry));

        ChatResponse response = assistantService.getSuggestions("testuser");

        assertThat(response.getSuggestions()).isNotEmpty();
        assertThat(response.getSuggestions().stream()
                .anyMatch(s -> s.contains("older than 90 days")))
                .isTrue();
    }

    // ── generatePassword ──────────────────────────────────────────────────────

    @Test
    void generatePassword_ShouldReturnPasswordAndReply() {
        when(passwordGeneratorService.generatePassword(any())).thenReturn("Secure!Pass#123");
        when(contextBuilder.buildSystemPrompt(anyString())).thenReturn("System prompt");
        when(openAIClient.chat(anyString(), anyList(), anyString()))
                .thenReturn("Here's a strong 20-character password for your bank account.");
        when(openAIClient.isConfigured()).thenReturn(false);

        ChatResponse response = assistantService.generatePassword("testuser", "for my bank account");

        assertThat(response.getGeneratedPassword()).isEqualTo("Secure!Pass#123");
        assertThat(response.getIntent()).isEqualTo("GENERATE_PASSWORD");
        assertThat(response.getReply()).isNotBlank();
        assertThat(response.getSuggestions()).isNotEmpty();
    }

    @Test
    void generatePassword_NoContext_ShouldStillWork() {
        when(passwordGeneratorService.generatePassword(any())).thenReturn("Secure!Pass#123");
        when(contextBuilder.buildSystemPrompt(anyString())).thenReturn("System prompt");
        when(openAIClient.chat(anyString(), anyList(), anyString())).thenReturn("Here's a password.");
        when(openAIClient.isConfigured()).thenReturn(false);

        ChatResponse response = assistantService.generatePassword("testuser", null);

        assertThat(response.getGeneratedPassword()).isNotNull();
    }

    // ── getSecurityInsights ───────────────────────────────────────────────────

    @Test
    void getSecurityInsights_EmptyVault_ShouldReturnEmptyInsights() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of());

        AIInsightResponse response = assistantService.getSecurityInsights("testuser");

        assertThat(response.getSecurityScore()).isEqualTo(100);
        assertThat(response.getInsights()).isEmpty();
        assertThat(response.getOverallAssessment()).isNotBlank();
    }

    @Test
    void getSecurityInsights_WithOldPasswords_ShouldReduceScore() {
        List<VaultEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(VaultEntry.builder()
                    .id((long) i).user(user).title("Entry" + i)
                    .username("u").password("p").isDeleted(false)
                    .updatedAt(LocalDateTime.now().minusDays(100))
                    .build());
        }

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(entries);

        AIInsightResponse response = assistantService.getSecurityInsights("testuser");

        assertThat(response.getSecurityScore()).isLessThan(100);
        assertThat(response.getInsights()).isNotEmpty();
        assertThat(response.getInsights().stream()
                .anyMatch(i -> i.getTitle().contains("Outdated")))
                .isTrue();
    }

    @Test
    void getSecurityInsights_ShouldAlwaysIncludeBreachMonitoringInsight() {
        VaultEntry entry = VaultEntry.builder()
                .id(1L).user(user).title("GitHub")
                .username("u").password("p").isDeleted(false)
                .updatedAt(LocalDateTime.now())
                .build();

        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(vaultEntryRepository.findByUserIdAndIsDeletedFalse(1L)).thenReturn(List.of(entry));

        AIInsightResponse response = assistantService.getSecurityInsights("testuser");

        assertThat(response.getInsights().stream()
                .anyMatch(i -> i.getTitle().contains("Breach")))
                .isTrue();
    }

    // ── clearSession ──────────────────────────────────────────────────────────

    @Test
    void clearSession_ExistingSession_ShouldDeleteIt() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findByUserId(1L)).thenReturn(Optional.of(session));

        assistantService.clearSession("testuser");

        verify(sessionRepository).delete(session);
    }

    @Test
    void clearSession_NoSession_ShouldNotThrow() {
        when(userRepository.findByUsernameOrThrow("testuser")).thenReturn(user);
        when(sessionRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assistantService.clearSession("testuser");

        verify(sessionRepository, never()).delete(any());
    }
}
