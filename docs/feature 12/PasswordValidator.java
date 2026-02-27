package com.revature.passwordmanager.util;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

  public boolean isValid(String password) {
    // Basic validation rules, can be expanded
    return password != null && password.length() >= 8;
  }
}
