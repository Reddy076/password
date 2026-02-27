package com.revature.passwordmanager.service.files;

import com.revature.passwordmanager.config.EncryptionConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Feature 40 – Secure File Storage Vault.
 *
 * <p>Encrypts and decrypts raw file bytes using AES-256-GCM.
 * The IV is generated fresh for each encryption and returned separately
 * so it can be stored in the {@link com.revature.passwordmanager.model.files.SecureFile} record.</p>
 */
@Service
@RequiredArgsConstructor
public class FileEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final EncryptionConfig encryptionConfig;

    /**
     * Encrypts raw file bytes with the given AES key.
     *
     * @param plainBytes the original file bytes
     * @param key        the user's derived AES-256 key
     * @return the encrypted bytes (IV is NOT prepended — stored separately)
     * @throws RuntimeException if encryption fails
     */
    public EncryptedFile encrypt(byte[] plainBytes, SecretKey key) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(encryptionConfig.getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] cipherBytes = cipher.doFinal(plainBytes);

            return new EncryptedFile(cipherBytes, Base64.getEncoder().encodeToString(iv));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt file", e);
        }
    }

    /**
     * Decrypts encrypted file bytes with the given AES key and IV.
     *
     * @param cipherBytes the encrypted file bytes
     * @param key         the user's derived AES-256 key
     * @param ivBase64    the Base64-encoded IV stored in the database
     * @return the original plaintext file bytes
     * @throws RuntimeException if decryption fails
     */
    public byte[] decrypt(byte[] cipherBytes, SecretKey key, String ivBase64) {
        try {
            byte[] iv = Base64.getDecoder().decode(ivBase64);
            Cipher cipher = Cipher.getInstance(encryptionConfig.getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt file", e);
        }
    }

    /**
     * Computes the SHA-256 checksum of the given bytes (hex-encoded).
     *
     * @param bytes the bytes to hash
     * @return hex-encoded SHA-256 digest
     */
    public String computeChecksum(byte[] bytes) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute checksum", e);
        }
    }

    /**
     * Result of an encryption operation.
     */
    public record EncryptedFile(byte[] cipherBytes, String ivBase64) {}
}
