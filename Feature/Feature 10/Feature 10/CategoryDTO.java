package com.revature.passwordmanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDTO {

  private Long id;
  private String name;
  private String icon;
  private Boolean isDefault;
  private LocalDateTime createdAt;

  // Count of vault entries in this category (populated by service)
  private Integer entryCount;
}
