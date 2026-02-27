import { Injectable, signal, computed } from '@angular/core';
import { UserProfile, UserSettings } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class AuthStateService {
  // ── Signals ────────────────────────────────────────────────
  readonly currentUser = signal<UserProfile | null>(null);
  readonly accessToken = signal<string | null>(null);  // In-memory only — never localStorage
  readonly refreshToken = signal<string | null>(null); // localStorage
  readonly is2FAEnabled = signal<boolean>(false);
  readonly isReadOnly = signal<boolean>(false);
  readonly settings = signal<UserSettings | null>(null);

  // ── Computed ───────────────────────────────────────────────
  readonly isAuthenticated = computed(() => !!this.accessToken());
  readonly username = computed(() => this.currentUser()?.username ?? null);
  readonly theme = computed(() => this.settings()?.theme ?? 'DARK');

  constructor() {
    // Restore refresh token from localStorage on init
    const stored = localStorage.getItem('rv_refresh_token');
    if (stored) {
      this.refreshToken.set(stored);
    }
  }

  setTokens(accessToken: string, refreshToken: string): void {
    this.accessToken.set(accessToken);
    this.refreshToken.set(refreshToken);
    localStorage.setItem('rv_refresh_token', refreshToken);
  }

  setUser(user: UserProfile): void {
    this.currentUser.set(user);
    this.is2FAEnabled.set(user.is2faEnabled);
  }

  setSettings(settings: UserSettings): void {
    this.settings.set(settings);
    this.isReadOnly.set(settings.readOnlyMode);
    this.applyTheme(settings.theme);
  }

  clearAuth(): void {
    this.accessToken.set(null);
    this.refreshToken.set(null);
    this.currentUser.set(null);
    this.is2FAEnabled.set(false);
    this.isReadOnly.set(false);
    localStorage.removeItem('rv_refresh_token');
  }

  private applyTheme(theme: 'DARK' | 'LIGHT' | 'SYSTEM'): void {
    const root = document.documentElement;
    if (theme === 'SYSTEM') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      root.setAttribute('data-theme', prefersDark ? 'dark' : 'light');
    } else {
      root.setAttribute('data-theme', theme.toLowerCase());
    }
  }
}
