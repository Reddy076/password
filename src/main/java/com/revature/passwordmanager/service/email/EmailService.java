package com.revature.passwordmanager.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

  private final JavaMailSender javaMailSender;

  @Value("${spring.mail.username}")
  private String fromEmail;

  @Async
  public void sendOtpEmail(String toEmail, String otpCode) {
    log.info("Sending OTP email to: {}", toEmail);
    // For manual local testing without SMTP:
    log.info("============== INTERCEPTED OTP CODE: {} ==============", otpCode);
    try {
      MimeMessage message = javaMailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true);

      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject("Your Security Code - Rev-PasswordManager");

      String content = String.format(
          "<h3>Rev-PasswordManager Security</h3>" +
              "<p>Your security verification code is:</p>" +
              "<h1>%s</h1>" +
              "<p>This code will expire in 15 minutes.</p>" +
              "<p>If you did not request this code, please ignore this email.</p>",
          otpCode);

      helper.setText(content, true);

      javaMailSender.send(message);
      log.info("OTP email sent successfully to: {}", toEmail);
    } catch (MessagingException e) {
      log.error("Failed to send OTP email", e);
      throw new RuntimeException("Failed to send email");
    }
  }
}
