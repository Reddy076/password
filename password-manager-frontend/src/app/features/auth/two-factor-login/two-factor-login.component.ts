import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/state/auth.state';
import { UiStateService } from '../../../core/state/ui.state';

@Component({
  selector: 'app-two-factor-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-form-container">
      <div class="auth-form-header">
        <div style="font-size:48px;margin-bottom:12px">🔐</div>
        <h2 class="auth-form-title">Two-Factor Auth</h2>
        <p class="auth-form-subtitle">Enter the 6-digit code from your authenticator app</p>
      </div>

      <form class="auth-form" (ngSubmit)="onSubmit()">
        <div class="pill-input-wrapper">
          <label class="pill-label">Authentication Code</label>
          <div class="pill-input-inner">
            <input
              type="text"
              class="pill-input"
              placeholder="000000"
              [(ngModel)]="code"
              name="code"
              maxlength="6"
              required
              autocomplete="one-time-code"
              style="letter-spacing: 8px; font-size: 24px; text-align: center; font-family: var(--font-mono);"
              [class.error]="submitted && !code"
            />
          </div>
          <span class="pill-input-error" *ngIf="submitted && !code">Code is required</span>
        </div>

        <div class="auth-error" *ngIf="errorMessage">
          <span>⚠️</span> {{ errorMessage }}
        </div>

        <button type="submit" class="pill-btn pill-btn-primary" style="width:100%"
          [class.loading]="loading()" [disabled]="loading()">
          <span class="btn-text">Verify</span>
          <span class="btn-spinner" *ngIf="loading()"></span>
        </button>

        <div class="auth-links">
          <a routerLink="/auth/login" class="auth-link">← Use a different account</a>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .auth-form-container { padding: 32px; }
    .auth-form-header { text-align: center; margin-bottom: 28px; }
    .auth-form-title { font-size: var(--font-size-xl); font-weight: 700; color: var(--text-primary); margin: 0 0 6px; }
    .auth-form-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }
    .auth-form { display: flex; flex-direction: column; gap: 16px; }
    .auth-error {
      padding: 10px 16px;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-danger);
      font-size: var(--font-size-sm);
      display: flex; align-items: center; gap: 8px;
    }
    .auth-links { display: flex; align-items: center; justify-content: center; }
    .auth-link { font-size: var(--font-size-sm); color: var(--accent-primary); text-decoration: none; &:hover { color: #818cf8; } }
  `]
})
export class TwoFactorLoginComponent implements OnInit {
  private authService = inject(AuthService);
  private authState = inject(AuthStateService);
  private uiState = inject(UiStateService);
  private router = inject(Router);

  username = '';
  code = '';
  submitted = false;
  loading = signal(false);
  errorMessage = '';

  ngOnInit(): void {
    this.username = sessionStorage.getItem('pending_2fa_user') ?? '';
    if (!this.username) {
      this.router.navigate(['/auth/login']);
    }
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (!this.code) return;

    this.loading.set(true);
    this.authService.verifyOtp(this.username, this.code).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.accessToken && res.refreshToken) {
          sessionStorage.removeItem('pending_2fa_user');
          this.authState.setTokens(res.accessToken, res.refreshToken);
          this.uiState.showSuccess('Welcome back!', 'Signed in');
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage = 'Authentication failed. Please try again.';
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage = err?.error?.message ?? 'Invalid code. Please try again.';
      }
    });
  }
}
