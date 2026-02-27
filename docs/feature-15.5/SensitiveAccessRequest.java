package com.revature.passwordmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SensitiveAccessRequest {
  @NotBlank(message = "Master password is required")
  private String masterPassword;

  private String otpToken; // Optional if 2FA is not enabled for user, but required if enabled
}
