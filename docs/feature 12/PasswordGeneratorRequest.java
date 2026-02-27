package com.revature.passwordmanager.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordGeneratorRequest {
  @Builder.Default
  private int length = 16;

  @Builder.Default
  private boolean includeUppercase = true;

  @Builder.Default
  private boolean includeLowercase = true;

  @Builder.Default
  private boolean includeNumbers = true;

  @Builder.Default
  private boolean includeSpecial = true;

  @Builder.Default
  private boolean excludeSimilar = false; // e.g. i, l, 1, L, o, 0, O

  @Builder.Default
  private boolean excludeAmbiguous = false; // e.g. { } [ ] ( ) / \ ' " ` ~ , ; : . < >
}
