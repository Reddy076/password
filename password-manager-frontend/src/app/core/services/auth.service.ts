import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  LoginRequest, AuthResponse, RegisterRequest, RegisterResponse,
  VerifyEmailRequest, RefreshTokenRequest, ResetPasswordRequest,
  ForgotPasswordRequest, VerifySecurityQuestionsRequest, PasswordHintResponse,
  UpdatePasswordHintRequest, SetDuressPasswordRequest, ValidateTokenResponse,
  MessageResponse, TwoFactorStatusResponse, TwoFactorSetupResponse,
  TwoFactorVerifyResponse, BackupCodesResponse, UserProfile, UpdateProfileRequest,
  ChangePasswordRequest, DeleteAccountRequest, UserSettings, UpdateSettingsRequest,
  ReadOnlyModeRequest, UserSession, SecurityQuestion
} from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  // ── Authentication ─────────────────────────────────────────
  login(req: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/login', req);
  }

  register(req: RegisterRequest): Observable<RegisterResponse> {
    return this.http.post<RegisterResponse>('/api/auth/register', req);
  }

  verifyEmail(req: VerifyEmailRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/verify-email', req);
  }

  resendVerificationOtp(username: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/resend-verification-otp', { username });
  }

  refreshToken(req: RefreshTokenRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/refresh-token', req);
  }

  logout(): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/logout', {});
  }

  validateToken(): Observable<ValidateTokenResponse> {
    return this.http.get<ValidateTokenResponse>('/api/auth/validate-token');
  }

  // ── Password Recovery ──────────────────────────────────────
  forgotPassword(req: ForgotPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/forgot-password', req);
  }

  getPasswordHint(username: string): Observable<PasswordHintResponse> {
    return this.http.get<PasswordHintResponse>(`/api/auth/password-hint?username=${username}`);
  }

  getSecurityQuestions(username: string): Observable<SecurityQuestion[]> {
    return this.http.get<SecurityQuestion[]>(`/api/auth/security-questions?username=${username}`);
  }

  verifySecurityQuestions(req: VerifySecurityQuestionsRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/verify-security-questions', req);
  }

  resetPassword(req: ResetPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/reset-password', req);
  }

  // ── OTP ────────────────────────────────────────────────────
  sendOtp(username: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/auth/send-otp', { username });
  }

  verifyOtp(username: string, otp: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/auth/verify-otp', { username, otp });
  }

  // ── 2FA ────────────────────────────────────────────────────
  get2FAStatus(): Observable<TwoFactorStatusResponse> {
    return this.http.get<TwoFactorStatusResponse>('/api/2fa/status');
  }

  setup2FA(): Observable<TwoFactorSetupResponse> {
    return this.http.post<TwoFactorSetupResponse>('/api/2fa/setup', {});
  }

  verify2FA(code: string): Observable<TwoFactorVerifyResponse> {
    return this.http.post<TwoFactorVerifyResponse>('/api/2fa/verify', { code });
  }

  disable2FA(code: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/2fa/disable', { code });
  }

  getBackupCodes(): Observable<BackupCodesResponse> {
    return this.http.get<BackupCodesResponse>('/api/2fa/backup-codes');
  }

  regenerateBackupCodes(): Observable<BackupCodesResponse> {
    return this.http.post<BackupCodesResponse>('/api/2fa/backup-codes/regenerate', {});
  }

  // ── User Profile ───────────────────────────────────────────
  getProfile(): Observable<UserProfile> {
    return this.http.get<UserProfile>('/api/user/profile');
  }

  updateProfile(req: UpdateProfileRequest): Observable<UserProfile> {
    return this.http.put<UserProfile>('/api/user/profile', req);
  }

  changePassword(req: ChangePasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/user/change-password', req);
  }

  deleteAccount(req: DeleteAccountRequest): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>('/api/user/account', { body: req });
  }

  updatePasswordHint(req: UpdatePasswordHintRequest): Observable<MessageResponse> {
    return this.http.put<MessageResponse>('/api/user/password-hint', req);
  }

  setDuressPassword(req: SetDuressPasswordRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/user/duress-password', req);
  }

  // ── Settings ───────────────────────────────────────────────
  getSettings(): Observable<UserSettings> {
    return this.http.get<UserSettings>('/api/settings');
  }

  updateSettings(req: UpdateSettingsRequest): Observable<UserSettings> {
    return this.http.put<UserSettings>('/api/settings', req);
  }

  setReadOnlyMode(req: ReadOnlyModeRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/settings/read-only', req);
  }

  // ── Sessions ───────────────────────────────────────────────
  getSessions(): Observable<UserSession[]> {
    return this.http.get<UserSession[]>('/api/sessions');
  }

  revokeSession(sessionId: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`/api/sessions/${sessionId}`);
  }

  revokeAllSessions(): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>('/api/sessions/all');
  }
}
