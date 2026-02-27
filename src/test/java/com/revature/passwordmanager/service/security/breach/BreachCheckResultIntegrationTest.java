package com.revature.passwordmanager.service.security.breach;

import com.revature.passwordmanager.model.security.breach.BreachCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link HaveIBeenPwnedClient#checkPasswordForEntry} method that
 * returns structured {@link BreachCheckResult} objects.
 *
 * <p>Gap closure: the original {@code checkPassword(String)} returned a raw {@code long},
 * making it impossible to distinguish "not compromised", "check failed", and
 * "compromised 0 times" without null-checking conventions. The new
 * {@code checkPasswordForEntry} method fixes this by returning a rich result object.</p>
 */
@ExtendWith(MockitoExtension.class)
class BreachCheckResultIntegrationTest {

    @Mock
    private RestTemplate restTemplate;

    private HaveIBeenPwnedClient client;

    @BeforeEach
    void setUp() {
        client = new HaveIBeenPwnedClient(restTemplate);
    }

    // ── checkPasswordForEntry — compromised ───────────────────────────────────

    @Test
    void checkPasswordForEntry_CompromisedPassword_ShouldReturnCompromisedResult() {
        // SHA-1 of "password" = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
        // prefix = 5BAA6, suffix = 1E4C9B93F3F0682250B6CF8331B7EE68FD8
        String hibpResponse = "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\nABCDEF:100";
        when(restTemplate.getForObject(contains("5BAA6"), eq(String.class))).thenReturn(hibpResponse);

        BreachCheckResult result = client.checkPasswordForEntry(1L, "TestEntry", "password");

        assertThat(result.isCompromised()).isTrue();
        assertThat(result.getPwnedCount()).isEqualTo(3861493L);
        assertThat(result.isCheckSucceeded()).isTrue();
        assertThat(result.getHashPrefix()).isEqualTo("5BAA6");
        assertThat(result.getVaultEntryId()).isEqualTo(1L);
        assertThat(result.getEntryTitle()).isEqualTo("TestEntry");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void checkPasswordForEntry_CleanPassword_ShouldReturnCleanResult() {
        // A response that does NOT contain our suffix → clean
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn("AAAAA:10\r\nBBBBB:20");

        BreachCheckResult result = client.checkPasswordForEntry(2L, "SafeEntry", "V3ryStr0ng&Unique!");

        assertThat(result.isCompromised()).isFalse();
        assertThat(result.getPwnedCount()).isZero();
        assertThat(result.isCheckSucceeded()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void checkPasswordForEntry_NullPassword_ShouldReturnCleanResultWithEmptyPrefix() {
        BreachCheckResult result = client.checkPasswordForEntry(3L, "NullEntry", null);

        assertThat(result.isCompromised()).isFalse();
        assertThat(result.isCheckSucceeded()).isTrue();
        assertThat(result.getHashPrefix()).isEmpty();
    }

    @Test
    void checkPasswordForEntry_BlankPassword_ShouldReturnCleanResult() {
        BreachCheckResult result = client.checkPasswordForEntry(4L, "BlankEntry", "   ");

        assertThat(result.isCompromised()).isFalse();
        assertThat(result.isCheckSucceeded()).isTrue();
    }

    // ── checkPasswordForEntry — API failure ───────────────────────────────────

    @Test
    void checkPasswordForEntry_HibpApiDown_ShouldReturnFailedResult() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        BreachCheckResult result = client.checkPasswordForEntry(5L, "TestEntry", "password");

        assertThat(result.isCompromised()).isFalse();
        assertThat(result.isCheckSucceeded()).isFalse();
        assertThat(result.getErrorMessage()).contains("HIBP API unavailable");
        assertThat(result.getVaultEntryId()).isEqualTo(5L);
    }

    @Test
    void checkPasswordForEntry_HibpReturnsNull_ShouldReturnCleanResult() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        BreachCheckResult result = client.checkPasswordForEntry(6L, "TestEntry", "anypassword");

        assertThat(result.isCompromised()).isFalse();
        assertThat(result.isCheckSucceeded()).isTrue();
    }

    @Test
    void checkPasswordForEntry_HibpReturnsEmpty_ShouldReturnCleanResult() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

        BreachCheckResult result = client.checkPasswordForEntry(7L, "TestEntry", "anypassword");

        assertThat(result.isCompromised()).isFalse();
        assertThat(result.isCheckSucceeded()).isTrue();
    }

    // ── checkPasswordForEntry vs checkPassword consistency ────────────────────

    @Test
    void checkPasswordForEntry_ShouldMatchCheckPasswordCount_WhenPasswordIsCompromised() {
        String hibpResponse = "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493";
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(hibpResponse);

        long rawCount = client.checkPassword("password");
        BreachCheckResult structured = client.checkPasswordForEntry(1L, "Entry", "password");

        assertThat(structured.getPwnedCount()).isEqualTo(rawCount);
        assertThat(structured.isCompromised()).isEqualTo(rawCount > 0);
    }

    // ── hashPrefix field ──────────────────────────────────────────────────────

    @Test
    void checkPasswordForEntry_CompromisedResult_ShouldHave5CharHashPrefix() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("SUFFIX:1");

        BreachCheckResult result = client.checkPasswordForEntry(1L, "Entry", "somepassword123");

        assertThat(result.getHashPrefix()).hasSize(5);
        assertThat(result.getHashPrefix()).matches("[0-9A-F]{5}");
    }
}
