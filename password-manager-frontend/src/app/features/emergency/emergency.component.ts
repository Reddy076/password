import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { UiStateService } from '../../core/state/ui.state';
import {
  EmergencyContact, CreateEmergencyContactRequest,
  EmergencyAccessRequest, RequestEmergencyAccessRequest
} from '../../core/models/security.models';

@Component({
  selector: 'app-emergency',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="content-padding">
      <div class="page-header">
        <div>
          <h1 class="page-title">🚨 Emergency Access</h1>
          <p class="page-subtitle">Grant trusted contacts access to your vault in emergencies</p>
        </div>
      </div>

      <div class="emergency-tabs">
        <button class="tab-btn" [class.active]="activeTab() === 'contacts'" (click)="activeTab.set('contacts')">
          👥 My Contacts
        </button>
        <button class="tab-btn" [class.active]="activeTab() === 'requests'" (click)="activeTab.set('requests')">
          📋 Access Requests
          <span class="tab-badge" *ngIf="pendingRequests() > 0">{{ pendingRequests() }}</span>
        </button>
      </div>

      <!-- Contacts Tab -->
      <div *ngIf="activeTab() === 'contacts'">
        <div class="section-header">
          <h3 class="section-title">Emergency Contacts</h3>
          <button class="pill-btn pill-btn-primary pill-btn-sm" (click)="showAddContact.set(true)">
            + Add Contact
          </button>
        </div>

        <div class="empty-state" *ngIf="contacts().length === 0">
          <div class="empty-icon">👥</div>
          <div class="empty-title">No emergency contacts</div>
          <div class="empty-message">Add trusted contacts who can access your vault in an emergency</div>
        </div>

        <div class="contacts-list" *ngIf="contacts().length > 0">
          <div class="contact-card" *ngFor="let contact of contacts()">
            <div class="contact-avatar">{{ contact.contactName.charAt(0).toUpperCase() }}</div>
            <div class="contact-info">
              <div class="contact-name">{{ contact.contactName }}</div>
              <div class="contact-email">{{ contact.contactEmail }}</div>
              <div class="contact-meta">
                {{ contact.relationship }} · {{ contact.waitingPeriodHours }}h waiting period
              </div>
            </div>
            <div class="contact-status">
              <span class="pill-badge" [class]="contact.verified ? 'badge-success' : 'badge-warning'">
                {{ contact.verified ? '✅ Verified' : '⏳ Pending' }}
              </span>
              <span class="pill-badge" [class]="contact.active ? 'badge-primary' : 'badge-secondary'">
                {{ contact.active ? 'Active' : 'Inactive' }}
              </span>
            </div>
            <button class="icon-btn icon-btn-danger" (click)="removeContact(contact)" title="Remove">🗑️</button>
          </div>
        </div>
      </div>

      <!-- Requests Tab -->
      <div *ngIf="activeTab() === 'requests'">
        <div class="section-header">
          <h3 class="section-title">Access Requests</h3>
          <button class="pill-btn pill-btn-secondary pill-btn-sm" (click)="showRequestAccess.set(true)">
            Request Access
          </button>
        </div>

        <div class="empty-state" *ngIf="requests().length === 0">
          <div class="empty-icon">📋</div>
          <div class="empty-title">No access requests</div>
          <div class="empty-message">Emergency access requests will appear here</div>
        </div>

        <div class="requests-list" *ngIf="requests().length > 0">
          <div class="request-card" *ngFor="let req of requests()"
            [class]="'request-' + req.status.toLowerCase()">
            <div class="request-info">
              <div class="request-title">{{ req.contactName }} → {{ req.ownerUsername }}</div>
              <div class="request-meta">
                Requested {{ formatDate(req.requestedAt) }}
                <span *ngIf="req.status === 'PENDING'">
                  · Auto-approves in {{ req.hoursUntilAutoApproval }}h
                </span>
              </div>
              <div class="request-message" *ngIf="req.requestMessage">
                "{{ req.requestMessage }}"
              </div>
            </div>
            <div class="request-status">
              <span class="pill-badge" [class]="getStatusBadge(req.status)">{{ req.status }}</span>
            </div>
            <div class="request-actions" *ngIf="req.status === 'PENDING'">
              <button class="pill-btn pill-btn-success pill-btn-sm" (click)="approveRequest(req)">Approve</button>
              <button class="pill-btn pill-btn-danger pill-btn-sm" (click)="denyRequest(req)">Deny</button>
            </div>
            <div *ngIf="req.status === 'APPROVED' && req.accessToken">
              <a [href]="'/emergency/vault/' + req.accessToken" target="_blank"
                class="pill-btn pill-btn-primary pill-btn-sm">
                View Vault →
              </a>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add Contact Modal -->
    <div class="modal-overlay" *ngIf="showAddContact()" (click)="showAddContact.set(false)">
      <div class="modal-panel" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Add Emergency Contact</h3>
          <button class="icon-btn" (click)="showAddContact.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <div class="pill-input-wrapper">
            <label class="pill-label">Contact Name *</label>
            <input type="text" class="pill-input" placeholder="Full name"
              [(ngModel)]="contactForm.contactName" name="name" />
          </div>
          <div class="pill-input-wrapper">
            <label class="pill-label">Email *</label>
            <div class="pill-input-inner">
              <span class="pill-input-icon icon-left">📧</span>
              <input type="email" class="pill-input has-icon-left" placeholder="contact@example.com"
                [(ngModel)]="contactForm.contactEmail" name="email" />
            </div>
          </div>
          <div class="pill-input-wrapper">
            <label class="pill-label">Relationship</label>
            <input type="text" class="pill-input" placeholder="e.g. Spouse, Parent, Lawyer"
              [(ngModel)]="contactForm.relationship" name="relationship" />
          </div>
          <div class="pill-input-wrapper">
            <label class="pill-label">Waiting Period (hours)</label>
            <input type="number" class="pill-input" min="1" max="168"
              [(ngModel)]="contactForm.waitingPeriodHours" name="waiting" />
            <span class="pill-input-hint">Time before auto-approval (1-168 hours)</span>
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showAddContact.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-primary" (click)="addContact()"
            [class.loading]="adding()" [disabled]="adding()">
            <span class="btn-text">Add Contact</span>
            <span class="btn-spinner" *ngIf="adding()"></span>
          </button>
        </div>
      </div>
    </div>

    <!-- Request Access Modal -->
    <div class="modal-overlay" *ngIf="showRequestAccess()" (click)="showRequestAccess.set(false)">
      <div class="modal-panel modal-sm" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h3 class="modal-title">Request Emergency Access</h3>
          <button class="icon-btn" (click)="showRequestAccess.set(false)">✕</button>
        </div>
        <div class="modal-body">
          <div class="pill-input-wrapper">
            <label class="pill-label">Account Username *</label>
            <input type="text" class="pill-input" placeholder="Username of the account"
              [(ngModel)]="requestForm.ownerUsername" name="owner" />
          </div>
          <div class="pill-input-wrapper">
            <label class="pill-label">Message</label>
            <textarea class="pill-input" rows="3" placeholder="Explain why you need access..."
              [(ngModel)]="requestForm.requestMessage" name="message"
              style="border-radius:var(--pill-radius-md);resize:none"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="pill-btn pill-btn-secondary" (click)="showRequestAccess.set(false)">Cancel</button>
          <button class="pill-btn pill-btn-primary" (click)="requestAccess()">Send Request</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header { padding: 24px 28px 0; display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
    .page-title { font-size: var(--font-size-2xl); font-weight: 700; color: var(--text-primary); margin: 0 0 4px; }
    .page-subtitle { font-size: var(--font-size-sm); color: var(--text-secondary); margin: 0; }

    .emergency-tabs { display: flex; gap: 4px; padding: 16px 28px 0; border-bottom: 1px solid var(--border-subtle); margin-bottom: 20px; }
    .tab-btn { display: flex; align-items: center; gap: 6px; padding: 8px 16px; border-radius: var(--pill-radius-full) var(--pill-radius-full) 0 0; background: none; border: none; cursor: pointer; font-size: 13px; color: var(--text-secondary); transition: all var(--transition-fast); &:hover { color: var(--text-primary); background: var(--bg-hover); } &.active { color: var(--accent-primary); background: var(--bg-active); font-weight: 600; } }
    .tab-badge { min-width: 18px; height: 18px; padding: 0 5px; background: var(--accent-danger); color: #fff; font-size: 10px; font-weight: 700; border-radius: 9px; display: flex; align-items: center; justify-content: center; }

    .section-header { display: flex; align-items: center; justify-content: space-between; padding: 0 28px; margin-bottom: 12px; }
    .section-title { font-size: 15px; font-weight: 600; color: var(--text-primary); margin: 0; }

    .contacts-list { display: flex; flex-direction: column; gap: 10px; padding: 0 28px; }
    .contact-card { display: flex; align-items: center; gap: 12px; padding: 14px 16px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--pill-radius-lg); }
    .contact-avatar { width: 44px; height: 44px; border-radius: 50%; background: linear-gradient(135deg, var(--accent-primary), var(--accent-secondary)); display: flex; align-items: center; justify-content: center; font-size: 18px; font-weight: 700; color: #fff; flex-shrink: 0; }
    .contact-info { flex: 1; }
    .contact-name { font-size: 14px; font-weight: 700; color: var(--text-primary); }
    .contact-email { font-size: 12px; color: var(--text-muted); }
    .contact-meta { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
    .contact-status { display: flex; flex-direction: column; gap: 4px; align-items: flex-end; flex-shrink: 0; }

    .requests-list { display: flex; flex-direction: column; gap: 10px; padding: 0 28px; }
    .request-card { display: flex; align-items: center; gap: 12px; padding: 14px 16px; background: var(--bg-surface); border: 1px solid var(--border-subtle); border-radius: var(--pill-radius-lg); border-left: 4px solid; &.request-pending { border-left-color: var(--accent-warning); } &.request-approved { border-left-color: var(--accent-success); } &.request-denied { border-left-color: var(--accent-danger); } &.request-expired { border-left-color: var(--text-muted); } }
    .request-info { flex: 1; }
    .request-title { font-size: 13px; font-weight: 600; color: var(--text-primary); }
    .request-meta { font-size: 11px; color: var(--text-muted); margin-top: 2px; }
    .request-message { font-size: 12px; color: var(--text-secondary); font-style: italic; margin-top: 4px; }
    .request-actions { display: flex; gap: 6px; flex-shrink: 0; }

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
  `]
})
export class EmergencyComponent implements OnInit {
  private http = inject(HttpClient);
  private uiState = inject(UiStateService);

  activeTab = signal<'contacts' | 'requests'>('contacts');
  contacts = signal<EmergencyContact[]>([]);
  requests = signal<EmergencyAccessRequest[]>([]);
  loading = signal(true);
  adding = signal(false);
  showAddContact = signal(false);
  showRequestAccess = signal(false);

  pendingRequests = signal(0);

  contactForm: CreateEmergencyContactRequest = {
    contactEmail: '', contactName: '', relationship: '', waitingPeriodHours: 24
  };
  requestForm: RequestEmergencyAccessRequest = { ownerUsername: '', requestMessage: '' };

  ngOnInit(): void {
    this.loadContacts();
    this.loadRequests();
  }

  loadContacts(): void {
    this.http.get<EmergencyContact[]>('/api/emergency/contacts').subscribe({
      next: (c) => { this.contacts.set(c); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }

  loadRequests(): void {
    this.http.get<EmergencyAccessRequest[]>('/api/emergency/requests').subscribe({
      next: (r) => {
        this.requests.set(r);
        this.pendingRequests.set(r.filter(x => x.status === 'PENDING').length);
      },
      error: () => {}
    });
  }

  addContact(): void {
    if (!this.contactForm.contactEmail || !this.contactForm.contactName) return;
    this.adding.set(true);
    this.http.post<EmergencyContact>('/api/emergency/contacts', this.contactForm).subscribe({
      next: (contact) => {
        this.adding.set(false);
        this.contacts.update(list => [...list, contact]);
        this.showAddContact.set(false);
        this.contactForm = { contactEmail: '', contactName: '', relationship: '', waitingPeriodHours: 24 };
        this.uiState.showSuccess('Emergency contact added.', 'Added');
      },
      error: () => { this.adding.set(false); this.uiState.showError('Failed to add contact.'); }
    });
  }

  removeContact(contact: EmergencyContact): void {
    if (!confirm(`Remove ${contact.contactName} as emergency contact?`)) return;
    this.http.delete(`/api/emergency/contacts/${contact.id}`).subscribe({
      next: () => {
        this.contacts.update(list => list.filter(c => c.id !== contact.id));
        this.uiState.showSuccess('Contact removed.', 'Removed');
      },
      error: () => this.uiState.showError('Failed to remove contact.')
    });
  }

  requestAccess(): void {
    if (!this.requestForm.ownerUsername) return;
    this.http.post('/api/emergency/request', this.requestForm).subscribe({
      next: () => {
        this.showRequestAccess.set(false);
        this.requestForm = { ownerUsername: '', requestMessage: '' };
        this.uiState.showSuccess('Emergency access request sent.', 'Sent');
        this.loadRequests();
      },
      error: () => this.uiState.showError('Failed to send request.')
    });
  }

  approveRequest(req: EmergencyAccessRequest): void {
    this.http.post(`/api/emergency/requests/${req.id}/approve`, {}).subscribe({
      next: () => {
        this.requests.update(list => list.map(r => r.id === req.id ? { ...r, status: 'APPROVED' as const } : r));
        this.pendingRequests.update(n => Math.max(0, n - 1));
        this.uiState.showSuccess('Request approved.', 'Approved');
      },
      error: () => this.uiState.showError('Failed to approve request.')
    });
  }

  denyRequest(req: EmergencyAccessRequest): void {
    this.http.post(`/api/emergency/requests/${req.id}/deny`, {}).subscribe({
      next: () => {
        this.requests.update(list => list.map(r => r.id === req.id ? { ...r, status: 'DENIED' as const } : r));
        this.pendingRequests.update(n => Math.max(0, n - 1));
        this.uiState.showSuccess('Request denied.', 'Denied');
      },
      error: () => this.uiState.showError('Failed to deny request.')
    });
  }

  getStatusBadge(status: string): string {
    const map: Record<string, string> = {
      'PENDING': 'badge-warning', 'APPROVED': 'badge-success', 'DENIED': 'badge-danger', 'EXPIRED': 'badge-secondary'
    };
    return map[status] ?? 'badge-secondary';
  }

  formatDate(dateStr: string): string {
    return new Date(dateStr).toLocaleString();
  }
}
