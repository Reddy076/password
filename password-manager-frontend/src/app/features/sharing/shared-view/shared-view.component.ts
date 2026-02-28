import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { SharedPasswordResponse } from '../../../core/models/vault.models';

@Component({
  selector: 'app-shared-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="shared-shell">
      <div class="shared-bg">
        <div class="orb orb-1"></div>
        <div class="orb orb-2"></div>
      </div>

      <div class="shared-container">
        <div class="shared-brand">
          <div class="brand-icon">🔐</div>
          <span class="brand-name">RevaultX</span>
        </div>

        <div class="shared-card">
          <!-- Loading -->
          <div class="shared-loading" *ngIf="loading()">
            <div class="spinner"></div>
            <p>Loading shared password...</p>
          </div>

          <!-- Error -->
          <div class="shared-error" *ngIf="!loading() && error()">
            <div class="error-icon">❌</div>
            <h3>{{ error() }}</h3>
            <p>This link may have expired or been revoked.</p>
          </div>

          <!-- Content -->
          <div class="shared-content" *ngIf="!loading() && !error() && sharedData()">
            <div class="shared-header">
              <div class="shared-icon">🔑</div>
              <div>
                <h2 class="shared-title">{{ sharedData()!.title }}</h2>
                <p class="shared-subtitle">Shared password — expires {{ formatDate(sharedData()!.expiresAt) }}</p>
              </div>
            </div>

            <div class="shared-fields">
              <div class="shared-field" *ngIf="sharedData()!.username">
                <div class="field-label">Username</div>
                <div class="field-value">
                  <span>{{ sharedData()!.username }}</span>
                  <button class="copy-btn" (click)="copyText(sharedData()!.username!)">📋 Copy</button>
                </div>
              </div>

              <div class="shared-field">
                <div class="field-label">Password</div>
                <div class="field-value">
                  <span class="password-value" [class.revealed]="showPassword">
                    {{ showPassword ? decryptedPassword : '••••••••••••' }}
                  </span>
                  <button class="copy-btn" (click)="togglePassword()">
                    {{ showPassword ? '🙈 Hide' : '👁️ Reveal' }}
                  </button>
                  <button class="copy-btn" (click)="copyPassword()" *ngIf="showPassword">📋 Copy</button>
                </div>
              </div>

              <div class="shared-field" *ngIf="sharedData()!.websiteUrl">
                <div class="field-label">Website</div>
                <div class="field-value">
                  <a [href]="sharedData()!.websiteUrl!" target="_blank" class="website-link">
                    {{ sharedData()!.websiteUrl }}
                  </a>
                </div>
              </div>
            </div>

            <div class="shared-meta">
              <span>👁️ {{ sharedData()!.viewCount }}{{ sharedData()!.maxViews ? '/' + sharedData()!.maxViews : '' }} views</span>
            </div>

            <div class="shared-warning">
              ⚠️ This is a one-time shared link. Do not share this URL with others.
            </div>
          </div>
        </div>

        <div class="shared-footer">
          <span>Powered by RevaultX — Secure Password Manager</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .shared-shell {
      min-height: 100vh;
      background: var(--bg-primary);
      display: flex; align-items: center; justify-content: center;
      position: relative; overflow: hidden; padding: 24px;
    }
    .shared-bg { position: fixed; inset: 0; pointer-events: none; }
    .orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.12; }
    .orb-1 { width: 400px; height: 400px; background: var(--accent-primary); top: -100px; left: -100px; }
    .orb-2 { width: 300px; height: 300px; background: var(--accent-secondary); bottom: -80px; right: -80px; }

    .shared-container { position: relative; z-index: 1; width: 100%; max-width: 480px; display: flex; flex-direction: column; align-items: center; gap: 20px; }
    .shared-brand { display: flex; align-items: center; gap: 10px; }
    .brand-icon { width: 40px; height: 40px; border-radius: 12px; background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); display: flex; align-items: center; justify-content: center; font-size: 20px; }
    .brand-name { font-size: 22px; font-weight: 800; color: var(--text-primary); }

    .shared-card {
      width: 100%;
      background: var(--island-bg); backdrop-filter: var(--island-blur);
      border: var(--island-border); border-radius: var(--pill-radius-xl);
      box-shadow: var(--island-shadow); overflow: hidden;
    }

    .shared-loading { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 40px; color: var(--text-secondary); font-size: 14px; }
    .spinner { width: 32px; height: 32px; border: 3px solid var(--border-default); border-top-color: var(--accent-primary); border-radius: 50%; animation: spinnerRotate 0.8s linear infinite; }

    .shared-error { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 40px; text-align: center; }
    .error-icon { font-size: 48px; }
    .shared-error h3 { font-size: 18px; font-weight: 700; color: var(--text-primary); margin: 0; }
    .shared-error p { font-size: 13px; color: var(--text-muted); margin: 0; }

    .shared-content { padding: 28px; display: flex; flex-direction: column; gap: 20px; }
    .shared-header { display: flex; align-items: center; gap: 14px; }
    .shared-icon { width: 48px; height: 48px; border-radius: 14px; background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); display: flex; align-items: center; justify-content: center; font-size: 22px; flex-shrink: 0; }
    .shared-title { font-size: 20px; font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .shared-subtitle { font-size: 12px; color: var(--text-muted); margin: 0; }

    .shared-fields { display: flex; flex-direction: column; gap: 12px; }
    .shared-field { background: var(--bg-elevated); border-radius: var(--pill-radius-lg); padding: 14px 16px; }
    .field-label { font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); margin-bottom: 6px; }
    .field-value { display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--text-primary); }
    .password-value { font-family: var(--font-mono); flex: 1; &.revealed { color: var(--accent-primary); } }
    .copy-btn { padding: 4px 10px; border-radius: var(--pill-radius-full); background: var(--bg-surface); border: 1px solid var(--border-default); cursor: pointer; font-size: 11px; color: var(--text-secondary); transition: all var(--transition-fast); &:hover { border-color: var(--accent-primary); color: var(--accent-primary); } }
    .website-link { color: var(--accent-primary); text-decoration: none; font-size: 13px; &:hover { text-decoration: underline; } }

    .shared-meta { font-size: 12px; color: var(--text-muted); }
    .shared-warning { padding: 10px 14px; background: rgba(245,158,11,0.1); border: 1px solid rgba(245,158,11,0.3); border-radius: var(--pill-radius-md); color: var(--accent-warning); font-size: 12px; }
    .shared-footer { font-size: 11px; color: var(--text-muted); }
  `]
})
export class SharedViewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

  sharedData = signal<SharedPasswordResponse | null>(null);
  loading = signal(true);
  error = signal('');
  showPassword = false;
  decryptedPassword = '';

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');
    if (!token) {
      this.error.set('Invalid share link');
      this.loading.set(false);
      return;
    }

    this.http.get<SharedPasswordResponse>(`/api/shares/${token}`).subscribe({
      next: (data) => {
        this.sharedData.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err?.error?.message ?? 'This link has expired or is invalid.');
      }
    });
  }

  togglePassword(): void {
    if (!this.showPassword) {
      // Decrypt password (simplified - in real app would use encryptionKey from URL fragment)
      this.decryptedPassword = this.sharedData()?.encryptedPassword ?? '';
      this.showPassword = true;
    } else {
      this.showPassword = false;
    }
  }

  copyPassword(): void {
    navigator.clipboard.writeText(this.decryptedPassword);
  }

  copyText(text: string): void {
    navigator.clipboard.writeText(text);
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString();
  }
}
