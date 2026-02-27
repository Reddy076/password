// ============================================================
// AUTH MODELS — TypeScript interfaces for all auth API responses
// ============================================================

export interface LoginRequest {
  username: string;
  masterPassword: string;
  captchaToken?: string;
}

export interface AuthResponse {
  accessToken: string | null;
  refreshToken: string | null;
  username: string;
  expiresIn: number;
  requires2FA: boolean;
  message: string | null;
}

export interface RegisterRequest {
  email: string;
  username: string;
  masterPassword: string;
  passwordHint?: string;
  securityQuestions: SecurityQuestion[];
}

export interface RegisterResponse {
  id: number;
  email: string;
  username: string;
  is2faEnabled: boolean;
  createdAt: string;
}

export interface SecurityQuestion {
  questionText: string;
  answer: string;
}

export interface VerifyEmailRequest {
  username: string;
  code: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface VerifyMasterPasswordRequest {
  masterPassword: string;
}

export interface ResetPasswordRequest {
  username: string;
  securityQuestions: SecurityQuestion[];
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface VerifySecurityQuestionsRequest {
  username: string;
  securityQuestions: SecurityQuestion[];
}

export interface PasswordHintResponse {
  hint: string;
}

export interface UpdatePasswordHintRequest {
  hint: string;
  masterPassword: string;
}

export interface SetDuressPasswordRequest {
  duressPassword: string;
}

export interface ValidateTokenResponse {
  valid: boolean;
}

export interface MessageResponse {
  message: string;
}

// ── 2FA Models ────────────────────────────────────────────────

export interface TwoFactorStatusResponse {
  enabled: boolean;
}

export interface TwoFactorSetupResponse {
  secret: string;
  qrCodeUrl: string;
  qrCodeImage: string;
}

export interface TwoFactorVerifyResponse {
  success: boolean;
  message: string;
  backupCodes: string[];
}

export interface BackupCodesResponse {
  success: boolean;
  message: string;
  codes: string[];
}

// ── User Profile ──────────────────────────────────────────────

export interface UserProfile {
  id: number;
  email: string;
  username: string;
  name: string | null;
  phoneNumber: string | null;
  is2faEnabled: boolean;
  createdAt: string;
}

export interface UpdateProfileRequest {
  name?: string;
  phoneNumber?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface DeleteAccountRequest {
  masterPassword: string;
}

// ── Settings ──────────────────────────────────────────────────

export interface UserSettings {
  id: number;
  theme: 'DARK' | 'LIGHT' | 'SYSTEM';
  language: string;
  autoLogoutMinutes: number;
  readOnlyMode: boolean;
}

export interface UpdateSettingsRequest {
  theme?: 'DARK' | 'LIGHT' | 'SYSTEM';
  language?: string;
  autoLogoutMinutes?: number;
}

export interface ReadOnlyModeRequest {
  readOnlyMode: boolean;
}

// ── Sessions ──────────────────────────────────────────────────

export interface UserSession {
  id: number;
  deviceInfo: string;
  ipAddress: string;
  location: string | null;
  isActive: boolean;
  createdAt: string;
  lastAccessedAt: string;
  expiresAt: string;
}
