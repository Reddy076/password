package com.revature.passwordmanager.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordStrengthCalculator {

  public int calculateScore(String password) {
    if (password == null || password.isEmpty()) {
      return 0;
    }

    int score = 0;
    int length = password.length();

    // 1. Length points
    score += length * 4;

    // 2. Character types points
    if (Pattern.compile("[a-z]").matcher(password).find())
      score += 10; // Lowercase
    if (Pattern.compile("[A-Z]").matcher(password).find())
      score += 10; // Uppercase
    if (Pattern.compile("[0-9]").matcher(password).find())
      score += 10; // Numbers
    if (Pattern.compile("[^a-zA-Z0-9]").matcher(password).find())
      score += 15; // Symbols

    // 3. Bonus for combination
    int typeCount = 0;
    if (Pattern.compile("[a-z]").matcher(password).find())
      typeCount++;
    if (Pattern.compile("[A-Z]").matcher(password).find())
      typeCount++;
    if (Pattern.compile("[0-9]").matcher(password).find())
      typeCount++;
    if (Pattern.compile("[^a-zA-Z0-9]").matcher(password).find())
      typeCount++;

    if (typeCount >= 3)
      score += 10;
    if (typeCount == 4)
      score += 10;

    // 4. Penalties
    // Only letters
    if (password.matches("[a-zA-Z]+"))
      score -= 10;
    // Only numbers
    if (password.matches("[0-9]+"))
      score -= 10;
    // Repeating characters (e.g., "aaa")
    if (Pattern.compile("(.)\\1{2,}").matcher(password).find())
      score -= 5;

    // Normalize score to 0-100
    if (score < 0)
      score = 0;
    if (score > 100)
      score = 100;

    return score;
  }

  public String getStrengthLabel(int score) {
    if (score < 20)
      return "Very Weak";
    if (score < 40)
      return "Weak";
    if (score < 60)
      return "Fair";
    if (score < 80)
      return "Good";
    return "Strong";
  }
}
