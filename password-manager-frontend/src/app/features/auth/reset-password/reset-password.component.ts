import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UiStateService } from '../../../core/state/ui.state';
import { SecurityQuestion } from '../../../core/models/auth.models';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="auth-form-container">
      <div class="auth-form-header">
        <h2 class="auth-form-title">Reset Password</h2>
        <p class="auth-form-subtitle">Answer your security questions to reset your master password</p>
      </div>

      <form class="auth-form" (ngSubmit)="onSubmit()">
        <!-- Security Questions -->
        <ng-container *ngFor="let q of questions; let i = index">
          <div class="question-block">
            <div class="question-text">{{ q.questionText }}</div>
            <div class="pill-input-wrapper" style="margin-top:6px">
              <div class="pill-input-inner">
                <span class="pill-input-icon icon-left">💬</span>
                <input type="text" class="pill-input has-icon-left" placeholder="Your answer"
                  [(ngModel)]="answers[i]" [name]="'answer' + i" required
                  [class.error]="submitted && !answers[i]" />
              </div>
              <span class="pill-input-error" *ngIf="submitted && !answers[i]">Answer is required</span>
            </div>
          </div>
        </ng-container>

        <div class="divider"></div>

        <!-- New Password -->
        <div class="pill-input-wrapper">
          <label class="pill-label">New Master Password</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">🔒</span>
            <input [type]="showPassword ? 'text' : 'password'" class="pill-input has-icon-left has-both"
              placeholder="Enter new master password"
              [(ngModel)]="newPassword" name="newPassword" required
              [class.error]="submitted && !newPassword" />
            <button type="button" class="pill-input-icon icon-right"
              (click)="showPassword = !showPassword" style="background:none;border:none;cursor:pointer;">
              {{ showPassword ? '🙈' : '👁️' }}
            </button>
          </div>
          <span class="pill-input-error" *ngIf="submitted && !newPassword">Password is required</span>
        </div>

        <div class="pill-input-wrapper">
          <label class="pill-label">Confirm New Password</label>
          <div class="pill-input-inner">
            <span class="pill-input-icon icon-left">🔒</span>
            <input [type]="showPassword ? 'text' : 'password'" class="pill-input has-icon-left"
              placeholder="Confirm new master password"
              [(ngModel)]="confirmPassword" name="confirmPassword" required
              [class.error]="submitted && newPassword !== confirmPassword" />
          </div>
          <span class="pill-input-error" *ngIf="submitted && newPassword !== confirmPassword">Passwords do not match</span>
        </div>

        <div class="auth-error" *ngIf="errorMessage">
          <span>⚠️</span> {{ errorMessage }}
        </div>

        <button type="submit" class="pill-btn pill-btn-primary" style="width:100%"
          [class.loading]="loading()" [disabled]="loading()">
          <span class="btn-text">Reset Password</span>
          <span class="btn-spinner" *ngIf="loading()"></span>
        </button>

        <div class="auth-links">
          <a routerLink="/auth/login" class="auth-link">← Back to login</a>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .auth-form-container { padding: 32px; }
    .auth-form-header { text-align: center; margin-bottom: 24px; }
    .auth-form-title { font-size: var(--font-size-xl); font-weight: 700; color: var(--text-primary); margin: 0 0 6px; }
    .auth-form-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }
    .auth-form { display: flex; flex-direction: column; gap: 12px; }
    .question-block { background: var(--bg-elevated); border-radius: var(--pill-radius-md); padding: 12px 16px; }
    .question-text { font-size: var(--font-size-sm); color: var(--text-secondary); font-weight: 500; }
    .auth-error {
      padding: 10px 16px;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-danger);
      font-size: var(--font-size-sm);
      display: flex; align-items: center; gap: 8px;
    }
    .auth-links { display: flex; align-items: center; justify-content: center; }
    .auth-link { font-size: var(--font-size-sm); color: var(--accent-primary); text-decoration: none; &:hover { color: #818cf8; } }
    .divider { height: 1px; background: var(--border-subtle); }
  `]
})
export class ResetPasswordComponent implements OnInit {
  private authService = inject(AuthService);
  private uiState = inject(UiStateService);
  private router = inject(Router);

  username = '';
  questions: SecurityQuestion[] = [];
  answers: string[] = ['', '', ''];
  newPassword = '';
  confirmPassword = '';
  showPassword = false;
  submitted = false;
  loading = signal(false);
  errorMessage = '';

  ngOnInit(): void {
    this.username = sessionStorage.getItem('reset_username') ?? '';
    if (!this.username) {
      this.router.navigate(['/auth/forgot-password']);
      return;
    }
    this.authService.getSecurityQuestions(this.username).subscribe({
      next: (qs) => {
        this.questions = qs;
        this.answers = qs.map(() => '');
      },
      error: () => {
        this.errorMessage = 'Could not load security questions.';
      }
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';

    const allAnswered = this.answers.every(a => a.trim());
    if (!allAnswered || !this.newPassword || this.newPassword !== this.confirmPassword) return;
    if (this.newPassword.length < 8) {
      this.errorMessage = 'Password must be at least 8 characters.';
      return;
    }

    const securityQuestions = this.questions.map((q, i) => ({
      questionText: q.questionText,
      answer: this.answers[i]
    }));

    this.loading.set(true);
    this.authService.resetPassword({
      username: this.username,
      securityQuestions,
      newPassword: this.newPassword
    }).subscribe({
      next: () => {
        this.loading.set(false);
        sessionStorage.removeItem('reset_username');
        this.uiState.showSuccess('Password reset successfully! Please sign in.', 'Password Reset');
        this.router.navigate(['/auth/login']);
      },
      error: (err) => {
        this.loading.set(false);
        this.errorMessage = err?.error?.message ?? 'Reset failed. Check your answers and try again.';
      }
    });
  }
}
