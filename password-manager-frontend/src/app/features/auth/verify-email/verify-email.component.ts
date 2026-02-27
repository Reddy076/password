import { Component, inject, signal, OnInit, OnDestroy, ViewChildren, QueryList, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="verify-page">
      <div class="auth-header">
        <div class="verify-icon">📧</div>
        <h2 class="auth-title">Verify your email</h2>
        <p class="auth-subtitle">
          We sent a 6-digit code to<br>
          <strong>{{ maskedEmail() }}</strong>
        </p>
      </div>

      <!-- OTP Boxes (reusing same pattern as 2FA) -->
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
          />
          @if (i === 2) {
            <div class="otp-separator">—</div>
          }
        }
      </div>

      @if (errorMessage()) {
        <div class="auth-error-banner mt-3">{{ errorMessage() }}</div>
      }

      @if (success()) {
        <div class="auth-success-banner mt-3">
          ✓ Email verified! Redirecting to login...
        </div>
      }

      <button type="button" class="pill-btn pill-btn-primary w-100 mt-4"
        [disabled]="otpDigits().join('').length < 6 || loading() || success()"
        [class.loading]="loading()"
        (click)="verify()">
        @if (loading()) {
          <span class="btn-spinner"></span>
        } @else {
          <span class="btn-text">Verify Email</span>
        }
      </button>

      <div class="tfa-resend mt-3">
        @if (resendCooldown() > 0) {
          <span class="resend-timer">Resend code in {{ resendCooldown() }}s</span>
        } @else {
          <button type="button" class="resend-btn" (click)="resendCode()">Resend code</button>
        }
      </div>

      <div class="auth-links mt-3">
        <a routerLink="/auth/login" class="auth-link">← Back to login</a>
      </div>
    </div>
  `,
  styles: [`
    .verify-page { width: 100%; }
    .auth-header { text-align: center; margin-bottom: 28px; }
    .verify-icon { font-size: 40px; margin-bottom: 12px; }
    .auth-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 8px; }
    .auth-subtitle { font-size: 14px; color: var(--text-secondary); margin: 0; line-height: 1.6; }

    .otp-container {
      display: flex; align-items: center; justify-content: center; gap: 8px;
    }
    .otp-box {
      width: 48px; height: 56px; text-align: center; font-size: 22px; font-weight: 700;
      font-family: var(--font-mono); color: var(--text-primary); background: var(--bg-elevated);
      border: 2px solid var(--border-default); border-radius: var(--pill-radius-md); outline: none;
      transition: all var(--transition-fast);
      &:focus { border-color: var(--accent-primary); box-shadow: 0 0 0 3px rgba(99,102,241,0.15); }
      &.otp-filled { border-color: var(--accent-primary); background: rgba(99,102,241,0.08); }
    }
    .otp-error .otp-box { border-color: var(--accent-danger); animation: shake 400ms ease both; }
    .otp-separator { color: var(--text-muted); font-size: 20px; margin: 0 4px; }

    .auth-error-banner {
      padding: 12px 16px; background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md); color: #f87171; font-size: 13px;
    }
    .auth-success-banner {
      padding: 12px 16px; background: rgba(16,185,129,0.1); border: 1px solid rgba(16,185,129,0.3);
      border-radius: var(--pill-radius-md); color: #34d399; font-size: 13px;
    }
    .tfa-resend { text-align: center; }
    .resend-timer { font-size: 13px; color: var(--text-muted); }
    .resend-btn { background: none; border: none; color: var(--accent-primary); font-size: 13px; cursor: pointer; }
    .auth-links { display: flex; justify-content: center; }
    .auth-link { font-size: 13px; color: var(--text-secondary); text-decoration: none; }
    .w-100 { width: 100%; }
    .mt-3 { margin-top: 12px; }
    .mt-4 { margin-top: 16px; }
  `]
})
export class VerifyEmailComponent implements OnInit, OnDestroy {
  @ViewChildren('otpInput') otpInputs!: QueryList<ElementRef<HTMLInputElement>>;

  private authService = inject(AuthService);
  private router = inject(Router);
  private uiState = inject(UiStateService);

  otpDigits = signal<string[]>(['', '', '', '', '', '']);
  loading = signal(false);
  errorMessage = signal('');
  success = signal(false);
  hasError = signal(false);
  resendCooldown = signal(0);
  username = signal('');

  private resendTimer: ReturnType<typeof setInterval> | null = null;

  maskedEmail(): string {
    const u = this.username();
    if (!u) return 'your email';
    return u.length > 3 ? u[0] + '***@example.com' : '***@example.com';
  }

  ngOnInit(): void {
    const stored = sessionStorage.getItem('verify_email_username');
    if (stored) {
      this.username.set(stored);
    }
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
    if (value && index < 5) {
      setTimeout(() => this.otpInputs.toArray()[index + 1]?.nativeElement.focus(), 0);
    }
    if (digits.every(d => d)) setTimeout(() => this.verify(), 100);
  }

  onKeyDown(event: KeyboardEvent, index: number): void {
    if (event.key === 'Backspace') {
      const digits = [...this.otpDigits()];
      if (!digits[index] && index > 0) {
        digits[index - 1] = '';
        this.otpDigits.set(digits);
        setTimeout(() => this.otpInputs.toArray()[index - 1]?.nativeElement.focus(), 0);
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
    if (digits.length === 6) setTimeout(() => this.verify(), 100);
  }

  verify(): void {
    const code = this.otpDigits().join('');
    if (code.length < 6 || this.loading()) return;
    this.loading.set(true);
    this.authService.verifyEmail(this.username(), code).subscribe({
      next: () => {
        this.loading.set(false);
        this.success.set(true);
        sessionStorage.removeItem('verify_email_username');
        setTimeout(() => {
          this.uiState.showSuccess('Email verified! You can now sign in.');
          this.router.navigate(['/auth/login']);
        }, 1000);
      },
      error: (err) => {
        this.loading.set(false);
        this.hasError.set(true);
        this.otpDigits.set(['', '', '', '', '', '']);
        this.errorMessage.set(err.error?.message || 'Invalid code. Please try again.');
      }
    });
  }

  resendCode(): void {
    this.authService.resendVerificationOtp(this.username()).subscribe({
      next: () => {
        this.uiState.showInfo('Verification code resent.');
        this.startResendCooldown(60);
      },
      error: () => this.uiState.showError('Failed to resend code.')
    });
  }

  private startResendCooldown(seconds: number): void {
    this.resendCooldown.set(seconds);
    if (this.resendTimer) clearInterval(this.resendTimer);
    this.resendTimer = setInterval(() => {
      this.resendCooldown.update(n => {
        if (n <= 1) { clearInterval(this.resendTimer!); return 0; }
        return n - 1;
      });
    }, 1000);
  }
}
