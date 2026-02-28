import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { UiStateService } from '../../core/state/ui.state';
import { Team, CreateTeamRequest, InviteMemberRequest, TeamRole } from '../../core/models/security.models';

@Component({
  selector: 'app-teams',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">🗂️ Teams</h1>
          <p class="page-subtitle">Collaborate and share passwords with your team</p>
        </div>
        <button class="pill-btn pill-btn-primary pill-btn-sm" (click)="showCreateModal.set(true)">
          + Create Team
        </button>
      </div>

      <div class="loading-state" *ngIf="loading()">
        <div class="skeleton" style="height:120px;margin-bottom:12px" *ngFor="let i of [1,2]"></div>
      </div>

      <div class="empty-state" *ngIf="!loading() && teams().length === 0">
        <div class="empty-icon">🗂️</div>
        <div class="empty-title">No teams yet</div>
        <div class="empty-message">Create a team to share passwords with colleagues</div>
        <button class="pill-btn pill-btn-primary" (click)="showCreateModal.set(true)" style="margin-top:12px">
          Create First Team
        </button>
      </div>

      <div class="teams-grid" *ngIf="!loading() && teams().length > 0">
        <div class="team-card" *ngFor="let team of teams()" (click)="selectTeam(team)">
          <div class="team-card-header">
            <div class="team-avatar">{{ team.name.charAt(0).toUpperCase() }}</div>
            <div class="team-info">
              <div class="team-name">{{ team.name }}</div>
              <div class="team-desc">{{ team.description ?? 'No description' }}</div>
            </div>
            <span class="pill-badge badge-primary">{{ team.currentUserRole }}</span>
          </div>
          <div class="team-stats">
            <div class="team-stat">
              <span class="stat-num">{{ team.memberCount }}</span>
              <span class="stat-lbl">Members</span>
            </div>
            <div class="team-stat">
              <span class="stat-num">{{ team.sharedEntryCount }}</span>
              <span class="stat-lbl">Shared Passwords</span>
            </div>
          </div>
          <div class="team-members-preview">
            <div class="member-avatar" *ngFor="let member of team.members?.slice(0, 5)">
              {{ member.username.charAt(0).toUpperCase() }}
            </div>
            <div class="member-more" *ngIf="(team.memberCount ?? 0) > 5">+{{ team.memberCount - 5 }}</div>
          </div>
        </div>
      </div>

      <!-- Team Detail -->
      <div class="team-detail" *ngIf="selectedTeam()">
        <div class="team-detail-header">
          <h3>{{ selectedTeam()!.name }}</h3>
          <button class="icon-btn" (click)="selectedTeam.set(null)">✕</button>
        </div>

        <div class="team-detail-body">
          <div class="flex-between" style="margin-bottom:12px">
            <h4 style="margin:0;font-size:14px;color:var(--text-secondary)">Members</h4>
            <button class="pill-btn pill-btn-secondary pill-btn-sm"
              *ngIf="canManage()" (click)="showInviteModal.set(true)">
              + Invite Member
            </button>
          </div>

          <div class="members-list">
            <div class="member-row" *ngFor="let member of selectedTeam()!.members">
              <div class="member-avatar-lg">{{ member.username.charAt(0).toUpperCase() }}</div>
              <div class="member-info">
                <div class="member-name">{{ member.username }}</div>
                <div class="member-email">{{ member.email }}</div>
              </div>
              <span class="pill-badge" [class]="getRoleBadge(member.role)">{{ member.role }}</span>
              <button class="icon-btn icon-btn-danger" *ngIf="canManage() && member.role !== 'OWNER'"
                (click)="removeMember(member.memberId)" title="Remove">🗑️</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Create Team Modal -->
    <div class="modal-overlay" *ngIf="showCreateModal()" (click)="showCreateModal.set(false)">
      <div class="modal-panel" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Create Team</h3>
          <button class="icon-btn" (click)="showCreateModal.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <div class="pill-input-wrapper">
            <label class="pill-label">Team Name *</label>
            <input type="text" class="pill-input" placeholder="e.g. Engineering Team"
              [(ngModel)]="createForm.name" name="name" />
          </div>
          <div class="pill-input-wrapper">
            <label class="pill-label">Description</label>
            <textarea class="pill-input" rows="2" placeholder="Optional description"
              [(ngModel)]="createForm.description" name="desc"
              style="border-radius:var(--pill-radius-md);resize:none"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showCreateModal.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-primary" (click)="createTeam()"
            [class.loading]="creating()" [disabled]="creating()">
            <span class="btn-text">Create Team</span>
            <span class="btn-spinner" *ngIf="creating()"></span>
          </button>
        </div>
      </div>
    </div>

    <!-- Invite Modal -->
    <div class="modal-overlay" *ngIf="showInviteModal()" (click)="showInviteModal.set(false)">
      <div class="modal-panel modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Invite Member</h3>
          <button class="icon-btn" (click)="showInviteModal.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <div class="pill-input-wrapper">
            <label class="pill-label">Email *</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">📧</span>
              <input type="email" class="pill-input has-icon-left" placeholder="member@example.com"
                [(ngModel)]="inviteForm.email" name="email" />
            </div>
          </div>
          <div class="pill-input-wrapper">
            <label class="pill-label">Role</label>
            <select class="pill-input" [(ngModel)]="inviteForm.role" name="role">
              <option value="ADMIN">Admin</option>
              <option value="MEMBER">Member</option>
              <option value="VIEWER">Viewer</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showInviteModal.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-primary" (click)="inviteMember()">Send Invite</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .teams-grid {
      display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 16px;
      padding: 20px 28px 0;
    }
    .team-card {
      background: var(--bg-surface); border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl); padding: 20px;
      cursor: pointer; transition: all var(--transition-base);
      display: flex; flex-direction: column; gap: 14px;
      &:hover { border-color: var(--accent-primary); transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.2); }
    }
    .team-card-header { display: flex; align-items: center; gap: 12px; }
    .team-avatar {
      width: 44px; height: 44px; border-radius: 14px;
      background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary));
      display: flex; align-items: center; justify-content: center;
      font-size: 20px; font-weight: 700; color: #fff; flex-shrink: 0;
    }
    .team-info { flex: 1; overflow: hidden; }
    .team-name { font-size: 15px; font-weight: 700; color: var(--text-primary); }
    .team-desc { font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .team-stats { display: flex; gap: 20px; }
    .team-stat { display: flex; flex-direction: column; gap: 2px; }
    .stat-num { font-size: 20px; font-weight: 700; color: var(--text-primary); }
    .stat-lbl { font-size: 11px; color: var(--text-muted); }
    .team-members-preview { display: flex; align-items: center; gap: -4px; }
    .member-avatar {
      width: 28px; height: 28px; border-radius: 50%;
      background: var(--bg-elevated); border: 2px solid var(--bg-surface);
      display: flex; align-items: center; justify-content: center;
      font-size: 12px; font-weight: 700; color: var(--accent-primary);
      margin-left: -6px; &:first-child { margin-left: 0; }
    }
    .member-more { font-size: 11px; color: var(--text-muted); margin-left: 6px; }

    .team-detail {
      margin: 16px 28px 0;
      background: var(--bg-surface); border: 1px solid var(--border-subtle);
      border-radius: var(--pill-radius-xl); overflow: hidden;
    }
    .team-detail-header {
      display: flex; align-items: center; justify-content: space-between;
      padding: 16px 20px; border-bottom: 1px solid var(--border-subtle);
      h3 { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0; }
    }
    .team-detail-body { padding: 16px 20px; }
    .members-list { display: flex; flex-direction: column; gap: 8px; }
    .member-row {
      display: flex; align-items: center; gap: 10px;
      padding: 10px 12px; border-radius: var(--pill-radius-xs);
      &:hover { background: var(--bg-hover); }
    }
    .member-avatar-lg {
      width: 36px; height: 36px; border-radius: 50%;
      background: var(--bg-elevated);
      display: flex; align-items: center; justify-content: center;
      font-size: 14px; font-weight: 700; color: var(--accent-primary); flex-shrink: 0;
    }
    .member-info { flex: 1; }
    .member-name { font-size: 13px; font-weight: 600; color: var(--text-primary); }
    .member-email { font-size: 11px; color: var(--text-muted); }

    .loading-state { padding: 20px 28px; }
    .empty-state { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; gap: 8px; }
    .empty-icon { font-size: 48px; }
    .empty-title { font-size: 16px; font-weight: 600; color: var(--text-primary); }
    .empty-message { font-size: 13px; color: var(--text-muted); }

    .modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.6); backdrop-filter: blur(4px); display: flex; align-items: center; justify-content: center; z-index: var(--z-modal); padding: 20px; }
    .modal-panel { background: var(--bg-surface); border: 1px solid var(--border-default); border-radius: var(--pill-radius-xl); width: 100%; max-width: 480px; display: flex; flex-direction: column; box-shadow: 0 24px 64px rgba(0,0,0,0.5); }
    .modal-sm { max-width: 380px; }
    .modal-header { display: flex; align-items: center; justify-content: space-between; padding: 20px 24px; border-bottom: 1px solid var(--border-subtle); }
    .modal-title { font-size: 16px; font-weight: 700; color: var(--text-primary); margin: 0; }
    .modal-body { padding: 20px 24px; display: flex; flex-direction: column; gap: 14px; }
    .modal-footer { padding: 16px 24px; border-top: 1px solid var(--border-subtle); display: flex; justify-content: flex-end; gap: 10px; }
    .icon-btn { width: 28px; height: 28px; border-radius: var(--pill-radius-xs); background: none; border: none; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 14px; color: var(--text-muted); transition: all var(--transition-fast); &:hover { background: var(--bg-hover); color: var(--text-primary); } &.icon-btn-danger:hover { background: rgba(239,68,68,0.1); color: var(--accent-danger); } }
    select.pill-input { appearance: none; cursor: pointer; }
  `]
})
export class TeamsComponent implements OnInit {
  private http = inject(HttpClient);
  private uiState = inject(UiStateService);

  teams = signal<Team[]>([]);
  selectedTeam = signal<Team | null>(null);
  loading = signal(true);
  creating = signal(false);
  showCreateModal = signal(false);
  showInviteModal = signal(false);

  createForm: CreateTeamRequest = { name: '', description: '' };
  inviteForm: InviteMemberRequest = { email: '', role: 'MEMBER' };

  ngOnInit(): void {
    this.loadTeams();
  }

  loadTeams(): void {
    this.loading.set(true);
    this.http.get<Team[]>('/api/teams').subscribe({
      next: (t) => { this.teams.set(t); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  selectTeam(team: Team): void {
    this.selectedTeam.set(team);
  }

  createTeam(): void {
    if (!this.createForm.name) return;
    this.creating.set(true);
    this.http.post<Team>('/api/teams', this.createForm).subscribe({
      next: (team) => {
        this.creating.set(false);
        this.teams.update(list => [...list, team]);
        this.showCreateModal.set(false);
        this.createForm = { name: '', description: '' };
        this.uiState.showSuccess('Team created!', 'Created');
      },
      error: () => { this.creating.set(false); this.uiState.showError('Failed to create team.'); }
    });
  }

  inviteMember(): void {
    const team = this.selectedTeam();
    if (!team || !this.inviteForm.email) return;
    this.http.post(`/api/teams/${team.id}/invite`, this.inviteForm).subscribe({
      next: () => {
        this.showInviteModal.set(false);
        this.inviteForm = { email: '', role: 'MEMBER' };
        this.uiState.showSuccess('Invitation sent!', 'Invited');
      },
      error: () => this.uiState.showError('Failed to send invitation.')
    });
  }

  removeMember(memberId: number): void {
    const team = this.selectedTeam();
    if (!team) return;
    this.http.delete(`/api/teams/${team.id}/members/${memberId}`).subscribe({
      next: () => {
        this.selectedTeam.update(t => t ? { ...t, members: t.members.filter(m => m.memberId !== memberId) } : null);
        this.uiState.showSuccess('Member removed.', 'Removed');
      },
      error: () => this.uiState.showError('Failed to remove member.')
    });
  }

  canManage(): boolean {
    const team = this.selectedTeam();
    return team?.currentUserRole === 'OWNER' || team?.currentUserRole === 'ADMIN';
  }

  getRoleBadge(role: TeamRole): string {
    const map: Record<TeamRole, string> = {
      'OWNER': 'badge-danger', 'ADMIN': 'badge-warning', 'MEMBER': 'badge-primary', 'VIEWER': 'badge-secondary'
    };
    return map[role] ?? 'badge-secondary';
  }
}
