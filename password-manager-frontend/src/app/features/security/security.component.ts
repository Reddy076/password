import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SecurityService } from '../../core/services/security.service';
import { UiStateService } from '../../core/state/ui.state';
import {
  SecurityAuditReport, SecurityScore, BreachStatus, CompromisedCredential,
  SecurityAlert, LoginAttempt, AuditLog
} from '../../core/models/security.models';

type SecurityTab = 'overview' | 'audit' | 'breach' | 'alerts' | 'history' | 'logs';

@Component({
  selector: 'app-security',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">Security Center</h1>
          <p class="page-subtitle">Monitor and improve your vault security</p>
        </div>
        <button class="pill-btn pill-btn-secondary pill-btn-sm" (click)="refreshAll()">
          🔄 Refresh
        </button>
      </div>

      <!-- Tab Navigation -->
      <div class="tab-nav">
        <button class="tab-btn" [class.active]="activeTab() === 'overview'" (click)="activeTab.set('overview')">
          🛡️ Overview
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'audit'" (click)="activeTab.set('audit')">
          📊 Password Audit
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'breach'" (click)="activeTab.set('breach')">
          🚨 Breach Monitor
          <span class="tab-badge" *ngIf="(breachStatus()?.compromisedCount ?? 0) > 0">
            {{ breachStatus()!.compromisedCount }}
          </span>
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'alerts'" (click)="activeTab.set('alerts')">
          🔔 Alerts
          <span class="tab-badge" *ngIf="unreadAlerts() > 0">{{ unreadAlerts() }}</span>
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'history'" (click)="activeTab.set('history')">
          📋 Login History
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'logs'" (click)="activeTab.set('logs')">
          📝 Audit Logs
        </button>
      </div>

      <!-- Overview Tab -->
      <div *ngIf="activeTab() === 'overview'">
        <!-- Score Card -->
        <div class="score-card" *ngIf="securityScore()">
          <div class="score-circle" [class]="getScoreClass(securityScore()!.overallScore)">
            <div class="score-number">{{ securityScore()!.overallScore }}</div>
            <div class="score-max">/100</div>
          </div>
          <div class="score-details">
            <h3 class="score-label">{{ securityScore()!.scoreLabel }}</h3>
            <p class="score-recommendation">{{ securityScore()!.recommendation }}</p>
            <div class="score-stats">
              <div class="score-stat">
                <span class="stat-num text-success">{{ securityScore()!.strongPasswords }}</span>
                <span class="stat-lbl">Strong</span>
              </div>
              <div class="score-stat">
                <span class="stat-num text-warning">{{ securityScore()!.fairPasswords }}</span>
                <span class="stat-lbl">Fair</span>
              </div>
              <div class="score-stat">
                <span class="stat-num text-danger">{{ securityScore()!.weakPasswords }}</span>
                <span class="stat-lbl">Weak</span>
              </div>
              <div class="score-stat">
                <span class="stat-num text-warning">{{ securityScore()!.reusedPasswords }}</span>
                <span class="stat-lbl">Reused</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Quick Actions -->
        <div class="quick-actions-grid">
          <button class="action-tile" (click)="runBreachScan()" [class.loading-tile]="scanning()">
            <span class="action-tile-icon">🔍</span>
            <span class="action-tile-label">{{ scanning() ? 'Scanning...' : 'Run Breach Scan' }}</span>
          </button>
          <button class="action-tile" (click)="activeTab.set('audit')">
            <span class="action-tile-icon">📊</span>
            <span class="action-tile-label">Password Audit</span>
          </button>
          <a class="action-tile" routerLink="/generator">
            <span class="action-tile-icon">⚡</span>
            <span class="action-tile-label">Generate Password</span>
          </a>
          <button class="action-tile" (click)="activeTab.set('history')">
            <span class="action-tile-icon">📋</span>
            <span class="action-tile-label">Login History</span>
          </button>
        </div>
      </div>

      <!-- Audit Tab -->
      <div *ngIf="activeTab() === 'audit'">
        <div class="loading-state" *ngIf="loadingAudit()">
          <div class="skeleton" style="height:200px"></div>
        </div>

        <div *ngIf="!loadingAudit() && auditReport()">
          <!-- Weak Passwords -->
          <div class="audit-section" *ngIf="auditReport()!.weakPasswords?.length > 0">
            <div class="audit-section-header">
              <span class="audit-icon">⚠️</span>
              <h3>Weak Passwords ({{ auditReport()!.weakPasswords.length }})</h3>
            </div>
            <div class="audit-list">
              <div class="audit-item" *ngFor="let entry of auditReport()!.weakPasswords">
                <div class="audit-item-info">
                  <div class="audit-item-title">{{ entry.title }}</div>
                  <div class="audit-item-sub">{{ entry.username ?? entry.websiteUrl ?? 'No details' }}</div>
                </div>
                <span class="pill-badge badge-danger">{{ entry.strengthLabel ?? 'Weak' }}</span>
                <a routerLink="/vault" class="pill-btn pill-btn-secondary pill-btn-sm">Fix →</a>
              </div>
            </div>
          </div>

          <!-- Reused Passwords -->
          <div class="audit-section" *ngIf="auditReport()!.reusedPasswords?.length > 0">
            <div class="audit-section-header">
              <span class="audit-icon">🔄</span>
              <h3>Reused Passwords ({{ auditReport()!.reusedPasswords.length }})</h3>
            </div>
            <div class="audit-list">
              <div class="audit-item" *ngFor="let entry of auditReport()!.reusedPasswords">
                <div class="audit-item-info">
                  <div class="audit-item-title">{{ entry.title }}</div>
                  <div class="audit-item-sub">{{ entry.username ?? 'No username' }}</div>
                </div>
                <span class="pill-badge badge-warning">Reused</span>
                <a routerLink="/vault" class="pill-btn pill-btn-secondary pill-btn-sm">Fix →</a>
              </div>
            </div>
          </div>

          <!-- Old Passwords -->
          <div class="audit-section" *ngIf="auditReport()!.oldPasswords?.length > 0">
            <div class="audit-section-header">
              <span class="audit-icon">⏰</span>
              <h3>Old Passwords ({{ auditReport()!.oldPasswords.length }})</h3>
            </div>
            <div class="audit-list">
              <div class="audit-item" *ngFor="let entry of auditReport()!.oldPasswords">
                <div class="audit-item-info">
                  <div class="audit-item-title">{{ entry.title }}</div>
                  <div class="audit-item-sub">{{ entry.username ?? 'No username' }}</div>
                </div>
                <span class="pill-badge badge-warning">Outdated</span>
                <a routerLink="/vault" class="pill-btn pill-btn-secondary pill-btn-sm">Update →</a>
              </div>
            </div>
          </div>

          <div class="empty-state" *ngIf="!auditReport()!.weakPasswords?.length && !auditReport()!.reusedPasswords?.length && !auditReport()!.oldPasswords?.length">
            <div class="empty-icon">✅</div>
            <div class="empty-title">All passwords look great!</div>
            <div class="empty-message">No weak, reused, or outdated passwords found.</div>
          </div>
        </div>
      </div>

      <!-- Breach Monitor Tab -->
      <div *ngIf="activeTab() === 'breach'">
        <div class="breach-status-card" *ngIf="breachStatus()"
          [class.breach-safe]="breachStatus()!.status === 'SAFE'"
          [class.breach-risk]="breachStatus()!.status === 'AT_RISK'">
          <div class="breach-status-icon">
            {{ breachStatus()!.status === 'SAFE' ? '✅' : '🚨' }}
          </div>
          <div class="breach-status-info">
            <h3>{{ breachStatus()!.status === 'SAFE' ? 'No Breaches Detected' : 'Breaches Detected!' }}</h3>
            <p>{{ breachStatus()!.compromisedCount }} compromised credential{{ breachStatus()!.compromisedCount !== 1 ? 's' : '' }} found</p>
            <p *ngIf="breachStatus()!.lastScanAt" style="font-size:12px;opacity:0.7">
              Last scan: {{ formatDate(breachStatus()!.lastScanAt!) }}
            </p>
          </div>
          <button class="pill-btn pill-btn-primary" (click)="runBreachScan()" [class.loading]="scanning()">
            <span class="btn-text">🔍 Scan Now</span>
            <span class="btn-spinner" *ngIf="scanning()"></span>
          </button>
        </div>

        <div class="audit-section" *ngIf="compromisedCredentials().length > 0">
          <div class="audit-section-header">
            <span class="audit-icon">🚨</span>
            <h3>Compromised Credentials ({{ compromisedCredentials().length }})</h3>
          </div>
          <div class="audit-list">
            <div class="audit-item" *ngFor="let cred of compromisedCredentials()">
              <div class="audit-item-info">
                <div class="audit-item-title">{{ cred.vaultEntryTitle }}</div>
                <div class="audit-item-sub">Found in {{ cred.pwnedCount | number }} data breaches</div>
              </div>
              <span class="pill-badge" [class]="cred.isResolved ? 'badge-success' : 'badge-danger'">
                {{ cred.isResolved ? 'Resolved' : 'At Risk' }}
              </span>
              <a routerLink="/vault" class="pill-btn pill-btn-danger pill-btn-sm" *ngIf="!cred.isResolved">
                Change Password →
              </a>
            </div>
          </div>
        </div>
      </div>

      <!-- Alerts Tab -->
      <div *ngIf="activeTab() === 'alerts'">
        <div class="empty-state" *ngIf="alerts().length === 0">
          <div class="empty-icon">✅</div>
          <div class="empty-title">No security alerts</div>
          <div class="empty-message">Your account is secure</div>
        </div>

        <div class="alerts-list" *ngIf="alerts().length > 0">
          <div class="alert-card" *ngFor="let alert of alerts()"
            [class]="'alert-card-' + alert.severity.toLowerCase()"
            [class.alert-read]="alert.isRead">
            <div class="alert-card-icon">{{ getSeverityIcon(alert.severity) }}</div>
            <div class="alert-card-content">
              <div class="alert-card-title">{{ alert.title }}</div>
              <div class="alert-card-message">{{ alert.message }}</div>
              <div class="alert-card-time">{{ formatDate(alert.createdAt) }}</div>
            </div>
            <div class="alert-card-actions">
              <span class="pill-badge" [class]="getSeverityBadge(alert.severity)">{{ alert.severity }}</span>
              <button class="pill-btn pill-btn-ghost pill-btn-sm" (click)="dismissAlert(alert)"
                *ngIf="!alert.isRead">Dismiss</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Login History Tab -->
      <div *ngIf="activeTab() === 'history'">
        <div class="data-table">
          <div class="table-header">
            <div class="th">Status</div>
            <div class="th">IP Address</div>
            <div class="th">Device</div>
            <div class="th">Location</div>
            <div class="th">Time</div>
          </div>
          <div class="table-row" *ngFor="let attempt of loginHistory()"
            [class.row-success]="attempt.status === 'SUCCESS'"
            [class.row-failed]="attempt.status === 'FAILED'">
            <div class="td">
              <span class="pill-badge" [class]="attempt.status === 'SUCCESS' ? 'badge-success' : 'badge-danger'">
                {{ attempt.status === 'SUCCESS' ? '✅ Success' : '❌ Failed' }}
              </span>
            </div>
            <div class="td text-mono" style="font-size:12px">{{ attempt.ipAddress }}</div>
            <div class="td" style="font-size:12px">{{ attempt.deviceInfo | slice:0:40 }}</div>
            <div class="td" style="font-size:12px">{{ attempt.location ?? 'Unknown' }}</div>
            <div class="td" style="font-size:12px">{{ formatDate(attempt.createdAt) }}</div>
          </div>
        </div>
      </div>

      <!-- Audit Logs Tab -->
      <div *ngIf="activeTab() === 'logs'">
        <div class="data-table">
          <div class="table-header">
            <div class="th">Action</div>
            <div class="th">Details</div>
            <div class="th">IP Address</div>
            <div class="th">Time</div>
          </div>
          <div class="table-row" *ngFor="let log of auditLogs()">
            <div class="td">
              <span class="pill-badge badge-primary">{{ log.action }}</span>
            </div>
            <div class="td" style="font-size:12px">{{ log.details | slice:0:60 }}</div>
            <div class="td text-mono" style="font-size:12px">{{ log.ipAddress }}</div>
            <div class="td" style="font-size:12px">{{ formatDate(log.createdAt) }}</div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .tab-nav {
      display: flex; gap: 4px; flex-wrap: wrap;
      padding: 16px 28px 0;
      border-bottom: 1px solid var(--border-subtle);
      margin-bottom: 20px;
    }
    .tab-btn {
      display: flex; align-items: center; gap: 6px;
      padding: 8px 16px;
      border-radius: var(--pill-radius-full) var(--pill-radius-full) 0 0;
      background: none; border: none; cursor: pointer;
      font-size: 13px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      position: relative;
      &:hover { color: var(--text-primary); background: var(--bg-hover); }
      &.active { color: var(--accent-primary); background: var(--bg-active); font-weight: 600; }
    }
    .tab-badge {
      min-width: 18px; height: 18px; padding: 0 5px;
      background: var(--accent-danger); color: #fff;
      font-size: 10px; font-weight: 700;
      border-radius: 9px;
      display: flex; align-items: center; justify-content: center;
    }

    .score-card {
      display: flex; align-items: center; gap: 24px;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl);
      padding: 24px 28px;
      margin-bottom: 16px;
      @media (max-width: 767px) { flex-direction: column; text-align: center; }
    }
    .score-circle {
      width: 100px; height: 100px;
      border-radius: 50%;
      display: flex; flex-direction: column; align-items: center; justify-content: center;
      flex-shrink: 0;
      border: 4px solid;
      &.score-excellent { border-color: var(--accent-success); color: var(--accent-success); }
      &.score-good { border-color: #84cc16; color: #84cc16; }
      &.score-fair { border-color: var(--accent-warning); color: var(--accent-warning); }
      &.score-poor { border-color: var(--accent-orange); color: var(--accent-orange); }
      &.score-critical { border-color: var(--accent-danger); color: var(--accent-danger); }
    }
    .score-number { font-size: 32px; font-weight: 800; line-height: 1; }
    .score-max { font-size: 12px; color: var(--text-muted); }
    .score-label { font-size: 18px; font-weight: 700; color: var(--text-primary); margin: 0 0 6px; }
    .score-recommendation { font-size: 13px; color: var(--text-secondary); margin: 0 0 12px; }
    .score-stats { display: flex; gap: 20px; }
    .score-stat { display: flex; flex-direction: column; align-items: center; gap: 2px; }
    .stat-num { font-size: 20px; font-weight: 700; }
    .stat-lbl { font-size: 11px; color: var(--text-muted); }

    .quick-actions-grid {
      display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px;
      @media (max-width: 767px) { grid-template-columns: repeat(2, 1fr); }
    }
    .action-tile {
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      padding: 20px 12px;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-lg);
      cursor: pointer; text-decoration: none;
      transition: all var(--transition-base);
      &:hover { border-color: var(--accent-primary); background: var(--bg-active); transform: translateY(-2px); }
      &.loading-tile { opacity: 0.7; pointer-events: none; }
    }
    .action-tile-icon { font-size: 28px; }
    .action-tile-label { font-size: 12px; font-weight: 500; color: var(--text-secondary); text-align: center; }

    .audit-section {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl);
      overflow: hidden;
      margin-bottom: 16px;
    }
    .audit-section-header {
      display: flex; align-items: center; gap: 10px;
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-subtle);
      background: var(--bg-elevated);
      h3 { font-size: 14px; font-weight: 600; color: var(--text-primary); margin: 0; }
    }
    .audit-icon { font-size: 18px; }
    .audit-list { padding: 8px; }
    .audit-item {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 12px;
      border-radius: var(--pill-radius-xs);
      &:hover { background: var(--bg-hover); }
    }
    .audit-item-info { flex: 1; overflow: hidden; }
    .audit-item-title { font-size: 13px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .audit-item-sub { font-size: 11px; color: var(--text-muted); }

    .breach-status-card {
      display: flex; align-items: center; gap: 20px;
      padding: 24px;
      border-radius: var(--pill-radius-xl);
      margin-bottom: 16px;
      border: 2px solid;
      &.breach-safe { background: rgba(16,185,129,0.08); border-color: var(--accent-success); }
      &.breach-risk { background: rgba(239,68,68,0.08); border-color: var(--accent-danger); }
    }
    .breach-status-icon { font-size: 40px; flex-shrink: 0; }
    .breach-status-info { flex: 1; h3 { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0 0 4px; } p { font-size: 13px; color: var(--text-secondary); margin: 0; } }

    .alerts-list { display: flex; flex-direction: column; gap: 8px; }
    .alert-card {
      display: flex; align-items: flex-start; gap: 12px;
      padding: 14px 16px;
      border-radius: var(--pill-radius-lg);
      border-left: 4px solid;
      background: var(--bg-surface);
      border-top: 1px solid var(--border-subtle);
      border-right: 1px solid var(--border-subtle);
      border-bottom: 1px solid var(--border-subtle);
      &.alert-card-low { border-left-color: var(--accent-info); }
      &.alert-card-medium { border-left-color: var(--accent-warning); }
      &.alert-card-high { border-left-color: var(--accent-orange); }
      &.alert-card-critical { border-left-color: var(--accent-danger); }
      &.alert-read { opacity: 0.6; }
    }
    .alert-card-icon { font-size: 20px; flex-shrink: 0; }
    .alert-card-content { flex: 1; }
    .alert-card-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
    .alert-card-message { font-size: 12px; color: var(--text-secondary); margin-top: 2px; }
    .alert-card-time { font-size: 11px; color: var(--text-muted); margin-top: 4px; }
    .alert-card-actions { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; flex-shrink: 0; }

    .data-table {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl);
      overflow: hidden;
    }
    .table-header {
      display: grid; grid-template-columns: 120px 1fr 1fr 1fr 140px;
      padding: 10px 16px;
      background: var(--bg-elevated);
      border-bottom: 1px solid var(--border-subtle);
      @media (max-width: 767px) { display: none; }
    }
    .th { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.05em; color: var(--text-muted); }
    .table-row {
      display: grid; grid-template-columns: 120px 1fr 1fr 1fr 140px;
      padding: 10px 16px;
      border-bottom: 1px solid var(--border-subtle);
      align-items: center;
      &:last-child { border-bottom: none; }
      &:hover { background: var(--bg-hover); }
      @media (max-width: 767px) { grid-template-columns: 1fr; gap: 4px; }
    }
    .td { font-size: 13px; color: var(--text-secondary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .row-success { }
    .row-failed { background: rgba(239,68,68,0.03); }

    .loading-state { padding: 20px; }
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; gap: 8px; }
    .empty-icon { font-size: 48px; }
    .empty-title { font-size: 16px; font-weight: 600; color: var(--text-primary); }
    .empty-message { font-size: 13px; color: var(--text-muted); }
  `]
})
export class SecurityComponent implements OnInit {
  private securityService = inject(SecurityService);
  private uiState = inject(UiStateService);

  activeTab = signal<SecurityTab>('overview');
  securityScore = signal<SecurityScore | null>(null);
  auditReport = signal<SecurityAuditReport | null>(null);
  breachStatus = signal<BreachStatus | null>(null);
  compromisedCredentials = signal<CompromisedCredential[]>([]);
  alerts = signal<SecurityAlert[]>([]);
  loginHistory = signal<LoginAttempt[]>([]);
  auditLogs = signal<AuditLog[]>([]);
  loadingAudit = signal(false);
  scanning = signal(false);

  unreadAlerts = signal(0);

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    this.securityService.getSecurityScore().subscribe({ next: s => this.securityScore.set(s), error: () => {} });
    this.securityService.getBreachStatus().subscribe({ next: b => this.breachStatus.set(b), error: () => {} });
    this.securityService.getAlerts().subscribe({
      next: (a) => {
        this.alerts.set(a);
        this.unreadAlerts.set(a.filter(x => !x.isRead).length);
      },
      error: () => {}
    });
    this.loadAudit();
    this.loadHistory();
    this.loadLogs();
    this.loadBreachDetails();
  }

  loadAudit(): void {
    this.loadingAudit.set(true);
    this.securityService.getSecurityAudit().subscribe({
      next: (r) => { this.auditReport.set(r); this.loadingAudit.set(false); },
      error: () => this.loadingAudit.set(false)
    });
  }

  loadHistory(): void {
    this.securityService.getLoginHistory().subscribe({ next: h => this.loginHistory.set(h), error: () => {} });
  }

  loadLogs(): void {
    this.securityService.getAuditLogs().subscribe({ next: l => this.auditLogs.set(l), error: () => {} });
  }

  loadBreachDetails(): void {
    this.securityService.getCompromisedCredentials().subscribe({ next: c => this.compromisedCredentials.set(c), error: () => {} });
  }

  refreshAll(): void {
    this.loadOverview();
    this.uiState.showInfo('Security data refreshed.', 'Refreshed');
  }

  runBreachScan(): void {
    this.scanning.set(true);
    this.securityService.scanForBreaches().subscribe({
      next: (result) => {
        this.scanning.set(false);
        this.uiState.showSuccess(`Scan complete. ${result.compromisedFound} compromised credentials found.`, 'Scan Complete');
        this.securityService.getBreachStatus().subscribe({ next: b => this.breachStatus.set(b), error: () => {} });
        this.loadBreachDetails();
      },
      error: () => {
        this.scanning.set(false);
        this.uiState.showError('Breach scan failed. Please try again.');
      }
    });
  }

  dismissAlert(alert: SecurityAlert): void {
    this.securityService.dismissAlert(alert.id).subscribe({
      next: () => {
        this.alerts.update(list => list.map(a => a.id === alert.id ? { ...a, isRead: true } : a));
        this.unreadAlerts.update(n => Math.max(0, n - 1));
      },
      error: () => {}
    });
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'score-excellent';
    if (score >= 60) return 'score-good';
    if (score >= 40) return 'score-fair';
    if (score >= 20) return 'score-poor';
    return 'score-critical';
  }

  getSeverityIcon(severity: string): string {
    const map: Record<string, string> = { 'LOW': 'ℹ️', 'MEDIUM': '⚠️', 'HIGH': '🔴', 'CRITICAL': '🚨' };
    return map[severity] ?? '⚠️';
  }

  getSeverityBadge(severity: string): string {
    const map: Record<string, string> = { 'LOW': 'badge-info', 'MEDIUM': 'badge-warning', 'HIGH': 'badge-danger', 'CRITICAL': 'badge-danger' };
    return map[severity] ?? 'badge-secondary';
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString();
  }
}
