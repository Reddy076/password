import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-form-container">
      <div class="auth-form-header">
        <div style="font-size:48px;margin-bottom:12px">🔑</div>
        <h2 class="auth-form-title">Forgot Password?</h2>
        <p class="auth-form-subtitle">Enter your username to get your password hint or reset via security questions</p>
      </div>

      <form class="auth-form" (ngSubmit)="onSubmit()">
        <div class="pill-input-wrapper">
          <label class="pill-label">Username</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">👤</span>
            <input type="text" class="pill-input has-icon-left" placeholder="Enter your username"
              [(ngModel)]="username" name="username" required [class.error]="submitted && !username" />
          </div>
          <span class="pill-input-error" *ngIf="submitted && !username">Username is required</span>
        </div>

        <!-- Password hint display -->
        <div class="hint-box" *ngIf="passwordHint">
          <span>💡</span>
          <div>
            <div style="font-size:12px;color:var(--text-muted);margin-bottom:2px">Your password hint:</div>
            <div style="font-size:14px;color:var(--text-primary)">{{ passwordHint }}</div>
          </div>
        </div>

        <div class="auth-error" *ngIf="errorMessage">
          <span>⚠️</span> {{ errorMessage }}
        </div>

        <button type="submit" class="pill-btn pill-btn-primary" style="width:100%"
          [class.loading]="loading()" [disabled]="loading()">
          <span class="btn-text">Get Password Hint</span>
          <span class="btn-spinner" *ngIf="loading()"></span>
        </button>

        <button type="button" class="pill-btn pill-btn-secondary" style="width:100%"
          (click)="goToReset()" [disabled]="!username">
          Reset via Security Questions
        </button>

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
    .auth-form { display: flex; flex-direction: column; gap: 14px; }
    .auth-error {
      padding: 10px 16px;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-danger);
      font-size: var(--font-size-sm);
      display: flex; align-items: center; gap: 8px;
    }
    .hint-box {
      padding: 12px 16px;
      background: rgba(99,102,241,0.1);
      border: 1px solid rgba(99,102,241,0.3);
      border-radius: var(--pill-radius-md);
      display: flex; align-items: flex-start; gap: 10px;
      font-size: 20px;
    }
    .auth-links { display: flex; align-items: center; justify-content: center; }
    .auth-link { font-size: var(--font-size-sm); color: var(--accent-primary); text-decoration: none; &:hover { color: #818cf8; } }
  `]
})
export class ForgotPasswordComponent {
  private authService = inject(AuthService);
  private uiState = inject(UiStateService);
  private router = inject(Router);

  username = '';
  submitted = false;
  loading = signal(false);
  errorMessage = '';
  passwordHint = '';

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.passwordHint = '';
    if (!this.username) return;

    this.loading.set(true);
    this.authService.getPasswordHint(this.username).subscribe({
      next: (res) => {
        this.loading.set(false);
        this.passwordHint = res.hint || 'No hint set for this account.';
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage = err?.error?.message ?? 'Username not found.';
      }
    });
  }

  goToReset(): void {
    if (!this.username) return;
    sessionStorage.setItem('reset_username', this.username);
    this.router.navigate(['/auth/reset-password']);
  }
}
