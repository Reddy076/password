import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { UiStateService } from '../../core/state/ui.state';
import { FileEntry, FileFolder } from '../../core/models/vault.models';

@Component({
  selector: 'app-files',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">📁 File Vault</h1>
          <p class="page-subtitle">Securely store and manage encrypted files</p>
        </div>
        <div style="display:flex;gap:8px">
          <button class="pill-btn pill-btn-secondary pill-btn-sm" (click)="showFolderModal.set(true)">
            📁 New Folder
          </button>
          <button class="pill-btn pill-btn-primary pill-btn-sm" (click)="fileInput.click()">
            📤 Upload File
          </button>
          <input #fileInput type="file" style="display:none" (change)="onFileSelect($event)" multiple />
        </div>
      </div>

      <!-- Storage Info -->
      <div class="storage-bar" *ngIf="totalStorage() > 0">
        <div class="storage-info">
          <span>{{ formatBytes(usedStorage()) }} used</span>
          <span class="text-muted-color">of {{ formatBytes(totalStorage()) }}</span>
        </div>
        <div class="storage-track">
          <div class="storage-fill" [style.width.%]="(usedStorage() / totalStorage()) * 100"></div>
        </div>
      </div>

      <!-- Breadcrumb -->
      <div class="breadcrumb" *ngIf="currentFolder()">
        <button class="breadcrumb-item" (click)="currentFolder.set(null)">📁 Root</button>
        <span class="breadcrumb-sep">›</span>
        <span class="breadcrumb-current">{{ currentFolder()!.name }}</span>
      </div>

      <div class="loading-state" *ngIf="loading()">
        <div class="skeleton" style="height:48px;margin-bottom:8px" *ngFor="let i of [1,2,3,4]"></div>
      </div>

      <div class="empty-state" *ngIf="!loading() && folders().length === 0 && files().length === 0">
        <div class="empty-icon">📁</div>
        <div class="empty-title">No files yet</div>
        <div class="empty-message">Upload files to store them securely in your vault</div>
      </div>

      <!-- Files Grid -->
      <div class="files-grid" *ngIf="!loading() && (folders().length > 0 || files().length > 0)">
        <!-- Folders -->
        <div class="file-item folder-item" *ngFor="let folder of folders()" (click)="openFolder(folder)">
          <div class="file-icon">📁</div>
          <div class="file-info">
            <div class="file-name">{{ folder.name }}</div>
            <div class="file-meta">Folder</div>
          </div>
          <button class="icon-btn icon-btn-danger" (click)="deleteFolder(folder); $event.stopPropagation()" title="Delete">🗑️</button>
        </div>

        <!-- Files -->
        <div class="file-item" *ngFor="let file of files()">
          <div class="file-icon">{{ getFileIcon(file.mimeType) }}</div>
          <div class="file-info">
            <div class="file-name">{{ file.originalFilename }}</div>
            <div class="file-meta">{{ formatBytes(file.fileSize) }} · {{ formatDate(file.uploadedAt) }}</div>
          </div>
          <div class="file-actions">
            <button class="icon-btn" (click)="downloadFile(file)" title="Download">⬇️</button>
            <button class="icon-btn icon-btn-danger" (click)="deleteFile(file)" title="Delete">🗑️</button>
          </div>
        </div>
      </div>
    </div>

    <!-- New Folder Modal -->
    <div class="modal-overlay" *ngIf="showFolderModal()" (click)="showFolderModal.set(false)">
      <div class="modal-panel modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">New Folder</h3>
          <button class="icon-btn" (click)="showFolderModal.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <div class="pill-input-wrapper">
            <label class="pill-label">Folder Name</label>
            <input type="text" class="pill-input" placeholder="Enter folder name"
              [(ngModel)]="newFolderName" name="folderName" />
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showFolderModal.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-primary" (click)="createFolder()">Create</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .storage-bar { padding: 12px 28px 0; }
    .storage-info { display: flex; gap: 6px; font-size: 12px; color: var(--text-secondary); margin-bottom: 6px; }
    .storage-track { height: 6px; background: var(--bg-elevated); border-radius: var(--pill-radius-full); overflow: hidden; }
    .storage-fill { height: 100%; background: var(--accent-primary); border-radius: var(--pill-radius-full); transition: width 0.5s ease; }

    .breadcrumb { display: flex; align-items: center; gap: 6px; padding: 12px 28px 0; font-size: 13px; }
    .breadcrumb-item { background: none; border: none; cursor: pointer; color: var(--accent-primary); font-size: 13px; padding: 0; &:hover { text-decoration: underline; } }
    .breadcrumb-sep { color: var(--text-muted); }
    .breadcrumb-current { color: var(--text-primary); font-weight: 600; }

    .files-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 10px;
      padding: 16px 28px;
    }
    .file-item {
      display: flex; align-items: center; gap: 10px;
      padding: 12px 14px;
      background: var(--bg-surface); border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-lg);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--border-default); }
    }
    .folder-item { cursor: pointer; &:hover { border-color: var(--accent-primary); background: var(--bg-active); } }
    .file-icon { font-size: 24px; flex-shrink: 0; }
    .file-info { flex: 1; overflow: hidden; }
    .file-name { font-size: 13px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .file-meta { font-size: 11px; color: var(--text-muted); }
    .file-actions { display: flex; gap: 4px; flex-shrink: 0; }
    .icon-btn { width: 28px; height: 28px; border-radius: var(--pill-radius-xs); background: none; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 14px; color: var(--text-muted); transition: all var(--transition-fast); &:hover { background: var(--bg-hover); color: var(--text-primary); } &.icon-btn-danger:hover { background: rgba(239,68,68,0.1); color: var(--accent-danger); } }

    .loading-state { padding: 16px 28px; }
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; gap: 8px; }
    .empty-icon { font-size: 48px; }
    .empty-title { font-size: 16px; font-weight: 600; color: var(--text-primary); }
    .empty-message { font-size: 13px; color: var(--text-muted); }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: var(--z-modal); padding: 20px; }
    .modal-panel { background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--pill-radius-xl); width: 100%; max-width: 480px; display: flex; flex-direction: column; box-shadow: 0 24px 64px rgba(0,0,0,0.5); }
    .modal-sm { max-width: 360px; }
    .modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--border-subtle); }
    .modal-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0; }
    .modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 14px; }
    .modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-subtle); display: flex; justify-content: flex-end; gap: 10px; }
  `]
})
export class FilesComponent implements OnInit {
  private http = inject(HttpClient);
  private uiState = inject(UiStateService);

  files = signal<FileEntry[]>([]);
  folders = signal<FileFolder[]>([]);
  currentFolder = signal<FileFolder | null>(null);
  loading = signal(true);
  showFolderModal = signal(false);
  newFolderName = '';
  usedStorage = signal(0);
  totalStorage = signal(0);

  ngOnInit(): void {
    this.loadFiles();
  }

  loadFiles(): void {
    this.loading.set(true);
    const folderId = this.currentFolder()?.id;
    const url = folderId ? `/api/files?folderId=${folderId}` : '/api/files';
    this.http.get<{ folders: FileFolder[]; files: FileEntry[]; totalFiles: number; totalStorageBytes: number }>(url).subscribe({
      next: (res) => {
        this.folders.set(res.folders ?? []);
        this.files.set(res.files ?? []);
        this.usedStorage.set(res.totalStorageBytes ?? 0);
        this.totalStorage.set(1073741824); // 1GB default
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
  }

  openFolder(folder: FileFolder): void {
    this.currentFolder.set(folder);
    this.loadFiles();
  }

  createFolder(): void {
    if (!this.newFolderName) return;
    const parentId = this.currentFolder()?.id;
    this.http.post<FileFolder>('/api/files/folders', { name: this.newFolderName, parentId }).subscribe({
      next: (folder) => {
        this.folders.update(list => [...list, folder]);
        this.showFolderModal.set(false);
        this.newFolderName = '';
        this.uiState.showSuccess('Folder created.', 'Created');
      },
      error: () => this.uiState.showError('Failed to create folder.')
    });
  }

  deleteFolder(folder: FileFolder): void {
    if (!confirm(`Delete folder "${folder.name}"?`)) return;
    this.http.delete(`/api/files/folders/${folder.id}`).subscribe({
      next: () => {
        this.folders.update(list => list.filter(f => f.id !== folder.id));
        this.uiState.showSuccess('Folder deleted.', 'Deleted');
      },
      error: () => this.uiState.showError('Failed to delete folder.')
    });
  }

  onFileSelect(event: Event): void {
    const files = (event.target as HTMLInputElement).files;
    if (!files) return;
    Array.from(files).forEach(file => this.uploadFile(file));
  }

  uploadFile(file: File): void {
    const formData = new FormData();
    formData.append('file', file);
    const folderId = this.currentFolder()?.id;
    if (folderId) formData.append('folderId', String(folderId));

    this.http.post<FileEntry>('/api/files/upload', formData).subscribe({
      next: (entry) => {
        this.files.update(list => [...list, entry]);
        this.uiState.showSuccess(`"${file.name}" uploaded.`, 'Uploaded');
      },
      error: () => this.uiState.showError(`Failed to upload "${file.name}".`)
    });
  }

  downloadFile(file: FileEntry): void {
    this.http.get(`/api/files/${file.id}/download`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = file.originalFilename;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.uiState.showError('Failed to download file.')
    });
  }

  deleteFile(file: FileEntry): void {
    if (!confirm(`Delete "${file.originalFilename}"?`)) return;
    this.http.delete(`/api/files/${file.id}`).subscribe({
      next: () => {
        this.files.update(list => list.filter(f => f.id !== file.id));
        this.uiState.showSuccess('File deleted.', 'Deleted');
      },
      error: () => this.uiState.showError('Failed to delete file.')
    });
  }

  getFileIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return '🖼️';
    if (mimeType.startsWith('video/')) return '🎬';
    if (mimeType.startsWith('audio/')) return '🎵';
    if (mimeType.includes('pdf')) return '📄';
    if (mimeType.includes('word') || mimeType.includes('document')) return '📝';
    if (mimeType.includes('sheet') || mimeType.includes('excel')) return '📊';
    if (mimeType.includes('zip') || mimeType.includes('archive')) return '🗜️';
    return '📎';
  }

  formatBytes(bytes: number): string {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(1))} ${sizes[i]}`;
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString();
  }
}
