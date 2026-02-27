package com.revature.passwordmanager.service.system;

import com.revature.passwordmanager.dto.response.HealthResponse;
import com.revature.passwordmanager.dto.response.HealthResponse.ComponentHealth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthService {

  private final DataSource dataSource;

  public HealthResponse getHealthStatus() {
    Map<String, ComponentHealth> components = new LinkedHashMap<>();

    components.put("database", checkDatabase());
    components.put("memory", checkMemory());
    components.put("diskSpace", checkDiskSpace());

    boolean allHealthy = components.values().stream()
        .allMatch(c -> "UP".equals(c.getStatus()));

    return HealthResponse.builder()
        .status(allHealthy ? "UP" : "DEGRADED")
        .timestamp(LocalDateTime.now())
        .components(components)
        .build();
  }

  private ComponentHealth checkDatabase() {
    try (Connection connection = dataSource.getConnection()) {
      if (connection.isValid(5)) {
        return ComponentHealth.builder().status("UP").details("Database connection healthy").build();
      }
    } catch (Exception e) {
      return ComponentHealth.builder().status("DOWN").details("Database error: " + e.getMessage()).build();
    }
    return ComponentHealth.builder().status("DOWN").details("Database connection invalid").build();
  }

  private ComponentHealth checkMemory() {
    Runtime runtime = Runtime.getRuntime();
    long maxMemory = runtime.maxMemory();
    long usedMemory = runtime.totalMemory() - runtime.freeMemory();
    double usage = (double) usedMemory / maxMemory * 100;

    String details = String.format("Used: %.1f%% (%.0f MB / %.0f MB)",
        usage, usedMemory / 1048576.0, maxMemory / 1048576.0);

    return ComponentHealth.builder()
        .status(usage < 90 ? "UP" : "WARNING")
        .details(details)
        .build();
  }

  private ComponentHealth checkDiskSpace() {
    java.io.File root = new java.io.File(".");
    long free = root.getFreeSpace();
    long total = root.getTotalSpace();
    double usage = (1.0 - (double) free / total) * 100;

    String details = String.format("Used: %.1f%% (%.0f GB free)",
        usage, free / 1073741824.0);

    return ComponentHealth.builder()
        .status(free > 1073741824L ? "UP" : "WARNING")
        .details(details)
        .build();
  }
}
