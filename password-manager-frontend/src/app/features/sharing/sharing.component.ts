import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { UiStateService } from '../../core/state/ui.state';
import { VaultService } from '../../core/services/vault.service';
import { ShareLinkResponse, CreateShareRequest, VaultEntry } from '../../core/models/vault.models';

@Component({
  selector: 'app-sharing',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">🔗 Secure Sharing</h1>
          <p class="page-subtitle">Share passwords securely with time-limited links</p>
        </div>
        <button class="pill-btn pill-btn-primary pill-btn-sm" (click)="showCreateModal.set(true)">
          + Create Share Link
        </button>
      </div>

      <!-- Active Shares -->
      <div class="shares-section">
        <h3 class="section-title" style="margin-bottom:12px">Active Share Links</h3>

        <div class="empty-state" *ngIf="!loading() && shares().length === 0">
          <div class="empty-icon">🔗</div>
          <div class="empty-title">No active share links</div>
          <div class="empty-message">Create a share link to securely share a password</div>
        </div>

        <div class="shares-list" *ngIf="shares().length > 0">
          <div class="share-card" *ngFor="let share of shares()" [class.revoked]="share.isRevoked">
            <div class="share-card-header">
              <div class="share-title">{{ share.vaultEntryTitle }}</div>
              <div class="share-badges">
                <span class="pill-badge badge-primary">{{ share.permission }}</span>
                <span class="pill-badge badge-danger" *ngIf="share.isRevoked">Revoked</span>
                <span class="pill-badge badge-warning" *ngIf="isExpired(share.expiresAt) && !share.isRevoked">Expired</span>
                <span class="pill-badge badge-success" *ngIf="!share.isRevoked && !isExpired(share.expiresAt)">Active</span>
              </div>
            </div>
            <div class="share-meta">
              <span>📧 {{ share.recipientEmail }}</span>
              <span>👁️ {{ share.viewCount }}{{ share.maxViews ? '/' + share.maxViews : '' }} views</span>
              <span>⏰ Expires {{ formatDate(share.expiresAt) }}</span>
            </div>
            <div class="share-url">
              <code class="share-link-text">{{ share.shareUrl }}</code>
              <button class="icon-btn" (click)="copyShareUrl(share)" title="Copy link">📋</button>
            </div>
            <div class="share-actions" *ngIf="!share.isRevoked">
              <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="revokeShare(share)">
                Revoke
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Share Modal -->
    <div class="modal-overlay" *ngIf="showCreateModal()" (click)="showCreateModal.set(false)">
      <div class="modal-panel" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Create Share Link</h3>
          <button class="icon-btn" (click)="showCreateModal.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <div class="pill-input-wrapper">
            <label class="pill-label">Select Password *</label>
            <select class="pill-input" [(ngModel)]="shareForm.vaultEntryId" name="entryId">
              <option [ngValue]="0" disabled>Choose a password...</option>
              <option *ngFor="let entry of vaultEntries()" [ngValue]="entry.id">{{ entry.title }}</option>
            </select>
          </div>

          <div class="pill-input-wrapper">
            <label class="pill-label">Recipient Email *</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">📧</span>
              <input type="email" class="pill-input has-icon-left" placeholder="recipient@example.com"
                [(ngModel)]="shareForm.recipientEmail" name="email" />
            </div>
          </div>

          <div class="pill-input-wrapper">
            <label class="pill-label">Permission</label>
            <select class="pill-input" [(ngModel)]="shareForm.permission" name="permission">
              <option value="VIEW_ONCE">View Once</option>
              <option value="VIEW_MULTIPLE">View Multiple Times</option>
              <option value="TEMPORARY_ACCESS">Temporary Access</option>
            </select>
          </div>

          <div class="form-row">
            <div class="pill-input-wrapper" *ngIf="shareForm.permission === 'VIEW_MULTIPLE'">
              <label class="pill-label">Max Views</label>
              <input type="number" class="pill-input" min="1" max="100"
                [(ngModel)]="shareForm.maxViews" name="maxViews" />
            </div>
            <div class="pill-input-wrapper">
              <label class="pill-label">Expires In (hours)</label>
              <input type="number" class="pill-input" min="1" max="720"
                [(ngModel)]="shareForm.expiresInHours" name="expires" />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showCreateModal.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-primary" (click)="createShare()"
            [class.loading]="creating()" [disabled]="creating()">
            <span class="btn-text">Create Link</span>
            <span class="btn-spinner" *ngIf="creating()"></span>
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }
    .shares-section { padding: 20px 28px; }
    .section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); }

    .shares-list { display: flex; flex-direction: column; gap: 12px; }
    .share-card {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-lg);
      padding: 16px 20px;
      display: flex; flex-direction: column; gap: 10px;
      &.revoked { opacity: 0.6; }
    }
    .share-card-header { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .share-title { font-size: 14px; font-weight: 700; color: var(--text-primary); }
    .share-badges { display: flex; gap: 6px; flex-wrap: wrap; }
    .share-meta { display: flex; gap: 16px; flex-wrap: wrap; font-size: 12px; color: var(--text-muted); }
    .share-url {
      display: flex; align-items: center; gap: 8px;
      padding: 8px 12px;
      background: var(--bg-elevated);
      border-radius: var(--pill-radius-md);
    }
    .share-link-text { font-family: var(--font-mono); font-size: 11px; color: var(--text-secondary); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .share-actions { display: flex; justify-content: flex-end; }
    .icon-btn {
      width: 28px; height: 28px; border-radius: var(--pill-radius-xs);
      background: none; border: none; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      font-size: 14px; color: var(--text-muted);
      transition: all var(--transition-fast);
      &:hover { background: var(--bg-hover); color: var(--text-primary); }
    }

    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; gap: 8px; }
    .empty-icon { font-size: 48px; }
    .empty-title { font-size: 16px; font-weight: 600; color: var(--text-primary); }
    .empty-message { font-size: 13px; color: var(--text-muted); }

    .modal-overlay {
      position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px);
      display: flex; align-items: center; justify-content: center; z-index: var(--z-modal); padding: 20px;
    }
    .modal-panel {
      background: var(--bg-surface); border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-xl); width: 100%; max-width: 480px;
      display: flex; flex-direction: column; box-shadow: 0 24px 64px rgba(0,0,0,0.5);
    }
    .modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--border-subtle); }
    .modal-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0; }
    .modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 14px; }
    .modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-subtle); display: flex; justify-content: flex-end; gap: 10px; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    select.pill-input { appearance: none; cursor: pointer; }
  `]
})
export class SharingComponent implements OnInit {
  private http = inject(HttpClient);
  private uiState = inject(UiStateService);
  private vaultService = inject(VaultService);

  shares = signal<ShareLinkResponse[]>([]);
  vaultEntries = signal<VaultEntry[]>([]);
  loading = signal(true);
  creating = signal(false);
  showCreateModal = signal(false);

  shareForm: CreateShareRequest = {
    vaultEntryId: 0,
    recipientEmail: '',
    permission: 'VIEW_ONCE',
    expiresInHours: 24
  };

  ngOnInit(): void {
    this.loadShares();
    this.vaultService.getEntries().subscribe({ next: e => this.vaultEntries.set(e), error: () => {} });
  }

  loadShares(): void {
    this.loading.set(true);
    this.http.get<ShareLinkResponse[]>('/api/shares').subscribe({
      next: (s) => { this.shares.set(s); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  createShare(): void {
    if (!this.shareForm.vaultEntryId || !this.shareForm.recipientEmail) {
      this.uiState.showError('Please fill all required fields.');
      return;
    }
    this.creating.set(true);
    this.http.post<ShareLinkResponse>('/api/shares', this.shareForm).subscribe({
      next: (share) => {
        this.creating.set(false);
        this.shares.update(list => [share, ...list]);
        this.showCreateModal.set(false);
        this.uiState.showSuccess('Share link created!', 'Created');
        navigator.clipboard.writeText(share.shareUrl);
      },
      error: () => { this.creating.set(false); this.uiState.showError('Failed to create share link.'); }
    });
  }

  revokeShare(share: ShareLinkResponse): void {
    this.http.post<any>(`/api/shares/${share.shareId}/revoke`, {}).subscribe({
      next: () => {
        this.shares.update(list => list.map(s => s.shareId === share.shareId ? { ...s, isRevoked: true } : s));
        this.uiState.showSuccess('Share link revoked.', 'Revoked');
      },
      error: () => this.uiState.showError('Failed to revoke.')
    });
  }

  copyShareUrl(share: ShareLinkResponse): void {
    navigator.clipboard.writeText(share.shareUrl);
    this.uiState.showSuccess('Link copied!', 'Copied');
  }

  isExpired(dateStr: string): boolean {
    return new Date(dateStr) < new Date();
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString();
  }
}
