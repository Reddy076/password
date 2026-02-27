package com.revature.passwordmanager.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultEntryRequest {
  private String title;
  private String username;
  private String password;
  private String websiteUrl;
  private String notes;
  private Long categoryId;
  private Long folderId;
  private Boolean isFavorite;
}
