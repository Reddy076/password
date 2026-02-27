package com.revature.passwordmanager.dto.request;

import com.revature.passwordmanager.dto.SecurityQuestionDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RecoveryRequest {

  @NotBlank(message = "Username is required")
  private String username;

  @NotNull(message = "Security answers are required")
  @Size(min = 3, max = 3, message = "You must provide exactly 3 security answers")
  private List<SecurityQuestionDTO> securityAnswers;

  // New password field included here to maintain atomic reset flow
  // as per current implementation strategy to avoid multi-step state management
  @NotBlank(message = "New password is required")
  @Size(min = 12, message = "Password must be at least 12 characters long")
  private String newMasterPassword;
}
