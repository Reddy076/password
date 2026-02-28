import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class UiStateService {
  // ── Sidebar ────────────────────────────────────────────────
  readonly sidebarCollapsed = signal<boolean>(false);
  readonly sidebarMobileOpen = signal<boolean>(false);

  // ── Loading ────────────────────────────────────────────────
  readonly globalLoading = signal<boolean>(false);

  // ── Toast notifications ────────────────────────────────────
  readonly toasts = signal<Toast[]>([]);

  // ── Health ─────────────────────────────────────────────────
  readonly healthStatus = signal<'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN'>('UNKNOWN');

  // ── Computed ───────────────────────────────────────────────
  readonly isMobile = computed(() => window.innerWidth < 768);

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }

  addToast(toast: Omit<Toast, 'id'>): void {
    const id = Date.now().toString();
    this.toasts.update(toasts => [...toasts, { ...toast, id }]);
    if (toast.duration !== 0) {
      setTimeout(() => this.removeToast(id), toast.duration ?? 4000);
    }
  }

  removeToast(id: string): void {
    this.toasts.update(toasts => toasts.filter(t => t.id !== id));
  }

  showSuccess(message: string, title?: string): void {
    this.addToast({ type: 'success', message, title: title ?? 'Success' });
  }

  showError(message: string, title?: string): void {
    this.addToast({ type: 'error', message, title: title ?? 'Error', duration: 6000 });
  }

  showWarning(message: string, title?: string): void {
    this.addToast({ type: 'warning', message, title: title ?? 'Warning' });
  }

  showInfo(message: string, title?: string): void {
    this.addToast({ type: 'info', message, title: title ?? 'Info' });
  }
}

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number; // ms, 0 = persistent
}
