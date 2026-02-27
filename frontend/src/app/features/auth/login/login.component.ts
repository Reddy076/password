import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthenticationService } from '../../../core/api/api/authentication.service';
import { LoginRequest } from '../../../core/api/model/models';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthenticationService);
  private http = inject(HttpClient);
  private router = inject(Router);

  loginForm = this.fb.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  otpForm = this.fb.group({
    otpCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
  });

  errorMessage = '';
  successMessage = '';
  isLoading = false;
  showOtpStep = false;
  pendingUsername = '';

  onSubmit() {
    if (this.loginForm.invalid) {
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    const req: LoginRequest = {
      username: this.loginForm.value.username as string,
      masterPassword: this.loginForm.value.password as string,
    };

    this.authService.login(req).subscribe({
      next: (response) => {
        if (response.requires2FA === true) {
          this.pendingUsername = response.username || req.username;
          this.showOtpStep = true;
          this.successMessage = 'Enter the 6-digit code from your authenticator app.';
          this.isLoading = false;
          return;
        }

        if (response.accessToken) {
          localStorage.setItem('access_token', response.accessToken);
          if (response.refreshToken) {
            localStorage.setItem('refresh_token', response.refreshToken);
          }
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage = 'Login failed. Please try again.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Invalid username or password';
        this.isLoading = false;
      }
    });
  }

  onSubmitOtp() {
    if (this.otpForm.invalid) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const code = (this.otpForm.get('otpCode')?.value || '').toString().trim();

    if (!code) {
      this.errorMessage = 'Please enter the verification code.';
      this.isLoading = false;
      return;
    }

    this.http.post<any>(`${environment.apiBaseUrl}/api/auth/verify-otp?username=${encodeURIComponent(this.pendingUsername)}&code=${encodeURIComponent(code)}`, null).subscribe({
      next: (response) => {
        if (response.accessToken) {
          localStorage.setItem('access_token', response.accessToken);
          if (response.refreshToken) {
            localStorage.setItem('refresh_token', response.refreshToken);
          }
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage = 'Verification failed. No token received.';
        }
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Invalid verification code. Please try again.';
        this.isLoading = false;
      }
    });
  }

  onResendOtp() {
    this.errorMessage = '';
    this.successMessage = '';
    this.isLoading = true;

    this.http.post<any>(`${environment.apiBaseUrl}/api/auth/resend-otp`, null, {
      params: { username: this.pendingUsername }
    }).subscribe({
      next: () => {
        this.successMessage = 'A new verification code has been sent.';
        this.isLoading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to resend code. Please try again.';
        this.isLoading = false;
      }
    });
  }

  backToLogin() {
    this.showOtpStep = false;
    this.pendingUsername = '';
    this.errorMessage = '';
    this.successMessage = '';
    this.otpForm.reset();
  }
}
