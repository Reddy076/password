import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';

const SECURITY_QUESTIONS = [
  "What was your first pet's name?",
  "What city were you born in?",
  "What is your mother's maiden name?",
  "What was the name of your first school?",
  "What is your favorite movie?",
  "What was the make of your first car?",
  "What is your oldest sibling's middle name?",
  "What street did you grow up on?",
];

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="register-page">
      <!-- Header -->
      <div class="auth-header">
        <div class="auth-logo">
          <div class="auth-logo-icon">🔐</div>
          <span class="auth-logo-text">RevaultX</span>
        </div>
        <h2 class="auth-title">Create your account</h2>
        <p class="auth-subtitle">Step {{ currentStep() }} of 4</p>
      </div>

      <!-- Step indicator -->
      <div class="step-indicator">
        @for (step of [1,2,3,4]; track step) {
          <div class="step-dot" [class.active]="step === currentStep()" [class.done]="step < currentStep()">
            @if (step < currentStep()) {
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                <polyline points="20 6 9 17 4 12"/>
              </svg>
            } @else {
              {{ step }}
            }
          </div>
          @if (step < 4) {
            <div class="step-line" [class.done]="step < currentStep()"></div>
          }
        }
      </div>

      <!-- Step 1: Account Details -->
      @if (currentStep() === 1) {
        <div class="step-content" [@slideIn]>
          <h3 class="step-title">Account Details</h3>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Email Address</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                  <polyline points="22,6 12,13 2,6"/>
                </svg>
              </span>
              <input type="email" class="pill-input has-icon-left" placeholder="you@example.com"
                [formControl]="step1Form.controls.email" autocomplete="email" />
            </div>
            @if (step1Form.controls.email.invalid && step1Form.controls.email.touched) {
              <div class="pill-input-error">Please enter a valid email address</div>
            }
          </div>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Username</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </span>
              <input type="text" class="pill-input has-icon-left" placeholder="Choose a username"
                [formControl]="step1Form.controls.username" autocomplete="username" />
            </div>
            @if (step1Form.controls.username.invalid && step1Form.controls.username.touched) {
              <div class="pill-input-error">Username must be 3-30 characters</div>
            }
          </div>

          <button type="button" class="pill-btn pill-btn-primary w-100 mt-2"
            [disabled]="step1Form.invalid" (click)="nextStep()">
            Continue →
          </button>
        </div>
      }

      <!-- Step 2: Master Password -->
      @if (currentStep() === 2) {
        <div class="step-content">
          <h3 class="step-title">Master Password</h3>
          <p class="step-desc">This is the key to your vault. Make it strong and memorable.</p>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Master Password</label>
            <div class="pill-input-inner">
              <input [type]="showPassword() ? 'text' : 'password'" class="pill-input has-icon-right"
                placeholder="Create a strong password"
                [formControl]="step2Form.controls.password"
                (input)="checkStrength()" autocomplete="new-password" />
              <button type="button" class="pill-input-icon icon-right" (click)="showPassword.set(!showPassword())">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
            </div>
          </div>

          <!-- Strength meter -->
          @if (step2Form.controls.password.value) {
            <div class="strength-meter mb-3">
              <div class="strength-bar">
                @for (i of [1,2,3,4,5]; track i) {
                  <div class="strength-segment" [class]="getSegmentClass(i)"></div>
                }
              </div>
              <div class="strength-label">
                <span class="label-text" [style.color]="strengthColor()">{{ strengthLabel() }}</span>
                <span class="label-score">{{ strengthScore() }}/100</span>
              </div>
            </div>
          }

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Confirm Password</label>
            <div class="pill-input-inner">
              <input [type]="showPassword() ? 'text' : 'password'" class="pill-input"
                placeholder="Confirm your password"
                [formControl]="step2Form.controls.confirmPassword" autocomplete="new-password" />
            </div>
            @if (step2Form.controls.confirmPassword.value && step2Form.errors?.['mismatch']) {
              <div class="pill-input-error">Passwords do not match</div>
            }
          </div>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Password Hint <span class="optional">(optional)</span></label>
            <input type="text" class="pill-input" placeholder="A hint to remember your password"
              [formControl]="step2Form.controls.hint" />
            <div class="pill-input-hint">Never include your actual password in the hint</div>
          </div>

          <div class="step-nav">
            <button type="button" class="pill-btn pill-btn-ghost" (click)="prevStep()">← Back</button>
            <button type="button" class="pill-btn pill-btn-primary"
              [disabled]="step2Form.invalid || strengthScore() < 40" (click)="nextStep()">
              Continue →
            </button>
          </div>
        </div>
      }

      <!-- Step 3: Security Questions -->
      @if (currentStep() === 3) {
        <div class="step-content">
          <h3 class="step-title">Security Questions</h3>
          <p class="step-desc">Used for account recovery if you forget your password.</p>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Question 1</label>
            <select class="pill-input" [formControl]="step3Form.controls.q1">
              <option value="">Select a question...</option>
              @for (q of securityQuestions; track q) {
                <option [value]="q">{{ q }}</option>
              }
            </select>
          </div>
          <div class="pill-input-wrapper mb-4">
            <label class="pill-label">Answer 1</label>
            <input type="text" class="pill-input" placeholder="Your answer"
              [formControl]="step3Form.controls.a1" />
          </div>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Question 2</label>
            <select class="pill-input" [formControl]="step3Form.controls.q2">
              <option value="">Select a question...</option>
              @for (q of securityQuestions; track q) {
                <option [value]="q">{{ q }}</option>
              }
            </select>
          </div>
          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Answer 2</label>
            <input type="text" class="pill-input" placeholder="Your answer"
              [formControl]="step3Form.controls.a2" />
          </div>

          <div class="step-nav">
            <button type="button" class="pill-btn pill-btn-ghost" (click)="prevStep()">← Back</button>
            <button type="button" class="pill-btn pill-btn-primary"
              [disabled]="step3Form.invalid" (click)="nextStep()">
              Continue →
            </button>
          </div>
        </div>
      }

      <!-- Step 4: Review & Submit -->
      @if (currentStep() === 4) {
        <div class="step-content">
          <h3 class="step-title">Review & Create</h3>

          <div class="review-card">
            <div class="review-row">
              <span class="review-label">Email</span>
              <span class="review-value">{{ step1Form.controls.email.value }}</span>
            </div>
            <div class="review-row">
              <span class="review-label">Username</span>
              <span class="review-value">{{ step1Form.controls.username.value }}</span>
            </div>
            <div class="review-row">
              <span class="review-label">Password</span>
              <span class="review-value">
                <span class="pill-badge badge-success">{{ strengthLabel() }}</span>
              </span>
            </div>
            <div class="review-row">
              <span class="review-label">Security Questions</span>
              <span class="review-value">2 questions set ✓</span>
            </div>
          </div>

          <!-- Terms -->
          <label class="terms-check mt-3">
            <input type="checkbox" [formControl]="termsAccepted" />
            <span class="terms-text">
              I agree to the <a href="#" class="auth-link">Terms of Service</a> and
              <a href="#" class="auth-link">Privacy Policy</a>
            </span>
          </label>

          @if (errorMessage()) {
            <div class="auth-error-banner mt-3">{{ errorMessage() }}</div>
          }

          <div class="step-nav mt-3">
            <button type="button" class="pill-btn pill-btn-ghost" (click)="prevStep()">← Back</button>
            <button type="button" class="pill-btn pill-btn-primary"
              [disabled]="!termsAccepted.value || loading()"
              [class.loading]="loading()"
              (click)="submit()">
              @if (loading()) {
                <span class="btn-spinner"></span>
              } @else {
                <span class="btn-text">Create Account</span>
              }
            </button>
          </div>
        </div>
      }

      <!-- Login link -->
      <div class="auth-links mt-4">
        <span class="auth-link-sep">Already have an account?</span>
        <a routerLink="/auth/login" class="auth-link">Sign in</a>
      </div>
    </div>
  `,
  styles: [`
    .register-page { width: 100%; }

    .auth-header { text-align: center; margin-bottom: 24px; }
    .auth-logo { display: inline-flex; align-items: center; gap: 8px; margin-bottom: 16px; }
    .auth-logo-icon {
      width: 32px; height: 32px; border-radius: 10px;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex; align-items: center; justify-content: center; font-size: 16px;
    }
    .auth-logo-text { font-size: 18px; font-weight: 800; color: var(--text-primary); }
    .auth-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
    .auth-subtitle { font-size: 13px; color: var(--text-secondary); margin: 0; }

    /* Step indicator */
    .step-indicator {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0;
      margin-bottom: 28px;
    }

    .step-dot {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      border: 2px solid var(--border-default);
      background: var(--bg-elevated);
      color: var(--text-muted);
      font-size: 13px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all var(--transition-base);
      flex-shrink: 0;

      &.active {
        border-color: var(--accent-primary);
        background: var(--accent-primary);
        color: #fff;
        box-shadow: 0 0 12px rgba(99, 102, 241, 0.4);
      }

      &.done {
        border-color: var(--accent-success);
        background: var(--accent-success);
        color: #fff;
      }
    }

    .step-line {
      flex: 1;
      height: 2px;
      background: var(--border-default);
      max-width: 40px;
      transition: background var(--transition-base);

      &.done { background: var(--accent-success); }
    }

    /* Step content */
    .step-title {
      font-size: 18px;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 4px;
    }

    .step-desc {
      font-size: 13px;
      color: var(--text-secondary);
      margin-bottom: 20px;
    }

    .step-nav {
      display: flex;
      gap: 12px;
      justify-content: space-between;
      margin-top: 8px;
    }

    .optional { color: var(--text-muted); font-weight: 400; }

    /* Review card */
    .review-card {
      background: var(--bg-elevated);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-md);
      overflow: hidden;
    }

    .review-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border-subtle);

      &:last-child { border-bottom: none; }
    }

    .review-label { font-size: 13px; color: var(--text-secondary); }
    .review-value { font-size: 13px; color: var(--text-primary); font-weight: 500; }

    /* Terms */
    .terms-check {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      cursor: pointer;

      input[type="checkbox"] {
        width: 16px;
        height: 16px;
        margin-top: 2px;
        accent-color: var(--accent-primary);
        flex-shrink: 0;
      }
    }

    .terms-text { font-size: 13px; color: var(--text-secondary); line-height: 1.5; }

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
      &:hover { color: #818cf8; }
    }

    .auth-link-sep { font-size: 13px; color: var(--text-secondary); }

    .w-100 { width: 100%; }
    .mt-2 { margin-top: 8px; }
    .mt-3 { margin-top: 12px; }
    .mt-4 { margin-top: 16px; }
    .mb-3 { margin-bottom: 12px; }
    .mb-4 { margin-bottom: 16px; }
  `]
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private uiState = inject(UiStateService);

  securityQuestions = SECURITY_QUESTIONS;

  currentStep = signal(1);
  loading = signal(false);
  errorMessage = signal('');
  showPassword = signal(false);
  strengthScore = signal(0);
  strengthLabel = signal('');
  strengthColor = signal('');

  // Step 1 form
  step1Form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
  });

  // Step 2 form
  step2Form = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
    hint: [''],
  }, { validators: this.passwordMatchValidator });

  // Step 3 form
  step3Form = this.fb.group({
    q1: ['', Validators.required],
    a1: ['', Validators.required],
    q2: ['', Validators.required],
    a2: ['', Validators.required],
  });

  termsAccepted = this.fb.control(false);

  private passwordMatchValidator(group: AbstractControl) {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return pass === confirm ? null : { mismatch: true };
  }

  checkStrength(): void {
    const password = this.step2Form.controls.password.value || '';
    let score = 0;
    if (password.length >= 8) score += 20;
    if (password.length >= 12) score += 10;
    if (password.length >= 16) score += 10;
    if (/[A-Z]/.test(password)) score += 15;
    if (/[a-z]/.test(password)) score += 15;
    if (/[0-9]/.test(password)) score += 15;
    if (/[^A-Za-z0-9]/.test(password)) score += 15;

    this.strengthScore.set(score);

    if (score < 30) { this.strengthLabel.set('Very Weak'); this.strengthColor.set('#ef4444'); }
    else if (score < 50) { this.strengthLabel.set('Weak'); this.strengthColor.set('#f97316'); }
    else if (score < 70) { this.strengthLabel.set('Fair'); this.strengthColor.set('#f59e0b'); }
    else if (score < 85) { this.strengthLabel.set('Strong'); this.strengthColor.set('#84cc16'); }
    else { this.strengthLabel.set('Very Strong'); this.strengthColor.set('#10b981'); }
  }

  getSegmentClass(segment: number): string {
    const score = this.strengthScore();
    const filled = Math.ceil(score / 20);
    if (segment > filled) return 'strength-segment';
    if (score < 30) return 'strength-segment filled-1';
    if (score < 50) return 'strength-segment filled-2';
    if (score < 70) return 'strength-segment filled-3';
    if (score < 85) return 'strength-segment filled-4';
    return 'strength-segment filled-5';
  }

  nextStep(): void {
    if (this.currentStep() < 4) {
      this.currentStep.update(n => n + 1);
    }
  }

  prevStep(): void {
    if (this.currentStep() > 1) {
      this.currentStep.update(n => n - 1);
    }
  }

  submit(): void {
    if (this.loading()) return;
    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.register({
      email: this.step1Form.controls.email.value!,
      username: this.step1Form.controls.username.value!,
      masterPassword: this.step2Form.controls.password.value!,
      passwordHint: this.step2Form.controls.hint.value || undefined,
      securityQuestions: [
        { questionText: this.step3Form.controls.q1.value!, answer: this.step3Form.controls.a1.value! },
        { questionText: this.step3Form.controls.q2.value!, answer: this.step3Form.controls.a2.value! },
      ]
    }).subscribe({
      next: (res) => {
        this.loading.set(false);
        sessionStorage.setItem('verify_email_username', res.username);
        this.uiState.showSuccess('Account created! Please verify your email.');
        this.router.navigate(['/auth/verify-email']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Registration failed. Please try again.');
        this.currentStep.set(4);
      }
    });
  }
}
