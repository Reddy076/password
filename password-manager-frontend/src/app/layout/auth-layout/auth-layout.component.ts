import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterOutlet, CommonModule],
  template: `
    <div class="auth-shell">
      <!-- Animated background -->
      <div class="auth-bg">
        <div class="auth-bg-orb orb-1"></div>
        <div class="auth-bg-orb orb-2"></div>
        <div class="auth-bg-orb orb-3"></div>
      </div>

      <!-- Split layout -->
      <div class="auth-container">
        <!-- Left panel — branding -->
        <div class="auth-brand-panel d-none d-lg-flex">
          <div class="brand-content">
            <div class="brand-logo">
              <div class="brand-icon">🔐</div>
              <span class="brand-name">RevaultX</span>
            </div>
            <h1 class="brand-tagline">
              Your passwords,<br>
              <span class="brand-accent">perfectly secured.</span>
            </h1>
            <p class="brand-desc">
              Military-grade encryption. Zero-knowledge architecture.
              Your data stays yours — always.
            </p>
            <!-- Feature pills -->
            <div class="brand-features">
              <div class="feature-pill">
                <span class="feature-icon">🛡️</span>
                <span>AES-256 Encryption</span>
              </div>
              <div class="feature-pill">
                <span class="feature-icon">🔑</span>
                <span>Zero-Knowledge</span>
              </div>
              <div class="feature-pill">
                <span class="feature-icon">📱</span>
                <span>All Devices</span>
              </div>
              <div class="feature-pill">
                <span class="feature-icon">⚡</span>
                <span>Instant Sync</span>
              </div>
            </div>
            <!-- Floating decorative pills -->
            <div class="deco-pills">
              <div class="deco-pill deco-1">github.com</div>
              <div class="deco-pill deco-2">●●●●●●●●●●</div>
              <div class="deco-pill deco-3">Strong 🟢</div>
            </div>
          </div>
        </div>

        <!-- Right panel — form -->
        <div class="auth-form-panel">
          <div class="auth-form-island">
            <router-outlet />
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .auth-shell {
      min-height: 100vh;
      background: var(--bg-primary);
      position: relative;
      overflow: hidden;
    }

    .auth-bg {
      position: fixed;
      inset: 0;
      pointer-events: none;
      z-index: 0;
    }

    .auth-bg-orb {
      position: absolute;
      border-radius: 50%;
      filter: blur(80px);
      opacity: 0.15;
    }

    .orb-1 {
      width: 600px;
      height: 600px;
      background: var(--accent-primary);
      top: -200px;
      left: -100px;
      animation: pillFloat 8s ease-in-out infinite;
    }

    .orb-2 {
      width: 400px;
      height: 400px;
      background: var(--accent-secondary);
      bottom: -100px;
      right: 20%;
      animation: pillFloat 10s ease-in-out infinite reverse;
    }

    .orb-3 {
      width: 300px;
      height: 300px;
      background: var(--accent-info);
      top: 40%;
      left: 30%;
      animation: pillFloat 12s ease-in-out infinite;
    }

    .auth-container {
      position: relative;
      z-index: 1;
      min-height: 100vh;
      display: flex;
    }

    /* ── Brand Panel ── */
    .auth-brand-panel {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 48px;
    }

    .brand-content {
      max-width: 440px;
    }

    .brand-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 40px;
    }

    .brand-icon {
      width: 48px;
      height: 48px;
      border-radius: 14px;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      box-shadow: 0 8px 24px rgba(99, 102, 241, 0.4);
    }

    .brand-name {
      font-size: 28px;
      font-weight: 800;
      color: var(--text-primary);
      letter-spacing: -0.5px;
    }

    .brand-tagline {
      font-size: 42px;
      font-weight: 800;
      color: var(--text-primary);
      line-height: 1.2;
      margin-bottom: 20px;
      letter-spacing: -1px;
    }

    .brand-accent {
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .brand-desc {
      font-size: 16px;
      color: var(--text-secondary);
      line-height: 1.7;
      margin-bottom: 32px;
    }

    .brand-features {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin-bottom: 48px;
    }

    .feature-pill {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-full);
      font-size: 13px;
      color: var(--text-secondary);
      backdrop-filter: blur(10px);
    }

    .feature-icon { font-size: 16px; }

    /* Decorative floating password pills */
    .deco-pills {
      position: relative;
      height: 120px;
    }

    .deco-pill {
      position: absolute;
      padding: 10px 20px;
      background: rgba(255, 255, 255, 0.06);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-full);
      font-size: 13px;
      color: var(--text-secondary);
      backdrop-filter: blur(10px);
      font-family: var(--font-mono);
    }

    .deco-1 { top: 0; left: 0; animation: pillFloat 4s ease-in-out infinite; }
    .deco-2 { top: 30px; left: 120px; animation: pillFloat 5s ease-in-out infinite 1s; }
    .deco-3 { top: 70px; left: 20px; animation: pillFloat 6s ease-in-out infinite 0.5s; }

    /* ── Form Panel ── */
    .auth-form-panel {
      width: 100%;
      max-width: 480px;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px;

      @media (min-width: 992px) {
        padding: 48px;
      }
    }

    .auth-form-island {
      width: 100%;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--island-radius);
      padding: 40px;
      box-shadow: 0 24px 64px rgba(0, 0, 0, 0.4);
      animation: islandEnterTop 400ms var(--spring, cubic-bezier(0.34, 1.56, 0.64, 1)) both;

      @media (max-width: 480px) {
        padding: 28px 20px;
        border-radius: var(--island-radius-sm);
      }
    }
  `]
})
export class AuthLayoutComponent {}
