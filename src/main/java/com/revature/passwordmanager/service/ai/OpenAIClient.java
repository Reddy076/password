package com.revature.passwordmanager.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Feature 41 – AI Password Assistant.
 *
 * <p>Calls the OpenAI Chat Completions API when an API key is configured.
 * Falls back to a rule-based response engine when no key is available.</p>
 *
 * <p>Configure via {@code ai.openai.api-key} in application.properties.
 * If the key is blank or absent, all calls use the local fallback.</p>
 */
@Component
public class OpenAIClient {

    private static final Logger logger = LoggerFactory.getLogger(OpenAIClient.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";

    private final String apiKey;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAIClient(
            @Value("${ai.openai.api-key:}") String apiKey,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * Returns {@code true} if a real OpenAI API key is configured.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Sends a chat completion request to OpenAI.
     *
     * @param systemPrompt the system context prompt
     * @param messages     list of prior messages (role + content pairs)
     * @param userMessage  the latest user message
     * @return the AI's reply text
     * @throws RuntimeException if the API call fails
     */
    public String chat(String systemPrompt, List<MessagePair> messages, String userMessage) {
        if (!isConfigured()) {
            return fallbackResponse(userMessage);
        }

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", DEFAULT_MODEL);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            ArrayNode messagesArray = requestBody.putArray("messages");

            // System message
            ObjectNode systemMsg = messagesArray.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            // Prior conversation history
            for (MessagePair msg : messages) {
                ObjectNode m = messagesArray.addObject();
                m.put("role", msg.role());
                m.put("content", msg.content());
            }

            // Current user message
            ObjectNode userMsg = messagesArray.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warn("OpenAI API returned status {}: {}", response.statusCode(), response.body());
                return fallbackResponse(userMessage);
            }

            JsonNode responseJson = objectMapper.readTree(response.body());
            return responseJson
                    .path("choices").get(0)
                    .path("message").path("content").asText();

        } catch (Exception e) {
            logger.warn("OpenAI API call failed: {}. Using fallback.", e.getMessage());
            return fallbackResponse(userMessage);
        }
    }

    /**
     * Rule-based fallback response when no API key is configured or the API call fails.
     * Handles common password management intents deterministically.
     */
    String fallbackResponse(String userMessage) {
        String lower = userMessage.toLowerCase();

        if (lower.contains("generate") || lower.contains("create") || lower.contains("make")) {
            if (lower.contains("password")) {
                return "I can help you generate a strong password! Use the password generator at " +
                       "/api/generator/generate with options like length (16+), uppercase, lowercase, " +
                       "numbers, and special characters. A strong password should be at least 16 characters " +
                       "and include all character types.";
            }
        }

        if (lower.contains("weak") || lower.contains("improve") || lower.contains("update")) {
            return "To improve your vault security: 1) Update passwords older than 90 days, " +
                   "2) Use unique passwords for each account, 3) Enable 2FA where possible, " +
                   "4) Use passwords of at least 16 characters with mixed character types. " +
                   "Check /api/security/audit-report for a full analysis of your vault.";
        }

        if (lower.contains("reused") || lower.contains("duplicate") || lower.contains("same password")) {
            return "Reusing passwords is a major security risk. If one account is compromised, " +
                   "all accounts with the same password are at risk. " +
                   "Visit /api/dashboard/reused-passwords to see which passwords you're reusing, " +
                   "then update each one with a unique password.";
        }

        if (lower.contains("breach") || lower.contains("compromised") || lower.contains("hacked")) {
            return "To check if your passwords have been compromised in data breaches, " +
                   "use the breach scanner at POST /api/security/breach-scan. " +
                   "This checks your passwords against the HaveIBeenPwned database using k-Anonymity " +
                   "(your actual passwords are never sent to the API).";
        }

        if (lower.contains("2fa") || lower.contains("two factor") || lower.contains("authenticator")) {
            return "Two-factor authentication (2FA) adds an extra layer of security. " +
                   "Enable it at POST /api/2fa/setup. You'll get a QR code to scan with " +
                   "Google Authenticator or Authy. Once enabled, you'll need both your " +
                   "master password and a 6-digit code to log in.";
        }

        if (lower.contains("strong") || lower.contains("secure") || lower.contains("best practice")) {
            return "Password security best practices: " +
                   "1) Use 16+ character passwords with mixed types, " +
                   "2) Never reuse passwords across accounts, " +
                   "3) Enable 2FA on all important accounts, " +
                   "4) Change passwords every 90 days, " +
                   "5) Use the breach scanner regularly, " +
                   "6) Store passwords in your vault — never in plain text.";
        }

        if (lower.contains("hello") || lower.contains("hi") || lower.contains("help")) {
            return "Hello! I'm your AI password security assistant. I can help you with: " +
                   "• Generating strong passwords, " +
                   "• Identifying weak or reused passwords, " +
                   "• Checking for data breaches, " +
                   "• Security best practices, " +
                   "• Setting up 2FA. " +
                   "What would you like help with today?";
        }

        return "I'm here to help with your password security! You can ask me about: " +
               "generating passwords, improving weak passwords, checking for breaches, " +
               "or general security best practices. What would you like to know?";
    }

    /**
     * Detects the intent of a user message.
     */
    public String detectIntent(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("generate") || lower.contains("create") || lower.contains("make")) {
            return "GENERATE_PASSWORD";
        }
        if (lower.contains("breach") || lower.contains("compromised") || lower.contains("hacked")) {
            return "BREACH_CHECK";
        }
        if (lower.contains("weak") || lower.contains("reused") || lower.contains("old") ||
            lower.contains("security") || lower.contains("audit")) {
            return "SECURITY_ANALYSIS";
        }
        if (lower.contains("2fa") || lower.contains("two factor")) {
            return "TWO_FACTOR";
        }
        return "GENERAL";
    }

    /** Immutable message pair for conversation history. */
    public record MessagePair(String role, String content) {}
}
