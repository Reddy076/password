import { Component, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';
import { SecurityQuestion } from '../../../core/models/auth.models';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  template: `
    <div class="forgot-page">
      <div class="auth-header">
        <div class="forgot-icon">🔑</div>
        <h2 class="auth-title">Reset Password</h2>
        <p class="auth-subtitle">Step {{ currentStep() }} of 3</p>
      </div>

      <!-- Step indicator -->
      <div class="step-indicator mb-4">
        @for (step of [1,2,3]; track step) {
          <div class="step-dot" [class.active]="step === currentStep()" [class.done]="step < currentStep()">
            @if (step < currentStep()) { ✓ } @else { {{ step }} }
          </div>
          @if (step < 3) {
            <div class="step-line" [class.done]="step < currentStep()"></div>
          }
        }
      </div>

      <!-- Step 1: Enter username -->
      @if (currentStep() === 1) {
        <div class="step-content">
          <h3 class="step-title">Find your account</h3>
          <p class="step-desc">Enter your username to retrieve your security questions.</p>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">Username</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </span>
              <input type="text" class="pill-input has-icon-left" placeholder="Your username"
                [formControl]="usernameControl" />
            </div>
          </div>

          @if (errorMessage()) {
            <div class="auth-error-banner mb-3">{{ errorMessage() }}</div>
          }

          <button type="button" class="pill-btn pill-btn-primary w-100"
            [disabled]="usernameControl.invalid || loading()"
            [class.loading]="loading()"
            (click)="fetchSecurityQuestions()">
            @if (loading()) { <span class="btn-spinner"></span> }
            @else { <span class="btn-text">Continue →</span> }
          </button>
        </div>
      }

      <!-- Step 2: Answer security questions -->
      @if (currentStep() === 2) {
        <div class="step-content">
          <h3 class="step-title">Security Questions</h3>
          <p class="step-desc">Answer your security questions to verify your identity.</p>

          @for (q of securityQuestions(); track q.questionText; let i = $index) {
            <div class="pill-input-wrapper mb-3">
              <label class="pill-label">{{ q.questionText }}</label>
              <input type="text" class="pill-input" placeholder="Your answer"
                [(ngModel)]="answers[i]" />
            </div>
          }

          @if (errorMessage()) {
            <div class="auth-error-banner mb-3">{{ errorMessage() }}</div>
          }

          <div class="step-nav">
            <button type="button" class="pill-btn pill-btn-ghost" (click)="currentStep.set(1)">← Back</button>
            <button type="button" class="pill-btn pill-btn-primary"
              [disabled]="hasEmptyAnswers() || loading()"
              [class.loading]="loading()"
              (click)="verifyAnswers()">
              @if (loading()) { <span class="btn-spinner"></span> }
              @else { <span class="btn-text">Verify →</span> }
            </button>
          </div>
        </div>
      }

      <!-- Step 3: New password -->
      @if (currentStep() === 3) {
        <div class="step-content">
          <h3 class="step-title">New Password</h3>
          <p class="step-desc">Create a new strong master password.</p>

          <div class="pill-input-wrapper mb-3">
            <label class="pill-label">New Master Password</label>
            <div class="pill-input-inner">
              <input [type]="showPassword() ? 'text' : 'password'" class="pill-input has-icon-right"
                placeholder="New password"
                [formControl]="newPasswordControl"
                (input)="checkStrength()" />
              <button type="button" class="pill-input-icon icon-right" (click)="showPassword.set(!showPassword())">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
              </button>
            </div>
          </div>

          @if (newPasswordControl.value) {
            <div class="strength-meter mb-3">
              <div class="strength-bar">
                @for (i of [1,2,3,4,5]; track i) {
                  <div class="strength-segment" [class]="getSegmentClass(i)"></div>
                }
              </div>
              <div class="strength-label">
                <span class="label-text" [style.color]="strengthColor()">{{ strengthLabel() }}</span>
              </div>
            </div>
          }

          @if (errorMessage()) {
            <div class="auth-error-banner mb-3">{{ errorMessage() }}</div>
          }

          <div class="step-nav">
            <button type="button" class="pill-btn pill-btn-ghost" (click)="currentStep.set(2)">← Back</button>
            <button type="button" class="pill-btn pill-btn-primary"
              [disabled]="newPasswordControl.invalid || strengthScore() < 40 || loading()"
              [class.loading]="loading()"
              (click)="resetPassword()">
              @if (loading()) { <span class="btn-spinner"></span> }
              @else { <span class="btn-text">Reset Password</span> }
            </button>
          </div>
        </div>
      }

      <!-- Success -->
      @if (currentStep() === 4) {
        <div class="success-state">
          <div class="success-icon">✅</div>
          <h3 class="step-title">Password Reset!</h3>
          <p class="step-desc">Your password has been changed successfully.</p>
          <a routerLink="/auth/login" class="pill-btn pill-btn-primary w-100 mt-3">Sign In</a>
        </div>
      }

      @if (currentStep() < 4) {
        <div class="auth-links mt-4">
          <a routerLink="/auth/login" class="auth-link">← Back to login</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .forgot-page { width: 100%; }
    .auth-header { text-align: center; margin-bottom: 20px; }
    .forgot-icon { font-size: 36px; margin-bottom: 12px; }
    .auth-title { font-size: 22px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
    .auth-subtitle { font-size: 13px; color: var(--text-secondary); margin: 0; }

    .step-indicator {
      display: flex; align-items: center; justify-content: center; gap: 0;
    }
    .step-dot {
      width: 28px; height: 28px; border-radius: 50%; border: 2px solid var(--border-default);
      background: var(--bg-elevated); color: var(--text-muted); font-size: 12px; font-weight: 600;
      display: flex; align-items: center; justify-content: center; flex-shrink: 0;
      transition: all var(--transition-base);
      &.active { border-color: var(--accent-primary); background: var(--accent-primary); color: #fff; }
      &.done { border-color: var(--accent-success); background: var(--accent-success); color: #fff; }
    }
    .step-line {
      flex: 1; height: 2px; background: var(--border-default); max-width: 40px;
      &.done { background: var(--accent-success); }
    }

    .step-title { font-size: 17px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
    .step-desc { font-size: 13px; color: var(--text-secondary); margin-bottom: 16px; }
    .step-nav { display: flex; gap: 12px; justify-content: space-between; margin-top: 8px; }

    .auth-error-banner {
      padding: 12px 16px; background: rgba(239,68,68,0.1); border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md); color: #f87171; font-size: 13px;
    }

    .success-state { text-align: center; }
    .success-icon { font-size: 48px; margin-bottom: 16px; }

    .auth-links { display: flex; justify-content: center; }
    .auth-link { font-size: 13px; color: var(--text-secondary); text-decoration: none; }
    .w-100 { width: 100%; }
    .mb-3 { margin-bottom: 12px; }
    .mb-4 { margin-bottom: 16px; }
    .mt-3 { margin-top: 12px; }
    .mt-4 { margin-top: 16px; }
  `]
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);
  private router = inject(Router);
  private uiState = inject(UiStateService);

  currentStep = signal(1);
  loading = signal(false);
  errorMessage = signal('');
  showPassword = signal(false);
  securityQuestions = signal<SecurityQuestion[]>([]);
  answers: string[] = ['', ''];
  strengthScore = signal(0);
  strengthLabel = signal('');
  strengthColor = signal('');

  hasEmptyAnswers(): boolean {
    return this.answers.some(a => !a || a.trim() === '');
  }

  usernameControl = this.fb.control('', [Validators.required]);
  newPasswordControl = this.fb.control('', [Validators.required, Validators.minLength(8)]);

  fetchSecurityQuestions(): void {
    const username = this.usernameControl.value!;
    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.getSecurityQuestions(username).subscribe({
      next: (questions) => {
        this.loading.set(false);
        this.securityQuestions.set(questions);
        this.answers = new Array(questions.length).fill('');
        this.currentStep.set(2);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Username not found.');
      }
    });
  }

  verifyAnswers(): void {
    const username = this.usernameControl.value!;
    const questions = this.securityQuestions().map((q, i) => ({
      questionText: q.questionText,
      answer: this.answers[i]
    }));

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.verifySecurityQuestions(username, questions).subscribe({
      next: () => {
        this.loading.set(false);
        this.currentStep.set(3);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Incorrect answers. Please try again.');
      }
    });
  }

  resetPassword(): void {
    const username = this.usernameControl.value!;
    const questions = this.securityQuestions().map((q, i) => ({
      questionText: q.questionText,
      answer: this.answers[i]
    }));

    this.loading.set(true);
    this.errorMessage.set('');

    this.authService.resetPassword(username, questions, this.newPasswordControl.value!).subscribe({
      next: () => {
        this.loading.set(false);
        this.currentStep.set(4);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage.set(err.error?.message || 'Failed to reset password.');
      }
    });
  }

  checkStrength(): void {
    const password = this.newPasswordControl.value || '';
    let score = 0;
    if (password.length >= 8) score += 20;
    if (password.length >= 12) score += 10;
    if (/[A-Z]/.test(password)) score += 20;
    if (/[a-z]/.test(password)) score += 15;
    if (/[0-9]/.test(password)) score += 15;
    if (/[^A-Za-z0-9]/.test(password)) score += 20;
    this.strengthScore.set(score);
    if (score < 40) { this.strengthLabel.set('Weak'); this.strengthColor.set('#ef4444'); }
    else if (score < 70) { this.strengthLabel.set('Fair'); this.strengthColor.set('#f59e0b'); }
    else { this.strengthLabel.set('Strong'); this.strengthColor.set('#10b981'); }
  }

  getSegmentClass(segment: number): string {
    const score = this.strengthScore();
    const filled = Math.ceil(score / 20);
    if (segment > filled) return 'strength-segment';
    if (score < 40) return 'strength-segment filled-1';
    if (score < 70) return 'strength-segment filled-3';
    return 'strength-segment filled-5';
  }
}
