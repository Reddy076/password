import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserControllerService } from '../../core/api/api/userController.service';
import { VaultService } from '../../core/api/api/vault.service';
import { DashboardResponse } from '../../core/api/model/dashboardResponse';
import { HeatmapResponse } from '../../core/api/model/heatmapResponse';
import { forkJoin } from 'rxjs';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule, LucideAngularModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private userControllerService = inject(UserControllerService);
  private vaultService = inject(VaultService);

  username = 'User';
  dashboardData: DashboardResponse | null = null;
  heatmapData: HeatmapResponse | null = null;
  trashCount: number = 0;
  isLoading = true;
  errorMessage = '';

  ngOnInit() {
    this.extractUsername();
    this.loadDashboardData();
  }

  private extractUsername() {
    try {
      const token = localStorage.getItem('access_token');
      if (token) {
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.username = payload.sub || 'User';
      }
    } catch {
      // ignore parse errors
    }
  }

  private loadDashboardData() {
    forkJoin({
      dashboard: this.userControllerService.getDashboard(),
      heatmap: this.userControllerService.getActivityHeatmap(),
      trash: this.vaultService.getTrashCount()
    }).subscribe({
      next: (result) => {
        this.dashboardData = result.dashboard;
        this.heatmapData = result.heatmap;
        this.trashCount = result.trash.count || 0;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load dashboard and security analytics.';
        this.isLoading = false;
      }
    });
  }

  /**
   * Compute a 0–100 security health score based on vault issues.
   * 100 = perfect, deductions for weak/reused/old passwords.
   */
  get securityScore(): number {
    if (!this.dashboardData) return 0;
    const total = this.dashboardData.totalVaultEntries || 0;
    if (total === 0) return 100;

    const weak = this.dashboardData.weakPasswordsCount || 0;
    const reused = this.dashboardData.reusedPasswordsCount || 0;
    const old = this.dashboardData.oldPasswordsCount || 0;

    // Weight: weak=3pts, reused=2pts, old=1pt per issue
    const deductions = (weak * 3) + (reused * 2) + (old * 1);
    const maxDeductions = total * 3;
    const score = Math.max(0, Math.round(100 - (deductions / maxDeductions) * 100));
    return score;
  }

  get scoreColor(): string {
    const s = this.securityScore;
    if (s >= 80) return '#10B981'; // green
    if (s >= 50) return '#F59E0B'; // amber
    return '#EF4444';              // red
  }

  get scoreLabel(): string {
    const s = this.securityScore;
    if (s >= 80) return 'Strong';
    if (s >= 50) return 'Fair';
    return 'At Risk';
  }

  /**
   * SVG donut ring: returns the stroke-dasharray value for the filled arc.
   * Circle circumference = 2 * π * r = 2 * π * 54 ≈ 339.3
   */
  get donutDashArray(): string {
    const circumference = 339.3;
    const filled = (this.securityScore / 100) * circumference;
    return `${filled} ${circumference - filled}`;
  }
}
