import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  // ── Auth Layout (no sidebar, public pages) ─────────────────
  {
    path: 'auth',
    loadComponent: () =>
      import('./layout/auth-layout/auth-layout.component').then(m => m.AuthLayoutComponent),
    canActivate: [guestGuard],
    children: [
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login.component').then(m => m.LoginComponent),
        title: 'Sign In — RevaultX'
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register.component').then(m => m.RegisterComponent),
        title: 'Create Account — RevaultX'
      },
      {
        path: 'verify-email',
        loadComponent: () =>
          import('./features/auth/verify-email/verify-email.component').then(m => m.VerifyEmailComponent),
        title: 'Verify Email — RevaultX'
      },
      {
        path: '2fa',
        loadComponent: () =>
          import('./features/auth/two-factor-login/two-factor-login.component').then(m => m.TwoFactorLoginComponent),
        title: 'Two-Factor Authentication — RevaultX'
      },
      {
        path: 'forgot-password',
        loadComponent: () =>
          import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
        title: 'Forgot Password — RevaultX'
      },
      {
        path: 'reset-password',
        loadComponent: () =>
          import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
        title: 'Reset Password — RevaultX'
      },
      { path: '', redirectTo: 'login', pathMatch: 'full' }
    ]
  },

  // ── Main App Layout (with island sidebar + topbar) ──────────
  {
    path: '',
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
        title: 'Dashboard — RevaultX'
      },
      {
        path: 'vault',
        loadComponent: () =>
          import('./features/vault/vault.component').then(m => m.VaultComponent),
        title: 'Vault — RevaultX'
      },
      {
        path: 'generator',
        loadComponent: () =>
          import('./features/generator/generator.component').then(m => m.GeneratorComponent),
        title: 'Password Generator — RevaultX'
      },
      {
        path: 'security',
        loadComponent: () =>
          import('./features/security/security.component').then(m => m.SecurityComponent),
        title: 'Security Center — RevaultX'
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then(m => m.SettingsComponent),
        title: 'Settings — RevaultX'
      },
      {
        path: 'backup',
        loadComponent: () =>
          import('./features/backup/backup.component').then(m => m.BackupComponent),
        title: 'Backup & Export — RevaultX'
      },
      {
        path: 'sharing',
        loadComponent: () =>
          import('./features/sharing/sharing.component').then(m => m.SharingComponent),
        title: 'Secure Sharing — RevaultX'
      },
      {
        path: 'teams',
        loadComponent: () =>
          import('./features/teams/teams.component').then(m => m.TeamsComponent),
        title: 'Teams — RevaultX'
      },
      {
        path: 'files',
        loadComponent: () =>
          import('./features/files/files.component').then(m => m.FilesComponent),
        title: 'File Vault — RevaultX'
      },
      {
        path: 'ai',
        loadComponent: () =>
          import('./features/ai-assistant/ai-assistant.component').then(m => m.AiAssistantComponent),
        title: 'AI Assistant — RevaultX'
      },
      {
        path: 'emergency',
        loadComponent: () =>
          import('./features/emergency/emergency.component').then(m => m.EmergencyComponent),
        title: 'Emergency Access — RevaultX'
      },
      {
        path: 'trash',
        loadComponent: () =>
          import('./features/vault/trash/trash.component').then(m => m.TrashComponent),
        title: 'Trash — RevaultX'
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },

  // ── Public Routes (no auth required) ───────────────────────
  {
    path: 'share/:token',
    loadComponent: () =>
      import('./features/sharing/shared-view/shared-view.component').then(m => m.SharedViewComponent),
    title: 'Shared Password — RevaultX'
  },
  {
    path: 'emergency/vault/:token',
    loadComponent: () =>
      import('./features/emergency/emergency-vault/emergency-vault.component').then(m => m.EmergencyVaultComponent),
    title: 'Emergency Vault Access — RevaultX'
  },

  // ── Fallback ────────────────────────────────────────────────
  { path: '**', redirectTo: 'auth/login' }
];
