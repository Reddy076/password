import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { VaultService } from '../../core/api/api/vault.service';
import { VaultEntryResponse } from '../../core/api/model/vaultEntryResponse';
import { VaultEntryDetailResponse } from '../../core/api/model/vaultEntryDetailResponse';
import { VaultEntryRequest } from '../../core/api/model/vaultEntryRequest';
import { TrashEntryResponse } from '../../core/api/model/trashEntryResponse';
import { VaultEntryModalComponent } from './vault-entry-modal/vault-entry-modal.component';
import { MasterPasswordModalComponent } from './master-password-modal/master-password-modal.component';
import { NotificationEventService } from '../../core/services/notification-event.service';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-vault',
  standalone: true,
  imports: [CommonModule, FormsModule, VaultEntryModalComponent, MasterPasswordModalComponent, LucideAngularModule],
  templateUrl: './vault.component.html',
  styleUrl: './vault.component.css'
})
export class VaultComponent implements OnInit {
  private vaultService = inject(VaultService);
  private notificationEventService = inject(NotificationEventService);
  private route = inject(ActivatedRoute);

  entries: VaultEntryResponse[] = [];
  filteredEntries: VaultEntryResponse[] = [];
  isLoading = true;
  errorMessage = '';
  searchQuery = '';
  currentFilter: { type: 'system' | 'category' | 'folder', id?: number, name: string } = { type: 'system', id: undefined, name: 'all' };

  showModal = false;
  selectedEntry: VaultEntryDetailResponse | null = null;
  /** Patched after the user unlocks a sensitive entry for editing */
  unlockedEntry: VaultEntryDetailResponse | null = null;

  // ── Trash view ─────────────────────────────────────────────────────────────
  isTrashView = false;
  trashEntries: TrashEntryResponse[] = [];
  isLoadingTrash = false;

  // ── Copy-password flow (existing) ──────────────────────────────────────────
  showMasterPasswordModal = false;
  entryToDecrypt: number | null = null;
  decryptedPasswordTimeout: any;
  copiedEntryId: number | null = null;

  // ── Sensitive-unlock-for-edit flow ─────────────────────────────────────────
  showSensitiveUnlockModal = false;
  sensitiveUnlockEntryId: number | null = null;

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      // Clear detail view when navigating
      this.closeModal();

      const filter = params['filter'];
      const categoryId = params['categoryId'];

