package com.revature.passwordmanager.service.security;

import com.revature.passwordmanager.dto.request.PasswordGeneratorRequest;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class PasswordGeneratorService {

  private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String NUMBERS = "0123456789";
  private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

  // Similar characters to exclude if requested: i, l, 1, L, o, 0, O
  private static final String SIMILAR_CHARS = "il1Lo0O";

  // Ambiguous characters: { } [ ] ( ) / \ ' " ` ~ , ; : . < >
  private static final String AMBIGUOUS_CHARS = "{}[]()/\\'\"`~,;:.<>";

  private final SecureRandom random = new SecureRandom();

  public String generatePassword(PasswordGeneratorRequest request) {
    StringBuilder charPool = new StringBuilder();
    List<Character> passwordChars = new ArrayList<>();

    // Ensure at least one character from each selected type is included
    if (request.isIncludeLowercase()) {
      charPool.append(filterChars(LOWERCASE, request));
      passwordChars.add(getRandomChar(filterChars(LOWERCASE, request)));
    }
    if (request.isIncludeUppercase()) {
      charPool.append(filterChars(UPPERCASE, request));
      passwordChars.add(getRandomChar(filterChars(UPPERCASE, request)));
    }
    if (request.isIncludeNumbers()) {
      charPool.append(filterChars(NUMBERS, request));
      passwordChars.add(getRandomChar(filterChars(NUMBERS, request)));
    }
    if (request.isIncludeSpecial()) {
      charPool.append(filterChars(SPECIAL_CHARS, request));
      passwordChars.add(getRandomChar(filterChars(SPECIAL_CHARS, request)));
    }

    // Check if any character types were selected
    if (charPool.length() == 0) {
      throw new IllegalArgumentException("At least one character type must be selected");
    }

    // Fill the rest of the password length
    int remainingLength = request.getLength() - passwordChars.size();
    if (remainingLength < 0) {
      // If minimum requirements exceed requested length, we return strict minimum or
      // throw error.
      // Here we'll just return the required chars shuffled, effectively ignoring
      // length constraints if too short.
      // But usually length >= 4 is expected.
    } else {
      String pool = charPool.toString();
      for (int i = 0; i < remainingLength; i++) {
        passwordChars.add(pool.charAt(random.nextInt(pool.length())));
      }
    }

    // Shuffle to randomize the order of forced characters
    Collections.shuffle(passwordChars, random);

    StringBuilder password = new StringBuilder();
    for (Character c : passwordChars) {
      password.append(c);
    }

    return password.toString();
  }

  private String filterChars(String source, PasswordGeneratorRequest request) {
    StringBuilder sb = new StringBuilder();
    for (char c : source.toCharArray()) {
      boolean skip = false;
      if (request.isExcludeSimilar() && SIMILAR_CHARS.indexOf(c) >= 0) {
        skip = true;
      }
      if (request.isExcludeAmbiguous() && AMBIGUOUS_CHARS.indexOf(c) >= 0) {
        skip = true;
      }
      if (!skip) {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  private char getRandomChar(String source) {
    if (source.isEmpty()) {
      // Fallback if filter removes all characters (unlikely but possible)
      return '?';
    }
    return source.charAt(random.nextInt(source.length()));
  }
}
