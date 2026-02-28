import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { EmergencyVaultResponse } from '../../../core/models/security.models';

@Component({
  selector: 'app-emergency-vault',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="emergency-shell">
      <div class="emergency-bg"></div>

      <div class="emergency-container">
        <div class="emergency-brand">
          <div class="brand-icon">🚨</div>
          <span class="brand-name">Emergency Vault Access</span>
        </div>

        <div class="emergency-card">
          <div class="loading-state" *ngIf="loading()">
            <div class="spinner"></div>
            <p>Loading emergency vault...</p>
          </div>

          <div class="error-state" *ngIf="!loading() && error()">
            <div class="error-icon">❌</div>
            <h3>{{ error() }}</h3>
            <p>This emergency access token may have expired.</p>
          </div>

          <div class="vault-content" *ngIf="!loading() && !error() && vaultData()">
            <div class="vault-header">
              <div>
                <h2 class="vault-title">{{ vaultData()!.ownerUsername }}'s Vault</h2>
                <p class="vault-subtitle">
                  Emergency access · Expires {{ formatDate(vaultData()!.expiresAt) }}
                  ({{ vaultData()!.hoursUntilExpiry }}h remaining)
                </p>
              </div>
              <span class="pill-badge badge-danger">Emergency Access</span>
            </div>

            <div class="vault-warning">
              ⚠️ You have emergency access to this vault. All actions are logged.
            </div>

            <div class="entries-list">
              <div class="entry-item" *ngFor="let entry of vaultData()!.entries">
                <div class="entry-icon">🔑</div>
                <div class="entry-info">
                  <div class="entry-title">{{ entry.title }}</div>
                  <div class="entry-meta">
                    <span *ngIf="entry.categoryName">📂 {{ entry.categoryName }}</span>
                    <span *ngIf="entry.websiteUrl">🌐 {{ entry.websiteUrl }}</span>
                    <span>Updated {{ formatDate(entry.lastUpdatedAt) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .emergency-shell {
      min-height: 100vh;
      background: #0a0f1e;
      display: flex; align-items: center; justify-content: center;
      position: relative; overflow: hidden; padding: 24px;
    }
    .emergency-bg {
      position: fixed; inset: 0;
      background: radial-gradient(ellipse at 50% 50%, rgba(239,68,68,0.08) 0%, transparent 70%);
      pointer-events: none;
    }
    .emergency-container { position: relative; z-index: 1; width: 100%; max-width: 600px; display: flex; flex-direction: column; align-items: center; gap: 20px; }
    .emergency-brand { display: flex; align-items: center; gap: 10px; }
    .brand-icon { width: 40px; height: 40px; border-radius: 12px; background: linear-gradient(135deg, #ef4444, #dc2626); display: flex; align-items: center; justify-content: center; font-size: 20px; }
    .brand-name { font-size: 18px; font-weight: 700; color: #f1f5f9; }

    .emergency-card {
      width: 100%;
      background: rgba(17,24,39,0.9); backdrop-filter: blur(20px);
      border: 1px solid rgba(239,68,68,0.2); border-radius: 40px;
      box-shadow: 0 8px 32px rgba(239,68,68,0.15);
    }

    .loading-state { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 40px; color: #94a3b8; font-size: 14px; }
    .spinner { width: 32px; height: 32px; border: 3px solid #1e293b; border-top-color: #ef4444; border-radius: 50%; animation: spinnerRotate 0.8s linear infinite; }
    .error-state { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 40px; text-align: center; }
    .error-icon { font-size: 48px; }
    .error-state h3 { font-size: 18px; font-weight: 700; color: #f1f5f9; margin: 0; }
    .error-state p { font-size: 13px; color: #64748b; margin: 0; }

    .vault-content { padding: 28px; display: flex; flex-direction: column; gap: 16px; }
    .vault-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .vault-title { font-size: 20px; font-weight: 700; color: #f1f5f9; margin: 0 0 4px; }
    .vault-subtitle { font-size: 12px; color: #64748b; margin: 0; }
    .vault-warning { padding: 10px 14px; background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3); border-radius: 16px; color: #f87171; font-size: 12px; }

    .entries-list { display: flex; flex-direction: column; gap: 8px; }
    .entry-item { display: flex; align-items: center; gap: 10px; padding: 12px 14px; background: rgba(30,41,59,0.5); border-radius: 16px; border: 1px solid rgba(255,255,255,0.05); }
    .entry-icon { font-size: 20px; flex-shrink: 0; }
    .entry-info { flex: 1; }
    .entry-title { font-size: 13px; font-weight: 600; color: #f1f5f9; }
    .entry-meta { display: flex; gap: 10px; flex-wrap: wrap; font-size: 11px; color: #64748b; margin-top: 2px; }
  `]
})
export class EmergencyVaultComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  vaultData = signal<EmergencyVaultResponse | null>(null);
  loading = signal(true);
  error = signal('');

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.error.set('Invalid access token');
      this.loading.set(false);
      return;
    }

    this.http.get<EmergencyVaultResponse>(`/api/emergency/vault/${token}`).subscribe({
      next: (data) => {
        this.vaultData.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'Access token is invalid or expired.');
      }
    });
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString();
  }
}
