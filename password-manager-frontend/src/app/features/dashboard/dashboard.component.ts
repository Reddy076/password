import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthStateService } from '../../core/state/auth.state';
import { VaultService } from '../../core/services/vault.service';
import { SecurityService } from '../../core/services/security.service';
import { VaultEntry } from '../../core/models/vault.models';
import { DashboardSummary, SecurityAlert } from '../../core/models/security.models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="content-padding">
      <!-- Header -->
      <div class="page-header">
        <div>
          <h1 class="page-title">Dashboard</h1>
          <p class="page-subtitle">Welcome back, {{ auth.username() }}!</p>
        </div>
        <a routerLink="/vault" class="pill-btn pill-btn-primary pill-btn-sm">
          + Add Password
        </a>
      </div>

      <!-- Stats Grid -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-icon" style="background: rgba(99,102,241,0.15); color: var(--accent-primary)">🔑</div>
          <div class="stat-content">
            <div class="stat-value">{{ summary()?.totalPasswords ?? '—' }}</div>
            <div class="stat-label">Total Passwords</div>
          </div>
        </div>
        <div class="stat-card" [class.stat-danger]="(summary()?.weakPasswords ?? 0) > 0">
          <div class="stat-icon" style="background: rgba(239,68,68,0.15); color: var(--accent-danger)">⚠️</div>
          <div class="stat-content">
            <div class="stat-value">{{ summary()?.weakPasswords ?? '—' }}</div>
            <div class="stat-label">Weak Passwords</div>
          </div>
        </div>
        <div class="stat-card" [class.stat-warning]="(summary()?.reusedPasswords ?? 0) > 0">
          <div class="stat-icon" style="background: rgba(245,158,11,0.15); color: var(--accent-warning)">🔄</div>
          <div class="stat-content">
            <div class="stat-value">{{ summary()?.reusedPasswords ?? '—' }}</div>
            <div class="stat-label">Reused Passwords</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon" style="background: rgba(16,185,129,0.15); color: var(--accent-success)">🛡️</div>
          <div class="stat-content">
            <div class="stat-value">{{ summary()?.securityScore ?? '—' }}<span style="font-size:14px;color:var(--text-muted)">/100</span></div>
            <div class="stat-label">Security Score</div>
          </div>
        </div>
      </div>

      <!-- Security Score Bar -->
      <div class="pill-card" style="margin-top:20px" *ngIf="summary()">
        <div class="flex-between" style="margin-bottom:12px">
          <div>
            <div style="font-size:15px;font-weight:600;color:var(--text-primary)">Security Score</div>
            <div style="font-size:12px;color:var(--text-muted)">Based on password strength, reuse, and age</div>
          </div>
          <div class="score-badge" [class]="getScoreClass(summary()!.securityScore)">
            {{ summary()!.securityScore }}/100
          </div>
        </div>
        <div class="score-bar-track">
          <div class="score-bar-fill" [style.width.%]="summary()!.securityScore"
            [class]="getScoreClass(summary()!.securityScore)"></div>
        </div>
        <div style="margin-top:12px;font-size:12px;color:var(--text-secondary)">
          <span *ngIf="(summary()!.weakPasswords ?? 0) > 0">
            ⚠️ {{ summary()!.weakPasswords }} weak password{{ summary()!.weakPasswords !== 1 ? 's' : '' }} need attention.
          </span>
          <span *ngIf="(summary()!.weakPasswords ?? 0) === 0">
            ✅ All passwords are strong!
          </span>
        </div>
      </div>

      <!-- Two columns -->
      <div class="dashboard-grid">
        <!-- Recent Entries -->
        <div class="pill-card">
          <div class="flex-between" style="margin-bottom:16px">
            <h3 class="section-title">Recent Passwords</h3>
            <a routerLink="/vault" class="pill-btn pill-btn-ghost pill-btn-sm">View All</a>
          </div>

          <div *ngIf="loadingEntries()" class="loading-list">
            <div class="skeleton skeleton-text" *ngFor="let i of [1,2,3,4,5]" style="height:48px;margin-bottom:8px"></div>
          </div>

          <div *ngIf="!loadingEntries() && recentEntries().length === 0" class="empty-state-sm">
            <div style="font-size:32px">🔑</div>
            <div>No passwords yet</div>
            <a routerLink="/vault" class="pill-btn pill-btn-primary pill-btn-sm" style="margin-top:8px">Add First Password</a>
          </div>

          <div class="entry-list" *ngIf="!loadingEntries() && recentEntries().length > 0">
            <div class="entry-item" *ngFor="let entry of recentEntries()" [routerLink]="'/vault'">
              <div class="entry-favicon">
                <img *ngIf="entry.websiteUrl" [src]="getFavicon(entry.websiteUrl)" alt="" (error)="onFaviconError($event)" />
                <span *ngIf="!entry.websiteUrl">{{ entry.title.charAt(0).toUpperCase() }}</span>
              </div>
              <div class="entry-info">
                <div class="entry-title">{{ entry.title }}</div>
                <div class="entry-username">{{ entry.username ?? 'No username' }}</div>
              </div>
              <div class="entry-meta">
                <span class="pill-badge" [class]="getStrengthBadge(entry.strengthLabel)">
                  {{ entry.strengthLabel }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <!-- Security Alerts -->
        <div class="pill-card">
          <div class="flex-between" style="margin-bottom:16px">
            <h3 class="section-title">Security Alerts</h3>
            <a routerLink="/security" class="pill-btn pill-btn-ghost pill-btn-sm">View All</a>
          </div>

          <div *ngIf="alerts().length === 0" class="empty-state-sm">
            <div style="font-size:32px">✅</div>
            <div>No security alerts</div>
            <div style="font-size:12px;color:var(--text-muted);margin-top:4px">Your account looks secure</div>
          </div>

          <div class="alert-list" *ngIf="alerts().length > 0">
            <div class="alert-item" *ngFor="let alert of alerts().slice(0, 5)"
              [class]="'alert-' + alert.severity.toLowerCase()">
              <div class="alert-icon">{{ getSeverityIcon(alert.severity) }}</div>
              <div class="alert-content">
                <div class="alert-title">{{ alert.title }}</div>
                <div class="alert-message">{{ alert.message }}</div>
                <div class="alert-time">{{ formatDate(alert.createdAt) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="quick-actions">
        <h3 class="section-title" style="margin-bottom:12px">Quick Actions</h3>
        <div class="actions-grid">
          <a routerLink="/generator" class="action-card">
            <span class="action-icon">⚡</span>
            <span class="action-label">Generate Password</span>
          </a>
          <a routerLink="/security" class="action-card">
            <span class="action-icon">🛡️</span>
            <span class="action-label">Security Audit</span>
          </a>
          <a routerLink="/backup" class="action-card">
            <span class="action-icon">💾</span>
            <span class="action-label">Export Vault</span>
          </a>
          <a routerLink="/sharing" class="action-card">
            <span class="action-icon">🔗</span>
            <span class="action-label">Share Password</span>
          </a>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      padding: 24px 28px 0;
      gap: 16px;
      flex-wrap: wrap;
    }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      padding: 20px 28px 0;
      @media (max-width: 991px) { grid-template-columns: repeat(2, 1fr); }
      @media (max-width: 480px) { grid-template-columns: 1fr; }
    }

    .stat-card {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-lg);
      padding: 16px 20px;
      display: flex;
      align-items: center;
      gap: 14px;
      transition: all var(--transition-base);
      &:hover { border-color: var(--border-default); transform: translateY(-1px); }
      &.stat-danger { border-color: rgba(239,68,68,0.3); }
      &.stat-warning { border-color: rgba(245,158,11,0.3); }
    }
    .stat-icon {
      width: 44px; height: 44px;
      border-radius: var(--pill-radius-md);
      display: flex; align-items: center; justify-content: center;
      font-size: 20px; flex-shrink: 0;
    }
    .stat-value { font-size: var(--font-size-xl); font-weight: 700; color: var(--text-primary); }
    .stat-label { font-size: var(--font-size-xs); color: var(--text-muted); margin-top: 2px; }

    .score-bar-track {
      height: 8px;
      background: var(--bg-elevated);
      border-radius: var(--pill-radius-full);
      overflow: hidden;
    }
    .score-bar-fill {
      height: 100%;
      border-radius: var(--pill-radius-full);
      transition: width 1s ease;
      &.score-critical { background: var(--accent-danger); }
      &.score-poor { background: var(--accent-orange); }
      &.score-fair { background: var(--accent-warning); }
      &.score-good { background: #84cc16; }
      &.score-excellent { background: var(--accent-success); }
    }
    .score-badge {
      padding: 4px 12px;
      border-radius: var(--pill-radius-full);
      font-size: 13px;
      font-weight: 700;
      &.score-critical { background: rgba(239,68,68,0.15); color: var(--accent-danger); }
      &.score-poor { background: rgba(249,115,22,0.15); color: var(--accent-orange); }
      &.score-fair { background: rgba(245,158,11,0.15); color: var(--accent-warning); }
      &.score-good { background: rgba(132,204,22,0.15); color: #84cc16; }
      &.score-excellent { background: rgba(16,185,129,0.15); color: var(--accent-success); }
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      padding: 16px 28px 0;
      @media (max-width: 767px) { grid-template-columns: 1fr; }
    }

    .section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }

    .entry-list { display: flex; flex-direction: column; gap: 4px; }
    .entry-item {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 12px;
      border-radius: var(--pill-radius-xs);
      cursor: pointer;
      transition: background var(--transition-fast);
      &:hover { background: var(--bg-hover); }
    }
    .entry-favicon {
      width: 36px; height: 36px;
      border-radius: 10px;
      background: var(--bg-elevated);
      display: flex; align-items: center; justify-content: center;
      font-size: 16px; font-weight: 700; color: var(--accent-primary);
      flex-shrink: 0; overflow: hidden;
      img { width: 100%; height: 100%; object-fit: cover; }
    }
    .entry-info { flex: 1; overflow: hidden; }
    .entry-title { font-size: 13px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .entry-username { font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

    .alert-list { display: flex; flex-direction: column; gap: 8px; }
    .alert-item {
      display: flex; align-items: flex-start; gap: 10px;
      padding: 10px 12px;
      border-radius: var(--pill-radius-xs);
      border-left: 3px solid;
      &.alert-low { border-color: var(--accent-info); background: rgba(59,130,246,0.05); }
      &.alert-medium { border-color: var(--accent-warning); background: rgba(245,158,11,0.05); }
      &.alert-high { border-color: var(--accent-orange); background: rgba(249,115,22,0.05); }
      &.alert-critical { border-color: var(--accent-danger); background: rgba(239,68,68,0.05); }
    }
    .alert-icon { font-size: 16px; flex-shrink: 0; margin-top: 1px; }
    .alert-title { font-size: 12px; font-weight: 600; color: var(--text-primary); }
    .alert-message { font-size: 11px; color: var(--text-secondary); margin-top: 2px; }
    .alert-time { font-size: 10px; color: var(--text-muted); margin-top: 3px; }

    .quick-actions { padding: 16px 28px 24px; }
    .actions-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      @media (max-width: 767px) { grid-template-columns: repeat(2, 1fr); }
    }
    .action-card {
      display: flex; flex-direction: column; align-items: center; gap: 8px;
      padding: 16px 12px;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-lg);
      cursor: pointer; text-decoration: none;
      transition: all var(--transition-base);
      &:hover { border-color: var(--accent-primary); background: var(--bg-active); transform: translateY(-2px); }
    }
    .action-icon { font-size: 28px; }
    .action-label { font-size: 12px; font-weight: 500; color: var(--text-secondary); text-align: center; }

    .empty-state-sm {
      display: flex; flex-direction: column; align-items: center;
      padding: 24px; color: var(--text-muted); font-size: 13px; gap: 4px;
    }
    .loading-list { padding: 8px 0; }
  `]
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthStateService);
  private vaultService = inject(VaultService);
  private securityService = inject(SecurityService);

  summary = signal<DashboardSummary | null>(null);
  recentEntries = signal<VaultEntry[]>([]);
  alerts = signal<SecurityAlert[]>([]);
  loadingEntries = signal(true);

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    // Load recent vault entries
    this.vaultService.getEntries({ sortBy: 'updatedAt', sortDir: 'desc' }).subscribe({
      next: (entries) => {
        this.recentEntries.set(entries.slice(0, 8));
        this.loadingEntries.set(false);
      },
      error: () => this.loadingEntries.set(false)
    });

    // Load security summary
    this.securityService.getDashboardSummary().subscribe({
      next: (s) => this.summary.set(s),
      error: () => {}
    });

    // Load alerts
    this.securityService.getAlerts().subscribe({
      next: (a) => this.alerts.set(a.filter(x => !x.isRead)),
      error: () => {}
    });
  }

  getFavicon(url: string): string {
    try {
      const domain = new URL(url).hostname;
      return `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
    } catch {
      return '';
    }
  }

  onFaviconError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  getStrengthBadge(label: string): string {
    const map: Record<string, string> = {
      'VERY_STRONG': 'badge-success',
      'STRONG': 'badge-success',
      'FAIR': 'badge-warning',
      'WEAK': 'badge-danger',
      'VERY_WEAK': 'badge-danger',
    };
    return map[label] ?? 'badge-secondary';
  }

  getScoreClass(score: number): string {
    if (score >= 80) return 'score-excellent';
    if (score >= 60) return 'score-good';
    if (score >= 40) return 'score-fair';
    if (score >= 20) return 'score-poor';
    return 'score-critical';
  }

  getSeverityIcon(severity: string): string {
    const map: Record<string, string> = {
      'LOW': 'ℹ️', 'MEDIUM': '⚠️', 'HIGH': '🔴', 'CRITICAL': '🚨'
    };
    return map[severity] ?? '⚠️';
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const hours = Math.floor(diff / 3600000);
    if (hours < 1) return 'Just now';
    if (hours < 24) return `${hours}h ago`;
    const days = Math.floor(hours / 24);
    if (days < 7) return `${days}d ago`;
    return date.toLocaleDateString();
  }
}
