import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AuditLog, LoginAttempt, SecurityAlert, SecurityAuditReport,
  BreachStatus, BreachScanResult, CompromisedCredential, BreachHistoryResponse,
  SecurityScore, PasswordHealth, ReusedPasswordsResponse, PasswordAgeResponse,
  SecurityTrends, ActivityHeatmap, DashboardSummary, Notification, UnreadCountResponse,
  TimelineResponse, TimelineSummary, HealthStatus
} from '../models/security.models';
import { MessageResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class SecurityService {
  private http = inject(HttpClient);

  // ── Audit Logs ─────────────────────────────────────────────
  getAuditLogs(page = 0, size = 20): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`/api/security/audit-logs?page=${page}&size=${size}`);
  }

  // ── Login History ──────────────────────────────────────────
  getLoginHistory(page = 0, size = 20): Observable<LoginAttempt[]> {
    return this.http.get<LoginAttempt[]>(`/api/security/login-history?page=${page}&size=${size}`);
  }

  // ── Security Alerts ────────────────────────────────────────
  getAlerts(): Observable<SecurityAlert[]> {
    return this.http.get<SecurityAlert[]>('/api/security/alerts');
  }

  dismissAlert(alertId: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`/api/security/alerts/${alertId}/dismiss`, {});
  }

  // ── Security Audit ─────────────────────────────────────────
  getSecurityAudit(): Observable<SecurityAuditReport> {
    return this.http.get<SecurityAuditReport>('/api/security/audit');
  }

  getSecurityScore(): Observable<SecurityScore> {
    return this.http.get<SecurityScore>('/api/security/score');
  }

  getPasswordHealth(): Observable<PasswordHealth> {
    return this.http.get<PasswordHealth>('/api/security/password-health');
  }

  getReusedPasswords(): Observable<ReusedPasswordsResponse> {
    return this.http.get<ReusedPasswordsResponse>('/api/security/reused-passwords');
  }

  getPasswordAge(): Observable<PasswordAgeResponse> {
    return this.http.get<PasswordAgeResponse>('/api/security/password-age');
  }

  getSecurityTrends(): Observable<SecurityTrends> {
    return this.http.get<SecurityTrends>('/api/security/trends');
  }

  // ── Breach Monitor ─────────────────────────────────────────
  getBreachStatus(): Observable<BreachStatus> {
    return this.http.get<BreachStatus>('/api/breach/status');
  }

  scanForBreaches(): Observable<BreachScanResult> {
    return this.http.post<BreachScanResult>('/api/breach/scan', {});
  }

  getCompromisedCredentials(): Observable<CompromisedCredential[]> {
    return this.http.get<CompromisedCredential[]>('/api/breach/compromised');
  }

  getBreachHistory(): Observable<BreachHistoryResponse> {
    return this.http.get<BreachHistoryResponse>('/api/breach/history');
  }

  // ── Access Heatmap ─────────────────────────────────────────
  getAccessHeatmap(): Observable<ActivityHeatmap> {
    return this.http.get<ActivityHeatmap>('/api/security/heatmap');
  }

  // ── Dashboard ──────────────────────────────────────────────
  getDashboardSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>('/api/dashboard/summary');
  }

  // ── Notifications ──────────────────────────────────────────
  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>('/api/notifications');
  }

  getUnreadCount(): Observable<UnreadCountResponse> {
    return this.http.get<UnreadCountResponse>('/api/notifications/unread-count');
  }

  markNotificationRead(id: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`/api/notifications/${id}/read`, {});
  }

  markAllRead(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/notifications/read-all', {});
  }

  // ── Timeline ───────────────────────────────────────────────
  getTimeline(period: string = 'WEEK'): Observable<TimelineResponse> {
    return this.http.get<TimelineResponse>(`/api/timeline?period=${period}`);
  }

  getTimelineSummary(): Observable<TimelineSummary> {
    return this.http.get<TimelineSummary>('/api/timeline/summary');
  }

  // ── Health ─────────────────────────────────────────────────
  getHealth(): Observable<HealthStatus> {
    return this.http.get<HealthStatus>('/api/health');
  }
}
