import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { VaultService } from '../../core/services/vault.service';
import { UiStateService } from '../../core/state/ui.state';
import { AuthStateService } from '../../core/state/auth.state';
import { VaultEntry, VaultEntryRequest, Category, Folder } from '../../core/models/vault.models';

@Component({
  selector: 'app-vault',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="vault-layout">
      <!-- Sidebar: Filters -->
      <aside class="vault-sidebar">
        <div class="vault-sidebar-inner">
          <div class="filter-section">
            <div class="filter-label">View</div>
            <button class="filter-item" [class.active]="activeFilter() === 'all'" (click)="setFilter('all')">
              <span>🔑</span> All Passwords
              <span class="filter-count">{{ entries().length }}</span>
            </button>
            <button class="filter-item" [class.active]="activeFilter() === 'favorites'" (click)="setFilter('favorites')">
              <span>⭐</span> Favorites
              <span class="filter-count">{{ favoriteCount() }}</span>
            </button>
            <button class="filter-item" [class.active]="activeFilter() === 'sensitive'" (click)="setFilter('sensitive')">
              <span>🔒</span> Highly Sensitive
            </button>
          </div>

          <div class="filter-section" *ngIf="categories().length > 0">
            <div class="filter-label">Categories</div>
            <button class="filter-item" [class.active]="activeCategoryId() === null" (click)="activeCategoryId.set(null)">
              <span>📂</span> All Categories
            </button>
            <button class="filter-item" *ngFor="let cat of categories()"
              [class.active]="activeCategoryId() === cat.id"
              (click)="activeCategoryId.set(cat.id)">
              <span>{{ cat.icon ?? '📁' }}</span> {{ cat.name }}
            </button>
          </div>

          <div class="filter-section" *ngIf="folders().length > 0">
            <div class="filter-label">Folders</div>
            <button class="filter-item" [class.active]="activeFolderId() === null" (click)="activeFolderId.set(null)">
              <span>📁</span> All Folders
            </button>
            <button class="filter-item" *ngFor="let folder of folders()"
              [class.active]="activeFolderId() === folder.id"
              (click)="activeFolderId.set(folder.id)">
              <span>📂</span> {{ folder.name }}
            </button>
          </div>
        </div>
      </aside>

      <!-- Main Content -->
      <div class="vault-main">
        <!-- Toolbar -->
        <div class="vault-toolbar">
          <div class="vault-search">
            <span class="search-icon">🔍</span>
            <input type="text" class="search-input" placeholder="Search passwords..."
              [(ngModel)]="searchQuery" (ngModelChange)="onSearch($event)" />
            <button *ngIf="searchQuery" class="search-clear" (click)="clearSearch()">✕</button>
          </div>

          <div class="toolbar-actions">
            <div class="view-toggle">
              <button class="view-btn" [class.active]="viewMode() === 'list'" (click)="viewMode.set('list')" title="List view">☰</button>
              <button class="view-btn" [class.active]="viewMode() === 'grid'" (click)="viewMode.set('grid')" title="Grid view">⊞</button>
            </div>
            <button class="pill-btn pill-btn-primary pill-btn-sm" (click)="openAddModal()" *ngIf="!auth.isReadOnly()">
              + Add Password
            </button>
          </div>
        </div>

        <!-- Loading -->
        <div class="vault-loading" *ngIf="loading()">
          <div class="skeleton" style="height:60px;margin-bottom:8px" *ngFor="let i of [1,2,3,4,5]"></div>
        </div>

        <!-- Empty state -->
        <div class="empty-state" *ngIf="!loading() && filteredEntries().length === 0">
          <div class="empty-icon">🔑</div>
          <div class="empty-title">{{ searchQuery ? 'No results found' : 'No passwords yet' }}</div>
          <div class="empty-message">{{ searchQuery ? 'Try a different search term' : 'Add your first password to get started' }}</div>
          <button class="pill-btn pill-btn-primary" (click)="openAddModal()" *ngIf="!searchQuery && !auth.isReadOnly()">
            + Add Password
          </button>
        </div>

        <!-- List View -->
        <div class="entry-list-view" *ngIf="!loading() && filteredEntries().length > 0 && viewMode() === 'list'">
          <div class="entry-row" *ngFor="let entry of filteredEntries()"
            [class.selected]="selectedEntry()?.id === entry.id">
            <div class="entry-row-favicon" (click)="selectEntry(entry)">
              <img *ngIf="entry.websiteUrl" [src]="getFavicon(entry.websiteUrl)" alt="" (error)="onFaviconError($event)" />
              <span *ngIf="!entry.websiteUrl">{{ entry.title.charAt(0).toUpperCase() }}</span>
            </div>
            <div class="entry-row-info" (click)="selectEntry(entry)">
              <div class="entry-row-title">
                {{ entry.title }}
                <span *ngIf="entry.isFavorite" title="Favorite">⭐</span>
                <span *ngIf="entry.isHighlySensitive" title="Highly Sensitive">🔒</span>
              </div>
              <div class="entry-row-sub">{{ entry.username ?? entry.websiteUrl ?? 'No details' }}</div>
            </div>
            <div class="entry-row-strength">
              <span class="pill-badge" [class]="getStrengthBadge(entry.strengthLabel)">
                {{ formatStrength(entry.strengthLabel) }}
              </span>
            </div>
            <div class="entry-row-actions">
              <button class="icon-btn" (click)="copyPassword(entry)" title="Copy password">📋</button>
              <button class="icon-btn" (click)="toggleFavorite(entry)" title="Toggle favorite">
                {{ entry.isFavorite ? '⭐' : '☆' }}
              </button>
              <button class="icon-btn" (click)="openEditModal(entry)" title="Edit" *ngIf="!auth.isReadOnly()">✏️</button>
              <button class="icon-btn icon-btn-danger" (click)="deleteEntry(entry)" title="Delete" *ngIf="!auth.isReadOnly()">🗑️</button>
            </div>
          </div>
        </div>

        <!-- Grid View -->
        <div class="entry-grid-view" *ngIf="!loading() && filteredEntries().length > 0 && viewMode() === 'grid'">
          <div class="entry-card" *ngFor="let entry of filteredEntries()" (click)="selectEntry(entry)">
            <div class="entry-card-header">
              <div class="entry-card-favicon">
                <img *ngIf="entry.websiteUrl" [src]="getFavicon(entry.websiteUrl)" alt="" (error)="onFaviconError($event)" />
                <span *ngIf="!entry.websiteUrl">{{ entry.title.charAt(0).toUpperCase() }}</span>
              </div>
              <div class="entry-card-badges">
                <span *ngIf="entry.isFavorite">⭐</span>
                <span *ngIf="entry.isHighlySensitive">🔒</span>
              </div>
            </div>
            <div class="entry-card-title">{{ entry.title }}</div>
            <div class="entry-card-sub">{{ entry.username ?? 'No username' }}</div>
            <div class="entry-card-footer">
              <span class="pill-badge" [class]="getStrengthBadge(entry.strengthLabel)">{{ formatStrength(entry.strengthLabel) }}</span>
              <div class="entry-card-actions">
                <button class="icon-btn" (click)="copyPassword(entry); $event.stopPropagation()" title="Copy">📋</button>
                <button class="icon-btn" (click)="openEditModal(entry); $event.stopPropagation()" title="Edit" *ngIf="!auth.isReadOnly()">✏️</button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Detail Panel -->
      <aside class="vault-detail" *ngIf="selectedEntry()">
        <div class="detail-header">
          <div class="detail-favicon">
            <img *ngIf="selectedEntry()!.websiteUrl" [src]="getFavicon(selectedEntry()!.websiteUrl!)" alt="" (error)="onFaviconError($event)" />
            <span *ngIf="!selectedEntry()!.websiteUrl">{{ selectedEntry()!.title.charAt(0).toUpperCase() }}</span>
          </div>
          <div class="detail-title-area">
            <h3 class="detail-title">{{ selectedEntry()!.title }}</h3>
            <div class="detail-badges">
              <span class="pill-badge badge-primary" *ngIf="selectedEntry()!.categoryName">{{ selectedEntry()!.categoryName }}</span>
              <span class="pill-badge badge-secondary" *ngIf="selectedEntry()!.folderName">📁 {{ selectedEntry()!.folderName }}</span>
            </div>
          </div>
          <button class="icon-btn" (click)="selectedEntry.set(null)" title="Close">✕</button>
        </div>

        <div class="detail-body">
          <div class="detail-field" *ngIf="selectedEntry()!.username">
            <div class="detail-field-label">Username</div>
            <div class="detail-field-value">
              <span>{{ selectedEntry()!.username }}</span>
              <button class="icon-btn" (click)="copyText(selectedEntry()!.username!)" title="Copy">📋</button>
            </div>
          </div>

          <div class="detail-field">
            <div class="detail-field-label">Password</div>
            <div class="detail-field-value">
              <span class="password-mask" *ngIf="!showDetailPassword">••••••••••••</span>
              <span class="text-mono" *ngIf="showDetailPassword">{{ revealedPassword }}</span>
              <button class="icon-btn" (click)="toggleRevealPassword()" title="Show/hide">
                {{ showDetailPassword ? '🙈' : '👁️' }}
              </button>
              <button class="icon-btn" (click)="copyPassword(selectedEntry()!)" title="Copy">📋</button>
            </div>
          </div>

          <div class="detail-field" *ngIf="selectedEntry()!.websiteUrl">
            <div class="detail-field-label">Website</div>
            <div class="detail-field-value">
              <a [href]="selectedEntry()!.websiteUrl!" target="_blank" class="detail-link">
                {{ selectedEntry()!.websiteUrl }}
              </a>
            </div>
          </div>

          <div class="detail-field" *ngIf="selectedEntry()!.notes">
            <div class="detail-field-label">Notes</div>
            <div class="detail-notes">{{ selectedEntry()!.notes }}</div>
          </div>

          <div class="detail-field">
            <div class="detail-field-label">Strength</div>
            <div class="strength-meter">
              <div class="strength-bar">
                <div class="strength-segment" *ngFor="let i of [1,2,3,4,5]"
                  [class]="'strength-segment ' + (i <= getStrengthScore(selectedEntry()!.strengthLabel) ? 'filled-' + getStrengthScore(selectedEntry()!.strengthLabel) : '')"></div>
              </div>
              <div class="strength-label">
                <span class="label-text">{{ formatStrength(selectedEntry()!.strengthLabel) }}</span>
                <span class="label-score">{{ selectedEntry()!.strengthScore }}/100</span>
              </div>
            </div>
          </div>

          <div class="detail-meta">
            <div>Created: {{ formatDate(selectedEntry()!.createdAt) }}</div>
            <div>Updated: {{ formatDate(selectedEntry()!.updatedAt) }}</div>
          </div>
        </div>

        <div class="detail-footer" *ngIf="!auth.isReadOnly()">
          <button class="pill-btn pill-btn-secondary pill-btn-sm" (click)="openEditModal(selectedEntry()!)">✏️ Edit</button>
          <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="deleteEntry(selectedEntry()!)">🗑️ Delete</button>
        </div>
      </aside>
    </div>

    <!-- Add/Edit Modal -->
    <div class="modal-overlay" *ngIf="showModal()" (click)="closeModal()">
      <div class="modal-panel" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">{{ editingEntry ? 'Edit Password' : 'Add Password' }}</h3>
          <button class="icon-btn" (click)="closeModal()">✕</button>
        </div>

        <form class="modal-body" (ngSubmit)="saveEntry()">
          <div class="pill-input-wrapper">
            <label class="pill-label">Title *</label>
            <div class="pill-input-inner">
              <input type="text" class="pill-input" placeholder="e.g. Gmail, Netflix"
                [(ngModel)]="form.title" name="title" required [class.error]="formSubmitted && !form.title" />
            </div>
            <span class="pill-input-error" *ngIf="formSubmitted && !form.title">Title is required</span>
          </div>

          <div class="form-row">
            <div class="pill-input-wrapper">
              <label class="pill-label">Username</label>
              <div class="pill-input-inner">
                <span class="pill-input-icon icon-left">👤</span>
                <input type="text" class="pill-input has-icon-left" placeholder="Username or email"
                  [(ngModel)]="form.username" name="username" />
              </div>
            </div>
            <div class="pill-input-wrapper">
              <label class="pill-label">Website URL</label>
              <div class="pill-input-inner">
                <span class="pill-input-icon icon-left">🌐</span>
                <input type="url" class="pill-input has-icon-left" placeholder="https://example.com"
                  [(ngModel)]="form.websiteUrl" name="websiteUrl" />
              </div>
            </div>
          </div>

          <div class="pill-input-wrapper">
            <label class="pill-label">Password</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">🔒</span>
              <input [type]="showFormPassword ? 'text' : 'password'" class="pill-input has-icon-left has-both"
                placeholder="Enter password"
                [(ngModel)]="form.password" name="password"
                (ngModelChange)="checkFormStrength($event)" />
              <button type="button" class="pill-input-icon icon-right"
                (click)="showFormPassword = !showFormPassword" style="background:none;border:none;cursor:pointer;">
                {{ showFormPassword ? '🙈' : '👁️' }}
              </button>
            </div>
            <!-- Strength meter -->
            <div class="strength-meter" *ngIf="form.password" style="margin-top:6px">
              <div class="strength-bar">
                <div class="strength-segment" *ngFor="let i of [1,2,3,4,5]"
                  [class]="'strength-segment ' + (i <= formStrengthScore ? 'filled-' + formStrengthScore : '')"></div>
              </div>
              <div class="strength-label">
                <span class="label-text" [style.color]="formStrengthColor">{{ formStrengthLabel }}</span>
              </div>
            </div>
            <div style="margin-top:6px">
              <button type="button" class="pill-btn pill-btn-ghost pill-btn-sm" (click)="generatePassword()">
                ⚡ Generate Strong Password
              </button>
            </div>
          </div>

          <div class="form-row">
            <div class="pill-input-wrapper">
              <label class="pill-label">Category</label>
              <select class="pill-input" [(ngModel)]="form.categoryId" name="categoryId">
                <option [ngValue]="undefined">No category</option>
                <option *ngFor="let cat of categories()" [ngValue]="cat.id">{{ cat.name }}</option>
              </select>
            </div>
            <div class="pill-input-wrapper">
              <label class="pill-label">Folder</label>
              <select class="pill-input" [(ngModel)]="form.folderId" name="folderId">
                <option [ngValue]="undefined">No folder</option>
                <option *ngFor="let folder of folders()" [ngValue]="folder.id">{{ folder.name }}</option>
              </select>
            </div>
          </div>

          <div class="pill-input-wrapper">
            <label class="pill-label">Notes</label>
            <textarea class="pill-input" rows="3" placeholder="Optional notes..."
              [(ngModel)]="form.notes" name="notes"
              style="border-radius:var(--pill-radius-md);resize:vertical;min-height:80px"></textarea>
          </div>

          <div class="form-toggles">
            <label class="pill-toggle">
              <input type="checkbox" [(ngModel)]="form.isFavorite" name="isFavorite" />
              <span class="toggle-track"></span>
              <span class="toggle-label">⭐ Mark as Favorite</span>
            </label>
            <label class="pill-toggle">
              <input type="checkbox" [(ngModel)]="form.isHighlySensitive" name="isHighlySensitive" />
              <span class="toggle-track"></span>
              <span class="toggle-label">🔒 Highly Sensitive</span>
            </label>
          </div>

          <div class="modal-footer">
            <button type="button" class="pill-btn pill-btn-secondary" (click)="closeModal()">Cancel</button>
            <button type="submit" class="pill-btn pill-btn-primary"
              [class.loading]="saving()" [disabled]="saving()">
              <span class="btn-text">{{ editingEntry ? 'Save Changes' : 'Add Password' }}</span>
              <span class="btn-spinner" *ngIf="saving()"></span>
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Master Password Modal (for viewing sensitive) -->
    <div class="modal-overlay" *ngIf="showMasterPasswordModal()" (click)="showMasterPasswordModal.set(false)">
      <div class="modal-panel modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">🔒 Verify Identity</h3>
          <button class="icon-btn" (click)="showMasterPasswordModal.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <p style="font-size:13px;color:var(--text-secondary);margin:0 0 16px">
            Enter your master password to view this password.
          </p>
          <div class="pill-input-wrapper">
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">🔒</span>
              <input [type]="showMasterPw ? 'text' : 'password'" class="pill-input has-icon-left has-both"
                placeholder="Master password"
                [(ngModel)]="masterPasswordInput" (keyup.enter)="confirmReveal()" />
              <button type="button" class="pill-input-icon icon-right"
                (click)="showMasterPw = !showMasterPw" style="background:none;border:none;cursor:pointer;">
                {{ showMasterPw ? '🙈' : '👁️' }}
              </button>
            </div>
          </div>
          <div class="auth-error" *ngIf="masterPwError" style="margin-top:8px">
            <span>⚠️</span> {{ masterPwError }}
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showMasterPasswordModal.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-primary" (click)="confirmReveal()"
            [class.loading]="revealLoading()" [disabled]="revealLoading()">
            <span class="btn-text">Reveal</span>
            <span class="btn-spinner" *ngIf="revealLoading()"></span>
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .vault-layout {
      display: flex;
      height: 100%;
      overflow: hidden;
    }

    /* Sidebar */
    .vault-sidebar {
      width: 200px;
      flex-shrink: 0;
      border-right: 1px solid var(--border-subtle);
      overflow-y: auto;
      scrollbar-width: none;
      &::-webkit-scrollbar { display: none; }
      @media (max-width: 767px) { display: none; }
    }
    .vault-sidebar-inner { padding: 16px 10px; }
    .filter-section { margin-bottom: 20px; }
    .filter-label {
      font-size: 10px; font-weight: 700; text-transform: uppercase;
      letter-spacing: 0.08em; color: var(--text-muted);
      padding: 0 8px 6px;
    }
    .filter-item {
      display: flex; align-items: center; gap: 8px;
      width: 100%; padding: 8px 10px;
      border-radius: var(--pill-radius-xs);
      background: none; border: none; cursor: pointer;
      font-size: 13px; color: var(--text-secondary);
      transition: all var(--transition-fast);
      text-align: left;
      &:hover { background: var(--bg-hover); color: var(--text-primary); }
      &.active { background: var(--bg-active); color: var(--accent-primary); font-weight: 600; }
    }
    .filter-count {
      margin-left: auto;
      font-size: 11px;
      background: var(--bg-elevated);
      padding: 1px 6px;
      border-radius: 10px;
      color: var(--text-muted);
    }

    /* Main */
    .vault-main {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }

    /* Toolbar */
    .vault-toolbar {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 20px;
      border-bottom: 1px solid var(--border-subtle);
      flex-shrink: 0;
    }
    .vault-search {
      position: relative;
      flex: 1;
      max-width: 400px;
      display: flex;
      align-items: center;
    }
    .search-icon { position: absolute; left: 12px; font-size: 14px; pointer-events: none; }
    .search-input {
      width: 100%; padding: 8px 36px;
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-full);
      color: var(--text-primary); font-size: 13px; outline: none;
      transition: all var(--transition-fast);
      &::placeholder { color: var(--text-muted); }
      &:focus { border-color: var(--accent-primary); box-shadow: 0 0 0 3px rgba(99,102,241,0.15); }
    }
    .search-clear {
      position: absolute; right: 10px;
      background: none; border: none; cursor: pointer;
      color: var(--text-muted); font-size: 12px;
      &:hover { color: var(--text-primary); }
    }
    .toolbar-actions { display: flex; align-items: center; gap: 8px; margin-left: auto; }
    .view-toggle { display: flex; gap: 2px; }
    .view-btn {
      width: 32px; height: 32px;
      border-radius: var(--pill-radius-xs);
      background: none; border: 1px solid var(--border-default);
      cursor: pointer; color: var(--text-muted); font-size: 14px;
      display: flex; align-items: center; justify-content: center;
      transition: all var(--transition-fast);
      &:hover { background: var(--bg-hover); color: var(--text-primary); }
      &.active { background: var(--bg-active); color: var(--accent-primary); border-color: var(--accent-primary); }
    }

    /* List view */
    .entry-list-view {
      flex: 1;
      overflow-y: auto;
      padding: 8px 12px;
    }
    .entry-row {
      display: flex; align-items: center; gap: 12px;
      padding: 10px 12px;
      border-radius: var(--pill-radius-xs);
      cursor: pointer;
      transition: background var(--transition-fast);
      &:hover { background: var(--bg-hover); }
      &.selected { background: var(--bg-active); }
    }
    .entry-row-favicon {
      width: 36px; height: 36px;
      border-radius: 10px;
      background: var(--bg-elevated);
      display: flex; align-items: center; justify-content: center;
      font-size: 16px; font-weight: 700; color: var(--accent-primary);
      flex-shrink: 0; overflow: hidden;
      img { width: 100%; height: 100%; object-fit: cover; }
    }
    .entry-row-info { flex: 1; overflow: hidden; }
    .entry-row-title {
      font-size: 13px; font-weight: 600; color: var(--text-primary);
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
      display: flex; align-items: center; gap: 4px;
    }
    .entry-row-sub { font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .entry-row-strength { flex-shrink: 0; }
    .entry-row-actions {
      display: flex; gap: 4px; flex-shrink: 0;
      opacity: 0; transition: opacity var(--transition-fast);
    }
    .entry-row:hover .entry-row-actions { opacity: 1; }

    /* Grid view */
    .entry-grid-view {
      flex: 1; overflow-y: auto;
      padding: 12px;
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 12px;
      align-content: start;
    }
    .entry-card {
      background: var(--bg-surface);
      border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-lg);
      padding: 16px;
      cursor: pointer;
      transition: all var(--transition-base);
      &:hover { border-color: var(--border-default); transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.2); }
    }
    .entry-card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
    .entry-card-favicon {
      width: 40px; height: 40px;
      border-radius: 12px;
      background: var(--bg-elevated);
      display: flex; align-items: center; justify-content: center;
      font-size: 18px; font-weight: 700; color: var(--accent-primary);
      overflow: hidden;
      img { width: 100%; height: 100%; object-fit: cover; }
    }
    .entry-card-badges { display: flex; gap: 2px; }
    .entry-card-title { font-size: 13px; font-weight: 600; color: var(--text-primary); margin-bottom: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .entry-card-sub { font-size: 11px; color: var(--text-muted); margin-bottom: 12px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .entry-card-footer { display: flex; align-items: center; justify-content: space-between; }
    .entry-card-actions { display: flex; gap: 4px; }

    /* Icon buttons */
    .icon-btn {
      width: 28px; height: 28px;
      border-radius: var(--pill-radius-xs);
      background: none; border: none; cursor: pointer;
      display: flex; align-items: center; justify-content: center;
      font-size: 14px; color: var(--text-muted);
      transition: all var(--transition-fast);
      &:hover { background: var(--bg-hover); color: var(--text-primary); }
      &.icon-btn-danger:hover { background: rgba(239,68,68,0.1); color: var(--accent-danger); }
    }

    /* Detail panel */
    .vault-detail {
      width: 280px;
      flex-shrink: 0;
      border-left: 1px solid var(--border-subtle);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      @media (max-width: 1199px) { display: none; }
    }
    .detail-header {
      display: flex; align-items: flex-start; gap: 10px;
      padding: 16px;
      border-bottom: 1px solid var(--border-subtle);
      flex-shrink: 0;
    }
    .detail-favicon {
      width: 44px; height: 44px;
      border-radius: 12px;
      background: var(--bg-elevated);
      display: flex; align-items: center; justify-content: center;
      font-size: 20px; font-weight: 700; color: var(--accent-primary);
      flex-shrink: 0; overflow: hidden;
      img { width: 100%; height: 100%; object-fit: cover; }
    }
    .detail-title-area { flex: 1; overflow: hidden; }
    .detail-title { font-size: 14px; font-weight: 700; color: var(--text-primary); margin: 0 0 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .detail-badges { display: flex; gap: 4px; flex-wrap: wrap; }
    .detail-body { flex: 1; overflow-y: auto; padding: 16px; display: flex; flex-direction: column; gap: 14px; }
    .detail-field-label { font-size: 10px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.08em; color: var(--text-muted); margin-bottom: 4px; }
    .detail-field-value {
      display: flex; align-items: center; gap: 6px;
      font-size: 13px; color: var(--text-primary);
    }
    .password-mask { letter-spacing: 3px; color: var(--text-muted); }
    .detail-link { color: var(--accent-primary); text-decoration: none; font-size: 12px; word-break: break-all; &:hover { text-decoration: underline; } }
    .detail-notes { font-size: 12px; color: var(--text-secondary); line-height: 1.6; white-space: pre-wrap; }
    .detail-meta { font-size: 11px; color: var(--text-muted); display: flex; flex-direction: column; gap: 2px; }
    .detail-footer {
      padding: 12px 16px;
      border-top: 1px solid var(--border-subtle);
      display: flex; gap: 8px;
      flex-shrink: 0;
    }

    /* Modal */
    .modal-overlay {
      position: fixed; inset: 0;
      background: rgba(0,0,0,0.6);
      backdrop-filter: blur(4px);
      display: flex; align-items: center; justify-content: center;
      z-index: var(--z-modal);
      padding: 20px;
    }
    .modal-panel {
      background: var(--bg-surface);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-xl);
      width: 100%; max-width: 560px;
      max-height: 90vh;
      display: flex; flex-direction: column;
      box-shadow: 0 24px 64px rgba(0,0,0,0.5);
      animation: slideUpFade 300ms ease both;
    }
    .modal-sm { max-width: 400px; }
    .modal-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 20px 24px;
      border-bottom: 1px solid var(--border-subtle);
      flex-shrink: 0;
    }
    .modal-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0; }
    .modal-body { padding: 20px 24px; overflow-y: auto; display: flex; flex-direction: column; gap: 14px; }
    .modal-footer {
      padding: 16px 24px;
      border-top: 1px solid var(--border-subtle);
      display: flex; justify-content: flex-end; gap: 10px;
      flex-shrink: 0;
    }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
    .form-toggles { display: flex; flex-direction: column; gap: 10px; }
    select.pill-input { appearance: none; cursor: pointer; }
    .auth-error {
      padding: 10px 16px;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      border-radius: var(--pill-radius-md);
      color: var(--accent-danger);
      font-size: var(--font-size-sm);
      display: flex; align-items: center; gap: 8px;
    }
    .vault-loading { padding: 12px; }
  `]
})
export class VaultComponent implements OnInit {
  private vaultService = inject(VaultService);
  private uiState = inject(UiStateService);
  auth = inject(AuthStateService);
  private route = inject(ActivatedRoute);

  entries = signal<VaultEntry[]>([]);
  categories = signal<Category[]>([]);
  folders = signal<Folder[]>([]);
  loading = signal(true);
  saving = signal(false);
  revealLoading = signal(false);

  searchQuery = '';
  activeFilter = signal<'all' | 'favorites' | 'sensitive'>('all');
  activeCategoryId = signal<number | null>(null);
  activeFolderId = signal<number | null>(null);
  viewMode = signal<'list' | 'grid'>('list');
  selectedEntry = signal<VaultEntry | null>(null);

  showModal = signal(false);
  showMasterPasswordModal = signal(false);
  editingEntry: VaultEntry | null = null;
  formSubmitted = false;
  showFormPassword = false;
  showDetailPassword = false;
  revealedPassword = '';
  masterPasswordInput = '';
  masterPwError = '';
  showMasterPw = false;
  formStrengthScore = 0;
  formStrengthLabel = '';
  formStrengthColor = '';

  form: VaultEntryRequest = this.emptyForm();

  favoriteCount = computed(() => this.entries().filter(e => e.isFavorite).length);

  filteredEntries = computed(() => {
    let list = this.entries();

    // Filter by type
    if (this.activeFilter() === 'favorites') list = list.filter(e => e.isFavorite);
    if (this.activeFilter() === 'sensitive') list = list.filter(e => e.isHighlySensitive);

    // Filter by category
    if (this.activeCategoryId() !== null) list = list.filter(e => e.categoryId === this.activeCategoryId());

    // Filter by folder
    if (this.activeFolderId() !== null) list = list.filter(e => e.folderId === this.activeFolderId());

    // Search
    if (this.searchQuery) {
      const q = this.searchQuery.toLowerCase();
      list = list.filter(e =>
        e.title.toLowerCase().includes(q) ||
        (e.username ?? '').toLowerCase().includes(q) ||
        (e.websiteUrl ?? '').toLowerCase().includes(q)
      );
    }

    return list;
  });

  ngOnInit(): void {
    this.loadAll();
    // Check for search query from URL
    this.route.queryParams.subscribe(params => {
      if (params['keyword']) {
        this.searchQuery = params['keyword'];
      }
    });
  }

  loadAll(): void {
    this.loading.set(true);
    this.vaultService.getEntries().subscribe({
      next: (entries) => { this.entries.set(entries); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
    this.vaultService.getCategories().subscribe({ next: (c) => this.categories.set(c), error: () => {} });
    this.vaultService.getFolders().subscribe({ next: (f) => this.folders.set(f), error: () => {} });
  }

  setFilter(filter: 'all' | 'favorites' | 'sensitive'): void {
    this.activeFilter.set(filter);
    this.activeCategoryId.set(null);
    this.activeFolderId.set(null);
  }

  onSearch(query: string): void {
    this.searchQuery = query;
  }

  clearSearch(): void {
    this.searchQuery = '';
  }

  selectEntry(entry: VaultEntry): void {
    this.selectedEntry.set(entry);
    this.showDetailPassword = false;
    this.revealedPassword = '';
  }

  openAddModal(): void {
    this.editingEntry = null;
    this.form = this.emptyForm();
    this.formSubmitted = false;
    this.showFormPassword = false;
    this.formStrengthScore = 0;
    this.showModal.set(true);
  }

  openEditModal(entry: VaultEntry): void {
    this.editingEntry = entry;
    this.form = {
      title: entry.title,
      username: entry.username ?? undefined,
      websiteUrl: entry.websiteUrl ?? undefined,
      notes: entry.notes ?? undefined,
      categoryId: entry.categoryId ?? undefined,
      folderId: entry.folderId ?? undefined,
      isFavorite: entry.isFavorite,
      isHighlySensitive: entry.isHighlySensitive,
    };
    this.formSubmitted = false;
    this.showFormPassword = false;
    this.showModal.set(true);
  }

  closeModal(): void {
    this.showModal.set(false);
    this.editingEntry = null;
  }

  saveEntry(): void {
    this.formSubmitted = true;
    if (!this.form.title) return;

    this.saving.set(true);
    const obs = this.editingEntry
      ? this.vaultService.updateEntry(this.editingEntry.id, this.form)
      : this.vaultService.createEntry(this.form);

    obs.subscribe({
      next: (entry) => {
        this.saving.set(false);
        this.closeModal();
        if (this.editingEntry) {
          this.entries.update(list => list.map(e => e.id === entry.id ? entry : e));
          this.uiState.showSuccess('Password updated successfully.', 'Updated');
        } else {
          this.entries.update(list => [entry, ...list]);
          this.uiState.showSuccess('Password added successfully.', 'Added');
        }
        this.selectedEntry.set(entry);
      },
      error: (err) => {
        this.saving.set(false);
        this.uiState.showError(err?.error?.message ?? 'Failed to save password.');
      }
    });
  }

  deleteEntry(entry: VaultEntry): void {
    if (!confirm(`Delete "${entry.title}"? It will be moved to trash.`)) return;
    this.vaultService.deleteEntry(entry.id).subscribe({
      next: () => {
        this.entries.update(list => list.filter(e => e.id !== entry.id));
        if (this.selectedEntry()?.id === entry.id) this.selectedEntry.set(null);
        this.uiState.showSuccess('Moved to trash.', 'Deleted');
      },
      error: () => this.uiState.showError('Failed to delete password.')
    });
  }

  toggleFavorite(entry: VaultEntry): void {
    this.vaultService.toggleFavorite(entry.id).subscribe({
      next: (updated) => {
        this.entries.update(list => list.map(e => e.id === updated.id ? updated : e));
        if (this.selectedEntry()?.id === updated.id) this.selectedEntry.set(updated);
      },
      error: () => {}
    });
  }

  copyPassword(entry: VaultEntry): void {
    if (entry.isHighlySensitive) {
      this.selectedEntry.set(entry);
      this.masterPasswordInput = '';
      this.masterPwError = '';
      this.showMasterPasswordModal.set(true);
      return;
    }
    this.vaultService.viewPassword(entry.id, { masterPassword: '' }).subscribe({
      next: (res) => {
        navigator.clipboard.writeText(res.password);
        this.uiState.showSuccess('Password copied to clipboard!', 'Copied');
      },
      error: () => {
        // Try without master password for non-sensitive
        this.uiState.showError('Could not copy password.');
      }
    });
  }

  toggleRevealPassword(): void {
    if (this.showDetailPassword) {
      this.showDetailPassword = false;
      this.revealedPassword = '';
      return;
    }
    if (this.selectedEntry()?.isHighlySensitive) {
      this.masterPasswordInput = '';
      this.masterPwError = '';
      this.showMasterPasswordModal.set(true);
    } else {
      this.revealPassword('');
    }
  }

  revealPassword(masterPassword: string): void {
    const entry = this.selectedEntry();
    if (!entry) return;
    this.revealLoading.set(true);
    this.vaultService.viewPassword(entry.id, { masterPassword }).subscribe({
      next: (res) => {
        this.revealLoading.set(false);
        this.revealedPassword = res.password;
        this.showDetailPassword = true;
        this.showMasterPasswordModal.set(false);
      },
      error: (err) => {
        this.revealLoading.set(false);
        this.masterPwError = err?.error?.message ?? 'Invalid master password.';
      }
    });
  }

  confirmReveal(): void {
    if (!this.masterPasswordInput) return;
    this.masterPwError = '';
    this.revealPassword(this.masterPasswordInput);
  }

  copyText(text: string): void {
    navigator.clipboard.writeText(text);
    this.uiState.showSuccess('Copied to clipboard!', 'Copied');
  }

  generatePassword(): void {
    this.vaultService.generatePassword({
      length: 16,
      includeUppercase: true,
      includeLowercase: true,
      includeNumbers: true,
      includeSpecial: true,
      excludeSimilar: false,
      excludeAmbiguous: false
    }).subscribe({
      next: (res) => {
        this.form.password = res.password;
        this.checkFormStrength(res.password);
        this.showFormPassword = true;
      },
      error: () => {}
    });
  }

  checkFormStrength(password: string): void {
    let score = 0;
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;
    this.formStrengthScore = score;
    const labels = ['', 'Very Weak', 'Weak', 'Fair', 'Strong', 'Very Strong'];
    const colors = ['', 'var(--accent-danger)', 'var(--accent-orange)', 'var(--accent-warning)', '#84cc16', 'var(--accent-success)'];
    this.formStrengthLabel = labels[score] || 'Very Weak';
    this.formStrengthColor = colors[score] || 'var(--accent-danger)';
  }

  getFavicon(url: string): string {
    try {
      const domain = new URL(url).hostname;
      return `https://www.google.com/s2/favicons?domain=${domain}&sz=32`;
    } catch { return ''; }
  }

  onFaviconError(event: Event): void {
    (event.target as HTMLImageElement).style.display = 'none';
  }

  getStrengthBadge(label: string): string {
    const map: Record<string, string> = {
      'VERY_STRONG': 'badge-success', 'STRONG': 'badge-success',
      'FAIR': 'badge-warning', 'WEAK': 'badge-danger', 'VERY_WEAK': 'badge-danger',
    };
    return map[label] ?? 'badge-secondary';
  }

  formatStrength(label: string): string {
    return label?.replace('_', ' ') ?? 'Unknown';
  }

  getStrengthScore(label: string): number {
    const map: Record<string, number> = {
      'VERY_WEAK': 1, 'WEAK': 2, 'FAIR': 3, 'STRONG': 4, 'VERY_STRONG': 5
    };
    return map[label] ?? 0;
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleDateString();
  }

  private emptyForm(): VaultEntryRequest {
    return { title: '', username: undefined, password: undefined, websiteUrl: undefined, notes: undefined, categoryId: undefined, folderId: undefined, isFavorite: false, isHighlySensitive: false };
  }
}
