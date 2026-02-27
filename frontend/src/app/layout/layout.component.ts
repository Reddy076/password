import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule, NgIf } from '@angular/common';
import { AuthenticationService } from '../core/api';
import { IdleService } from '../core/services/idle.service';
import { NotificationBellComponent } from './notification-bell/notification-bell.component';
import { CategoryControllerService } from '../core/api/api/categoryController.service';
import { CategoryDTO } from '../core/api/model/categoryDTO';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, CommonModule, NgIf, NotificationBellComponent,
    LucideAngularModule
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.css'
})
export class LayoutComponent implements OnInit, OnDestroy {
  private router = inject(Router);
  private authService = inject(AuthenticationService);
  private idleService = inject(IdleService);
  private categoryService = inject(CategoryControllerService);

  sidebarCollapsed = false;
  categories: CategoryDTO[] = [];
  categoriesLoading = false;

  ngOnInit() {
    this.idleService.startWatching();
    this.loadCategories();
  }

  ngOnDestroy() {
    this.idleService.stopWatching();
  }

  loadCategories() {
    this.categoriesLoading = true;
    this.categoryService.getAllCategories().subscribe({
      next: (cats) => {
        this.categories = cats || [];
        this.categoriesLoading = false;
      },
      error: () => {
        this.categoriesLoading = false;
      }
    });
  }

  get hasCategories(): boolean {
    return this.categories && this.categories.length > 0;
  }

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }

  logout() {
    this.idleService.stopWatching();
    const token = localStorage.getItem('access_token');

    const finalizeLogout = () => {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      this.router.navigate(['/login']);
    };

    if (token) {
      this.authService.logout(`Bearer ${token}`).subscribe({
        next: () => finalizeLogout(),
        error: () => finalizeLogout() // even if backend fails, clear locally
      });
    } else {
      finalizeLogout();
    }
  }

  get currentUsername(): string {
    try {
      const token = localStorage.getItem('access_token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return payload.sub || 'User';
      }
    } catch {
      // ignore parse errors
    }
    return 'User';
  }
}
