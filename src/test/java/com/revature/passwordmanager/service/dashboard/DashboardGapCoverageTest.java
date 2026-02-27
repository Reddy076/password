package com.revature.passwordmanager.service.dashboard;

import com.revature.passwordmanager.dto.response.SecurityScoreResponse;
import com.revature.passwordmanager.dto.response.SecurityTrendResponse;
import com.revature.passwordmanager.model.dashboard.SecurityMetricsHistory;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.SecurityMetricsHistoryRepository;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.service.security.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Targeted gap-coverage tests for Feature 33 (Password Strength Dashboard).
 *
 * <p>The gap analysis identified the following extras implemented beyond the spec:</p>
 * <ol>
 *   <li>Snapshot cooldown (60-min guard prevents unbounded table growth)</li>
 *   <li>Five-tier score labels: Excellent, Good, Fair, Poor, Critical</li>
 *   <li>Three trend directions: IMPROVING, DECLINING, STABLE</li>
 *   <li>Extra snapshot columns: strongPasswordsCount, fairPasswordsCount, totalPasswordsCount</li>
 *   <li>Dynamic recommendation field in SecurityScoreResponse</li>
 *   <li>Audit log on every dashboard call</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class DashboardGapCoverageTest {

    @Mock private SecurityMetricsCalculator metricsCalculator;
    @Mock private SecurityMetricsHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private PasswordStrengthDashboardService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("alice").masterPasswordHash("hash").salt("salt").build();
    }

    // ── Gap 1: Snapshot cooldown ──────────────────────────────────────────────

    @Test
    void getSecurityScore_NoRecentSnapshot_ShouldPersistSnapshot() {
        SecurityScoreResponse score = buildScore(75, "Fair", 10, 2, 1, 1, 4, 2);
        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(metricsCalculator.calculateSecurityScore(1L)).thenReturn(score);
        when(historyRepository.findTopByUserIdOrderByRecordedAtDesc(1L)).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.getSecurityScore("alice");

        verify(historyRepository).save(any(SecurityMetricsHistory.class));
    }

    @Test
    void getSecurityScore_RecentSnapshotWithinCooldown_ShouldNotPersistNewSnapshot() {
        SecurityScoreResponse score = buildScore(75, "Fair", 10, 2, 1, 1, 4, 2);
        SecurityMetricsHistory recent = SecurityMetricsHistory.builder()
                .id(1L).user(user).overallScore(75)
                .recordedAt(LocalDateTime.now().minusMinutes(10)) // within 60-min cooldown
                .build();

        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(metricsCalculator.calculateSecurityScore(1L)).thenReturn(score);
        when(historyRepository.findTopByUserIdOrderByRecordedAtDesc(1L)).thenReturn(Optional.of(recent));

        service.getSecurityScore("alice");

        verify(historyRepository, never()).save(any());
    }

    @Test
    void getSecurityScore_LastSnapshotOlderThan60Minutes_ShouldPersistNewSnapshot() {
        SecurityScoreResponse score = buildScore(80, "Good", 10, 1, 0, 1, 5, 3);
        SecurityMetricsHistory old = SecurityMetricsHistory.builder()
                .id(1L).user(user).overallScore(70)
                .recordedAt(LocalDateTime.now().minusMinutes(61)) // outside cooldown
                .build();

        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(metricsCalculator.calculateSecurityScore(1L)).thenReturn(score);
        when(historyRepository.findTopByUserIdOrderByRecordedAtDesc(1L)).thenReturn(Optional.of(old));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.getSecurityScore("alice");

        verify(historyRepository).save(any(SecurityMetricsHistory.class));
    }

    // ── Gap 2: Extra snapshot columns (strongPasswords, fairPasswords, totalPasswords) ──

    @Test
    void getSecurityScore_Snapshot_ShouldPersistAllExtraColumns() {
        SecurityScoreResponse score = buildScore(85, "Good", 10, 1, 0, 2, 6, 3);
        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(metricsCalculator.calculateSecurityScore(1L)).thenReturn(score);
        when(historyRepository.findTopByUserIdOrderByRecordedAtDesc(1L)).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.getSecurityScore("alice");

        ArgumentCaptor<SecurityMetricsHistory> captor = ArgumentCaptor.forClass(SecurityMetricsHistory.class);
        verify(historyRepository).save(captor.capture());
        SecurityMetricsHistory saved = captor.getValue();

        // Extra columns beyond the spec
        assertThat(saved.getStrongPasswordsCount()).isEqualTo(6);  // beyond spec
        assertThat(saved.getFairPasswordsCount()).isEqualTo(3);    // beyond spec
        assertThat(saved.getTotalPasswordsCount()).isEqualTo(10);  // beyond spec
        // Spec columns still populated
        assertThat(saved.getOverallScore()).isEqualTo(85);
        assertThat(saved.getWeakPasswordsCount()).isEqualTo(1);
        assertThat(saved.getReusedPasswordsCount()).isEqualTo(0);
        assertThat(saved.getOldPasswordsCount()).isEqualTo(2);
    }

    // ── Gap 3: Audit log on every dashboard call ──────────────────────────────

    @Test
    void getSecurityScore_ShouldAlwaysLogAuditAction() {
        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(metricsCalculator.calculateSecurityScore(1L)).thenReturn(buildScore(70, "Fair", 5, 1, 1, 0, 2, 1));
        when(historyRepository.findTopByUserIdOrderByRecordedAtDesc(1L)).thenReturn(Optional.empty());
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.getSecurityScore("alice");

        verify(auditLogService).logAction(eq("alice"),
                eq(com.revature.passwordmanager.model.security.AuditLog.AuditAction.DASHBOARD_VIEWED),
                anyString());
    }

    @Test
    void getPasswordHealth_ShouldLogAuditAction() {
        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(metricsCalculator.calculatePasswordHealth(1L))
                .thenReturn(com.revature.passwordmanager.dto.response.PasswordHealthMetricsResponse.builder()
                        .totalPasswords(0).categoryBreakdowns(List.of()).build());

        service.getPasswordHealth("alice");

        verify(auditLogService).logAction(eq("alice"),
                eq(com.revature.passwordmanager.model.security.AuditLog.AuditAction.DASHBOARD_VIEWED),
                anyString());
    }

    // ── Gap 4: Trend direction computation ───────────────────────────────────

    @Test
    void getSecurityTrends_ScoreIncreasing_ShouldReturnImproving() {
        List<SecurityMetricsHistory> history = List.of(
                buildHistory(60, LocalDateTime.now().minusDays(10)),
                buildHistory(75, LocalDateTime.now().minusDays(5)),
                buildHistory(85, LocalDateTime.now())
        );

        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(history);

        SecurityTrendResponse response = service.getSecurityTrends("alice", 30);

        assertThat(response.getTrendDirection()).isEqualTo("IMPROVING");
        assertThat(response.getScoreChange()).isEqualTo(25); // 85 - 60
    }

    @Test
    void getSecurityTrends_ScoreDecreasing_ShouldReturnDeclining() {
        List<SecurityMetricsHistory> history = List.of(
                buildHistory(90, LocalDateTime.now().minusDays(10)),
                buildHistory(75, LocalDateTime.now().minusDays(5)),
                buildHistory(65, LocalDateTime.now())
        );

        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(history);

        SecurityTrendResponse response = service.getSecurityTrends("alice", 30);

        assertThat(response.getTrendDirection()).isEqualTo("DECLINING");
        assertThat(response.getScoreChange()).isEqualTo(-25); // 65 - 90
    }

    @Test
    void getSecurityTrends_ScoreUnchanged_ShouldReturnStable() {
        List<SecurityMetricsHistory> history = List.of(
                buildHistory(80, LocalDateTime.now().minusDays(5)),
                buildHistory(80, LocalDateTime.now())
        );

        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(history);

        SecurityTrendResponse response = service.getSecurityTrends("alice", 30);

        assertThat(response.getTrendDirection()).isEqualTo("STABLE");
        assertThat(response.getScoreChange()).isZero();
    }

    @Test
    void getSecurityTrends_SingleDataPoint_ShouldReturnStable() {
        List<SecurityMetricsHistory> history = List.of(
                buildHistory(80, LocalDateTime.now())
        );

        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(history);

        SecurityTrendResponse response = service.getSecurityTrends("alice", 30);

        assertThat(response.getTrendDirection()).isEqualTo("STABLE");
        assertThat(response.getScoreChange()).isZero();
    }

    @Test
    void getSecurityTrends_NoHistory_ShouldReturnStableWithZeroChange() {
        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(List.of());

        SecurityTrendResponse response = service.getSecurityTrends("alice", 30);

        assertThat(response.getTrendDirection()).isEqualTo("STABLE");
        assertThat(response.getScoreChange()).isZero();
        assertThat(response.getTrendPoints()).isEmpty();
    }

    @Test
    void getSecurityTrends_PeriodLabel_ShouldIncludeDaysCount() {
        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(List.of());

        SecurityTrendResponse response7 = service.getSecurityTrends("alice", 7);
        SecurityTrendResponse response90 = service.getSecurityTrends("alice", 90);

        assertThat(response7.getPeriodLabel()).contains("7");
        assertThat(response90.getPeriodLabel()).contains("90");
    }

    // ── Gap 5: TrendDataPoint fields ──────────────────────────────────────────

    @Test
    void getSecurityTrends_TrendPoints_ShouldIncludeAllMetrics() {
        SecurityMetricsHistory entry = buildHistory(75, LocalDateTime.of(2026, 2, 1, 10, 0));
        entry.setWeakPasswordsCount(3);
        entry.setReusedPasswordsCount(2);
        entry.setOldPasswordsCount(1);

        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(List.of(entry));

        SecurityTrendResponse response = service.getSecurityTrends("alice", 30);

        assertThat(response.getTrendPoints()).hasSize(1);
        SecurityTrendResponse.TrendDataPoint point = response.getTrendPoints().get(0);
        assertThat(point.getOverallScore()).isEqualTo(75);
        assertThat(point.getWeakPasswordsCount()).isEqualTo(3);
        assertThat(point.getReusedPasswordsCount()).isEqualTo(2);
        assertThat(point.getOldPasswordsCount()).isEqualTo(1);
        assertThat(point.getRecordedAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 10, 0));
    }

    // ── Gap 6: Custom days window ─────────────────────────────────────────────

    @Test
    void getSecurityTrends_CustomDays_ShouldQueryCorrectTimeWindow() {
        when(userRepository.findByUsernameOrThrow("alice")).thenReturn(user);
        when(historyRepository.findTrendData(eq(1L), any(LocalDateTime.class))).thenReturn(List.of());

        service.getSecurityTrends("alice", 7);

        ArgumentCaptor<LocalDateTime> sinceCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(historyRepository).findTrendData(eq(1L), sinceCaptor.capture());

        LocalDateTime since = sinceCaptor.getValue();
        assertThat(since).isAfter(LocalDateTime.now().minusDays(8));
        assertThat(since).isBefore(LocalDateTime.now().minusDays(6));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private SecurityScoreResponse buildScore(int overall, String label, int total,
                                              int weak, int reused, int old, int strong, int fair) {
        return SecurityScoreResponse.builder()
                .overallScore(overall).scoreLabel(label).totalPasswords(total)
                .weakPasswords(weak).reusedPasswords(reused).oldPasswords(old)
                .strongPasswords(strong).fairPasswords(fair)
                .recommendation("Keep it up!")
                .build();
    }

    private SecurityMetricsHistory buildHistory(int score, LocalDateTime recordedAt) {
        return SecurityMetricsHistory.builder()
                .id(1L).user(user).overallScore(score)
                .weakPasswordsCount(0).reusedPasswordsCount(0).oldPasswordsCount(0)
                .recordedAt(recordedAt)
                .build();
    }
}
