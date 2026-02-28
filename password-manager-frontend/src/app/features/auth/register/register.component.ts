import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';

const SECURITY_QUESTIONS = [
  "What was the name of your first pet?",
  "What city were you born in?",
  "What is your mother's maiden name?",
  "What was the name of your elementary school?",
  "What was the make of your first car?",
  "What is your oldest sibling's middle name?",
  "What street did you grow up on?",
  "What was your childhood nickname?"
];

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-form-container">
      <div class="auth-form-header">
        <h2 class="auth-form-title">Create Account</h2>
        <p class="auth-form-subtitle">Set up your secure vault</p>
      </div>

      <!-- Step indicator -->
      <div class="step-indicator">
        <div class="step" [class.active]="step() === 1" [class.done]="step() > 1">
          <span class="step-num">{{ step() > 1 ? '✓' : '1' }}</span>
          <span class="step-label">Account</span>
        </div>
        <div class="step-line" [class.done]="step() > 1"></div>
        <div class="step" [class.active]="step() === 2" [class.done]="step() > 2">
          <span class="step-num">{{ step() > 2 ? '✓' : '2' }}</span>
          <span class="step-label">Security</span>
        </div>
        <div class="step-line" [class.done]="step() > 2"></div>
        <div class="step" [class.active]="step() === 3">
          <span class="step-num">3</span>
          <span class="step-label">Recovery</span>
        </div>
      </div>

      <!-- Step 1: Account Info -->
      <form class="auth-form" *ngIf="step() === 1" (ngSubmit)="nextStep()">
        <div class="pill-input-wrapper">
          <label class="pill-label">Email</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">📧</span>
            <input type="email" class="pill-input has-icon-left" placeholder="you@example.com"
              [(ngModel)]="email" name="email" required [class.error]="submitted && !email" />
          </div>
          <span class="pill-input-error" *ngIf="submitted && !email">Email is required</span>
        </div>

        <div class="pill-input-wrapper">
          <label class="pill-label">Username</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">👤</span>
            <input type="text" class="pill-input has-icon-left" placeholder="Choose a username"
              [(ngModel)]="username" name="username" required [class.error]="submitted && !username" />
          </div>
          <span class="pill-input-error" *ngIf="submitted && !username">Username is required</span>
        </div>

        <div class="auth-error" *ngIf="errorMessage">
          <span>⚠️</span> {{ errorMessage }}
        </div>

        <button type="submit" class="pill-btn pill-btn-primary" style="width:100%">
          Continue →
        </button>

        <div class="auth-links">
          <span class="text-secondary-color" style="font-size:13px">Already have an account?</span>
          <a routerLink="/auth/login" class="auth-link">Sign in</a>
        </div>
      </form>

      <!-- Step 2: Master Password -->
      <form class="auth-form" *ngIf="step() === 2" (ngSubmit)="nextStep()">
        <div class="pill-input-wrapper">
          <label class="pill-label">Master Password</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">🔒</span>
            <input [type]="showPassword ? 'text' : 'password'" class="pill-input has-icon-left has-both"
              placeholder="Create a strong master password"
              [(ngModel)]="masterPassword" name="masterPassword" required
              (ngModelChange)="checkStrength($event)"
              [class.error]="submitted && !masterPassword" />
            <button type="button" class="pill-input-icon icon-right"
              (click)="showPassword = !showPassword" style="background:none;border:none;cursor:pointer;">
              {{ showPassword ? '🙈' : '👁️' }}
            </button>
          </div>
          <!-- Strength meter -->
          <div class="strength-meter" *ngIf="masterPassword">
            <div class="strength-bar">
              <div class="strength-segment" *ngFor="let i of [1,2,3,4,5]"
                [class]="'strength-segment ' + (i <= strengthScore ? 'filled-' + strengthScore : '')"></div>
            </div>
            <div class="strength-label">
              <span class="label-text" [style.color]="strengthColor">{{ strengthLabel }}</span>
            </div>
          </div>
          <span class="pill-input-error" *ngIf="submitted && !masterPassword">Password is required</span>
        </div>

        <div class="pill-input-wrapper">
          <label class="pill-label">Confirm Password</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">🔒</span>
            <input [type]="showPassword ? 'text' : 'password'" class="pill-input has-icon-left"
              placeholder="Confirm your master password"
              [(ngModel)]="confirmPassword" name="confirmPassword" required
              [class.error]="submitted && masterPassword !== confirmPassword" />
          </div>
          <span class="pill-input-error" *ngIf="submitted && masterPassword !== confirmPassword">Passwords do not match</span>
        </div>

        <div class="pill-input-wrapper">
          <label class="pill-label">Password Hint <span style="color:var(--text-muted)">(optional)</span></label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">💡</span>
            <input type="text" class="pill-input has-icon-left" placeholder="A hint to remember your password"
              [(ngModel)]="passwordHint" name="passwordHint" />
          </div>
        </div>

        <div class="auth-warning">
          ⚠️ <strong>Important:</strong> Your master password cannot be recovered. Store it safely.
        </div>

        <div class="auth-error" *ngIf="errorMessage">
          <span>⚠️</span> {{ errorMessage }}
        </div>

        <div class="step-buttons">
          <button type="button" class="pill-btn pill-btn-secondary" (click)="step.set(1)">← Back</button>
          <button type="submit" class="pill-btn pill-btn-primary" style="flex:1">Continue →</button>
        </div>
      </form>

      <!-- Step 3: Security Questions -->
      <form class="auth-form" *ngIf="step() === 3" (ngSubmit)="onSubmit()">
        <p class="text-secondary-color" style="font-size:13px;margin:0 0 8px">
          Set up 3 security questions for account recovery.
        </p>

        <ng-container *ngFor="let q of securityQuestions; let i = index">
          <div class="pill-input-wrapper">
            <label class="pill-label">Question {{ i + 1 }}</label>
            <select class="pill-input" [(ngModel)]="q.questionText" [name]="'q' + i">
              <option value="" disabled>Select a question</option>
              <option *ngFor="let opt of availableQuestions(i)" [value]="opt">{{ opt }}</option>
            </select>
          </div>
          <div class="pill-input-wrapper">
            <label class="pill-label">Answer {{ i + 1 }}</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">💬</span>
              <input type="text" class="pill-input has-icon-left" placeholder="Your answer"
                [(ngModel)]="q.answer" [name]="'a' + i" required
                [class.error]="submitted && !q.answer" />
            </div>
            <span class="pill-input-error" *ngIf="submitted && !q.answer">Answer is required</span>
          </div>
        </ng-container>

        <div class="auth-error" *ngIf="errorMessage">
          <span>⚠️</span> {{ errorMessage }}
        </div>

        <div class="step-buttons">
          <button type="button" class="pill-btn pill-btn-secondary" (click)="step.set(2)">← Back</button>
          <button type="submit" class="pill-btn pill-btn-primary" style="flex:1"
            [class.loading]="loading()" [disabled]="loading()">
            <span class="btn-text">Create Account</span>
            <span class="btn-spinner" *ngIf="loading()"></span>
          </button>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .auth-form-container { padding: 32px; }
    .auth-form-header { text-align: center; margin-bottom: 24px; }
    .auth-form-title { font-size: var(--font-size-xl); font-weight: 700; color: var(--text-primary); margin: 0 0 6px; }
    .auth-form-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }
    .auth-form { display: flex; flex-direction: column; gap: 14px; }

    .step-indicator {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0;
      margin-bottom: 24px;
    }
    .step {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;
    }
    .step-num {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: var(--bg-elevated);
      border: 2px solid var(--border-default);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 700;
      color: var(--text-muted);
      transition: all var(--transition-base);
    }
    .step.active .step-num {
      background: var(--accent-primary);
      border-color: var(--accent-primary);
      color: #fff;
    }
    .step.done .step-num {
      background: var(--accent-success);
      border-color: var(--accent-success);
      color: #fff;
    }
    .step-label { font-size: 10px; color: var(--text-muted); }
    .step.active .step-label { color: var(--accent-primary); }
    .step-line {
      flex: 1;
      height: 2px;
      background: var(--border-default);
      margin: 0 8px;
      margin-bottom: 16px;
      min-width: 40px;
      transition: background var(--transition-base);
    }
    .step-line.done { background: var(--accent-success); }

    .auth-error {
      padding: 10px 16px;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-danger);
      font-size: var(--font-size-sm);
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .auth-warning {
      padding: 10px 16px;
      background: rgba(245,158,11,0.1);
      border: 1px solid rgba(245,158,11,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-warning);
      font-size: var(--font-size-xs);
    }
    .auth-links {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
    .auth-link {
      font-size: var(--font-size-sm);
      color: var(--accent-primary);
      text-decoration: none;
      &:hover { color: #818cf8; }
    }
    .step-buttons {
      display: flex;
      gap: 10px;
    }
    select.pill-input {
      appearance: none;
      cursor: pointer;
    }
  `]
})
export class RegisterComponent {
  private authService = inject(AuthService);
  private uiState = inject(UiStateService);
  private router = inject(Router);

  step = signal(1);
  submitted = false;
  loading = signal(false);
  errorMessage = '';

  // Step 1
  email = '';
  username = '';

  // Step 2
  masterPassword = '';
  confirmPassword = '';
  passwordHint = '';
  showPassword = false;
  strengthScore = 0;
  strengthLabel = '';
  strengthColor = '';

  // Step 3
  securityQuestions = [
    { questionText: '', answer: '' },
    { questionText: '', answer: '' },
    { questionText: '', answer: '' }
  ];

  allQuestions = SECURITY_QUESTIONS;

  availableQuestions(index: number): string[] {
    const selected = this.securityQuestions
      .filter((_, i) => i !== index)
      .map(q => q.questionText)
      .filter(q => q);
    return this.allQuestions.filter(q => !selected.includes(q));
  }

  checkStrength(password: string): void {
    let score = 0;
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    this.strengthScore = score;
    const labels = ['', 'Very Weak', 'Weak', 'Fair', 'Strong', 'Very Strong'];
    const colors = ['', 'var(--accent-danger)', 'var(--accent-orange)', 'var(--accent-warning)', '#84cc16', 'var(--accent-success)'];
    this.strengthLabel = labels[score] || 'Very Weak';
    this.strengthColor = colors[score] || 'var(--accent-danger)';
  }

  nextStep(): void {
    this.submitted = true;
    this.errorMessage = '';

    if (this.step() === 1) {
      if (!this.email || !this.username) return;
      if (!this.email.includes('@')) {
        this.errorMessage = 'Please enter a valid email address.';
        return;
      }
      this.submitted = false;
      this.step.set(2);
    } else if (this.step() === 2) {
      if (!this.masterPassword || !this.confirmPassword) return;
      if (this.masterPassword !== this.confirmPassword) return;
      if (this.masterPassword.length < 8) {
        this.errorMessage = 'Password must be at least 8 characters.';
        return;
      }
      this.submitted = false;
      this.step.set(3);
    }
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    const allAnswered = this.securityQuestions.every(q => q.questionText && q.answer);
    if (!allAnswered) return;

    this.loading.set(true);
    this.authService.register({
      email: this.email,
      username: this.username,
      masterPassword: this.masterPassword,
      passwordHint: this.passwordHint || undefined,
      securityQuestions: this.securityQuestions
    }).subscribe({
      next: () => {
        this.loading.set(false);
        sessionStorage.setItem('pending_verify_user', this.username);
        this.uiState.showSuccess('Account created! Please verify your email.', 'Registration Successful');
        this.router.navigate(['/auth/verify-email']);
      },
      error: (err) => {
        this.loading.set(false);
        const msg = err?.error?.message ?? err?.error ?? 'Registration failed. Please try again.';
        this.errorMessage = typeof msg === 'string' ? msg : 'Registration failed.';
      }
    });
  }
}
