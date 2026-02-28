import { Component, inject, signal, computed, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthStateService } from '../../core/state/auth.state';
import { UiStateService } from '../../core/state/ui.state';
import { AuthService } from '../../core/services/auth.service';

interface NavItem {
  icon: string;
  label: string;
  route: string;
  badge?: number;
  section?: string;
}

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, FormsModule],
  template: `
    <div class="app-shell">
      <!-- Background gradient layer -->
      <div class="bg-layer"></div>

      <!-- ── Sidebar Island ─────────────────────────────────── -->
      <nav class="sidebar-island" [class.collapsed]="ui.sidebarCollapsed()">

        <!-- Brand -->
        <div class="sidebar-brand">
          <div class="brand-icon">🔐</div>
          <span class="sidebar-brand-text">RevaultX</span>
          <button class="collapse-btn hide-mobile" (click)="ui.toggleSidebar()" title="Toggle sidebar">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path *ngIf="!ui.sidebarCollapsed()" d="M15 18l-6-6 6-6"/>
              <path *ngIf="ui.sidebarCollapsed()" d="M9 18l6-6-6-6"/>
            </svg>
          </button>
        </div>

        <!-- Navigation -->
        <div class="sidebar-nav">
          <!-- Main -->
          <div class="nav-section-label">Main</div>
          <ng-container *ngFor="let item of mainNav">
            <a class="nav-item-pill"
               [routerLink]="item.route"
               routerLinkActive="active"
               [title]="item.label">
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-label">{{ item.label }}</span>
              <span class="nav-badge" *ngIf="item.badge && item.badge > 0">{{ item.badge }}</span>
              <span class="nav-tooltip">{{ item.label }}</span>
            </a>
          </ng-container>

          <!-- Security -->
          <div class="nav-section-label">Security</div>
          <ng-container *ngFor="let item of securityNav">
            <a class="nav-item-pill"
               [routerLink]="item.route"
               routerLinkActive="active"
               [title]="item.label">
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-label">{{ item.label }}</span>
              <span class="nav-badge" *ngIf="item.badge && item.badge > 0">{{ item.badge }}</span>
              <span class="nav-tooltip">{{ item.label }}</span>
            </a>
          </ng-container>

          <!-- Tools -->
          <div class="nav-section-label">Tools</div>
          <ng-container *ngFor="let item of toolsNav">
            <a class="nav-item-pill"
               [routerLink]="item.route"
               routerLinkActive="active"
               [title]="item.label">
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-label">{{ item.label }}</span>
              <span class="nav-tooltip">{{ item.label }}</span>
            </a>
          </ng-container>
        </div>

        <!-- User section -->
        <div class="sidebar-user">
          <div class="user-card" [routerLink]="'/settings'">
            <div class="user-avatar">
              {{ userInitial() }}
            </div>
            <div class="user-info">
              <div class="user-name">{{ auth.username() ?? 'User' }}</div>
              <div class="user-email">{{ auth.currentUser()?.email ?? '' }}</div>
            </div>
          </div>
          <button class="nav-item-pill logout-btn" (click)="logout()" title="Sign out">
            <span class="nav-icon">🚪</span>
            <span class="nav-label">Sign Out</span>
            <span class="nav-tooltip">Sign Out</span>
          </button>
        </div>
      </nav>

      <!-- ── Topbar Island ───────────────────────────────────── -->
      <header class="topbar-island" [class.sidebar-collapsed]="ui.sidebarCollapsed()">
        <!-- Mobile menu button -->
        <button class="pill-btn pill-btn-ghost pill-btn-icon show-mobile" (click)="toggleMobileMenu()">
          ☰
        </button>

        <!-- Search -->
        <div class="topbar-search">
          <span class="search-icon">🔍</span>
          <input
            type="text"
            class="search-input"
            placeholder="Search vault..."
            [(ngModel)]="searchQuery"
            (keyup.enter)="onSearch()"
            [ngModel]="searchQuery"
            (ngModelChange)="searchQuery = $event"
          />
        </div>

        <!-- Spacer -->
        <div style="flex:1"></div>

        <!-- Read-only badge -->
        <span class="pill-badge badge-warning" *ngIf="auth.isReadOnly()">
          🔒 Read Only
        </span>

        <!-- Notifications -->
        <button class="topbar-action-btn" (click)="toggleNotifications()" title="Notifications">
          🔔
          <span class="action-badge" *ngIf="unreadCount() > 0">{{ unreadCount() }}</span>
        </button>

        <!-- Settings -->
        <a class="topbar-action-btn" [routerLink]="'/settings'" title="Settings">
          ⚙️
        </a>

        <!-- User avatar -->
        <div class="topbar-avatar" [routerLink]="'/settings'">
          {{ userInitial() }}
        </div>
      </header>

      <!-- ── Content Island ─────────────────────────────────── -->
      <main class="content-island" [class.sidebar-collapsed]="ui.sidebarCollapsed()">
        <router-outlet />
      </main>

      <!-- ── Bottom Nav (Mobile) ────────────────────────────── -->
      <nav class="bottom-nav-island">
        <a class="bottom-nav-pill" routerLink="/dashboard" routerLinkActive="active">
          <span class="nav-icon">🏠</span>
          <span>Home</span>
        </a>
        <a class="bottom-nav-pill" routerLink="/vault" routerLinkActive="active">
          <span class="nav-icon">🔑</span>
          <span>Vault</span>
        </a>
        <a class="bottom-nav-pill" routerLink="/generator" routerLinkActive="active">
          <span class="nav-icon">⚡</span>
          <span>Generate</span>
        </a>
        <a class="bottom-nav-pill" routerLink="/security" routerLinkActive="active">
          <span class="nav-icon">🛡️</span>
          <span>Security</span>
        </a>
        <a class="bottom-nav-pill" routerLink="/settings" routerLinkActive="active">
          <span class="nav-icon">⚙️</span>
          <span>Settings</span>
        </a>
      </nav>

      <!-- ── Toast Container ────────────────────────────────── -->
      <div class="toast-container">
        <div
          *ngFor="let toast of ui.toasts()"
          class="toast-pill"
          [class]="'toast-' + toast.type"
          (click)="ui.removeToast(toast.id)"
        >
          <span class="toast-icon">
            {{ toast.type === 'success' ? '✅' : toast.type === 'error' ? '❌' : toast.type === 'warning' ? '⚠️' : 'ℹ️' }}
          </span>
          <div class="toast-content">
            <div class="toast-title">{{ toast.title }}</div>
            <div class="toast-message">{{ toast.message }}</div>
          </div>
          <button class="toast-close">✕</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    /* Topbar sidebar-collapsed adjustment */
    .topbar-island.sidebar-collapsed {
      left: calc(var(--sidebar-collapsed) + var(--island-gap-lg) * 2);
    }
    .content-island.sidebar-collapsed {
      left: calc(var(--sidebar-collapsed) + var(--island-gap-lg) * 2);
    }

    /* User avatar */
    .user-avatar {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 700;
      color: #fff;
      flex-shrink: 0;
    }

    .topbar-avatar {
      width: 34px;
      height: 34px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 13px;
      font-weight: 700;
      color: #fff;
      cursor: pointer;
      flex-shrink: 0;
      transition: transform var(--transition-fast);
      &:hover { transform: scale(1.05); }
    }

    /* Topbar search */
    .topbar-search {
      position: relative;
      display: flex;
      align-items: center;
      max-width: 320px;
      width: 100%;
    }
    .search-icon {
      position: absolute;
      left: 12px;
      font-size: 14px;
      pointer-events: none;
    }
    .search-input {
      width: 100%;
      padding: 8px 16px 8px 36px;
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      border-radius: var(--pill-radius-full);
      color: var(--text-primary);
      font-size: var(--font-size-sm);
      outline: none;
      transition: all var(--transition-fast);
      &::placeholder { color: var(--text-muted); }
      &:focus {
        border-color: var(--accent-primary);
        box-shadow: 0 0 0 3px rgba(99,102,241,0.15);
      }
    }

    /* Topbar action buttons */
    .topbar-action-btn {
      position: relative;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: transparent;
      border: none;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
      color: var(--text-secondary);
      transition: all var(--transition-fast);
      text-decoration: none;
      &:hover {
        background: var(--bg-hover);
        color: var(--text-primary);
      }
    }
    .action-badge {
      position: absolute;
      top: 2px;
      right: 2px;
      min-width: 16px;
      height: 16px;
      padding: 0 4px;
      background: var(--accent-danger);
      color: #fff;
      font-size: 9px;
      font-weight: 700;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    /* Logout button */
    .logout-btn {
      width: 100%;
      color: var(--text-muted);
      &:hover { color: var(--accent-danger); background: rgba(239,68,68,0.08); }
    }

    /* Toast container */
    .toast-container {
      position: fixed;
      bottom: 24px;
      right: 24px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      z-index: var(--z-toast);
      max-width: 360px;
    }
    .toast-pill {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 12px 16px;
      border-radius: var(--pill-radius-md);
      background: var(--bg-elevated);
      border: 1px solid var(--border-default);
      box-shadow: var(--island-shadow);
      cursor: pointer;
      animation: slideUpFade 300ms ease both;
      &.toast-success { border-color: var(--accent-success); }
      &.toast-error   { border-color: var(--accent-danger); }
      &.toast-warning { border-color: var(--accent-warning); }
      &.toast-info    { border-color: var(--accent-info); }
    }
    .toast-icon { font-size: 18px; flex-shrink: 0; margin-top: 1px; }
    .toast-content { flex: 1; }
    .toast-title { font-size: var(--font-size-sm); font-weight: 600; color: var(--text-primary); }
    .toast-message { font-size: var(--font-size-xs); color: var(--text-secondary); margin-top: 2px; }
    .toast-close {
      background: none; border: none; color: var(--text-muted);
      cursor: pointer; font-size: 12px; padding: 0; flex-shrink: 0;
      &:hover { color: var(--text-primary); }
    }
  `]
})
export class MainLayoutComponent implements OnInit {
  auth = inject(AuthStateService);
  ui = inject(UiStateService);
  private authService = inject(AuthService);
  private router = inject(Router);

