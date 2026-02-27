package com.revature.passwordmanager.service.system;

import com.revature.passwordmanager.dto.response.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthServiceTest {

  @Mock
  private DataSource dataSource;

  @Mock
  private Connection connection;

  @InjectMocks
  private HealthService healthService;

  @BeforeEach
  void setUp() {
  }

  @Test
  void getHealthStatus_ShouldReturnUp_WhenDatabaseIsHealthy() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isValid(anyInt())).thenReturn(true);

    HealthResponse response = healthService.getHealthStatus();

    assertEquals("UP", response.getStatus());
    assertEquals("UP", response.getComponents().get("database").getStatus());
    assertNotNull(response.getComponents().get("memory"));
    assertNotNull(response.getComponents().get("diskSpace"));
  }

  @Test
  void getHealthStatus_ShouldReturnDegraded_WhenDatabaseIsDown() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.isValid(anyInt())).thenReturn(false);

    HealthResponse response = healthService.getHealthStatus();

    assertEquals("DEGRADED", response.getStatus()); // One down means overall degraded/down depending on logic logic is
                                                    // "All Match UP" -> if one is DOWN, then DEGRADED
    assertEquals("DOWN", response.getComponents().get("database").getStatus());
  }

  @Test
  void getHealthStatus_ShouldReturnDegraded_WhenDatabaseThrowsException() throws SQLException {
    when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

    HealthResponse response = healthService.getHealthStatus();

    assertEquals("DEGRADED", response.getStatus());
    assertEquals("DOWN", response.getComponents().get("database").getStatus());
    assertTrue(response.getComponents().get("database").getDetails().contains("Connection failed"));
  }
}
