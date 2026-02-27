import { Component, inject, signal, OnInit, OnDestroy, ViewChildren, QueryList, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';

@Component({
  selector: 'app-two-factor-login',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="tfa-page">
      <!-- Header -->
      <div class="auth-header">
        <div class="tfa-icon">🔐</div>
        <h2 class="auth-title">Two-Factor Authentication</h2>
        <p class="auth-subtitle">
          Enter the 6-digit code from your authenticator app
          @if (username()) {
            <br><strong>{{ username() }}</strong>
          }
        </p>
      </div>

      <!-- OTP Input Boxes -->
      <div class="otp-container" [class.otp-error]="hasError()">
        @for (i of [0,1,2,3,4,5]; track i) {
          <input
            #otpInput
            type="text"
            inputmode="numeric"
            maxlength="1"
            class="otp-box"
            [class.otp-filled]="otpDigits()[i]"
            [value]="otpDigits()[i]"
            (input)="onDigitInput($event, i)"
            (keydown)="onKeyDown($event, i)"
            (paste)="onPaste($event)"
            autocomplete="one-time-code"
          />
          @if (i === 2) {
            <div class="otp-separator">—</div>
          }
        }
      </div>

      <!-- Error -->
      @if (errorMessage()) {
        <div class="auth-error-banner mt-3">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          {{ errorMessage() }}
        </div>
      }

      <!-- Success -->
      @if (success()) {
        <div class="auth-success-banner mt-3">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="20 6 9 17 4 12"/>
          </svg>
          Verified! Redirecting...
        </div>
      }

      <!-- Verify button -->
      <button
        type="button"
        class="pill-btn pill-btn-primary w-100 mt-4"
        [disabled]="otpDigits().join('').length < 6 || loading() || success()"
        [class.loading]="loading()"
        (click)="verify()"
      >
        @if (loading()) {
          <span class="btn-spinner"></span>
        } @else {
          <span class="btn-text">Verify Code</span>
        }
      </button>

      <!-- Resend -->
      <div class="tfa-resend mt-3">
        @if (resendCooldown() > 0) {
          <span class="resend-timer">Resend code in {{ resendCooldown() }}s</span>
        } @else {
          <button type="button" class="resend-btn" (click)="resendCode()">
            Resend code
          </button>
        }
      </div>

      <!-- Back link -->
      <div class="auth-links mt-3">
        <a routerLink="/auth/login" class="auth-link">← Back to login</a>
      </div>
    </div>
  `,
  styles: [`
    .tfa-page { width: 100%; }

    .auth-header {
      text-align: center;
      margin-bottom: 32px;
    }

    .tfa-icon {
      font-size: 40px;
      margin-bottom: 16px;
    }

    .auth-title {
      font-size: 24px;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 8px;
    }

    .auth-subtitle {
      font-size: 14px;
      color: var(--text-secondary);
      margin: 0;
      line-height: 1.6;
    }

    /* OTP Boxes */
    .otp-container {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin: 0 auto;
    }

    .otp-box {
      width: 48px;
      height: 56px;
      text-align: center;
      font-size: 22px;
      font-weight: 700;
      font-family: var(--font-mono);
      color: var(--text-primary);
      background: var(--bg-elevated);
      border: 2px solid var(--border-default);
      border-radius: var(--pill-radius-md);
      outline: none;
      transition: all var(--transition-fast);
      caret-color: var(--accent-primary);

      &:focus {
        border-color: var(--accent-primary);
        box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.15);
        background: var(--bg-surface);
      }

      &.otp-filled {
        border-color: var(--accent-primary);
        background: rgba(99, 102, 241, 0.08);
      }
    }

    .otp-error .otp-box {
      border-color: var(--accent-danger);
      animation: shake 400ms ease both;
    }

    .otp-separator {
      color: var(--text-muted);
      font-size: 20px;
      margin: 0 4px;
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
      animation: slideUpFade 200ms ease both;
    }

    .auth-success-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.3);
      border-radius: var(--pill-radius-md);
      color: #34d399;
      font-size: 13px;
      animation: slideUpFade 200ms ease both;
    }

    .tfa-resend {
      text-align: center;
    }

    .resend-timer {
      font-size: 13px;
      color: var(--text-muted);
    }

    .resend-btn {
      background: none;
      border: none;
      color: var(--accent-primary);
      font-size: 13px;
      cursor: pointer;
      padding: 0;
      transition: color var(--transition-fast);

      &:hover { color: #818cf8; }
    }

    .auth-links {
      display: flex;
      justify-content: center;
    }

    .auth-link {
      font-size: 13px;
      color: var(--text-secondary);
      text-decoration: none;
      transition: color var(--transition-fast);

      &:hover { color: var(--text-primary); }
    }

    .w-100 { width: 100%; }
    .mt-3 { margin-top: 12px; }
    .mt-4 { margin-top: 16px; }
  `]
})
export class TwoFactorLoginComponent implements OnInit, OnDestroy {
  @ViewChildren('otpInput') otpInputs!: QueryList<ElementRef<HTMLInputElement>>;

  private authService = inject(AuthService);
  private router = inject(Router);
  private uiState = inject(UiStateService);

  // Signals
  otpDigits = signal<string[]>(['', '', '', '', '', '']);
  loading = signal(false);
  errorMessage = signal('');
  success = signal(false);
  hasError = signal(false);
  resendCooldown = signal(0);
  username = signal('');

  private resendTimer: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    const stored = sessionStorage.getItem('pending_2fa_username');
    if (stored) {
      this.username.set(stored);
    } else {
      this.router.navigate(['/auth/login']);
    }
    // Start resend cooldown
    this.startResendCooldown(60);
  }

  ngOnDestroy(): void {
    if (this.resendTimer) clearInterval(this.resendTimer);
  }

  onDigitInput(event: Event, index: number): void {
    const input = event.target as HTMLInputElement;
    const value = input.value.replace(/\D/g, '').slice(-1);

    const digits = [...this.otpDigits()];
    digits[index] = value;
    this.otpDigits.set(digits);
    this.errorMessage.set('');
    this.hasError.set(false);

    // Auto-advance to next box
    if (value && index < 5) {
      setTimeout(() => {
        const inputs = this.otpInputs.toArray();
        inputs[index + 1]?.nativeElement.focus();
      }, 0);
    }

    // Auto-submit when all 6 digits filled
    if (digits.every(d => d) && digits.join('').length === 6) {
      setTimeout(() => this.verify(), 100);
    }
  }

  onKeyDown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      const digits = [...this.otpDigits()];
      if (!digits[index] && index > 0) {
        // Move to previous box
        digits[index - 1] = '';
        this.otpDigits.set(digits);
        setTimeout(() => {
          const inputs = this.otpInputs.toArray();
          inputs[index - 1]?.nativeElement.focus();
        }, 0);
      } else {
        digits[index] = '';
        this.otpDigits.set(digits);
      }
    }
  }

  onPaste(event: ClipboardEvent): void {
    event.preventDefault();
    const text = event.clipboardData?.getData('text') ?? '';
    const digits = text.replace(/\D/g, '').slice(0, 6).split('');

    const filled = [...this.otpDigits()];
    digits.forEach((d, i) => { filled[i] = d; });
    this.otpDigits.set(filled);

    // Focus last filled box
    const lastIndex = Math.min(digits.length - 1, 5);
    setTimeout(() => {
      const inputs = this.otpInputs.toArray();
      inputs[lastIndex]?.nativeElement.focus();
    }, 0);

    if (digits.length === 6) {
      setTimeout(() => this.verify(), 100);
    }
  }

  verify(): void {
    const code = this.otpDigits().join('');
    if (code.length < 6 || this.loading()) return;

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.verifyOtp(this.username(), code).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        sessionStorage.removeItem('pending_2fa_username');
        setTimeout(() => {
          this.uiState.showSuccess('Welcome back!', 'Signed in');
          this.router.navigate(['/dashboard']);
        }, 800);
      },
      error: (err) => {
        this.loading.set(false);
        this.hasError.set(true);
        this.otpDigits.set(['', '', '', '', '', '']);
        this.errorMessage.set(err.error?.message || 'Invalid code. Please try again.');
        setTimeout(() => {
          const inputs = this.otpInputs.toArray();
          inputs[0]?.nativeElement.focus();
        }, 100);
      }
    });
  }

  resendCode(): void {
    this.authService.resendOtp(this.username()).subscribe({
      next: () => {
        this.uiState.showInfo('Code resent to your email.');
        this.startResendCooldown(60);
      },
      error: () => {
        this.uiState.showError('Failed to resend code.');
      }
    });
  }

  private startResendCooldown(seconds: number): void {
    this.resendCooldown.set(seconds);
    if (this.resendTimer) clearInterval(this.resendTimer);
    this.resendTimer = setInterval(() => {
      this.resendCooldown.update(n => {
        if (n <= 1) {
          clearInterval(this.resendTimer!);
          return 0;
        }
        return n - 1;
      });
    }, 1000);
  }
}
