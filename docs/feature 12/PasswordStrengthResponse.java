package com.revature.passwordmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordStrengthResponse {
  private int score; // 0-100
  private String label; // Very Weak, Weak, Fair, Good, Strong
  private List<String> feedback; // List of improvement suggestions
}
