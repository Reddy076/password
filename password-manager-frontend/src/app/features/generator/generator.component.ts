import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VaultService } from '../../core/services/vault.service';
import { UiStateService } from '../../core/state/ui.state';
import { PasswordStrengthResponse } from '../../core/models/vault.models';

@Component({
  selector: 'app-generator',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">Password Generator</h1>
          <p class="page-subtitle">Generate strong, secure passwords</p>
        </div>
      </div>

      <div class="generator-layout">
        <!-- Generator Panel -->
        <div class="generator-panel">
          <!-- Generated Password Display -->
          <div class="password-display">
            <div class="password-value" [class.empty]="!generatedPassword()">
              {{ generatedPassword() || 'Click Generate to create a password' }}
            </div>
            <div class="password-actions">
              <button class="icon-action-btn" (click)="copyPassword()" title="Copy" [disabled]="!generatedPassword()">
                📋
              </button>
              <button class="icon-action-btn" (click)="generate()" title="Regenerate">
                🔄
              </button>
            </div>
          </div>

          <!-- Strength Meter -->
          <div class="strength-meter" *ngIf="strengthResult()">
            <div class="strength-bar">
              <div class="strength-segment" *ngFor="let i of [1,2,3,4,5]"
                [class]="'strength-segment ' + (i <= getStrengthScore(strengthResult()!.label) ? 'filled-' + getStrengthScore(strengthResult()!.label) : '')"></div>
            </div>
            <div class="strength-label">
              <span class="label-text" [style.color]="getStrengthColor(strengthResult()!.label)">
                {{ strengthResult()!.label.replace('_', ' ') }}
              </span>
              <span class="label-score">Score: {{ strengthResult()!.score }}/100</span>
            </div>
          </div>

          <!-- Strength Details -->
          <div class="strength-details" *ngIf="strengthResult()">
            <div class="strength-detail-item">
              <span class="detail-label">Entropy</span>
              <span class="detail-value">{{ strengthResult()!.entropy.toFixed(1) }} bits</span>
            </div>
            <div class="strength-detail-item">
              <span class="detail-label">Crack Time</span>
              <span class="detail-value">{{ strengthResult()!.crackTime }}</span>
            </div>
          </div>

          <!-- Feedback -->
          <div class="feedback-list" *ngIf="strengthResult()?.feedback?.length">
            <div class="feedback-item" *ngFor="let tip of strengthResult()!.feedback">
              💡 {{ tip }}
            </div>
          </div>

          <!-- Generate Button -->
          <button class="pill-btn pill-btn-primary" style="width:100%;margin-top:8px"
            (click)="generate()" [class.loading]="generating()">
            <span class="btn-text">⚡ Generate Password</span>
            <span class="btn-spinner" *ngIf="generating()"></span>
          </button>
        </div>

        <!-- Settings Panel -->
        <div class="settings-panel">
          <h3 class="settings-title">Generator Settings</h3>

          <!-- Length -->
          <div class="setting-group">
            <div class="setting-header">
              <label class="setting-label">Length</label>
              <span class="setting-value">{{ length }}</span>
            </div>
            <input type="range" class="range-slider" min="4" max="128" [(ngModel)]="length" (ngModelChange)="onSettingChange()" />
            <div class="range-labels">
              <span>4</span>
              <span>128</span>
            </div>
          </div>

          <!-- Character Types -->
          <div class="setting-group">
            <div class="setting-label" style="margin-bottom:10px">Character Types</div>
            <div class="char-options">
              <label class="char-option" [class.active]="includeUppercase">
                <input type="checkbox" [(ngModel)]="includeUppercase" (ngModelChange)="onSettingChange()" />
                <span class="char-preview">ABC</span>
                <span>Uppercase</span>
              </label>
              <label class="char-option" [class.active]="includeLowercase">
                <input type="checkbox" [(ngModel)]="includeLowercase" (ngModelChange)="onSettingChange()" />
                <span class="char-preview">abc</span>
                <span>Lowercase</span>
              </label>
              <label class="char-option" [class.active]="includeNumbers">
                <input type="checkbox" [(ngModel)]="includeNumbers" (ngModelChange)="onSettingChange()" />
                <span class="char-preview">123</span>
                <span>Numbers</span>
              </label>
              <label class="char-option" [class.active]="includeSpecial">
                <input type="checkbox" [(ngModel)]="includeSpecial" (ngModelChange)="onSettingChange()" />
                <span class="char-preview">!@#</span>
                <span>Symbols</span>
              </label>
            </div>
          </div>

          <!-- Exclusions -->
          <div class="setting-group">
            <div class="setting-label" style="margin-bottom:10px">Exclusions</div>
            <label class="pill-toggle">
              <input type="checkbox" [(ngModel)]="excludeSimilar" (ngModelChange)="onSettingChange()" />
              <span class="toggle-track"></span>
              <span class="toggle-label">Exclude similar characters (i, l, 1, L, o, 0, O)</span>
            </label>
            <label class="pill-toggle" style="margin-top:8px">
              <input type="checkbox" [(ngModel)]="excludeAmbiguous" (ngModelChange)="onSettingChange()" />
              <span class="toggle-track"></span>
              <span class="toggle-label">Exclude ambiguous characters ({ } [ ] ( ) / \\ ' " ` ~ , ; : . &lt; &gt;)</span>
            </label>
          </div>

          <!-- Presets -->
          <div class="setting-group">
            <div class="setting-label" style="margin-bottom:10px">Quick Presets</div>
            <div class="presets-grid">
              <button class="preset-btn" (click)="applyPreset('pin')">📱 PIN (6)</button>
              <button class="preset-btn" (click)="applyPreset('simple')">🔤 Simple (12)</button>
              <button class="preset-btn" (click)="applyPreset('strong')">💪 Strong (16)</button>
              <button class="preset-btn" (click)="applyPreset('ultra')">🔐 Ultra (32)</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Password Checker -->
      <div class="pill-card" style="margin-top:20px">
        <h3 class="section-title" style="margin-bottom:16px">🔍 Password Strength Checker</h3>
        <div class="checker-layout">
          <div class="pill-input-wrapper" style="flex:1">
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">🔒</span>
              <input [type]="showCheckerPw ? 'text' : 'password'" class="pill-input has-icon-left has-both"
                placeholder="Enter any password to check its strength"
                [(ngModel)]="checkerPassword" (ngModelChange)="checkPassword($event)" />
              <button type="button" class="pill-input-icon icon-right"
                (click)="showCheckerPw = !showCheckerPw" style="background:none;border:none;cursor:pointer;">
                {{ showCheckerPw ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>
        </div>

        <div class="checker-result" *ngIf="checkerResult()">
          <div class="strength-meter" style="margin-top:12px">
            <div class="strength-bar">
              <div class="strength-segment" *ngFor="let i of [1,2,3,4,5]"
                [class]="'strength-segment ' + (i <= getStrengthScore(checkerResult()!.label) ? 'filled-' + getStrengthScore(checkerResult()!.label) : '')"></div>
            </div>
            <div class="strength-label">
              <span class="label-text" [style.color]="getStrengthColor(checkerResult()!.label)">
                {{ checkerResult()!.label.replace('_', ' ') }}
              </span>
              <span class="label-score">{{ checkerResult()!.score }}/100 · {{ checkerResult()!.entropy.toFixed(1) }} bits · {{ checkerResult()!.crackTime }}</span>
            </div>
          </div>
          <div class="feedback-list" *ngIf="checkerResult()!.feedback?.length" style="margin-top:8px">
            <div class="feedback-item" *ngFor="let tip of checkerResult()!.feedback">💡 {{ tip }}</div>
          </div>
        </div>
      </div>

      <!-- Multiple Passwords -->
      <div class="pill-card" style="margin-top:16px">
        <div class="flex-between" style="margin-bottom:16px">
          <h3 class="section-title">Generate Multiple Passwords</h3>
          <div style="display:flex;align-items:center;gap:10px">
            <div class="pill-input-wrapper" style="width:80px">
              <input type="number" class="pill-input" min="2" max="20" [(ngModel)]="multiCount" style="text-align:center" />
            </div>
            <button class="pill-btn pill-btn-secondary pill-btn-sm" (click)="generateMultiple()">Generate</button>
          </div>
        </div>
        <div class="multi-list" *ngIf="multiPasswords().length > 0">
          <div class="multi-item" *ngFor="let pw of multiPasswords(); let i = index">
            <span class="multi-index">{{ i + 1 }}</span>
            <span class="multi-password text-mono">{{ pw }}</span>
            <button class="icon-btn" (click)="copyText(pw)" title="Copy">📋</button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .generator-layout {
      display: grid;
      grid-template-columns: 1fr 340px;
      gap: 20px;
      padding: 20px 28px 0;
      @media (max-width: 991px) { grid-template-columns: 1fr; }
    }

    .generator-panel {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl);
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .password-display {
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-lg);
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 12px;
      min-height: 80px;
    }
    .password-value {
      flex: 1;
      font-family: var(--font-mono);
      font-size: 18px;
      color: var(--text-primary);
      word-break: break-all;
      line-height: 1.5;
      &.empty { color: var(--text-muted); font-family: var(--font-sans); font-size: 14px; }
    }
    .password-actions { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
    .icon-action-btn {
      width: 36px; height: 36px;
      border-radius: var(--pill-radius-xs);
      background: var(--bg-surface);
      border: 1px solid var(--border-default);
      cursor: pointer; font-size: 16px;
      display: flex; align-items: center; justify-content: center;
      transition: all var(--transition-fast);
      &:hover { background: var(--bg-hover); border-color: var(--accent-primary); }
      &:disabled { opacity: 0.4; cursor: not-allowed; }
    }

    .strength-details {
      display: flex; gap: 20px;
    }
    .strength-detail-item { display: flex; flex-direction: column; gap: 2px; }
    .detail-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    .detail-value { font-size: 14px; font-weight: 600; color: var(--text-primary); }

    .feedback-list { display: flex; flex-direction: column; gap: 4px; }
    .feedback-item { font-size: 12px; color: var(--text-secondary); padding: 4px 0; }

    .settings-panel {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl);
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }
    .settings-title { font-size: 15px; font-weight: 700; color: var(--text-primary); margin: 0; }

    .setting-group { display: flex; flex-direction: column; }
    .setting-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .setting-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
    .setting-value { font-size: 20px; font-weight: 700; color: var(--accent-primary); }

    .range-slider {
      width: 100%;
      accent-color: var(--accent-primary);
      cursor: pointer;
    }
    .range-labels { display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); margin-top: 2px; }

    .char-options { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
    .char-option {
      display: flex; align-items: center; gap: 8px;
      padding: 10px 12px;
      border-radius: var(--pill-radius-md);
      border: 1px solid var(--border-default);
      cursor: pointer;
      transition: all var(--transition-fast);
      font-size: 12px; color: var(--text-secondary);
      input { display: none; }
      &.active { border-color: var(--accent-primary); background: var(--bg-active); color: var(--accent-primary); }
      &:hover { border-color: var(--accent-primary); }
    }
    .char-preview { font-family: var(--font-mono); font-size: 13px; font-weight: 700; }

    .presets-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
    .preset-btn {
      padding: 8px 12px;
      border-radius: var(--pill-radius-md);
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      cursor: pointer; font-size: 12px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--accent-primary); color: var(--accent-primary); background: var(--bg-active); }
    }

    .checker-layout { display: flex; gap: 12px; align-items: flex-start; }
    .section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }

    .multi-list { display: flex; flex-direction: column; gap: 6px; }
    .multi-item {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 14px;
      background: var(--bg-elevated);
      border-radius: var(--pill-radius-md);
    }
    .multi-index { font-size: 11px; color: var(--text-muted); width: 20px; flex-shrink: 0; }
    .multi-password { flex: 1; font-size: 13px; word-break: break-all; }
    .icon-btn {
      width: 28px; height: 28px;
      border-radius: var(--pill-radius-xs);
      background: none; border: none; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      font-size: 14px; color: var(--text-muted);
      transition: all var(--transition-fast);
      &:hover { background: var(--bg-hover); color: var(--text-primary); }
    }
  `]
})
export class GeneratorComponent implements OnInit {
  private vaultService = inject(VaultService);
  private uiState = inject(UiStateService);

  generatedPassword = signal('');
  strengthResult = signal<PasswordStrengthResponse | null>(null);
  checkerResult = signal<PasswordStrengthResponse | null>(null);
  multiPasswords = signal<string[]>([]);
  generating = signal(false);

  // Settings
  length = 16;
  includeUppercase = true;
  includeLowercase = true;
  includeNumbers = true;
  includeSpecial = true;
  excludeSimilar = false;
  excludeAmbiguous = false;

  checkerPassword = '';
  showCheckerPw = false;
  multiCount = 5;

  ngOnInit(): void {
    this.generate();
  }

  generate(): void {
    this.generating.set(true);
    this.vaultService.generatePassword({
      length: this.length,
      includeUppercase: this.includeUppercase,
      includeLowercase: this.includeLowercase,
      includeNumbers: this.includeNumbers,
      includeSpecial: this.includeSpecial,
      excludeSimilar: this.excludeSimilar,
      excludeAmbiguous: this.excludeAmbiguous
    }).subscribe({
      next: (res) => {
        this.generating.set(false);
        this.generatedPassword.set(res.password);
        this.checkStrength(res.password, true);
      },
      error: () => {
        this.generating.set(false);
        // Fallback: generate locally
        this.generatedPassword.set(this.generateLocally());
      }
    });
  }

  onSettingChange(): void {
    // Auto-regenerate when settings change
    if (this.generatedPassword()) {
      this.generate();
    }
  }

  checkStrength(password: string, isGenerated = false): void {
    if (!password) {
      if (!isGenerated) this.checkerResult.set(null);
      return;
    }
    this.vaultService.checkStrength({ password }).subscribe({
      next: (res) => {
        if (isGenerated) this.strengthResult.set(res);
        else this.checkerResult.set(res);
      },
      error: () => {}
    });
  }

  checkPassword(password: string): void {
    if (password.length >= 1) {
      this.checkStrength(password, false);
    } else {
      this.checkerResult.set(null);
    }
  }

  generateMultiple(): void {
    this.vaultService.generateMultiple({
      length: this.length,
      includeUppercase: this.includeUppercase,
      includeLowercase: this.includeLowercase,
      includeNumbers: this.includeNumbers,
      includeSpecial: this.includeSpecial,
      excludeSimilar: this.excludeSimilar,
      excludeAmbiguous: this.excludeAmbiguous,
      count: this.multiCount
    }).subscribe({
      next: (res) => this.multiPasswords.set(res.passwords),
      error: () => {}
    });
  }

  copyPassword(): void {
    if (!this.generatedPassword()) return;
    navigator.clipboard.writeText(this.generatedPassword());
    this.uiState.showSuccess('Password copied to clipboard!', 'Copied');
  }

  copyText(text: string): void {
    navigator.clipboard.writeText(text);
    this.uiState.showSuccess('Copied!', 'Copied');
  }

  applyPreset(preset: string): void {
    switch (preset) {
      case 'pin':
        this.length = 6;
        this.includeUppercase = false;
        this.includeLowercase = false;
        this.includeNumbers = true;
        this.includeSpecial = false;
        break;
      case 'simple':
        this.length = 12;
        this.includeUppercase = true;
        this.includeLowercase = true;
        this.includeNumbers = true;
        this.includeSpecial = false;
        break;
      case 'strong':
        this.length = 16;
        this.includeUppercase = true;
        this.includeLowercase = true;
        this.includeNumbers = true;
        this.includeSpecial = true;
        break;
      case 'ultra':
        this.length = 32;
        this.includeUppercase = true;
        this.includeLowercase = true;
        this.includeNumbers = true;
        this.includeSpecial = true;
        break;
    }
    this.generate();
  }

  getStrengthScore(label: string): number {
    const map: Record<string, number> = {
      'VERY_WEAK': 1, 'WEAK': 2, 'FAIR': 3, 'STRONG': 4, 'VERY_STRONG': 5
    };
    return map[label] ?? 0;
  }

  getStrengthColor(label: string): string {
    const map: Record<string, string> = {
      'VERY_WEAK': 'var(--accent-danger)',
      'WEAK': 'var(--accent-orange)',
      'FAIR': 'var(--accent-warning)',
      'STRONG': '#84cc16',
      'VERY_STRONG': 'var(--accent-success)'
    };
    return map[label] ?? 'var(--text-muted)';
  }

  private generateLocally(): string {
    const chars = [
      this.includeLowercase ? 'abcdefghijklmnopqrstuvwxyz' : '',
      this.includeUppercase ? 'ABCDEFGHIJKLMNOPQRSTUVWXYZ' : '',
      this.includeNumbers ? '0123456789' : '',
      this.includeSpecial ? '!@#$%^&*()_+-=[]{}|;:,.<>?' : ''
    ].join('');
    if (!chars) return '';
    return Array.from({ length: this.length }, () => chars[Math.floor(Math.random() * chars.length)]).join('');
  }
}
