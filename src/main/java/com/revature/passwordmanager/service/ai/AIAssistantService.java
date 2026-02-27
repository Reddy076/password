package com.revature.passwordmanager.service.ai;

import com.revature.passwordmanager.dto.request.ChatRequest;
import com.revature.passwordmanager.dto.response.AIInsightResponse;
import com.revature.passwordmanager.dto.response.AIInsightResponse.Insight;
import com.revature.passwordmanager.dto.response.ChatResponse;
import com.revature.passwordmanager.dto.response.ChatResponse.MessageDto;
import com.revature.passwordmanager.model.ai.ChatMessage;
import com.revature.passwordmanager.model.ai.ChatMessage.MessageRole;
import com.revature.passwordmanager.model.ai.ChatSession;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.ChatSessionRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.service.security.PasswordGeneratorService;
import com.revature.passwordmanager.dto.request.PasswordGeneratorRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Feature 41 – AI Password Assistant.
 *
 * <p>Orchestrates the AI chat, contextual suggestions, password generation,
 * and security insights features.</p>
 */
@Service
@RequiredArgsConstructor
public class AIAssistantService {

    private final UserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final VaultEntryRepository vaultEntryRepository;
    private final ContextBuilder contextBuilder;
    private final OpenAIClient openAIClient;
    private final PasswordGeneratorService passwordGeneratorService;

    // ── Chat ──────────────────────────────────────────────────────────────────

