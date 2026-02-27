package com.revature.passwordmanager.service.analytics;

import com.revature.passwordmanager.dto.response.HeatmapResponse;
import com.revature.passwordmanager.model.security.AuditLog;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.AuditLogRepository;
import com.revature.passwordmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessHeatmapServiceTest {

  @Mock
  private AuditLogRepository auditLogRepository;
  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private AccessHeatmapService accessHeatmapService;

  private User user;

  @BeforeEach
  void setUp() {
    user = User.builder()
        .id(1L).username("testuser")
        .masterPasswordHash("hash").salt("salt")
        .build();
  }

  @Test
  void getAccessHeatmap_ShouldReturnCorrectDistribution() {
    AuditLog log1 = AuditLog.builder().id(1L).user(user)
        .action(AuditLog.AuditAction.LOGIN)
        .timestamp(LocalDateTime.of(2026, 2, 10, 9, 0)) // Monday 9am
        .build();
    AuditLog log2 = AuditLog.builder().id(2L).user(user)
        .action(AuditLog.AuditAction.PASSWORD_VIEWED)
        .timestamp(LocalDateTime.of(2026, 2, 10, 9, 30)) // Monday 9am
        .build();
    AuditLog log3 = AuditLog.builder().id(3L).user(user)
        .action(AuditLog.AuditAction.LOGIN)
        .timestamp(LocalDateTime.of(2026, 2, 11, 14, 0)) // Tuesday 2pm
        .build();

    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(auditLogRepository.findByUserIdOrderByTimestampDesc(1L))
        .thenReturn(List.of(log1, log2, log3));

    HeatmapResponse result = accessHeatmapService.getAccessHeatmap("testuser");

    assertNotNull(result);
    assertEquals(3, result.getTotalAccesses());
    assertEquals(9, result.getPeakHour());
    assertEquals("Monday", result.getPeakDay());
    assertEquals(2, result.getAccessByHour()[9]);
    assertEquals(1, result.getAccessByHour()[14]);
  }

  @Test
  void getAccessHeatmap_EmptyLogs_ShouldReturnZeros() {
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
    when(auditLogRepository.findByUserIdOrderByTimestampDesc(1L)).thenReturn(List.of());

    HeatmapResponse result = accessHeatmapService.getAccessHeatmap("testuser");

    assertEquals(0, result.getTotalAccesses());
    assertEquals(0, result.getPeakHour());
  }
}
