import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserControllerService, UserSettingsControllerService, UserResponse, UserSettingsResponse, ChangePasswordRequest, AccountDeletionRequest, SessionControllerService, SessionResponse } from '../../../core/api';
import { TwoFactorAuthControllerService } from '../../../core/api/api/twoFactorAuthController.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { NotificationEventService } from '../../../core/services/notification-event.service';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, LucideAngularModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  private userService = inject(UserControllerService);
  private settingsService = inject(UserSettingsControllerService);
  private twoFactorService = inject(TwoFactorAuthControllerService);
  private sessionService = inject(SessionControllerService);
  private notificationEventService = inject(NotificationEventService);
  private fb = inject(FormBuilder);

  userProfile: UserResponse | null = null;
  userSettings: UserSettingsResponse | null = null;

  isLoadingProfile = true;
  isLoadingSettings = true;
  successMessage = '';
  errorMessage = '';

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]]
  });

  profileForm = this.fb.group({
    name: ['', [Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(100)]],
    phoneNumber: ['', [Validators.maxLength(20)]]
  });

  isEditingProfile = false;

  readOnlyForm = this.fb.group({
    readOnlyMode: [false]
  });

  // 2FA State
  is2FAEnabled = false;
  isLoading2FA = true;
  showSetupStep = false;
  showVerifyStep = false;
  showBackupCodes = false;
  qrCodeUrl = '';
  secretKey = '';
  backupCodes: string[] = [];

  setupCodeForm = this.fb.group({
    verificationCode: ['', [Validators.required, Validators.minLength(6), Validators.maxLength(6)]]
  });

  // Account Deletion State
  private router = inject(Router);
  showDeleteModal = false;
  isDeletionPending = false;
  deletionDate: string | null = null;
  isDeletingAccount = false;
  isCancellingDeletion = false;

  deleteForm = this.fb.group({
    masterPassword: ['', [Validators.required]],
    confirmation: [false, [Validators.requiredTrue]]
  });

  // Session Management State
  activeSessions: SessionResponse[] = [];
  currentSessionId: number | null = null;
  isLoadingSessions = true;

  ngOnInit() {
    this.loadProfile();
    this.loadSettings();
    this.load2FAStatus();
    this.loadSessions();
  }

  private checkDeletionStatus() {
    if (this.userProfile && (this.userProfile as any).deletionScheduledAt) {
      this.isDeletionPending = true;
      const date = new Date((this.userProfile as any).deletionScheduledAt);
      this.deletionDate = date.toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' });
    } else {
      this.isDeletionPending = false;
      this.deletionDate = null;
    }
  }

  loadProfile() {
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.profileForm.patchValue({
          name: profile.name || '',
          email: profile.email || '',
          phoneNumber: profile.phoneNumber || ''
        });
        this.isLoadingProfile = false;
        this.checkDeletionStatus();
      },
      error: () => {
        this.errorMessage = 'Failed to load user profile';
        this.isLoadingProfile = false;
      }
    });
  }

  onEditProfile() {
    this.isEditingProfile = true;
    this.errorMessage = '';
    this.successMessage = '';
  }

  onCancelEditProfile() {
    this.isEditingProfile = false;
    // Reset form to current profile values
    if (this.userProfile) {
      this.profileForm.patchValue({
        name: this.userProfile.name || '',
        email: this.userProfile.email || '',
        phoneNumber: this.userProfile.phoneNumber || ''
      });
    }
    this.errorMessage = '';
    this.successMessage = '';
  }

  onUpdateProfileSubmit() {
    if (this.profileForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const req = {
      name: this.profileForm.value.name as string || undefined,
      email: this.profileForm.value.email as string,
      phoneNumber: this.profileForm.value.phoneNumber as string || undefined
    };

    this.userService.updateProfile(req).subscribe({
      next: (updatedProfile) => {
        this.userProfile = updatedProfile;
        this.isEditingProfile = false;
        this.successMessage = 'Profile updated successfully!';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update profile. Email might be in use.';
      }
    });
  }

  loadSettings() {
    this.settingsService.getSettings().subscribe({
      next: (settings) => {
        this.userSettings = settings;
        this.readOnlyForm.patchValue({
          readOnlyMode: settings.readOnlyMode || false
        });
        this.isLoadingSettings = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load user settings';
        this.isLoadingSettings = false;
      }
    });
  }

  onChangePasswordSubmit() {
    if (this.passwordForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const req: ChangePasswordRequest = {
      oldPassword: this.passwordForm.value.currentPassword as string,
      newPassword: this.passwordForm.value.newPassword as string
    };

    this.userService.changePassword(req).subscribe({
      next: () => {
        this.successMessage = 'Password updated successfully!';
        this.passwordForm.reset();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update password';
      }
    });
  }

  onToggleReadOnly() {
    this.errorMessage = '';
    this.successMessage = '';

    const newStatus = this.readOnlyForm.value.readOnlyMode as boolean;

    this.settingsService.updateSettings({ readOnlyMode: newStatus }).subscribe({
      next: (settings) => {
        this.userSettings = settings;
        this.successMessage = 'Read-only mode updated!';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to update read-only mode';
        this.readOnlyForm.patchValue({ readOnlyMode: !newStatus });
      }
    });
  }

  // --- 2FA Methods ---

  load2FAStatus() {
    this.twoFactorService.getStatus().subscribe({
      next: (status) => {
        this.is2FAEnabled = status.enabled || false;
        this.isLoading2FA = false;
      },
      error: () => {
        this.isLoading2FA = false;
      }
    });
  }

  onStartSetup() {
    this.errorMessage = '';
    this.successMessage = '';
    this.showSetupStep = true;
    this.showVerifyStep = false;

    this.twoFactorService.setup2FA().subscribe({
      next: (response) => {
        this.qrCodeUrl = response.qrCodeUrl || '';
        this.secretKey = response.secretKey || '';
        this.showVerifyStep = true;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to initialize 2FA setup.';
        this.showSetupStep = false;
      }
    });
  }

  onVerifySetup() {
    if (this.setupCodeForm.invalid) return;

    this.errorMessage = '';
    this.successMessage = '';

    const code = this.setupCodeForm.value.verificationCode as string;

    this.twoFactorService.verifySetup(code).subscribe({
      next: (response) => {
        this.is2FAEnabled = true;
        this.backupCodes = response.backupCodes || [];
        this.showSetupStep = false;
        this.showVerifyStep = false;
        this.showBackupCodes = true;
        this.setupCodeForm.reset();
        this.successMessage = '2FA has been enabled successfully! Save your backup codes.';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Invalid verification code. Please try again.';
      }
    });
  }

  onCancelSetup() {
    this.showSetupStep = false;
    this.showVerifyStep = false;
    this.qrCodeUrl = '';
    this.secretKey = '';
    this.setupCodeForm.reset();
  }

  onDisable2FA() {
    this.errorMessage = '';
    this.successMessage = '';

    this.twoFactorService.disable2FA().subscribe({
      next: () => {
        this.is2FAEnabled = false;
        this.backupCodes = [];
        this.showBackupCodes = false;
        this.successMessage = '2FA has been disabled.';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to disable 2FA.';
      }
    });
  }

  onViewBackupCodes() {
    this.errorMessage = '';

    this.twoFactorService.getBackupCodes().subscribe({
      next: (codes) => {
        this.backupCodes = codes;
        this.showBackupCodes = true;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load backup codes.';
      }
    });
  }

  onRegenerateCodes() {
    this.errorMessage = '';
    this.successMessage = '';

    this.twoFactorService.regenerateCodes().subscribe({
      next: (response) => {
        this.backupCodes = response.backupCodes || [];
        this.showBackupCodes = true;
        this.successMessage = 'Backup codes regenerated successfully! Save your new codes.';
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to regenerate backup codes.';
      }
    });
  }

  onHideBackupCodes() {
    this.showBackupCodes = false;
  }

  copyBackupCodes() {
    const codesText = this.backupCodes.join('\n');
    navigator.clipboard.writeText(codesText).then(() => {
      this.successMessage = 'Backup codes copied to clipboard!';
    });
  }

  // --- Account Deletion Methods ---

  onDeleteAccount() {
    this.showDeleteModal = true;
    this.deleteForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }

  onCancelDeleteModal() {
    this.showDeleteModal = false;
    this.deleteForm.reset();
  }

  onConfirmDelete() {
    if (this.deleteForm.invalid) return;

    this.isDeletingAccount = true;
    this.errorMessage = '';
    this.successMessage = '';

    const request: AccountDeletionRequest = {
      masterPassword: this.deleteForm.value.masterPassword as string,
      confirmation: true
    };

    this.userService.deleteAccount(request).subscribe({
      next: (response) => {
        this.isDeletingAccount = false;
        this.showDeleteModal = false;
        this.successMessage = response.message || 'Account scheduled for deletion in 30 days.';
        this.loadProfile();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.isDeletingAccount = false;
        this.errorMessage = err.error?.message || 'Failed to delete account. Please check your password.';
      }
    });
  }

  onCancelDeletion() {
    this.isCancellingDeletion = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.userService.cancelDeletion().subscribe({
      next: (response) => {
        this.isCancellingDeletion = false;
        this.successMessage = response.message || 'Account deletion cancelled.';
        this.loadProfile();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.isCancellingDeletion = false;
        this.errorMessage = err.error?.message || 'Failed to cancel deletion.';
      }
    });
  }

  // --- Session Management Methods ---

  loadSessions() {
    this.isLoadingSessions = true;

    // Load all sessions
    this.sessionService.getActiveSessions().subscribe({
      next: (sessions) => {
        // Sort sessions by lastAccessedAt descending
        this.activeSessions = sessions.sort((a, b) => {
          return new Date(b.lastAccessedAt || 0).getTime() - new Date(a.lastAccessedAt || 0).getTime();
        });
        this.isLoadingSessions = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load active sessions.';
        this.isLoadingSessions = false;
      }
    });

    // Load current session ID
    const token = localStorage.getItem('access_token');
    if (token) {
      this.sessionService.getCurrentSession(`Bearer ${token}`).subscribe({
        next: (session) => {
          this.currentSessionId = session.id || null;
        }
      });
    }
  }

  onRevokeSession(sessionId: number) {
    if (!confirm('Are you sure you want to log out this device?')) return;

    this.errorMessage = '';
    this.successMessage = '';

    this.sessionService.terminateSession(sessionId).subscribe({
      next: () => {
        this.successMessage = 'Device logged out successfully.';
        this.loadSessions();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to log out device.';
      }
    });
  }

  onRevokeAllOtherSessions() {
    if (!confirm('Are you sure you want to log out of all other devices?')) return;

    this.errorMessage = '';
    this.successMessage = '';

    this.sessionService.terminateAllSessions().subscribe({
      next: () => {
        this.successMessage = 'All other devices logged out successfully.';
        this.loadSessions();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to log out other devices.';
      }
    });
  }

  formatDeviceInfo(userAgent: string | undefined): string {
    if (!userAgent) return 'Unknown Device';
    if (!userAgent.includes('Mozilla/')) return userAgent; // If it's already formatted or not a browser UA

    let os = 'Unknown OS';
    let browser = 'Unknown Browser';

    // Basic OS extraction
    if (userAgent.indexOf('Win') !== -1) os = 'Windows';
    else if (userAgent.indexOf('Mac') !== -1) os = 'macOS';
    else if (userAgent.indexOf('X11') !== -1) os = 'UNIX';
    else if (userAgent.indexOf('Linux') !== -1) os = 'Linux';
    else if (userAgent.indexOf('Android') !== -1) os = 'Android';
    else if (userAgent.indexOf('iPhone') !== -1 || userAgent.indexOf('iPad') !== -1) os = 'iOS';

    // Basic Browser extraction
    if (userAgent.indexOf('Edg/') !== -1) browser = 'Edge';
    else if (userAgent.indexOf('Chrome/') !== -1 && userAgent.indexOf('Edg/') === -1) browser = 'Chrome';
    else if (userAgent.indexOf('Safari/') !== -1 && userAgent.indexOf('Chrome/') === -1) browser = 'Safari';
    else if (userAgent.indexOf('Firefox/') !== -1) browser = 'Firefox';
    else if (userAgent.indexOf('Opera/') !== -1 || userAgent.indexOf('OPR/') !== -1) browser = 'Opera';

    return `${os} • ${browser}`;
  }
}
