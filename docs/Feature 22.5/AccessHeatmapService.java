package com.revature.passwordmanager.service.analytics;

import com.revature.passwordmanager.dto.response.HeatmapResponse;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.security.AuditLog;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.AuditLogRepository;
import com.revature.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccessHeatmapService {

  private final AuditLogRepository auditLogRepository;
  private final UserRepository userRepository;

  private static final String[] DAY_NAMES = {
      "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
  };

  @Transactional(readOnly = true)
  public HeatmapResponse getAccessHeatmap(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    List<AuditLog> logs = auditLogRepository.findByUserIdOrderByTimestampDesc(user.getId());

    int[] accessByHour = new int[24];
    int[] accessByDay = new int[7];

    for (AuditLog log : logs) {
      if (log.getTimestamp() != null) {
        int hour = log.getTimestamp().getHour();
        accessByHour[hour]++;

        DayOfWeek day = log.getTimestamp().getDayOfWeek();
        accessByDay[day.getValue() - 1]++;
      }
    }

    int peakHour = findPeakIndex(accessByHour);
    int peakDayIndex = findPeakIndex(accessByDay);

    return HeatmapResponse.builder()
        .accessByHour(accessByHour)
        .accessByDay(accessByDay)
        .peakHour(peakHour)
        .peakDay(DAY_NAMES[peakDayIndex])
        .totalAccesses(logs.size())
        .period("all-time")
        .build();
  }

  private int findPeakIndex(int[] array) {
    int maxIndex = 0;
    for (int i = 1; i < array.length; i++) {
      if (array[i] > array[maxIndex]) {
        maxIndex = i;
      }
    }
    return maxIndex;
  }
}