  searchQuery = '';
  unreadCount = signal(0);
  showNotifications = signal(false);

  userInitial = computed(() => {
    const name = this.auth.username() ?? this.auth.currentUser()?.email ?? 'U';
    return name.charAt(0).toUpperCase();
  });

  mainNav: NavItem[] = [
    { icon: '🏠', label: 'Dashboard', route: '/dashboard' },
    { icon: '🔑', label: 'Vault', route: '/vault' },
    { icon: '⚡', label: 'Generator', route: '/generator' },
    { icon: '🗂️', label: 'Teams', route: '/teams' },
  ];

  securityNav: NavItem[] = [
    { icon: '🛡️', label: 'Security Center', route: '/security' },
    { icon: '🔗', label: 'Secure Sharing', route: '/sharing' },
    { icon: '🚨', label: 'Emergency Access', route: '/emergency' },
  ];

  toolsNav: NavItem[] = [
    { icon: '🤖', label: 'AI Assistant', route: '/ai' },
    { icon: '📁', label: 'File Vault', route: '/files' },
    { icon: '💾', label: 'Backup & Export', route: '/backup' },
    { icon: '🗑️', label: 'Trash', route: '/trash' },
    { icon: '⚙️', label: 'Settings', route: '/settings' },
  ];

  ngOnInit(): void {
    // Load user profile if not loaded
    if (!this.auth.currentUser()) {
      this.authService.getProfile().subscribe({
        next: user => this.auth.setUser(user),
        error: () => {}
      });
    }
    // Load settings
    if (!this.auth.settings()) {
      this.authService.getSettings().subscribe({
        next: settings => this.auth.setSettings(settings),
        error: () => {}
      });
    }
  }

  toggleMobileMenu(): void {
    this.ui.sidebarMobileOpen.update(v => !v);
  }

  toggleNotifications(): void {
    this.showNotifications.update(v => !v);
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.router.navigate(['/vault'], { queryParams: { keyword: this.searchQuery } });
    }
  }

  logout(): void {
    this.authService.logout().subscribe({
      complete: () => {
        this.auth.clearAuth();
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.auth.clearAuth();
        this.router.navigate(['/auth/login']);
      }
    });
  }
}
