import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthStateService } from '../state/auth.state';
import {
  LoginRequest, AuthResponse, RegisterRequest, RegisterResponse,
  MessageResponse, ValidateTokenResponse, PasswordHintResponse,
  UpdatePasswordHintRequest, TwoFactorStatusResponse, TwoFactorSetupResponse,
  TwoFactorVerifyResponse, BackupCodesResponse, UserProfile, UpdateProfileRequest,
  ChangePasswordRequest, DeleteAccountRequest, UserSettings, UpdateSettingsRequest,
  SecurityQuestion, SetDuressPasswordRequest
} from '../models/auth.models';

const API = 'http://localhost:8080';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private authState = inject(AuthStateService);
  private router = inject(Router);

  // ── Registration ──────────────────────────────────────────

  register(data: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>(`${API}/api/auth/register`, data);
  }

  verifyEmail(username: string, code: string): Observable<MessageResponse> {
    const params = new HttpParams().set('username', username).set('code', code);
    return this.http.post<MessageResponse>(`${API}/api/auth/verify-email`, null, { params });
  }

  resendVerificationOtp(username: string): Observable<MessageResponse> {
    const params = new HttpParams().set('username', username);
    return this.http.post<MessageResponse>(`${API}/api/auth/resend-verification-otp`, null, { params });
  }

  // ── Login ─────────────────────────────────────────────────

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/api/auth/login`, credentials).pipe(
      tap(response => {
        if (response.accessToken && response.refreshToken) {
          this.authState.setTokens(response.accessToken, response.refreshToken);
        }
      })
    );
  }

  verifyOtp(username: string, code: string): Observable<AuthResponse> {
    const params = new HttpParams().set('username', username).set('code', code);
    return this.http.post<AuthResponse>(`${API}/api/auth/verify-otp`, null, { params }).pipe(
      tap(response => {
        if (response.accessToken && response.refreshToken) {
          this.authState.setTokens(response.accessToken, response.refreshToken);
        }
      })
    );
  }

  sendOtp(username: string): Observable<MessageResponse> {
    const params = new HttpParams().set('username', username);
    return this.http.post<MessageResponse>(`${API}/api/auth/send-otp`, null, { params });
  }

  resendOtp(username: string): Observable<MessageResponse> {
    const params = new HttpParams().set('username', username);
    return this.http.post<MessageResponse>(`${API}/api/auth/resend-otp`, null, { params });
  }

  refreshToken(refreshToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${API}/api/auth/refresh-token`, { refreshToken }).pipe(
      tap(response => {
        if (response.accessToken && response.refreshToken) {
          this.authState.setTokens(response.accessToken, response.refreshToken);
        }
      })
    );
  }

  logout(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/auth/logout`, null).pipe(
      tap(() => {
        this.authState.clearAuth();
        this.router.navigate(['/auth/login']);
      })
    );
  }

  validateToken(): Observable<ValidateTokenResponse> {
    return this.http.get<ValidateTokenResponse>(`${API}/api/auth/validate-token`);
  }

  verifyMasterPassword(masterPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/auth/verify-master-password`, { masterPassword });
  }

  // ── Account Recovery ──────────────────────────────────────

  getSecurityQuestions(username: string): Observable<SecurityQuestion[]> {
    return this.http.get<SecurityQuestion[]>(`${API}/api/auth/security-questions/${username}`);
  }

  forgotPassword(email: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/auth/forgot-password`, { email });
  }

  verifySecurityQuestions(username: string, securityQuestions: SecurityQuestion[]): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/auth/verify-security-questions`, { username, securityQuestions });
  }

  resetPassword(username: string, securityQuestions: SecurityQuestion[], newPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/auth/reset-password`, { username, securityQuestions, newPassword });
  }

  // ── Password Hint ─────────────────────────────────────────

  getPasswordHint(username: string): Observable<PasswordHintResponse> {
    return this.http.get<PasswordHintResponse>(`${API}/api/auth/password-hint/${username}`);
  }

  updatePasswordHint(data: UpdatePasswordHintRequest): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${API}/api/auth/password-hint`, data);
  }

  // ── Duress ────────────────────────────────────────────────

  setDuressPassword(data: SetDuressPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/auth/set-duress-password`, data);
  }

  // ── 2FA ───────────────────────────────────────────────────

  get2FAStatus(): Observable<TwoFactorStatusResponse> {
    return this.http.get<TwoFactorStatusResponse>(`${API}/api/2fa/status`);
  }

  setup2FA(): Observable<TwoFactorSetupResponse> {
    return this.http.post<TwoFactorSetupResponse>(`${API}/api/2fa/setup`, null);
  }

  verifySetup2FA(code: string): Observable<TwoFactorVerifyResponse> {
    const params = new HttpParams().set('code', code);
    return this.http.post<TwoFactorVerifyResponse>(`${API}/api/2fa/verify-setup`, null, { params });
  }

  disable2FA(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/2fa/disable`, null);
  }

  getBackupCodes(): Observable<string[]> {
    return this.http.get<string[]>(`${API}/api/2fa/backup-codes`);
  }

  regenerateBackupCodes(): Observable<BackupCodesResponse> {
    return this.http.post<BackupCodesResponse>(`${API}/api/2fa/regenerate-codes`, null);
  }

  // ── User Profile ──────────────────────────────────────────

  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>(`${API}/api/users/profile`).pipe(
      tap(user => this.authState.setUser(user))
    );
  }

  updateProfile(data: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${API}/api/users/profile`, data).pipe(
      tap(user => this.authState.setUser(user))
    );
  }

  changePassword(data: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.put<MessageResponse>(`${API}/api/users/change-password`, data);
  }

  deleteAccount(data: DeleteAccountRequest): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`${API}/api/users/account`, { body: data });
  }

  cancelAccountDeletion(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${API}/api/users/account/cancel-deletion`, null);
  }

  // ── Settings ──────────────────────────────────────────────

  getSettings(): Observable<UserSettings> {
    return this.http.get<UserSettings>(`${API}/api/settings`).pipe(
      tap(settings => this.authState.setSettings(settings))
    );
  }

  updateSettings(data: UpdateSettingsRequest): Observable<UserSettings> {
    return this.http.put<UserSettings>(`${API}/api/settings`, data).pipe(
      tap(settings => this.authState.setSettings(settings))
    );
  }
}
