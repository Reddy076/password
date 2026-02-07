package com.revature.passwordmanager.service.security;

import com.revature.passwordmanager.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class EncryptionService {

  private final EncryptionUtil encryptionUtil;

  // In a real production scenario, we would use a Key Management Service (KMS)
  // For this project, we might derive keys or store a master key securely.
  // This is a placeholder for the actual key strategy.

  public String encrypt(String data, SecretKey key) {
    try {
      return encryptionUtil.encrypt(data, key);
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while encrypting data", e);
    }
  }

  public String decrypt(String encryptedData, SecretKey key) {
    try {
      return encryptionUtil.decrypt(encryptedData, key);
    } catch (Exception e) {
      throw new RuntimeException("Error occurred while decrypting data", e);
    }
  }

  public SecretKey generateNewKey() {
    try {
      return encryptionUtil.generateKey();
    } catch (Exception e) {
      throw new RuntimeException("Error generating encryption key", e);
    }
  }

  public String encodeKey(SecretKey key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }

  public SecretKey decodeKey(String base64Key) {
    byte[] decodedKey = Base64.getDecoder().decode(base64Key);
    return encryptionUtil.getKeyFromBytes(decodedKey);
  }
}
