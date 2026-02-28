import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-form-container">
      <div class="auth-form-header">
        <div style="font-size:48px;margin-bottom:12px">📧</div>
        <h2 class="auth-form-title">Verify Your Email</h2>
        <p class="auth-form-subtitle">
          We sent a 6-digit code to your email.<br>
          <strong>{{ username }}</strong>
        </p>
      </div>

      <form class="auth-form" (ngSubmit)="onSubmit()">
        <div class="pill-input-wrapper">
          <label class="pill-label">Verification Code</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">🔢</span>
            <input
              type="text"
              class="pill-input has-icon-left"
              placeholder="Enter 6-digit code"
              [(ngModel)]="code"
              name="code"
              maxlength="6"
              required
              autocomplete="one-time-code"
              style="letter-spacing: 4px; font-size: 20px; text-align: center;"
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
          <span class="btn-text">Verify Email</span>
          <span class="btn-spinner" *ngIf="loading()"></span>
        </button>

        <div class="auth-links">
          <span class="text-secondary-color" style="font-size:13px">Didn't receive the code?</span>
          <button type="button" class="auth-link-btn" (click)="resendCode()" [disabled]="resendCooldown > 0">
            {{ resendCooldown > 0 ? 'Resend in ' + resendCooldown + 's' : 'Resend' }}
          </button>
        </div>

        <div class="auth-links">
          <a routerLink="/auth/login" class="auth-link">← Back to login</a>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .auth-form-container { padding: 32px; }
    .auth-form-header { text-align: center; margin-bottom: 28px; }
    .auth-form-title { font-size: var(--font-size-xl); font-weight: 700; color: var(--text-primary); margin: 0 0 6px; }
    .auth-form-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; line-height: 1.6; }
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
    .auth-links { display: flex; align-items: center; justify-content: center; gap: 8px; }
    .auth-link { font-size: var(--font-size-sm); color: var(--accent-primary); text-decoration: none; &:hover { color: #818cf8; } }
    .auth-link-btn {
      background: none; border: none; color: var(--accent-primary);
      font-size: var(--font-size-sm); cursor: pointer; padding: 0;
      &:hover { color: #818cf8; }
      &:disabled { color: var(--text-muted); cursor: not-allowed; }
    }
  `]
})
export class VerifyEmailComponent implements OnInit {
  private authService = inject(AuthService);
  private uiState = inject(UiStateService);
  private router = inject(Router);

  username = '';
  code = '';
  submitted = false;
  loading = signal(false);
  errorMessage = '';
  resendCooldown = 0;

  ngOnInit(): void {
    this.username = sessionStorage.getItem('pending_verify_user') ?? '';
    if (!this.username) {
      this.router.navigate(['/auth/login']);
    }
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    if (!this.code) return;

    this.loading.set(true);
    this.authService.verifyEmail({ username: this.username, code: this.code }).subscribe({
      next: () => {
        this.loading.set(false);
        sessionStorage.removeItem('pending_verify_user');
        this.uiState.showSuccess('Email verified! You can now sign in.', 'Verified');
        this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage = err?.error?.message ?? 'Invalid or expired code.';
      }
    });
  }

  resendCode(): void {
    this.authService.resendVerificationOtp(this.username).subscribe({
      next: () => {
        this.uiState.showSuccess('Code resent to your email.', 'Code Sent');
        this.startCooldown();
      },
      error: () => {
        this.uiState.showError('Failed to resend code. Please try again.');
      }
    });
  }

  private startCooldown(): void {
    this.resendCooldown = 60;
    const interval = setInterval(() => {
      this.resendCooldown--;
      if (this.resendCooldown <= 0) clearInterval(interval);
    }, 1000);
  }
}
