package com.revature.passwordmanager.service.security.breach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Client for the HaveIBeenPwned Pwned Passwords API v3.
 *
 * <p>Uses k-anonymity: only the first 5 hex characters of the SHA-1 hash are
 * sent to the remote API. The response contains all suffixes that match that
 * prefix along with their occurrence counts. The client checks locally whether
 * the full hash suffix is present — no plain-text password ever leaves the JVM.
 *
 * <p>API docs: https://haveibeenpwned.com/API/v3#PwnedPasswords
 */
@Component
public class HaveIBeenPwnedClient {

    private static final Logger logger = LoggerFactory.getLogger(HaveIBeenPwnedClient.class);
    static final String HIBP_RANGE_URL = "https://api.pwnedpasswords.com/range/";

    private final RestTemplate restTemplate;

    public HaveIBeenPwnedClient() {
        this.restTemplate = new RestTemplate();
    }

    /** Package-visible for testing — allows injection of a mock RestTemplate. */
    HaveIBeenPwnedClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Checks whether the given plain-text password appears in known data breaches.
     *
     * @param password the plain-text password to check
     * @return number of times it has been seen in breaches (0 = not found)
     */
    public long checkPassword(String password) {
        if (password == null || password.isBlank()) {
            return 0;
        }
        try {
            String sha1 = sha1Hex(password).toUpperCase();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);

            String responseBody = restTemplate.getForObject(HIBP_RANGE_URL + prefix, String.class);
            if (responseBody == null || responseBody.isBlank()) {
                return 0;
            }

            return parseCount(responseBody, suffix);
        } catch (RestClientException e) {
            logger.warn("HIBP API call failed (network/timeout) — treating as no breach: {}", e.getMessage());
            return 0;
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-1 algorithm unavailable", e);
            return 0;
        }
    }

    /**
     * Returns the SHA-1 hash prefix (first 5 uppercase hex chars) for the
     * given password. Used externally for storing/displaying the hash prefix.
     */
    public String getHashPrefix(String password) {
        try {
            return sha1Hex(password).substring(0, 5).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-1 algorithm unavailable", e);
            return "";
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Parses the HIBP range response body and returns the occurrence count for
     * the given full hash suffix (case-insensitive).
     *
     * <p>Format of each line: {@code <SUFFIX_UPPERCASE>:<COUNT>}
     */
    long parseCount(String responseBody, String suffix) {
        String upperSuffix = suffix.toUpperCase();
        for (String line : responseBody.split("\r?\n")) {
            String[] parts = line.split(":");
            if (parts.length == 2 && parts[0].trim().equalsIgnoreCase(upperSuffix)) {
                try {
                    return Long.parseLong(parts[1].trim());
                } catch (NumberFormatException e) {
                    logger.warn("Unexpected count format in HIBP response: {}", line);
                    return 0;
                }
            }
        }
        return 0;
    }

    String sha1Hex(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
