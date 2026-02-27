package com.revature.passwordmanager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import com.revature.passwordmanager.dto.SecurityQuestionDTO;

@Data
public class RegistrationRequest {

  @NotBlank(message = "Email is required")
  @Email(message = "Invalid email format")
  private String email;

  @NotBlank(message = "Username is required")
  @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
  private String username;

  @NotBlank(message = "Master password is required")
  @Size(min = 12, message = "Master password must be at least 12 characters long")
  private String masterPassword;

  @jakarta.validation.Valid
  @NotNull(message = "Security questions are required")
  @Size(min = 3, max = 3, message = "You must provide exactly 3 security questions")
  private List<SecurityQuestionDTO> securityQuestions;

  @Size(max = 500, message = "Password hint must not exceed 500 characters")
  private String passwordHint;
}