      if (filter === 'trash') {
        this.isTrashView = true;
        this.currentFilter = { type: 'system', name: 'trash' };
        if (this.trashEntries.length === 0) {
          this.loadTrashEntries();
        }
      } else {
        this.isTrashView = false;
        if (filter === 'favorites') {
          this.currentFilter = { type: 'system', name: 'favorites' };
        } else if (categoryId) {
          this.currentFilter = { type: 'category', id: +categoryId, name: 'category' };
        } else {
          this.currentFilter = { type: 'system', name: 'all' };
        }

        // If we haven't loaded main entries yet, load them, otherwise apply filter directly
        if (this.entries.length === 0) {
          this.loadEntries();
        } else {
          this.applyFilter();
        }
      }
    });
  }

  loadEntries() {
    this.isLoading = true;
    this.errorMessage = '';
    this.vaultService.getAllEntries().subscribe({
      next: (data) => {
        this.entries = data;
        this.applyFilter();
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load vault entries.';
        this.isLoading = false;
      }
    });
  }

  loadTrashEntries() {
    this.isLoadingTrash = true;
    this.errorMessage = '';
    this.vaultService.getTrashEntries().subscribe({
      next: (data) => {
        this.trashEntries = data;
        this.isLoadingTrash = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load trash entries.';
        this.isLoadingTrash = false;
      }
    });
  }

  private applyFilter() {
    let filtered = this.entries;

    // 1. Sidebar Filter
    if (this.currentFilter.type === 'system') {
      if (this.currentFilter.name === 'favorites') {
        filtered = filtered.filter(e => e.isFavorite);
      }
      // 'trash' is handled by isTrashView + trashEntries
    } else if (this.currentFilter.type === 'category') {
      filtered = filtered.filter(e => e.categoryId === this.currentFilter.id);
    } else if (this.currentFilter.type === 'folder') {
      filtered = filtered.filter(e => (e as any).folderId === this.currentFilter.id);
    }

    // 2. Search Query Filter
    if (this.searchQuery && this.searchQuery.trim().length > 0) {
      const q = this.searchQuery.toLowerCase().trim();
      filtered = filtered.filter(e =>
        (e.title && e.title.toLowerCase().includes(q)) ||
        (e.username && e.username.toLowerCase().includes(q)) ||
        (e.websiteUrl && e.websiteUrl.toLowerCase().includes(q))
      );
    }

    this.filteredEntries = filtered;
  }

  onSearch() {
    this.applyFilter();
  }

  onAddNew() {
    this.selectedEntry = null;
    this.unlockedEntry = null;
    this.showModal = true;
  }

  onViewEntry(entry: VaultEntryResponse) {
    if (!entry.id) {
      this.selectedEntry = entry as unknown as VaultEntryDetailResponse;
      this.unlockedEntry = null;
      this.showModal = true;
      return;
    }
    this.vaultService.getEntry(entry.id).subscribe({
      next: (detailEntry) => {
        console.log('Fetched Detail Entry from Backend:', detailEntry);
        this.selectedEntry = detailEntry;
        this.unlockedEntry = null;
        this.showModal = true;
      },
      error: () => {
        this.errorMessage = 'Failed to load entry details.';
      }
    });
  }

  closeModal() {
    this.showModal = false;
    this.selectedEntry = null;
    this.unlockedEntry = null;
  }

  saveEntry(request: VaultEntryRequest) {
    const requestObservable = this.selectedEntry?.id
      ? this.vaultService.updateEntry(this.selectedEntry.id, request)
      : this.vaultService.createEntry(request);

    requestObservable.subscribe({
      next: () => {
        this.closeModal();
        this.loadEntries();
        this.notificationEventService.triggerRefresh();
      },
      error: (err) => {
        console.error('Failed to save the vault entry:', err.status, err.message, err.error);
        let detailedError = 'Failed to save the vault entry.';
        if (err.error && typeof err.error === 'string') {
          detailedError = err.error;
        } else if (err.error && err.error.message) {
          detailedError = err.error.message;
        }
        alert('DEBUG: HTTP Status: ' + err.status + ' message: ' + detailedError);
        this.errorMessage = detailedError;
        this.closeModal();
      }
    });
  }

  toggleFavorite(event: Event, entry: VaultEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    this.vaultService.toggleFavorite(entry.id).subscribe({
      next: () => {
        this.loadEntries();
        this.notificationEventService.triggerRefresh();
      },
      error: () => { this.errorMessage = 'Failed to toggle favorite status.'; }
    });
  }

  deleteEntry(event: Event, entry: VaultEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    if (confirm(`Are you sure you want to move "${entry.title}" to the trash?`)) {
      this.vaultService.deleteEntry(entry.id).subscribe({
        next: () => {
          this.loadEntries();
          this.notificationEventService.triggerRefresh();
        },
        error: () => { this.errorMessage = 'Failed to move entry to trash.'; }
      });
    }
  }

  // ── Trash actions ───────────────────────────────────────────────────────────

  restoreEntry(event: Event, entry: TrashEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    this.vaultService.restoreEntry(entry.id).subscribe({
      next: () => { this.loadTrashEntries(); },
      error: () => { this.errorMessage = 'Failed to restore entry.'; }
    });
  }

  permanentDeleteEntry(event: Event, entry: TrashEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    if (confirm(`Permanently delete "${entry.title}"? This cannot be undone.`)) {
      this.vaultService.permanentDelete(entry.id).subscribe({
        next: () => {
          this.loadTrashEntries();
          this.notificationEventService.triggerRefresh();
        },
        error: () => { this.errorMessage = 'Failed to permanently delete entry.'; }
      });
    }
  }

  /**
   * Fallback when a favicon image fails to load — replace with the initial letter.
   */
  onFaviconError(event: Event, title: string | undefined) {
    const img = event.target as HTMLImageElement;
    // Hide the broken image
    img.style.display = 'none';
    // Show the sibling initial span
    const parent = img.parentElement;
    if (parent) {
      const span = parent.querySelector('.entry-initial') as HTMLElement;
      if (span) {
        span.style.display = 'flex';
      } else {
        // Create one if it doesn't exist
        const newSpan = document.createElement('span');
        newSpan.className = 'entry-initial';
        newSpan.textContent = title ? title.charAt(0).toUpperCase() : '?';
        parent.appendChild(newSpan);
      }
    }
  }

  emptyTrash() {
    if (confirm('Empty trash? All items will be permanently deleted.')) {
      this.vaultService.emptyTrash().subscribe({
        next: () => {
          this.loadTrashEntries();
          this.notificationEventService.triggerRefresh();
        },
        error: () => { this.errorMessage = 'Failed to empty trash.'; }
      });
    }
  }

  // ── Copy-password flow (existing) ──────────────────────────────────────────

  requestPasswordCopy(event: Event, entry: VaultEntryResponse) {
    event.stopPropagation();
    if (!entry.id) return;
    this.entryToDecrypt = entry.id;
    this.showMasterPasswordModal = true;
  }

  handlePasswordDecrypted(password: string) {
    this.showMasterPasswordModal = false;
    navigator.clipboard.writeText(password).then(() => {
      if (this.entryToDecrypt) {
        this.copiedEntryId = this.entryToDecrypt;
        if (this.decryptedPasswordTimeout) clearTimeout(this.decryptedPasswordTimeout);
        this.decryptedPasswordTimeout = setTimeout(() => {
          this.copiedEntryId = null;
          this.entryToDecrypt = null;
        }, 2000);
      }
    }).catch(err => {
      console.error('Failed to copy password:', err);
      this.errorMessage = 'Failed to copy password to clipboard.';
    });
  }

  closeMasterPasswordModal() {
    this.showMasterPasswordModal = false;
    this.entryToDecrypt = null;
  }

  // ── Sensitive-unlock-for-edit flow ─────────────────────────────────────────

  onSensitiveUnlockRequested(entryId: number) {
    this.sensitiveUnlockEntryId = entryId;
    this.showSensitiveUnlockModal = true;
  }

  onEntryUnlocked(detail: VaultEntryDetailResponse) {
    this.showSensitiveUnlockModal = false;
    this.sensitiveUnlockEntryId = null;
    this.unlockedEntry = detail;
  }

  closeSensitiveUnlockModal() {
    this.showSensitiveUnlockModal = false;
    this.sensitiveUnlockEntryId = null;
  }
}
