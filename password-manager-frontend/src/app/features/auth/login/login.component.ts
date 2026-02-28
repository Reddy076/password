import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { AuthStateService } from '../../../core/state/auth.state';
import { UiStateService } from '../../../core/state/ui.state';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-form-container">
      <div class="auth-form-header">
        <h2 class="auth-form-title">Welcome back</h2>
        <p class="auth-form-subtitle">Sign in to your vault</p>
      </div>

      <form class="auth-form" (ngSubmit)="onSubmit()" #loginForm="ngForm">
        <!-- Username -->
        <div class="pill-input-wrapper">
          <label class="pill-label">Username</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">👤</span>
            <input
              type="text"
              class="pill-input has-icon-left"
              placeholder="Enter your username"
              [(ngModel)]="username"
              name="username"
              required
              autocomplete="username"
              [class.error]="submitted && !username"
            />
          </div>
          <span class="pill-input-error" *ngIf="submitted && !username">Username is required</span>
        </div>

        <!-- Master Password -->
        <div class="pill-input-wrapper">
          <label class="pill-label">Master Password</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">🔒</span>
            <input
              [type]="showPassword ? 'text' : 'password'"
              class="pill-input has-icon-left has-both"
              placeholder="Enter your master password"
              [(ngModel)]="masterPassword"
              name="masterPassword"
              required
              autocomplete="current-password"
              [class.error]="submitted && !masterPassword"
            />
            <button
              type="button"
              class="pill-input-icon icon-right"
              (click)="showPassword = !showPassword"
              style="background:none;border:none;cursor:pointer;"
            >
              {{ showPassword ? '🙈' : '👁️' }}
            </button>
          </div>
          <span class="pill-input-error" *ngIf="submitted && !masterPassword">Password is required</span>
        </div>

        <!-- Error message -->
        <div class="auth-error" *ngIf="errorMessage">
          <span>⚠️</span> {{ errorMessage }}
        </div>

        <!-- Submit -->
        <button
          type="submit"
          class="pill-btn pill-btn-primary"
          style="width:100%"
          [class.loading]="loading()"
          [disabled]="loading()"
        >
          <span class="btn-text">Sign In</span>
          <span class="btn-spinner" *ngIf="loading()"></span>
        </button>

        <!-- Links -->
        <div class="auth-links">
          <a routerLink="/auth/forgot-password" class="auth-link">Forgot password?</a>
          <span class="auth-link-divider">·</span>
          <a routerLink="/auth/register" class="auth-link">Create account</a>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .auth-form-container {
      padding: 32px;
    }

    .auth-form-header {
      text-align: center;
      margin-bottom: 28px;
    }

    .auth-form-title {
      font-size: var(--font-size-xl);
      font-weight: 700;
      color: var(--text-primary);
      margin: 0 0 6px;
    }

    .auth-form-subtitle {
      font-size: var(--font-size-sm);
      color: var(--text-secondary);
      margin: 0;
    }

    .auth-form {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .auth-error {
      padding: 10px 16px;
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-danger);
      font-size: var(--font-size-sm);
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .auth-links {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-top: 4px;
    }

    .auth-link {
      font-size: var(--font-size-sm);
      color: var(--accent-primary);
      text-decoration: none;
      transition: color var(--transition-fast);
      &:hover { color: #818cf8; }
    }

    .auth-link-divider {
      color: var(--text-muted);
      font-size: var(--font-size-sm);
    }
  `]
})
export class LoginComponent {
  private authService = inject(AuthService);
  private authState = inject(AuthStateService);
  private uiState = inject(UiStateService);
  private router = inject(Router);

  username = '';
  masterPassword = '';
  showPassword = false;
  submitted = false;
  errorMessage = '';
  loading = signal(false);

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    if (!this.username || !this.masterPassword) return;

    this.loading.set(true);
    this.authService.login({ username: this.username, masterPassword: this.masterPassword }).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res.requires2FA) {
          // Store username for 2FA step
          sessionStorage.setItem('pending_2fa_user', this.username);
          this.router.navigate(['/auth/2fa']);
        } else if (res.accessToken && res.refreshToken) {
          this.authState.setTokens(res.accessToken, res.refreshToken);
          this.uiState.showSuccess('Welcome back!', 'Signed in');
          this.router.navigate(['/dashboard']);
        } else {
          // Email verification needed
          sessionStorage.setItem('pending_verify_user', this.username);
          this.router.navigate(['/auth/verify-email']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? err?.error ?? 'Invalid credentials. Please try again.';
        this.errorMessage = typeof msg === 'string' ? msg : 'Login failed. Please try again.';
      }
    });
  }
}
