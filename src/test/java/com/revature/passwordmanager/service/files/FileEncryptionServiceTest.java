package com.revature.passwordmanager.service.files;

import com.revature.passwordmanager.config.EncryptionConfig;
import com.revature.passwordmanager.service.files.FileEncryptionService.EncryptedFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Pure unit tests for {@link FileEncryptionService} — no Spring context needed.</p>
 */
class FileEncryptionServiceTest {

    private FileEncryptionService encryptionService;
    private SecretKey aesKey;

    @BeforeEach
    void setUp() throws Exception {
        EncryptionConfig config = new EncryptionConfig();
        config.setAlgorithm("AES/GCM/NoPadding");
        config.setKeySize(256);
        encryptionService = new FileEncryptionService(config);

        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        aesKey = keyGen.generateKey();
    }

    // ── encrypt / decrypt round-trip ──────────────────────────────────────────

    @Test
    void encryptDecrypt_SmallFile_ShouldRoundTrip() {
        byte[] original = "Hello, World!".getBytes(StandardCharsets.UTF_8);

        EncryptedFile encrypted = encryptionService.encrypt(original, aesKey);
        byte[] decrypted = encryptionService.decrypt(encrypted.cipherBytes(), aesKey, encrypted.ivBase64());

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encryptDecrypt_LargeFile_ShouldRoundTrip() {
        byte[] original = new byte[1024 * 1024]; // 1 MB
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) (i % 256);
        }

        EncryptedFile encrypted = encryptionService.encrypt(original, aesKey);
        byte[] decrypted = encryptionService.decrypt(encrypted.cipherBytes(), aesKey, encrypted.ivBase64());

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encryptDecrypt_EmptyFile_ShouldRoundTrip() {
        byte[] original = new byte[0];

        EncryptedFile encrypted = encryptionService.encrypt(original, aesKey);
        byte[] decrypted = encryptionService.decrypt(encrypted.cipherBytes(), aesKey, encrypted.ivBase64());

        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encrypt_ShouldProduceDifferentCiphertextEachTime() {
        byte[] original = "Same content".getBytes(StandardCharsets.UTF_8);

        EncryptedFile enc1 = encryptionService.encrypt(original, aesKey);
        EncryptedFile enc2 = encryptionService.encrypt(original, aesKey);

        // Different IVs → different ciphertext
        assertThat(enc1.ivBase64()).isNotEqualTo(enc2.ivBase64());
        assertThat(enc1.cipherBytes()).isNotEqualTo(enc2.cipherBytes());
    }

    @Test
    void encrypt_ShouldReturnNonNullIv() {
        byte[] original = "test".getBytes(StandardCharsets.UTF_8);
        EncryptedFile encrypted = encryptionService.encrypt(original, aesKey);

        assertThat(encrypted.ivBase64()).isNotNull();
        assertThat(encrypted.ivBase64()).isNotBlank();
    }

    @Test
    void decrypt_WrongKey_ShouldThrowRuntimeException() throws Exception {
        byte[] original = "Secret data".getBytes(StandardCharsets.UTF_8);
        EncryptedFile encrypted = encryptionService.encrypt(original, aesKey);

        // Generate a different key
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        SecretKey wrongKey = keyGen.generateKey();

        assertThatThrownBy(() -> encryptionService.decrypt(encrypted.cipherBytes(), wrongKey, encrypted.ivBase64()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to decrypt file");
    }

    @Test
    void decrypt_WrongIv_ShouldThrowRuntimeException() {
        byte[] original = "Secret data".getBytes(StandardCharsets.UTF_8);
        EncryptedFile encrypted = encryptionService.encrypt(original, aesKey);

        // Use a different IV (same length but different content)
        byte[] wrongIvBytes = new byte[12];
        String wrongIv = java.util.Base64.getEncoder().encodeToString(wrongIvBytes);

        assertThatThrownBy(() -> encryptionService.decrypt(encrypted.cipherBytes(), aesKey, wrongIv))
                .isInstanceOf(RuntimeException.class);
    }

    // ── computeChecksum ───────────────────────────────────────────────────────

    @Test
    void computeChecksum_SameInput_ShouldReturnSameHash() {
        byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

        String hash1 = encryptionService.computeChecksum(data);
        String hash2 = encryptionService.computeChecksum(data);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void computeChecksum_DifferentInput_ShouldReturnDifferentHash() {
        byte[] data1 = "test data 1".getBytes(StandardCharsets.UTF_8);
        byte[] data2 = "test data 2".getBytes(StandardCharsets.UTF_8);

        String hash1 = encryptionService.computeChecksum(data1);
        String hash2 = encryptionService.computeChecksum(data2);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void computeChecksum_ShouldReturn64CharHexString() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String hash = encryptionService.computeChecksum(data);

        // SHA-256 produces 32 bytes = 64 hex chars
        assertThat(hash).hasSize(64);
        assertThat(hash).matches("[0-9a-f]+");
    }

    @Test
    void computeChecksum_KnownValue_ShouldMatchExpected() {
        // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String hash = encryptionService.computeChecksum(data);

        assertThat(hash).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }
}
