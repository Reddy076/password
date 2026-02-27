package com.revature.passwordmanager.service.security.breach;

import com.revature.passwordmanager.model.security.SecurityAlert.AlertType;
import com.revature.passwordmanager.model.security.SecurityAlert.Severity;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.service.security.SecurityAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BreachNotificationServiceTest {

    @Mock
    private SecurityAlertService securityAlertService;

    @InjectMocks
    private BreachNotificationService notificationService;

    private User user;
    private VaultEntry entry;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("testuser").email("t@e.com")
                .masterPasswordHash("h").salt("s").build();

        entry = VaultEntry.builder()
                .id(10L).title("Gmail").username("user@gmail.com")
                .password("encPass").websiteUrl("https://gmail.com")
                .user(user).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .isDeleted(false).isFavorite(false).isHighlySensitive(false).build();
    }

    // ── notifyBreach ──────────────────────────────────────────────────────────

    @Test
    void notifyBreach_HighCount_ShouldCreateCriticalAlert() {
        notificationService.notifyBreach("testuser", entry, 50000L);

        ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
        verify(securityAlertService).createAlert(
                eq("testuser"),
                eq(AlertType.PASSWORD_BREACHED),
                argThat(t -> t.contains("Gmail")),
                argThat(m -> m.contains("50,000") || m.contains("50000")),
                severityCaptor.capture()
        );

        assertEquals(Severity.CRITICAL, severityCaptor.getValue());
    }

    @Test
    void notifyBreach_LowCount_ShouldCreateHighAlert() {
        notificationService.notifyBreach("testuser", entry, 100L);

        ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
        verify(securityAlertService).createAlert(
                eq("testuser"),
                eq(AlertType.PASSWORD_BREACHED),
                anyString(),
                anyString(),
                severityCaptor.capture()
        );

        assertEquals(Severity.HIGH, severityCaptor.getValue());
    }

    @Test
    void notifyBreach_ExactThreshold10000_ShouldCreateHighAlert() {
        notificationService.notifyBreach("testuser", entry, 10000L);

        ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
        verify(securityAlertService).createAlert(
                eq("testuser"), eq(AlertType.PASSWORD_BREACHED),
                anyString(), anyString(), severityCaptor.capture());

        // 10000 is NOT > 10000 so should be HIGH
        assertEquals(Severity.HIGH, severityCaptor.getValue());
    }

    @Test
    void notifyBreach_TitleIncludesEntryName() {
        notificationService.notifyBreach("testuser", entry, 500L);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(securityAlertService).createAlert(
                anyString(), any(), titleCaptor.capture(), anyString(), any());

        assertTrue(titleCaptor.getValue().contains("Gmail"));
    }

    @Test
    void notifyBreach_MessageIncludesPwnedCount() {
        notificationService.notifyBreach("testuser", entry, 1234L);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(securityAlertService).createAlert(
                anyString(), any(), anyString(), messageCaptor.capture(), any());

        assertTrue(messageCaptor.getValue().contains("1,234") || messageCaptor.getValue().contains("1234"));
    }

    @Test
    void notifyBreach_NoWebsiteUrl_ShouldHandleGracefully() {
        entry.setWebsiteUrl(null);

        assertDoesNotThrow(() -> notificationService.notifyBreach("testuser", entry, 500L));
        verify(securityAlertService).createAlert(anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void notifyBreach_AlertServiceThrows_ShouldNotPropagate() {
        doThrow(new RuntimeException("DB error"))
                .when(securityAlertService).createAlert(anyString(), any(), anyString(), anyString(), any());

        assertDoesNotThrow(() -> notificationService.notifyBreach("testuser", entry, 500L));
    }

    // ── notifyScanComplete ────────────────────────────────────────────────────

    @Test
    void notifyScanComplete_NoCompromised_ShouldCreateLowSeverityAlert() {
        notificationService.notifyScanComplete("testuser", 0, 10);

        ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        // Gap 7 fix: all-clear now uses BREACH_SCAN_COMPLETE, not PASSWORD_BREACHED
        verify(securityAlertService).createAlert(
                eq("testuser"), eq(AlertType.BREACH_SCAN_COMPLETE),
                titleCaptor.capture(), anyString(), severityCaptor.capture());

        assertEquals(Severity.LOW, severityCaptor.getValue());
        assertTrue(titleCaptor.getValue().contains("All Clear"));
    }

    @Test
    void notifyScanComplete_FewCompromised_ShouldCreateHighAlert() {
        notificationService.notifyScanComplete("testuser", 2, 10);

        ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
        verify(securityAlertService).createAlert(
                anyString(), any(), anyString(), anyString(), severityCaptor.capture());

        assertEquals(Severity.HIGH, severityCaptor.getValue());
    }

    @Test
    void notifyScanComplete_ManyCompromised_ShouldCreateCriticalAlert() {
        notificationService.notifyScanComplete("testuser", 5, 10);

        ArgumentCaptor<Severity> severityCaptor = ArgumentCaptor.forClass(Severity.class);
        verify(securityAlertService).createAlert(
                anyString(), any(), anyString(), anyString(), severityCaptor.capture());

        assertEquals(Severity.CRITICAL, severityCaptor.getValue());
    }

    @Test
    void notifyScanComplete_TitleIncludesCompromisedCount() {
        notificationService.notifyScanComplete("testuser", 3, 10);

        ArgumentCaptor<String> titleCaptor = ArgumentCaptor.forClass(String.class);
        verify(securityAlertService).createAlert(
                anyString(), any(), titleCaptor.capture(), anyString(), any());

        assertTrue(titleCaptor.getValue().contains("3"));
    }

    @Test
    void notifyScanComplete_MessageIncludesScannedCount() {
        notificationService.notifyScanComplete("testuser", 0, 15);

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(securityAlertService).createAlert(
                anyString(), any(), anyString(), messageCaptor.capture(), any());

        assertTrue(messageCaptor.getValue().contains("15"));
    }
}
