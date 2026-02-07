package com.revature.passwordmanager.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

  @NotBlank(message = "Username is required")
  private String username;

  @NotBlank(message = "Master password is required")
  private String masterPassword;
}
