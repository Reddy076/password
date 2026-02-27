package com.revature.passwordmanager.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeatmapResponse {
  private int[] accessByHour;
  private int[] accessByDay;
  private int peakHour;
  private String peakDay;
  private long totalAccesses;
  private String period;
}
