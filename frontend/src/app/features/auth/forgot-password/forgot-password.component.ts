import { Component, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthenticationService } from '../../../core/api/api/authentication.service';
import { RecoveryRequest } from '../../../core/api/model/models';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthenticationService);
  private router = inject(Router);

  currentStep = 1; // 1 = username, 2 = security questions, 3 = new password

  // Step 1: Username
  usernameForm = this.fb.group({
    username: ['', Validators.required]
  });

  // Step 2: Security Questions
  securityForm = this.fb.group({
    securityAnswers: this.fb.array([
      this.createAnswerGroup(),
      this.createAnswerGroup(),
      this.createAnswerGroup()
    ])
  });

  // Step 3: New Password
  passwordForm = this.fb.group({
    newMasterPassword: ['', [Validators.required, Validators.minLength(12)]],
    confirmPassword: ['', [Validators.required]]
  });

  errorMessage = '';
  successMessage = '';
  isLoading = false;

  get securityAnswers() {
    return this.securityForm.get('securityAnswers') as FormArray;
  }

  private createAnswerGroup(): FormGroup {
    return this.fb.group({
      question: ['', Validators.required],
      answer: ['', Validators.required]
    });
  }

  goToStep(step: number) {
    this.errorMessage = '';
    this.currentStep = step;
  }

  onSubmitUsername() {
    if (this.usernameForm.invalid) return;
    this.goToStep(2);
  }

  onSubmitRecovery() {
    if (this.securityForm.invalid || this.passwordForm.invalid) return;

    // Check passwords match
    if (this.passwordForm.value.newMasterPassword !== this.passwordForm.value.confirmPassword) {
      this.errorMessage = 'New passwords do not match.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const req: RecoveryRequest = {
      username: this.usernameForm.value.username as string,
      securityAnswers: this.securityForm.value.securityAnswers as any[],
      newMasterPassword: this.passwordForm.value.newMasterPassword as string
    };

    this.authService.resetPassword(req).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMessage = 'Password has been reset successfully! Redirecting to login...';
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Recovery failed. Please verify your answers and try again.';
        this.isLoading = false;
      }
    });
  }
}