    /**
     * Sends a message to the AI assistant and returns the response.
     * Maintains conversation history per user session.
     *
     * @param username the authenticated user
     * @param request  the chat request
     * @return the AI response with history
     */
    @Transactional
    public ChatResponse chat(String username, ChatRequest request) {
        User user = userRepository.findByUsernameOrThrow(username);

        // Get or create session
        ChatSession session = sessionRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    ChatSession s = ChatSession.builder().user(user).build();
                    return sessionRepository.save(s);
                });

        // Build conversation history for the API call
        List<OpenAIClient.MessagePair> history = session.getMessages().stream()
                .map(m -> new OpenAIClient.MessagePair(
                        m.getRole().name().toLowerCase(), m.getContent()))
                .collect(Collectors.toList());

        // Build vault context
        String vaultContext = contextBuilder.buildVaultContext(user.getId());
        String systemPrompt = contextBuilder.buildSystemPrompt(vaultContext);

        // Detect intent
        String intent = openAIClient.detectIntent(request.getMessage());

        // Get AI reply
        String reply = openAIClient.chat(systemPrompt, history, request.getMessage());

        // Generate password if intent is GENERATE_PASSWORD
        String generatedPassword = null;
        if ("GENERATE_PASSWORD".equals(intent)) {
            PasswordGeneratorRequest genRequest = PasswordGeneratorRequest.builder()
                    .length(16)
                    .includeUppercase(true)
                    .includeLowercase(true)
                    .includeNumbers(true)
                    .includeSpecial(true)
                    .build();
            generatedPassword = passwordGeneratorService.generatePassword(genRequest);
        }

        // Save user message
        ChatMessage userMsg = ChatMessage.builder()
                .session(session)
                .role(MessageRole.USER)
                .content(request.getMessage())
                .build();
        session.getMessages().add(userMsg);

        // Save assistant message
        ChatMessage assistantMsg = ChatMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(reply)
                .build();
        session.getMessages().add(assistantMsg);

        sessionRepository.save(session);

        // Build response
        List<MessageDto> historyDtos = session.getMessages().stream()
                .map(m -> MessageDto.builder()
                        .role(m.getRole().name().toLowerCase())
                        .content(m.getContent())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .reply(reply)
                .intent(intent)
                .generatedPassword(generatedPassword)
                .suggestions(buildSuggestions(intent))
                .timestamp(LocalDateTime.now())
                .aiPowered(openAIClient.isConfigured())
                .history(historyDtos)
                .build();
    }

    // ── Suggestions ───────────────────────────────────────────────────────────

    /**
     * Returns contextual quick-action suggestions based on the user's vault state.
     */
    @Transactional(readOnly = true)
    public ChatResponse getSuggestions(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());

        List<String> suggestions = new ArrayList<>();

        if (entries.isEmpty()) {
            suggestions.add("Add your first password to the vault");
            suggestions.add("Import passwords from Chrome or Firefox");
        } else {
            long oldCount = entries.stream()
                    .filter(e -> e.getUpdatedAt() != null &&
                                 e.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(90)))
                    .count();
            if (oldCount > 0) {
                suggestions.add("Update " + oldCount + " password(s) older than 90 days");
            }
            suggestions.add("Generate a strong password");
            suggestions.add("Check for breached passwords");
            suggestions.add("View security score");
            suggestions.add("Enable 2FA for better security");
        }

        return ChatResponse.builder()
                .reply("Here are some suggestions based on your vault:")
                .intent("SUGGESTIONS")
                .suggestions(suggestions)
                .timestamp(LocalDateTime.now())
                .aiPowered(false)
                .history(List.of())
                .build();
    }

    // ── Generate password ─────────────────────────────────────────────────────

    /**
     * Generates a contextual password with an AI-generated explanation.
     *
     * @param username the authenticated user
     * @param context  optional context (e.g., "for my bank account")
     * @return a chat response with the generated password
     */
    @Transactional
    public ChatResponse generatePassword(String username, String context) {
        PasswordGeneratorRequest genRequest = PasswordGeneratorRequest.builder()
                .length(20)
                .includeUppercase(true)
                .includeLowercase(true)
                .includeNumbers(true)
                .includeSpecial(true)
                .build();
        String password = passwordGeneratorService.generatePassword(genRequest);

        String contextMsg = (context != null && !context.isBlank())
                ? "Generate a strong password " + context
                : "Generate a strong password";

        String reply = openAIClient.chat(
                contextBuilder.buildSystemPrompt(""),
                List.of(),
                contextMsg);

        return ChatResponse.builder()
                .reply(reply)
                .intent("GENERATE_PASSWORD")
                .generatedPassword(password)
                .suggestions(List.of("Save this password to your vault", "Generate another password"))
                .timestamp(LocalDateTime.now())
                .aiPowered(openAIClient.isConfigured())
                .history(List.of())
                .build();
    }

    // ── Security insights ─────────────────────────────────────────────────────

    /**
     * Analyzes the user's vault and returns AI-generated security insights.
     */
    @Transactional(readOnly = true)
    public AIInsightResponse getSecurityInsights(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        List<VaultEntry> entries = vaultEntryRepository.findByUserIdAndIsDeletedFalse(user.getId());

        List<Insight> insights = new ArrayList<>();
        int score = 100;

        if (entries.isEmpty()) {
            return AIInsightResponse.builder()
                    .overallAssessment("Your vault is empty. Start adding passwords to track your security.")
                    .securityScore(100)
                    .insights(List.of())
                    .aiPowered(false)
                    .build();
        }

        // Check for old passwords
        long oldCount = entries.stream()
                .filter(e -> e.getUpdatedAt() != null &&
                             e.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(90)))
                .count();
        if (oldCount > 0) {
            score -= (int) Math.min(30, oldCount * 5);
            insights.add(Insight.builder()
                    .severity(oldCount > 5 ? "HIGH" : "MEDIUM")
                    .title("Outdated Passwords")
                    .description(oldCount + " password(s) haven't been changed in over 90 days. " +
                                 "Regular password rotation reduces the risk of credential compromise.")
                    .affectedCount((int) oldCount)
                    .build());
        }

        // Check for missing 2FA (vault entries without notes about 2FA)
        long noFavoriteCount = entries.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsFavorite()))
                .count();

        // Check for highly sensitive entries
        long sensitiveCount = entries.stream()
                .filter(e -> Boolean.TRUE.equals(e.getIsHighlySensitive()))
                .count();
        if (sensitiveCount > 0) {
            insights.add(Insight.builder()
                    .severity("INFO")
                    .title("Highly Sensitive Entries Protected")
                    .description(sensitiveCount + " entry/entries are marked as highly sensitive " +
                                 "and require master password re-verification to view. Good practice!")
                    .affectedCount((int) sensitiveCount)
                    .build());
        }

        // General recommendation
        insights.add(Insight.builder()
                .severity("INFO")
                .title("Enable Breach Monitoring")
                .description("Run a breach scan to check if any of your passwords have appeared " +
                             "in known data breaches. Use POST /api/security/breach-scan.")
                .affectedCount(entries.size())
                .build());

        String assessment;
        if (score >= 80) {
            assessment = "Your vault security is GOOD. Keep up the good work!";
        } else if (score >= 60) {
            assessment = "Your vault security is FAIR. Address the recommendations below to improve.";
        } else {
            assessment = "Your vault security needs ATTENTION. Please review the critical issues below.";
        }

        return AIInsightResponse.builder()
                .overallAssessment(assessment)
                .securityScore(Math.max(0, score))
                .insights(insights)
                .aiPowered(false)
                .build();
    }

    // ── Clear session ─────────────────────────────────────────────────────────

    /**
     * Clears the user's chat history.
     */
    @Transactional
    public void clearSession(String username) {
        User user = userRepository.findByUsernameOrThrow(username);
        sessionRepository.findByUserId(user.getId())
                .ifPresent(sessionRepository::delete);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private List<String> buildSuggestions(String intent) {
        return switch (intent) {
            case "GENERATE_PASSWORD" -> List.of(
                    "Save this password to vault",
                    "Generate another password",
                    "Check password strength");
            case "BREACH_CHECK" -> List.of(
                    "Run breach scan",
                    "View compromised credentials",
                    "Update compromised passwords");
            case "SECURITY_ANALYSIS" -> List.of(
                    "View security score",
                    "Check weak passwords",
                    "View reused passwords");
            default -> List.of(
                    "Generate a strong password",
                    "Check for breaches",
                    "View security insights");
        };
    }
}
