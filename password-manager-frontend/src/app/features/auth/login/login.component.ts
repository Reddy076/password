import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="login-page">
      <!-- Header -->
      <div class="auth-header">
        <div class="auth-logo">
          <div class="auth-logo-icon">🔐</div>
          <span class="auth-logo-text">RevaultX</span>
        </div>
        <h2 class="auth-title">Welcome back</h2>
        <p class="auth-subtitle">Sign in to your secure vault</p>
      </div>

      <!-- Form -->
      <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" [class.shake]="shakeForm()">

        <!-- Username -->
        <div class="pill-input-wrapper mb-3">
          <label class="pill-label">Username</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
            </span>
            <input
              type="text"
              class="pill-input has-icon-left"
              placeholder="Enter your username"
              formControlName="username"
              autocomplete="username"
              (blur)="onUsernameBlur()"
            />
          </div>
          @if (loginForm.get('username')?.invalid && loginForm.get('username')?.touched) {
            <div class="pill-input-error">Username is required</div>
          }
        </div>

        <!-- Password -->
        <div class="pill-input-wrapper mb-3">
          <div class="d-flex justify-content-between align-items-center">
            <label class="pill-label">Master Password</label>
            @if (passwordHint()) {
              <span class="hint-text">Hint: {{ passwordHint() }}</span>
            }
          </div>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </span>
            <input
              [type]="showPassword() ? 'text' : 'password'"
              class="pill-input has-both"
              placeholder="Enter your master password"
              formControlName="password"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="pill-input-icon icon-right"
              (click)="showPassword.set(!showPassword())"
              [attr.aria-label]="showPassword() ? 'Hide password' : 'Show password'"
            >
              @if (showPassword()) {
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94"/>
                  <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              } @else {
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              }
            </button>
          </div>
          @if (loginForm.get('password')?.invalid && loginForm.get('password')?.touched) {
            <div class="pill-input-error">Password is required</div>
          }
        </div>

        <!-- Error message -->
        @if (errorMessage()) {
          <div class="auth-error-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="8" x2="12" y2="12"/>
              <line x1="12" y1="16" x2="12.01" y2="16"/>
            </svg>
            {{ errorMessage() }}
          </div>
        }

        <!-- Lockout timer -->
        @if (lockoutSeconds() > 0) {
          <div class="auth-lockout-banner">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <polyline points="12 6 12 12 16 14"/>
            </svg>
            Account locked. Try again in {{ lockoutSeconds() }}s
          </div>
        }

        <!-- Submit button -->
        <button
          type="submit"
          class="pill-btn pill-btn-primary w-100 mt-2"
          [disabled]="loginForm.invalid || loading() || lockoutSeconds() > 0"
          [class.loading]="loading()"
        >
          @if (loading()) {
            <span class="btn-spinner"></span>
          } @else {
            <span class="btn-text">Sign In</span>
          }
        </button>

        <!-- Links -->
        <div class="auth-links mt-4">
          <a routerLink="/auth/forgot-password" class="auth-link">Forgot password?</a>
          <span class="auth-link-sep">·</span>
          <a routerLink="/auth/register" class="auth-link">Create account</a>
        </div>

      </form>
    </div>
  `,
  styles: [`
    .login-page { width: 100%; }

    .auth-header {
      text-align: center;
      margin-bottom: 32px;
    }

    .auth-logo {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 20px;
    }

    .auth-logo-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
    }

    .auth-logo-text {
      font-size: 20px;
      font-weight: 800;
      color: var(--text-primary);
    }

    .auth-title {
      font-size: 26px;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 6px;
    }

    .auth-subtitle {
      font-size: 14px;
      color: var(--text-secondary);
      margin: 0;
    }

    .hint-text {
      font-size: 11px;
      color: var(--text-muted);
      font-style: italic;
    }

    .auth-error-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.3);
      border-radius: var(--pill-radius-md);
      color: #f87171;
      font-size: 13px;
      margin-bottom: 12px;
      animation: slideUpFade 200ms ease both;
    }

    .auth-lockout-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: rgba(245, 158, 11, 0.1);
      border: 1px solid rgba(245, 158, 11, 0.3);
      border-radius: var(--pill-radius-md);
      color: #fbbf24;
      font-size: 13px;
      margin-bottom: 12px;
    }

    .auth-links {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .auth-link {
      font-size: 13px;
      color: var(--accent-primary);
      text-decoration: none;
      transition: color var(--transition-fast);

      &:hover { color: #818cf8; }
    }

    .auth-link-sep {
      color: var(--text-muted);
      font-size: 13px;
    }

    .shake {
      animation: shake 400ms ease both;
    }

    .w-100 { width: 100%; }
    .mt-2 { margin-top: 8px; }
    .mt-4 { margin-top: 16px; }
    .mb-3 { margin-bottom: 12px; }
  `]
})
export class LoginComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private uiState = inject(UiStateService);

  // Signals
  loading = signal(false);
  showPassword = signal(false);
  errorMessage = signal('');
  shakeForm = signal(false);
  passwordHint = signal('');
  lockoutSeconds = signal(0);
  failedAttempts = signal(0);

  private lockoutTimer: ReturnType<typeof setInterval> | null = null;
  private pendingUsername = '';

  loginForm = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  ngOnInit(): void {}

  onUsernameBlur(): void {
    const username = this.loginForm.get('username')?.value?.trim();
    if (username && username !== this.pendingUsername) {
      this.pendingUsername = username;
      // Fetch password hint after 2+ failed attempts
      if (this.failedAttempts() >= 2) {
        this.authService.getPasswordHint(username).subscribe({
          next: (res) => this.passwordHint.set(res.hint),
          error: () => {}
        });
      }
    }
  }

  onSubmit(): void {
    if (this.loginForm.invalid || this.loading() || this.lockoutSeconds() > 0) return;

    this.loading.set(true);
    this.errorMessage.set('');

    const { username, password } = this.loginForm.value;

    this.authService.login({
      username: username!,
      masterPassword: password!,
    }).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.requires2FA) {
          // Store username for 2FA step
          sessionStorage.setItem('pending_2fa_username', username!);
          this.router.navigate(['/auth/2fa']);
        } else {
          this.uiState.showSuccess('Welcome back!', 'Signed in');
          this.router.navigate(['/dashboard']);
        }
      },
      error: (err) => {
        this.loading.set(false);
        this.failedAttempts.update(n => n + 1);
        this.triggerShake();

        const status = err.status;
        if (status === 401) {
          this.errorMessage.set('Invalid username or password.');
          // Show hint after 2 failures
          if (this.failedAttempts() >= 2 && username) {
            this.authService.getPasswordHint(username).subscribe({
              next: (res) => this.passwordHint.set(res.hint),
              error: () => {}
            });
          }
        } else if (status === 429) {
          this.startLockoutTimer(30);
          this.errorMessage.set('Too many attempts. Please wait.');
        } else if (err.error?.message?.includes('CAPTCHA')) {
          this.errorMessage.set('Please complete the CAPTCHA verification.');
        } else {
          this.errorMessage.set(err.error?.message || 'Login failed. Please try again.');
        }
      }
    });
  }

  private triggerShake(): void {
    this.shakeForm.set(true);
    setTimeout(() => this.shakeForm.set(false), 500);
  }

  private startLockoutTimer(seconds: number): void {
    this.lockoutSeconds.set(seconds);
    if (this.lockoutTimer) clearInterval(this.lockoutTimer);
    this.lockoutTimer = setInterval(() => {
      this.lockoutSeconds.update(n => {
        if (n <= 1) {
          clearInterval(this.lockoutTimer!);
          return 0;
        }
        return n - 1;
      });
    }, 1000);
  }
}
