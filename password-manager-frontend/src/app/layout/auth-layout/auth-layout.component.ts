import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, CommonModule],
  template: `
    <div class="auth-shell">
      <!-- Background -->
      <div class="auth-bg">
        <div class="auth-bg-orb orb-1"></div>
        <div class="auth-bg-orb orb-2"></div>
        <div class="auth-bg-orb orb-3"></div>
      </div>

      <!-- Auth Card -->
      <div class="auth-container">
        <!-- Brand -->
        <div class="auth-brand">
          <div class="auth-brand-icon">🔐</div>
          <span class="auth-brand-name">RevaultX</span>
        </div>

        <!-- Content -->
        <div class="auth-card">
          <router-outlet />
        </div>

        <!-- Footer -->
        <div class="auth-footer">
          <span class="text-muted-color" style="font-size: 12px;">
            Secure Password Manager — End-to-End Encrypted
          </span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-shell {
      min-height: 100vh;
      background: var(--bg-primary);
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      overflow: hidden;
      padding: 24px;
    }

    .auth-bg {
      position: fixed;
      inset: 0;
      pointer-events: none;
    }

    .auth-bg-orb {
      position: absolute;
      border-radius: 50%;
      filter: blur(80px);
      opacity: 0.15;
    }

    .orb-1 {
      width: 500px;
      height: 500px;
      background: var(--accent-primary);
      top: -100px;
      left: -100px;
    }

    .orb-2 {
      width: 400px;
      height: 400px;
      background: var(--accent-secondary);
      bottom: -80px;
      right: -80px;
    }

    .orb-3 {
      width: 300px;
      height: 300px;
      background: var(--accent-success);
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
    }

    .auth-container {
      position: relative;
      z-index: 1;
      width: 100%;
      max-width: 440px;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 20px;
    }

    .auth-brand {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .auth-brand-icon {
      width: 44px;
      height: 44px;
      border-radius: 14px;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 22px;
      box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
    }

    .auth-brand-name {
      font-size: 24px;
      font-weight: 800;
      color: var(--text-primary);
      letter-spacing: -0.5px;
    }

    .auth-card {
      width: 100%;
      background: var(--island-bg);
      backdrop-filter: var(--island-blur);
      -webkit-backdrop-filter: var(--island-blur);
      border: var(--island-border);
      border-radius: var(--pill-radius-xl);
      box-shadow: var(--island-shadow);
      overflow: hidden;
    }

    .auth-footer {
      text-align: center;
    }
  `]
})
export class AuthLayoutComponent {}
