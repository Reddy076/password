import {
  Component,
  OnInit,
  OnDestroy,
  HostListener,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { interval, Subscription, merge } from 'rxjs';
import { switchMap, startWith } from 'rxjs/operators';
import { NotificationControllerService } from '../../core/api/api/notificationController.service';
import { NotificationDTO } from '../../core/api/model/notificationDTO';
import { NotificationEventService } from '../../core/services/notification-event.service';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.component.html',
  styleUrl: './notification-bell.component.css',
})
export class NotificationBellComponent implements OnInit, OnDestroy {
  private notificationService = inject(NotificationControllerService);
  private notificationEventService = inject(NotificationEventService);

  notifications = signal<NotificationDTO[]>([]);
  unreadCount = signal<number>(0);
  dropdownOpen = signal<boolean>(false);
  loading = signal<boolean>(false);

  hasUnread = computed(() => this.unreadCount() > 0);

  private pollSub?: Subscription;
  private readonly POLL_INTERVAL_MS = 30_000;

  ngOnInit(): void {
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  private startPolling(): void {
    this.pollSub = merge(
      interval(this.POLL_INTERVAL_MS).pipe(startWith(0)),
      this.notificationEventService.refresh$
    )
      .pipe(
        switchMap(() => this.notificationService.getUnreadCount())
      )
      .subscribe({
        next: (res) => this.unreadCount.set(res.unreadCount ?? 0),
        error: () => { },
      });
  }

  toggleDropdown(): void {
    if (!this.dropdownOpen()) {
      this.openDropdown();
    } else {
      this.dropdownOpen.set(false);
    }
  }

  private openDropdown(): void {
    this.loading.set(true);
    this.dropdownOpen.set(true);
    this.notificationService.getNotifications().subscribe({
      next: (items) => {
        this.notifications.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  markAsRead(n: NotificationDTO, event: Event): void {
    event.stopPropagation();
    if (n.read || n.id == null) return;
    this.notificationService.markAsRead(n.id).subscribe({
      next: () => {
        this.notifications.update((list) =>
          list.map((item) => (item.id === n.id ? { ...item, read: true } : item))
        );
        this.unreadCount.update((c) => Math.max(0, c - 1));
      },
      error: () => { },
    });
  }

  markAllAsRead(event: Event): void {
    event.stopPropagation();
    this.notificationService.markAllAsRead().subscribe({
      next: () => {
        this.notifications.update((list) =>
          list.map((item) => ({ ...item, read: true }))
        );
        this.unreadCount.set(0);
      },
      error: () => { },
    });
  }

  deleteNotification(n: NotificationDTO, event: Event): void {
    event.stopPropagation();
    if (n.id == null) return;
    this.notificationService.deleteNotification(n.id).subscribe({
      next: () => {
        const wasUnread = !n.read;
        this.notifications.update((list) =>
          list.filter((item) => item.id !== n.id)
        );
        if (wasUnread) {
          this.unreadCount.update((c) => Math.max(0, c - 1));
        }
      },
      error: () => { },
    });
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.notification-bell-wrapper')) {
      this.dropdownOpen.set(false);
    }
  }

  formatTime(dateStr?: string): string {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60_000);
    if (diffMins < 1) return 'just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHrs = Math.floor(diffMins / 60);
    if (diffHrs < 24) return `${diffHrs}h ago`;
    const diffDays = Math.floor(diffHrs / 24);
    return `${diffDays}d ago`;
  }
}
