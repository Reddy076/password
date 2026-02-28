import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VaultService } from '../../core/services/vault.service';
import { UiStateService } from '../../core/state/ui.state';
import { BackupFormat, ImportSource } from '../../core/models/vault.models';

@Component({
  selector: 'app-backup',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">Backup & Export</h1>
          <p class="page-subtitle">Export your vault or import from other password managers</p>
        </div>
      </div>

      <div class="backup-grid">
        <!-- Export Section -->
        <div class="pill-card">
          <h3 class="section-title">📤 Export Vault</h3>
          <p class="section-desc">Download all your passwords in a secure format.</p>

          <div class="format-options">
            <button class="format-btn" [class.active]="exportFormat === 'JSON'" (click)="exportFormat = 'JSON'">
              <span class="format-icon">{ }</span>
              <span>JSON</span>
              <span class="format-desc">Structured format, best for re-import</span>
            </button>
            <button class="format-btn" [class.active]="exportFormat === 'CSV'" (click)="exportFormat = 'CSV'">
              <span class="format-icon">📊</span>
              <span>CSV</span>
              <span class="format-desc">Spreadsheet format, widely compatible</span>
            </button>
          </div>

          <div class="export-warning">
            ⚠️ <strong>Security Warning:</strong> Exported files contain your passwords in plain text. Store them securely and delete after use.
          </div>

          <button class="pill-btn pill-btn-primary" style="width:100%" (click)="exportVault()"
            [class.loading]="exporting()" [disabled]="exporting()">
            <span class="btn-text">📥 Export Vault</span>
            <span class="btn-spinner" *ngIf="exporting()"></span>
          </button>
        </div>

        <!-- Import Section -->
        <div class="pill-card">
          <h3 class="section-title">📥 Import Passwords</h3>
          <p class="section-desc">Import from a backup file or another password manager.</p>

          <div class="import-tabs">
            <button class="import-tab" [class.active]="importMode === 'file'" (click)="importMode = 'file'">
              From File
            </button>
            <button class="import-tab" [class.active]="importMode === 'external'" (click)="importMode = 'external'">
              From Browser/App
            </button>
          </div>

          <!-- File Import -->
          <div *ngIf="importMode === 'file'">
            <div class="format-options">
              <button class="format-btn" [class.active]="importFormat === 'JSON'" (click)="importFormat = 'JSON'">
                <span>{ }</span> JSON
              </button>
              <button class="format-btn" [class.active]="importFormat === 'CSV'" (click)="importFormat = 'CSV'">
                <span>📊</span> CSV
              </button>
            </div>

            <div class="file-drop-zone" (click)="fileInput.click()" (dragover)="$event.preventDefault()" (drop)="onFileDrop($event)">
              <input #fileInput type="file" [accept]="importFormat === 'JSON' ? '.json' : '.csv'" style="display:none" (change)="onFileSelect($event)" />
              <div class="drop-icon">📁</div>
              <div class="drop-text">{{ importFile ? importFile.name : 'Click or drag file here' }}</div>
              <div class="drop-hint">{{ importFormat }} files only</div>
            </div>

            <button class="pill-btn pill-btn-primary" style="width:100%" (click)="importFromFile()"
              [disabled]="!importFile || importing()" [class.loading]="importing()">
              <span class="btn-text">Import</span>
              <span class="btn-spinner" *ngIf="importing()"></span>
            </button>
          </div>

          <!-- External Import -->
          <div *ngIf="importMode === 'external'">
            <div class="source-options">
              <button class="source-btn" *ngFor="let src of importSources"
                [class.active]="importSource === src.value"
                (click)="importSource = src.value">
                <span class="source-icon">{{ src.icon }}</span>
                <span>{{ src.label }}</span>
              </button>
            </div>

            <div class="file-drop-zone" (click)="extFileInput.click()">
              <input #extFileInput type="file" accept=".csv,.json" style="display:none" (change)="onExtFileSelect($event)" />
              <div class="drop-icon">📁</div>
              <div class="drop-text">{{ extImportFile ? extImportFile.name : 'Select exported file' }}</div>
            </div>

            <button class="pill-btn pill-btn-primary" style="width:100%" (click)="importExternal()"
              [disabled]="!extImportFile || importing()" [class.loading]="importing()">
              <span class="btn-text">Import from {{ importSource }}</span>
              <span class="btn-spinner" *ngIf="importing()"></span>
            </button>
          </div>
        </div>
      </div>

      <!-- Import Result -->
      <div class="import-result" *ngIf="importResult()">
        <div class="result-stat result-success">
          <span class="result-num">{{ importResult()!.imported }}</span>
          <span class="result-lbl">Imported</span>
        </div>
        <div class="result-stat result-warning">
          <span class="result-num">{{ importResult()!.skipped }}</span>
          <span class="result-lbl">Skipped</span>
        </div>
        <div class="result-stat result-danger">
          <span class="result-num">{{ importResult()!.failed }}</span>
          <span class="result-lbl">Failed</span>
        </div>
        <div class="result-errors" *ngIf="importResult()!.errors?.length > 0">
          <div *ngFor="let err of importResult()!.errors" class="result-error">⚠️ {{ err }}</div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .backup-grid {
      display: grid; grid-template-columns: 1fr 1fr; gap: 20px;
      padding: 20px 28px 0;
      @media (max-width: 767px) { grid-template-columns: 1fr; }
    }

    .section-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0 0 6px; }
    .section-desc { font-size: 13px; color: var(--text-secondary); margin: 0 0 16px; }

    .format-options { display: flex; gap: 8px; margin-bottom: 16px; }
    .format-btn {
      flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;
      padding: 12px; border-radius: var(--pill-radius-lg);
      background: var(--bg-elevated); border: 1px solid var(--border-default);
      cursor: pointer; font-size: 13px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--accent-primary); }
      &.active { border-color: var(--accent-primary); background: var(--bg-active); color: var(--accent-primary); }
    }
    .format-icon { font-size: 20px; font-family: var(--font-mono); }
    .format-desc { font-size: 10px; color: var(--text-muted); text-align: center; }

    .export-warning {
      padding: 10px 14px;
      background: rgba(245,158,11,0.1);
      border: 1px solid rgba(245,158,11,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-warning);
      font-size: 12px;
      margin-bottom: 16px;
    }

    .import-tabs { display: flex; gap: 4px; margin-bottom: 16px; }
    .import-tab {
      padding: 6px 14px; border-radius: var(--pill-radius-full);
      background: none; border: 1px solid var(--border-default);
      cursor: pointer; font-size: 12px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--accent-primary); }
      &.active { background: var(--bg-active); border-color: var(--accent-primary); color: var(--accent-primary); }
    }

    .file-drop-zone {
      display: flex; flex-direction: column; align-items: center; gap: 6px;
      padding: 24px;
      border: 2px dashed var(--border-default);
      border-radius: var(--pill-radius-lg);
      cursor: pointer; margin-bottom: 12px;
      transition: all var(--transition-fast);
      &:hover { border-color: var(--accent-primary); background: var(--bg-active); }
    }
    .drop-icon { font-size: 32px; }
    .drop-text { font-size: 13px; color: var(--text-secondary); }
    .drop-hint { font-size: 11px; color: var(--text-muted); }

    .source-options { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; margin-bottom: 12px; }
    .source-btn {
      display: flex; align-items: center; gap: 8px;
      padding: 10px 12px; border-radius: var(--pill-radius-md);
      background: var(--bg-elevated); border: 1px solid var(--border-default);
      cursor: pointer; font-size: 12px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--accent-primary); }
      &.active { border-color: var(--accent-primary); background: var(--bg-active); color: var(--accent-primary); }
    }
    .source-icon { font-size: 18px; }

    .import-result {
      display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
      padding: 16px 28px;
    }
    .result-stat { display: flex; flex-direction: column; align-items: center; gap: 2px; padding: 12px 20px; border-radius: var(--pill-radius-lg); }
    .result-success { background: rgba(16,185,129,0.1); }
    .result-warning { background: rgba(245,158,11,0.1); }
    .result-danger { background: rgba(239,68,68,0.1); }
    .result-num { font-size: 24px; font-weight: 700; color: var(--text-primary); }
    .result-lbl { font-size: 11px; color: var(--text-muted); }
    .result-errors { flex: 1; display: flex; flex-direction: column; gap: 4px; }
    .result-error { font-size: 12px; color: var(--accent-danger); }
  `]
})
export class BackupComponent {
  private vaultService = inject(VaultService);
  private uiState = inject(UiStateService);

  exportFormat: BackupFormat = 'JSON';
  importFormat: BackupFormat = 'JSON';
  importSource: ImportSource = 'CHROME';
  importMode: 'file' | 'external' = 'file';
  importFile: File | null = null;
  extImportFile: File | null = null;
  exporting = signal(false);
  importing = signal(false);
  importResult = signal<{ imported: number; skipped: number; failed: number; errors: string[] } | null>(null);

  importSources = [
    { value: 'CHROME' as ImportSource, label: 'Chrome', icon: '🌐' },
    { value: 'FIREFOX' as ImportSource, label: 'Firefox', icon: '🦊' },
    { value: 'LASTPASS' as ImportSource, label: 'LastPass', icon: '🔑' },
    { value: '1PASSWORD' as ImportSource, label: '1Password', icon: '🔐' },
  ];

  exportVault(): void {
    this.exporting.set(true);
    this.vaultService.getEntries().subscribe({
      next: (entries) => {
        this.exporting.set(false);
        const data = this.exportFormat === 'JSON'
          ? JSON.stringify(entries, null, 2)
          : this.toCSV(entries);
        const blob = new Blob([data], { type: this.exportFormat === 'JSON' ? 'application/json' : 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `revaultx-export-${new Date().toISOString().split('T')[0]}.${this.exportFormat.toLowerCase()}`;
        a.click();
        URL.revokeObjectURL(url);
        this.uiState.showSuccess('Vault exported successfully.', 'Exported');
      },
      error: () => { this.exporting.set(false); this.uiState.showError('Export failed.'); }
    });
  }

  onFileSelect(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.importFile = file;
  }

  onExtFileSelect(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.extImportFile = file;
  }

  onFileDrop(event: DragEvent): void {
    event.preventDefault();
    const file = event.dataTransfer?.files?.[0];
    if (file) this.importFile = file;
  }

  importFromFile(): void {
    if (!this.importFile) return;
    this.importing.set(true);
    const reader = new FileReader();
    reader.onload = (e) => {
      const data = e.target?.result as string;
      this.vaultService.getEntries().subscribe({
        next: () => {
          this.importing.set(false);
          this.importResult.set({ imported: 0, skipped: 0, failed: 0, errors: ['Import API not available in demo mode'] });
        },
        error: () => { this.importing.set(false); this.uiState.showError('Import failed.'); }
      });
    };
    reader.readAsText(this.importFile);
  }

  importExternal(): void {
    if (!this.extImportFile) return;
    this.importing.set(true);
    const reader = new FileReader();
    reader.onload = (e) => {
      const data = e.target?.result as string;
      this.importing.set(false);
      this.importResult.set({ imported: 0, skipped: 0, failed: 0, errors: ['External import API not available in demo mode'] });
    };
    reader.readAsText(this.extImportFile);
  }

  private toCSV(entries: any[]): string {
    const headers = ['title', 'username', 'password', 'websiteUrl', 'notes', 'categoryName', 'folderName'];
    const rows = entries.map(e => headers.map(h => `"${(e[h] ?? '').toString().replace(/"/g, '""')}"`).join(','));
    return [headers.join(','), ...rows].join('\n');
  }
}
