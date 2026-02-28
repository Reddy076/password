import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { AuthStateService } from '../../core/state/auth.state';
import { UiStateService } from '../../core/state/ui.state';
import { UserProfile, UserSettings, UserSession } from '../../core/models/auth.models';

type SettingsTab = 'profile' | 'security' | 'sessions' | 'preferences' | '2fa';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">Settings</h1>
          <p class="page-subtitle">Manage your account and preferences</p>
        </div>
      </div>

      <div class="settings-layout">
        <!-- Sidebar -->
        <aside class="settings-sidebar">
          <button class="settings-nav-item" [class.active]="activeTab() === 'profile'" (click)="activeTab.set('profile')">
            👤 Profile
          </button>
          <button class="settings-nav-item" [class.active]="activeTab() === 'security'" (click)="activeTab.set('security')">
            🔒 Security
          </button>
          <button class="settings-nav-item" [class.active]="activeTab() === '2fa'" (click)="activeTab.set('2fa')">
            📱 Two-Factor Auth
          </button>
          <button class="settings-nav-item" [class.active]="activeTab() === 'sessions'" (click)="activeTab.set('sessions')">
            💻 Sessions
          </button>
          <button class="settings-nav-item" [class.active]="activeTab() === 'preferences'" (click)="activeTab.set('preferences')">
            ⚙️ Preferences
          </button>
        </aside>

        <!-- Content -->
        <div class="settings-content">

          <!-- Profile Tab -->
          <div *ngIf="activeTab() === 'profile'" class="settings-section">
            <h2 class="settings-section-title">Profile Information</h2>

            <div class="profile-avatar-section">
              <div class="profile-avatar">{{ userInitial() }}</div>
              <div>
                <div class="profile-name">{{ profile()?.username ?? 'User' }}</div>
                <div class="profile-email">{{ profile()?.email ?? '' }}</div>
              </div>
            </div>

            <div class="form-grid">
              <div class="pill-input-wrapper">
                <label class="pill-label">Display Name</label>
                <div class="pill-input-inner">
                  <span class="pill-input-icon icon-left">👤</span>
                  <input type="text" class="pill-input has-icon-left" placeholder="Your display name"
                    [(ngModel)]="profileForm.name" name="name" />
                </div>
              </div>
              <div class="pill-input-wrapper">
                <label class="pill-label">Phone Number</label>
                <div class="pill-input-inner">
                  <span class="pill-input-icon icon-left">📱</span>
                  <input type="tel" class="pill-input has-icon-left" placeholder="+1 234 567 8900"
                    [(ngModel)]="profileForm.phoneNumber" name="phone" />
                </div>
              </div>
            </div>

            <button class="pill-btn pill-btn-primary" (click)="saveProfile()"
              [class.loading]="savingProfile()" [disabled]="savingProfile()">
              <span class="btn-text">Save Changes</span>
              <span class="btn-spinner" *ngIf="savingProfile()"></span>
            </button>

            <div class="divider"></div>

            <h3 class="settings-subsection-title">Password Hint</h3>
            <div class="pill-input-wrapper">
              <label class="pill-label">Hint (visible without login)</label>
              <div class="pill-input-inner">
                <span class="pill-input-icon icon-left">💡</span>
                <input type="text" class="pill-input has-icon-left" placeholder="A hint to remember your master password"
                  [(ngModel)]="passwordHint" name="hint" />
              </div>
            </div>
            <div class="pill-input-wrapper">
              <label class="pill-label">Current Master Password (required)</label>
              <div class="pill-input-inner">
                <span class="pill-input-icon icon-left">🔒</span>
                <input [type]="showHintPw ? 'text' : 'password'" class="pill-input has-icon-left has-both"
                  placeholder="Verify your master password"
                  [(ngModel)]="hintMasterPw" name="hintMasterPw" />
                <button type="button" class="pill-input-icon icon-right"
                  (click)="showHintPw = !showHintPw" style="background:none;border:none;cursor:pointer;">
                  {{ showHintPw ? '🙈' : '👁️' }}
                </button>
              </div>
            </div>
            <button class="pill-btn pill-btn-secondary" (click)="updateHint()">Update Hint</button>

            <div class="divider"></div>

            <h3 class="settings-subsection-title danger-zone">⚠️ Danger Zone</h3>
            <div class="danger-card">
              <div>
                <div style="font-weight:600;color:var(--text-primary)">Delete Account</div>
                <div style="font-size:12px;color:var(--text-muted);margin-top:2px">Permanently delete your account and all data. This cannot be undone.</div>
              </div>
              <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="showDeleteModal.set(true)">Delete Account</button>
            </div>
          </div>

          <!-- Security Tab -->
          <div *ngIf="activeTab() === 'security'" class="settings-section">
            <h2 class="settings-section-title">Security Settings</h2>

            <h3 class="settings-subsection-title">Change Master Password</h3>
            <div class="form-stack">
              <div class="pill-input-wrapper">
                <label class="pill-label">Current Password</label>
                <div class="pill-input-inner">
                  <span class="pill-input-icon icon-left">🔒</span>
                  <input [type]="showCurrentPw ? 'text' : 'password'" class="pill-input has-icon-left has-both"
                    placeholder="Current master password"
                    [(ngModel)]="changePwForm.currentPassword" name="currentPw" />
                  <button type="button" class="pill-input-icon icon-right"
                    (click)="showCurrentPw = !showCurrentPw" style="background:none;border:none;cursor:pointer;">
                    {{ showCurrentPw ? '🙈' : '👁️' }}
                  </button>
                </div>
              </div>
              <div class="pill-input-wrapper">
                <label class="pill-label">New Password</label>
                <div class="pill-input-inner">
                  <span class="pill-input-icon icon-left">🔒</span>
                  <input [type]="showNewPw ? 'text' : 'password'" class="pill-input has-icon-left has-both"
                    placeholder="New master password"
                    [(ngModel)]="changePwForm.newPassword" name="newPw" />
                  <button type="button" class="pill-input-icon icon-right"
                    (click)="showNewPw = !showNewPw" style="background:none;border:none;cursor:pointer;">
                    {{ showNewPw ? '🙈' : '👁️' }}
                  </button>
                </div>
              </div>
              <div class="pill-input-wrapper">
                <label class="pill-label">Confirm New Password</label>
                <div class="pill-input-inner">
                  <span class="pill-input-icon icon-left">🔒</span>
                  <input [type]="showNewPw ? 'text' : 'password'" class="pill-input has-icon-left"
                    placeholder="Confirm new password"
                    [(ngModel)]="confirmNewPw" name="confirmNewPw" />
                </div>
              </div>
              <div class="auth-error" *ngIf="changePwError">⚠️ {{ changePwError }}</div>
              <button class="pill-btn pill-btn-primary" (click)="changePassword()"
                [class.loading]="changingPw()" [disabled]="changingPw()">
                <span class="btn-text">Change Password</span>
                <span class="btn-spinner" *ngIf="changingPw()"></span>
              </button>
            </div>

            <div class="divider"></div>

            <h3 class="settings-subsection-title">Duress Password</h3>
            <p style="font-size:13px;color:var(--text-secondary);margin:0 0 12px">
              Set a duress password that shows a decoy vault when used under coercion.
            </p>
            <div class="pill-input-wrapper">
              <label class="pill-label">Duress Password</label>
              <div class="pill-input-inner">
                <span class="pill-input-icon icon-left">🎭</span>
                <input [type]="showDuressPw ? 'text' : 'password'" class="pill-input has-icon-left has-both"
                  placeholder="Set a duress password"
                  [(ngModel)]="duressPassword" name="duressPw" />
                <button type="button" class="pill-input-icon icon-right"
                  (click)="showDuressPw = !showDuressPw" style="background:none;border:none;cursor:pointer;">
                  {{ showDuressPw ? '🙈' : '👁️' }}
                </button>
              </div>
            </div>
            <button class="pill-btn pill-btn-secondary" (click)="setDuressPassword()">Set Duress Password</button>
          </div>

          <!-- 2FA Tab -->
          <div *ngIf="activeTab() === '2fa'" class="settings-section">
            <h2 class="settings-section-title">Two-Factor Authentication</h2>

            <div class="twofa-status-card" [class.enabled]="auth.is2FAEnabled()">
              <div class="twofa-status-icon">{{ auth.is2FAEnabled() ? '✅' : '⚠️' }}</div>
              <div>
                <div style="font-weight:600;color:var(--text-primary)">
                  2FA is {{ auth.is2FAEnabled() ? 'Enabled' : 'Disabled' }}
                </div>
                <div style="font-size:12px;color:var(--text-secondary);margin-top:2px">
                  {{ auth.is2FAEnabled() ? 'Your account is protected with two-factor authentication.' : 'Enable 2FA for extra security.' }}
                </div>
              </div>
              <button class="pill-btn pill-btn-sm"
                [class]="auth.is2FAEnabled() ? 'pill-btn-danger' : 'pill-btn-primary'"
                (click)="auth.is2FAEnabled() ? showDisable2FA.set(true) : setup2FA()">
                {{ auth.is2FAEnabled() ? 'Disable 2FA' : 'Enable 2FA' }}
              </button>
            </div>

            <!-- Setup 2FA -->
            <div class="twofa-setup" *ngIf="twoFASetup() && !auth.is2FAEnabled()">
              <div class="qr-container">
                <img [src]="twoFASetup()!.qrCodeImage" alt="QR Code" class="qr-code" />
              </div>
              <div class="setup-instructions">
                <p>1. Scan this QR code with your authenticator app (Google Authenticator, Authy, etc.)</p>
                <p>2. Or manually enter this secret key:</p>
                <div class="secret-key">
                  <code>{{ twoFASetup()!.secret }}</code>
                  <button class="icon-btn" (click)="copyText(twoFASetup()!.secret)">📋</button>
                </div>
                <p>3. Enter the 6-digit code from your app:</p>
                <div class="pill-input-wrapper">
                  <div class="pill-input-inner">
                    <input type="text" class="pill-input" placeholder="000000" maxlength="6"
                      [(ngModel)]="verifyCode" name="verifyCode"
                      style="letter-spacing:6px;font-size:20px;text-align:center" />
                  </div>
                </div>
                <button class="pill-btn pill-btn-primary" (click)="verify2FA()"
                  [class.loading]="verifying2FA()" [disabled]="verifying2FA()">
                  <span class="btn-text">Verify & Enable</span>
                  <span class="btn-spinner" *ngIf="verifying2FA()"></span>
                </button>
              </div>
            </div>

            <!-- Backup Codes -->
            <div class="backup-codes-section" *ngIf="auth.is2FAEnabled()">
              <h3 class="settings-subsection-title">Backup Codes</h3>
              <p style="font-size:13px;color:var(--text-secondary);margin:0 0 12px">
                Use these codes if you lose access to your authenticator app. Each code can only be used once.
              </p>
              <button class="pill-btn pill-btn-secondary pill-btn-sm" (click)="loadBackupCodes()">
                View Backup Codes
              </button>
              <div class="backup-codes-grid" *ngIf="backupCodes().length > 0">
                <code class="backup-code" *ngFor="let code of backupCodes()">{{ code }}</code>
              </div>
            </div>
          </div>

          <!-- Sessions Tab -->
          <div *ngIf="activeTab() === 'sessions'" class="settings-section">
            <div class="flex-between" style="margin-bottom:16px">
              <h2 class="settings-section-title" style="margin:0">Active Sessions</h2>
              <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="revokeAllSessions()">
                Revoke All
              </button>
            </div>

            <div class="sessions-list">
              <div class="session-card" *ngFor="let session of sessions()">
                <div class="session-icon">💻</div>
                <div class="session-info">
                  <div class="session-device">{{ session.deviceInfo | slice:0:60 }}</div>
                  <div class="session-meta">
                    {{ session.ipAddress }} · {{ session.location ?? 'Unknown location' }}
                  </div>
                  <div class="session-time">
                    Last active: {{ formatDate(session.lastAccessedAt) }}
                  </div>
                </div>
                <div class="session-actions">
                  <span class="pill-badge badge-success" *ngIf="session.isActive">Active</span>
                  <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="revokeSession(session)">
                    Revoke
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- Preferences Tab -->
          <div *ngIf="activeTab() === 'preferences'" class="settings-section">
            <h2 class="settings-section-title">Preferences</h2>

            <div class="pref-group">
              <h3 class="settings-subsection-title">Theme</h3>
              <div class="theme-options">
                <button class="theme-btn" [class.active]="prefsForm.theme === 'DARK'" (click)="prefsForm.theme = 'DARK'">
                  🌙 Dark
                </button>
                <button class="theme-btn" [class.active]="prefsForm.theme === 'LIGHT'" (click)="prefsForm.theme = 'LIGHT'">
                  ☀️ Light
                </button>
                <button class="theme-btn" [class.active]="prefsForm.theme === 'SYSTEM'" (click)="prefsForm.theme = 'SYSTEM'">
                  💻 System
                </button>
              </div>
            </div>

            <div class="pref-group">
              <h3 class="settings-subsection-title">Auto-Logout</h3>
              <div class="pill-input-wrapper" style="max-width:200px">
                <label class="pill-label">Timeout (minutes)</label>
                <input type="number" class="pill-input" min="1" max="1440"
                  [(ngModel)]="prefsForm.autoLogoutMinutes" name="autoLogout" />
              </div>
            </div>

            <div class="pref-group">
              <h3 class="settings-subsection-title">Read-Only Mode</h3>
              <label class="pill-toggle">
                <input type="checkbox" [(ngModel)]="readOnlyMode" name="readOnly" (ngModelChange)="toggleReadOnly($event)" />
                <span class="toggle-track"></span>
                <span class="toggle-label">Enable read-only mode (prevents accidental changes)</span>
              </label>
            </div>

            <button class="pill-btn pill-btn-primary" (click)="savePreferences()"
              [class.loading]="savingPrefs()" [disabled]="savingPrefs()">
              <span class="btn-text">Save Preferences</span>
              <span class="btn-spinner" *ngIf="savingPrefs()"></span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Delete Account Modal -->
    <div class="modal-overlay" *ngIf="showDeleteModal()" (click)="showDeleteModal.set(false)">
      <div class="modal-panel modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">⚠️ Delete Account</h3>
          <button class="icon-btn" (click)="showDeleteModal.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <p style="font-size:13px;color:var(--accent-danger);margin:0 0 16px">
            This will permanently delete your account and ALL your passwords. This cannot be undone!
          </p>
          <div class="pill-input-wrapper">
            <label class="pill-label">Enter master password to confirm</label>
            <div class="pill-input-inner">
              <input type="password" class="pill-input" placeholder="Master password"
                [(ngModel)]="deleteConfirmPw" name="deletePw" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showDeleteModal.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-danger" (click)="deleteAccount()"
            [class.loading]="deletingAccount()" [disabled]="deletingAccount()">
            <span class="btn-text">Delete Forever</span>
            <span class="btn-spinner" *ngIf="deletingAccount()"></span>
          </button>
        </div>
      </div>
    </div>

    <!-- Disable 2FA Modal -->
    <div class="modal-overlay" *ngIf="showDisable2FA()" (click)="showDisable2FA.set(false)">
      <div class="modal-panel modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Disable 2FA</h3>
          <button class="icon-btn" (click)="showDisable2FA.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <div class="pill-input-wrapper">
            <label class="pill-label">Enter 2FA code to confirm</label>
            <input type="text" class="pill-input" placeholder="000000" maxlength="6"
              [(ngModel)]="disable2FACode" name="disable2fa"
              style="letter-spacing:6px;font-size:20px;text-align:center" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showDisable2FA.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-danger" (click)="disable2FA()">Disable 2FA</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .settings-layout {
      display: grid;
      grid-template-columns: 200px 1fr;
      gap: 20px;
      padding: 20px 28px 24px;
      @media (max-width: 767px) { grid-template-columns: 1fr; }
    }

    .settings-sidebar {
      display: flex; flex-direction: column; gap: 2px;
      @media (max-width: 767px) { flex-direction: row; flex-wrap: wrap; }
    }
    .settings-nav-item {
      display: flex; align-items: center; gap: 8px;
      padding: 10px 14px;
      border-radius: var(--pill-radius-xs);
      background: none; border: none; cursor: pointer;
      font-size: 13px; color: var(--text-secondary);
      text-align: left; transition: all var(--transition-fast);
      &:hover { background: var(--bg-hover); color: var(--text-primary); }
      &.active { background: var(--bg-active); color: var(--accent-primary); font-weight: 600; }
    }

    .settings-content {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl);
      overflow: hidden;
    }

    .settings-section { padding: 24px 28px; display: flex; flex-direction: column; gap: 16px; }
    .settings-section-title { font-size: 18px; font-weight: 700; color: var(--text-primary); margin: 0; }
    .settings-subsection-title { font-size: 14px; font-weight: 600; color: var(--text-secondary); margin: 0; }
    .settings-subsection-title.danger-zone { color: var(--accent-danger); }

    .profile-avatar-section { display: flex; align-items: center; gap: 16px; }
    .profile-avatar {
      width: 60px; height: 60px; border-radius: 50%;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex; align-items: center; justify-content: center;
      font-size: 24px; font-weight: 700; color: #fff; flex-shrink: 0;
    }
    .profile-name { font-size: 16px; font-weight: 700; color: var(--text-primary); }
    .profile-email { font-size: 13px; color: var(--text-muted); }

    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; @media (max-width: 600px) { grid-template-columns: 1fr; } }
    .form-stack { display: flex; flex-direction: column; gap: 12px; }

    .danger-card {
      display: flex; align-items: center; justify-content: space-between; gap: 16px;
      padding: 16px 20px;
      background: rgba(239,68,68,0.05);
      border: 1px solid rgba(239,68,68,0.2);
      border-radius: var(--pill-radius-lg);
    }

    .twofa-status-card {
      display: flex; align-items: center; gap: 16px;
      padding: 16px 20px;
      border-radius: var(--pill-radius-lg);
      border: 2px solid var(--border-default);
      &.enabled { border-color: var(--accent-success); background: rgba(16,185,129,0.05); }
    }
    .twofa-status-icon { font-size: 28px; flex-shrink: 0; }

    .twofa-setup { display: flex; gap: 24px; flex-wrap: wrap; }
    .qr-container { flex-shrink: 0; }
    .qr-code { width: 160px; height: 160px; border-radius: var(--pill-radius-md); border: 2px solid var(--border-default); }
    .setup-instructions { flex: 1; display: flex; flex-direction: column; gap: 8px; font-size: 13px; color: var(--text-secondary); p { margin: 0; } }
    .secret-key {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 12px;
      background: var(--bg-elevated);
      border-radius: var(--pill-radius-md);
      code { font-family: var(--font-mono); font-size: 13px; color: var(--accent-primary); word-break: break-all; }
    }

    .backup-codes-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); gap: 8px; margin-top: 8px; }
    .backup-code {
      padding: 8px 12px;
      background: var(--bg-elevated);
      border-radius: var(--pill-radius-md);
      font-family: var(--font-mono); font-size: 13px; color: var(--text-primary);
      text-align: center;
    }

    .sessions-list { display: flex; flex-direction: column; gap: 8px; }
    .session-card {
      display: flex; align-items: flex-start; gap: 12px;
      padding: 14px 16px;
      background: var(--bg-elevated);
      border-radius: var(--pill-radius-lg);
      border: 1px solid var(--border-subtle);
    }
    .session-icon { font-size: 24px; flex-shrink: 0; }
    .session-info { flex: 1; }
    .session-device { font-size: 13px; font-weight: 600; color: var(--text-primary); }
    .session-meta { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
    .session-time { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
    .session-actions { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; flex-shrink: 0; }

    .pref-group { display: flex; flex-direction: column; gap: 10px; }
    .theme-options { display: flex; gap: 8px; }
    .theme-btn {
      padding: 8px 16px;
      border-radius: var(--pill-radius-full);
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      cursor: pointer; font-size: 13px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--accent-primary); color: var(--text-primary); }
      &.active { background: var(--bg-active); border-color: var(--accent-primary); color: var(--accent-primary); font-weight: 600; }
    }

    .auth-error {
      padding: 10px 16px;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-danger);
      font-size: var(--font-size-sm);
    }

    .modal-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.6);
      backdrop-filter: blur(4px);
      display: flex; align-items: center; justify-content: center;
      z-index: var(--z-modal); padding: 20px;
    }
    .modal-panel {
      background: var(--bg-surface);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-xl);
      width: 100%; max-width: 560px;
      display: flex; flex-direction: column;
      box-shadow: 0 24px 64px rgba(0,0,0,0.5);
    }
    .modal-sm { max-width: 400px; }
    .modal-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 20px 24px; border-bottom: 1px solid var(--border-subtle);
    }
    .modal-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0; }
    .modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 14px; }
    .modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-subtle); display: flex; justify-content: flex-end; gap: 10px; }
    .icon-btn {
      width: 28px; height: 28px; border-radius: var(--pill-radius-xs);
      background: none; border: none; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      font-size: 14px; color: var(--text-muted);
      transition: all var(--transition-fast);
      &:hover { background: var(--bg-hover); color: var(--text-primary); }
    }
  `]
})
export class SettingsComponent implements OnInit {
  private authService = inject(AuthService);
  auth = inject(AuthStateService);
  private uiState = inject(UiStateService);

  activeTab = signal<SettingsTab>('profile');
  profile = signal<UserProfile | null>(null);
  sessions = signal<UserSession[]>([]);
  backupCodes = signal<string[]>([]);
  twoFASetup = signal<{ secret: string; qrCodeUrl: string; qrCodeImage: string } | null>(null);

  savingProfile = signal(false);
  changingPw = signal(false);
  savingPrefs = signal(false);
  verifying2FA = signal(false);
  deletingAccount = signal(false);

  showDeleteModal = signal(false);
  showDisable2FA = signal(false);

  profileForm = { name: '', phoneNumber: '' };
  changePwForm = { currentPassword: '', newPassword: '' };
  confirmNewPw = '';
  changePwError = '';
  prefsForm: { theme: 'DARK' | 'LIGHT' | 'SYSTEM'; autoLogoutMinutes: number } = { theme: 'DARK', autoLogoutMinutes: 30 };
  readOnlyMode = false;
  passwordHint = '';
  hintMasterPw = '';
  duressPassword = '';
  verifyCode = '';
  disable2FACode = '';
  deleteConfirmPw = '';
  showCurrentPw = false;
  showNewPw = false;
  showHintPw = false;
  showDuressPw = false;

  userInitial() {
    const name = this.auth.username() ?? 'U';
    return name.charAt(0).toUpperCase();
  }

  ngOnInit(): void {
    this.loadProfile();
    this.loadSessions();
    this.loadSettings();
  }

  loadProfile(): void {
    this.authService.getProfile().subscribe({
      next: (p) => {
        this.profile.set(p);
        this.auth.setUser(p);
        this.profileForm.name = p.name ?? '';
        this.profileForm.phoneNumber = p.phoneNumber ?? '';
      },
      error: () => {}
    });
  }

  loadSessions(): void {
    this.authService.getSessions().subscribe({ next: s => this.sessions.set(s), error: () => {} });
  }

  loadSettings(): void {
    this.authService.getSettings().subscribe({
      next: (s) => {
        this.auth.setSettings(s);
        this.prefsForm.theme = s.theme;
        this.prefsForm.autoLogoutMinutes = s.autoLogoutMinutes;
        this.readOnlyMode = s.readOnlyMode;
      },
      error: () => {}
    });
  }

  saveProfile(): void {
    this.savingProfile.set(true);
    this.authService.updateProfile(this.profileForm).subscribe({
      next: (p) => {
        this.savingProfile.set(false);
        this.profile.set(p);
        this.auth.setUser(p);
        this.uiState.showSuccess('Profile updated.', 'Saved');
      },
      error: () => { this.savingProfile.set(false); this.uiState.showError('Failed to update profile.'); }
    });
  }

  updateHint(): void {
    if (!this.hintMasterPw) { this.uiState.showError('Master password required.'); return; }
    this.authService.updatePasswordHint({ hint: this.passwordHint, masterPassword: this.hintMasterPw }).subscribe({
      next: () => { this.uiState.showSuccess('Password hint updated.', 'Saved'); this.hintMasterPw = ''; },
      error: () => this.uiState.showError('Failed to update hint.')
    });
  }

  changePassword(): void {
    this.changePwError = '';
    if (!this.changePwForm.currentPassword || !this.changePwForm.newPassword) {
      this.changePwError = 'All fields are required.'; return;
    }
    if (this.changePwForm.newPassword !== this.confirmNewPw) {
      this.changePwError = 'Passwords do not match.'; return;
    }
    this.changingPw.set(true);
    this.authService.changePassword(this.changePwForm).subscribe({
      next: () => {
        this.changingPw.set(false);
        this.changePwForm = { currentPassword: '', newPassword: '' };
        this.confirmNewPw = '';
        this.uiState.showSuccess('Password changed successfully.', 'Changed');
      },
      error: (err) => {
        this.changingPw.set(false);
        this.changePwError = err?.error?.message ?? 'Failed to change password.';
      }
    });
  }

  setDuressPassword(): void {
    if (!this.duressPassword) return;
    this.authService.setDuressPassword({ duressPassword: this.duressPassword }).subscribe({
      next: () => { this.uiState.showSuccess('Duress password set.', 'Saved'); this.duressPassword = ''; },
      error: () => this.uiState.showError('Failed to set duress password.')
    });
  }

  setup2FA(): void {
    this.authService.setup2FA().subscribe({
      next: (setup) => this.twoFASetup.set(setup),
      error: () => this.uiState.showError('Failed to setup 2FA.')
    });
  }

  verify2FA(): void {
    if (!this.verifyCode) return;
    this.verifying2FA.set(true);
    this.authService.verify2FA(this.verifyCode).subscribe({
      next: (res) => {
        this.verifying2FA.set(false);
        if (res.success) {
          this.auth.is2FAEnabled.set(true);
          this.twoFASetup.set(null);
          this.uiState.showSuccess('2FA enabled successfully!', '2FA Enabled');
          if (res.backupCodes?.length) this.backupCodes.set(res.backupCodes);
        }
      },
      error: () => { this.verifying2FA.set(false); this.uiState.showError('Invalid code. Please try again.'); }
    });
  }

  disable2FA(): void {
    if (!this.disable2FACode) return;
    this.authService.disable2FA(this.disable2FACode).subscribe({
      next: () => {
        this.auth.is2FAEnabled.set(false);
        this.showDisable2FA.set(false);
        this.disable2FACode = '';
        this.uiState.showSuccess('2FA disabled.', '2FA Disabled');
      },
      error: () => this.uiState.showError('Invalid code.')
    });
  }

  loadBackupCodes(): void {
    this.authService.getBackupCodes().subscribe({
      next: (res) => this.backupCodes.set(res.codes),
      error: () => this.uiState.showError('Failed to load backup codes.')
    });
  }

  revokeSession(session: UserSession): void {
    this.authService.revokeSession(session.id).subscribe({
      next: () => {
        this.sessions.update(list => list.filter(s => s.id !== session.id));
        this.uiState.showSuccess('Session revoked.', 'Revoked');
      },
      error: () => this.uiState.showError('Failed to revoke session.')
    });
  }

  revokeAllSessions(): void {
    if (!confirm('Revoke all other sessions?')) return;
    this.authService.revokeAllSessions().subscribe({
      next: () => { this.loadSessions(); this.uiState.showSuccess('All sessions revoked.', 'Revoked'); },
      error: () => this.uiState.showError('Failed to revoke sessions.')
    });
  }

  savePreferences(): void {
    this.savingPrefs.set(true);
    this.authService.updateSettings(this.prefsForm).subscribe({
      next: (s) => {
        this.savingPrefs.set(false);
        this.auth.setSettings(s);
        this.uiState.showSuccess('Preferences saved.', 'Saved');
      },
      error: () => { this.savingPrefs.set(false); this.uiState.showError('Failed to save preferences.'); }
    });
  }

  toggleReadOnly(value: boolean): void {
    this.authService.setReadOnlyMode({ readOnlyMode: value }).subscribe({
      next: () => this.uiState.showInfo(`Read-only mode ${value ? 'enabled' : 'disabled'}.`),
      error: () => {}
    });
  }

  deleteAccount(): void {
    if (!this.deleteConfirmPw) return;
    this.deletingAccount.set(true);
    this.authService.deleteAccount({ masterPassword: this.deleteConfirmPw }).subscribe({
      next: () => {
        this.auth.clearAuth();
        window.location.href = '/auth/login';
      },
      error: (err) => {
        this.deletingAccount.set(false);
        this.uiState.showError(err?.error?.message ?? 'Failed to delete account.');
      }
    });
  }

  copyText(text: string): void {
    navigator.clipboard.writeText(text);
    this.uiState.showSuccess('Copied!', 'Copied');
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString();
  }
}
