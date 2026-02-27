import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="reset-page">
      <div class="auth-header">
        <div class="reset-icon">🔑</div>
        <h2 class="auth-title">Reset Password</h2>
        <p class="auth-subtitle">
          Use the forgot password flow to reset your master password using security questions.
        </p>
      </div>
      <a routerLink="/auth/forgot-password" class="pill-btn pill-btn-primary w-100">
        Reset via Security Questions
      </a>
      <div class="auth-links mt-3">
        <a routerLink="/auth/login" class="auth-link">← Back to login</a>
      </div>
    </div>
  `,
  styles: [`
    .reset-page { width: 100%; }
    .auth-header { text-align: center; margin-bottom: 24px; }
    .reset-icon { font-size: 36px; margin-bottom: 12px; }
    .auth-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
    .auth-subtitle { font-size: 14px; color: var(--text-secondary); margin: 0; line-height: 1.6; }
    .auth-links { display: flex; justify-content: center; }
    .auth-link { font-size: 13px; color: var(--text-secondary); text-decoration: none; }
    .w-100 { width: 100%; }
    .mt-3 { margin-top: 12px; }
  `]
})
export class ResetPasswordComponent {}
