package com.revature.passwordmanager.service.auth;

import com.revature.passwordmanager.dto.response.TwoFactorSetupResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.auth.TwoFactorAuth;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.TwoFactorAuthRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.util.TOTPUtil;
import dev.samstevens.totp.exceptions.QrGenerationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

  private final TwoFactorAuthRepository twoFactorAuthRepository;
  private final UserRepository userRepository;
  private final TOTPUtil totpUtil;
  private final OtpService otpService;

  private static final SecureRandom secureRandom = new SecureRandom();
  private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  @Transactional
  public TwoFactorSetupResponse setup2FA(User user) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElse(TwoFactorAuth.builder().user(user).build());

    // Generate new secret if not exists or if re-setting up
    String secretKey = totpUtil.generateSecret();
    twoFactorAuth.setSecretKey(secretKey);
    twoFactorAuth.setEnabled(false); // Not enabled until verified

    twoFactorAuthRepository.save(twoFactorAuth);

    try {
      String qrCodeUrl = totpUtil.getQrCodeUrl(secretKey, user.getEmail());
      return TwoFactorSetupResponse.builder()
          .secretKey(secretKey)
          .qrCodeUrl(qrCodeUrl)
          .build();
    } catch (QrGenerationException e) {
      throw new AuthenticationException("Failed to generate QR code for 2FA setup");
    }
  }

  @Transactional
  public void verifySetup(User user, String code) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA setup not initiated"));

    if (totpUtil.verifyCode(twoFactorAuth.getSecretKey(), code)) {
      twoFactorAuth.setEnabled(true);
      twoFactorAuth.setRecoveryCodes(generateRecoveryCodes());
      twoFactorAuthRepository.save(twoFactorAuth);

      user.set2faEnabled(true);
      userRepository.save(user);
    } else {
      throw new AuthenticationException("Invalid 2FA code");
    }
  }

  @Transactional
  public void disable2FA(User user) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA not set up for this user"));

    user.set2faEnabled(false);
    userRepository.save(user);

    twoFactorAuthRepository.delete(twoFactorAuth);
  }

  @Transactional
  public boolean verifyCode(User user, String code) {
    // 1. Try Email OTP first (since it's one-time and specific)
    try {
      if (otpService.validateOtp(user, code, "EMAIL")) {
        return true;
      }
    } catch (AuthenticationException e) {
      // Fallthrough to TOTP checks
    }

    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA not set up"));

    if (!twoFactorAuth.isEnabled()) {
      return false;
    }

    if (totpUtil.verifyCode(twoFactorAuth.getSecretKey(), code)) {
      return true;
    }

    // Check recovery codes
    if (twoFactorAuth.getRecoveryCodes().contains(code)) {
      // Remove used recovery code
      twoFactorAuth.getRecoveryCodes().remove(code);
      twoFactorAuthRepository.save(twoFactorAuth);
      return true;
    }

    return false;
  }

  @Transactional(readOnly = true)
  public List<String> getRecoveryCodes(User user) {
    TwoFactorAuth twoFactorAuth = twoFactorAuthRepository.findByUser(user)
        .orElseThrow(() -> new AuthenticationException("2FA not set up"));

    return twoFactorAuth.getRecoveryCodes();
  }

  private List<String> generateRecoveryCodes() {
    return IntStream.range(0, 10)
        .mapToObj(i -> generateRecoveryCode())
        .collect(Collectors.toList());
  }

  private String generateRecoveryCode() {
    StringBuilder sb = new StringBuilder(10);
    for (int i = 0; i < 10; i++) {
      sb.append(BASE32_ALPHABET.charAt(secureRandom.nextInt(BASE32_ALPHABET.length())));
    }
    return sb.toString();
  }
}
