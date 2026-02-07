package com.revature.passwordmanager.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class MasterPasswordValidator {

  // Minimum 12 chars, 1 uppercase, 1 lowercase, 1 digit, 1 special char
  private static final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{12,}$";

  private final Pattern pattern = Pattern.compile(PASSWORD_PATTERN);

  public boolean isValid(String password) {
    if (password == null) {
      return false;
    }
    return pattern.matcher(password).matches();
  }

  public String getRequirementsMessage() {
    return "Master password must be at least 12 characters long and contain at " +
        "least one digit, one lowercase letter, one uppercase letter, " +
        "and one special character (@#$%^&+=!). Details must not contain whitespace.";
  }
}
