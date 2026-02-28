import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VaultService } from '../../../core/services/vault.service';
import { UiStateService } from '../../../core/state/ui.state';
import { TrashEntry } from '../../../core/models/vault.models';

@Component({
  selector: 'app-trash',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">🗑️ Trash</h1>
          <p class="page-subtitle">Deleted passwords are kept for 30 days before permanent deletion</p>
        </div>
        <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="emptyTrash()"
          *ngIf="entries().length > 0" [disabled]="loading()">
          Empty Trash
        </button>
      </div>

      <div class="loading-state" *ngIf="loading()">
        <div class="skeleton" style="height:60px;margin-bottom:8px" *ngFor="let i of [1,2,3]"></div>
      </div>

      <div class="empty-state" *ngIf="!loading() && entries().length === 0">
        <div class="empty-icon">🗑️</div>
        <div class="empty-title">Trash is empty</div>
        <div class="empty-message">Deleted passwords will appear here</div>
      </div>

      <div class="trash-list" *ngIf="!loading() && entries().length > 0">
        <div class="trash-item" *ngFor="let entry of entries()">
          <div class="trash-item-info">
            <div class="trash-item-title">{{ entry.title }}</div>
            <div class="trash-item-meta">
              <span *ngIf="entry.categoryName">📂 {{ entry.categoryName }}</span>
              <span *ngIf="entry.websiteUrl">🌐 {{ entry.websiteUrl }}</span>
              <span>Deleted {{ formatDate(entry.deletedAt) }}</span>
            </div>
          </div>
          <div class="trash-item-expiry">
            <span class="pill-badge" [class]="entry.daysRemaining <= 3 ? 'badge-danger' : entry.daysRemaining <= 7 ? 'badge-warning' : 'badge-secondary'">
              {{ entry.daysRemaining }}d left
            </span>
          </div>
          <div class="trash-item-actions">
            <button class="pill-btn pill-btn-secondary pill-btn-sm" (click)="restore(entry)">
              ↩️ Restore
            </button>
            <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="permanentDelete(entry)">
              🗑️ Delete
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .trash-list { display: flex; flex-direction: column; gap: 8px; padding: 16px 28px; }
    .trash-item {
      display: flex; align-items: center; gap: 12px;
      padding: 14px 16px;
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-lg);
      transition: all var(--transition-fast);
      &:hover { border-color: var(--border-default); }
    }
    .trash-item-info { flex: 1; overflow: hidden; }
    .trash-item-title { font-size: 14px; font-weight: 600; color: var(--text-primary); }
    .trash-item-meta { display: flex; gap: 12px; flex-wrap: wrap; margin-top: 4px; font-size: 11px; color: var(--text-muted); }
    .trash-item-expiry { flex-shrink: 0; }
    .trash-item-actions { display: flex; gap: 6px; flex-shrink: 0; }

    .loading-state { padding: 16px 28px; }
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; gap: 8px; }
    .empty-icon { font-size: 48px; }
    .empty-title { font-size: 16px; font-weight: 600; color: var(--text-primary); }
    .empty-message { font-size: 13px; color: var(--text-muted); }
  `]
})
export class TrashComponent implements OnInit {
  private vaultService = inject(VaultService);
  private uiState = inject(UiStateService);

  entries = signal<TrashEntry[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.loadTrash();
  }

  loadTrash(): void {
    this.loading.set(true);
    this.vaultService.getTrash().subscribe({
      next: (e) => { this.entries.set(e); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  restore(entry: TrashEntry): void {
    this.vaultService.restoreEntry(entry.id).subscribe({
      next: () => {
        this.entries.update(list => list.filter(e => e.id !== entry.id));
        this.uiState.showSuccess(`"${entry.title}" restored.`, 'Restored');
      },
      error: () => this.uiState.showError('Failed to restore.')
    });
  }

  permanentDelete(entry: TrashEntry): void {
    if (!confirm(`Permanently delete "${entry.title}"? This cannot be undone.`)) return;
    this.vaultService.permanentlyDelete(entry.id).subscribe({
      next: () => {
        this.entries.update(list => list.filter(e => e.id !== entry.id));
        this.uiState.showSuccess('Permanently deleted.', 'Deleted');
      },
      error: () => this.uiState.showError('Failed to delete.')
    });
  }

  emptyTrash(): void {
    if (!confirm('Empty trash? All items will be permanently deleted.')) return;
    this.vaultService.emptyTrash().subscribe({
      next: () => { this.entries.set([]); this.uiState.showSuccess('Trash emptied.', 'Emptied'); },
      error: () => this.uiState.showError('Failed to empty trash.')
    });
  }

  formatDate(dateStr: string): string {
    const date = new Date(dateStr);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    const days = Math.floor(diff / 86400000);
    if (days === 0) return 'today';
    if (days === 1) return 'yesterday';
    return `${days} days ago`;
  }
}
