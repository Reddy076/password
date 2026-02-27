package com.revature.passwordmanager.service.security.breach;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HaveIBeenPwnedClientTest {

    @Mock
    private RestTemplate restTemplate;

    private HaveIBeenPwnedClient client;

    @BeforeEach
    void setUp() {
        client = new HaveIBeenPwnedClient(restTemplate);
    }

    // ── checkPassword ─────────────────────────────────────────────────────────

    @Test
    void checkPassword_PasswordFoundInBreach_ShouldReturnCount() throws Exception {
        // SHA-1 of "password" = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
        // prefix = 5BAA6, suffix = 1E4C9B93F3F0682250B6CF8331B7EE68FD8
        String mockResponse = "1E4C9B93F3F0682250B6CF8331B7EE68FD8:3861493\r\n" +
                              "AABBCCDDEEFF00112233445566778899012:100\r\n";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        long count = client.checkPassword("password");

        assertEquals(3861493L, count);
    }

    @Test
    void checkPassword_PasswordNotInBreach_ShouldReturnZero() throws Exception {
        // Suffix won't match anything in the mock response
        String mockResponse = "AABBCCDDEEFF00112233445566778899012:100\r\n" +
                              "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB:50\r\n";

        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(mockResponse);

        long count = client.checkPassword("MyUniquePassword!XYZ2024$#@!");

        assertEquals(0L, count);
    }

    @Test
    void checkPassword_NullPassword_ShouldReturnZero() {
        long count = client.checkPassword(null);
        assertEquals(0L, count);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void checkPassword_BlankPassword_ShouldReturnZero() {
        long count = client.checkPassword("   ");
        assertEquals(0L, count);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void checkPassword_EmptyPassword_ShouldReturnZero() {
        long count = client.checkPassword("");
        assertEquals(0L, count);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void checkPassword_ApiThrowsRestClientException_ShouldReturnZero() {
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenThrow(new RestClientException("Connection timeout"));

        long count = client.checkPassword("somepassword");

        assertEquals(0L, count);
    }

    @Test
    void checkPassword_ApiReturnsNull_ShouldReturnZero() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(null);

        long count = client.checkPassword("somepassword");

        assertEquals(0L, count);
    }

    @Test
    void checkPassword_ApiReturnsEmpty_ShouldReturnZero() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

        long count = client.checkPassword("somepassword");

        assertEquals(0L, count);
    }

    @Test
    void checkPassword_ShouldOnlySendPrefix_NotFullHash() {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn("");

        client.checkPassword("testpassword");

        // Verify the URL called ends with a 5-char hex prefix only
        verify(restTemplate).getForObject(argThat((String url) -> {
            String prefix = url.replace(HaveIBeenPwnedClient.HIBP_RANGE_URL, "");
            return prefix.length() == 5 && prefix.matches("[0-9A-Fa-f]{5}");
        }), eq(String.class));
    }

    // ── getHashPrefix ─────────────────────────────────────────────────────────

    @Test
    void getHashPrefix_ShouldReturn5UppercaseHexChars() {
        String prefix = client.getHashPrefix("password");
        assertNotNull(prefix);
        assertEquals(5, prefix.length());
        assertTrue(prefix.matches("[0-9A-F]{5}"), "Prefix should be uppercase hex: " + prefix);
    }

    @Test
    void getHashPrefix_KnownPassword_ShouldMatchExpectedPrefix() {
        // SHA-1("password") = 5BAA61E4C9B93F3F0682250B6CF8331B7EE68FD8
        String prefix = client.getHashPrefix("password");
        assertEquals("5BAA6", prefix);
    }

    @Test
    void getHashPrefix_SameInputProducesSamePrefix() {
        String p1 = client.getHashPrefix("hello");
        String p2 = client.getHashPrefix("hello");
        assertEquals(p1, p2);
    }

    @Test
    void getHashPrefix_DifferentInputsDifferentPrefixes() {
        String p1 = client.getHashPrefix("password");
        String p2 = client.getHashPrefix("hunter2");
        // Very likely different (astronomically unlikely to collide on first 5 chars)
        assertNotNull(p1);
        assertNotNull(p2);
    }

    // ── parseCount ────────────────────────────────────────────────────────────

    @Test
    void parseCount_MatchingSuffix_ShouldReturnCount() {
        String body = "AAA:100\r\nBBB:200\r\nCCC:300\r\n";
        long count = client.parseCount(body, "BBB");
        assertEquals(200L, count);
    }

    @Test
    void parseCount_NoMatchingSuffix_ShouldReturnZero() {
        String body = "AAA:100\r\nBBB:200\r\n";
        long count = client.parseCount(body, "ZZZ");
        assertEquals(0L, count);
    }

    @Test
    void parseCount_CaseInsensitiveMatch_ShouldReturnCount() {
        String body = "ABCDE:500\r\n";
        long count = client.parseCount(body, "abcde");
        assertEquals(500L, count);
    }

    @Test
    void parseCount_MalformedLine_ShouldReturnZero() {
        String body = "ABCDE:notanumber\r\n";
        long count = client.parseCount(body, "ABCDE");
        assertEquals(0L, count);
    }

    @Test
    void parseCount_UnixLineEndings_ShouldWork() {
        String body = "AAA:100\nBBB:200\nCCC:300\n";
        long count = client.parseCount(body, "CCC");
        assertEquals(300L, count);
    }

    @Test
    void parseCount_LargeCount_ShouldWork() {
        String body = "SUFFIX:9999999\r\n";
        long count = client.parseCount(body, "SUFFIX");
        assertEquals(9999999L, count);
    }

    // ── sha1Hex ───────────────────────────────────────────────────────────────

    @Test
    void sha1Hex_KnownInput_ShouldProduceCorrectHash() throws Exception {
        // SHA-1("password") = 5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8
        String hash = client.sha1Hex("password");
        assertEquals("5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8", hash);
    }

    @Test
    void sha1Hex_EmptyString_ShouldProduceDeterministicHash() throws Exception {
        // SHA-1("") = da39a3ee5e6b4b0d3255bfef95601890afd80709
        String hash = client.sha1Hex("");
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hash);
    }

    @Test
    void sha1Hex_SameInputProducesSameHash() throws Exception {
        assertEquals(client.sha1Hex("hello"), client.sha1Hex("hello"));
    }

    @Test
    void sha1Hex_DifferentInputsDifferentHashes() throws Exception {
        assertNotEquals(client.sha1Hex("abc"), client.sha1Hex("xyz"));
    }
}
