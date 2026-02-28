import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  VaultEntry, VaultEntryRequest, VaultSearchParams, ViewPasswordResponse,
  SensitiveViewRequest, BulkDeleteRequest, TrashEntry, TrashCountResponse,
  PasswordSnapshot, Category, CategoryRequest, Folder,
  GeneratorRequest, GeneratorResponse, GeneratorMultipleResponse,
  PasswordStrengthRequest, PasswordStrengthResponse,
  ExpiryStatusResponse, ExpiryPolicy
} from '../models/vault.models';
import { MessageResponse } from '../models/auth.models';

@Injectable({ providedIn: 'root' })
export class VaultService {
  private http = inject(HttpClient);

  // ── Vault Entries ──────────────────────────────────────────
  getEntries(params?: VaultSearchParams): Observable<VaultEntry[]> {
    let httpParams = new HttpParams();
    if (params) {
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null) httpParams = httpParams.set(k, String(v));
      });
    }
    return this.http.get<VaultEntry[]>('/api/vault', { params: httpParams });
  }

  getEntry(id: number): Observable<VaultEntry> {
    return this.http.get<VaultEntry>(`/api/vault/${id}`);
  }

  createEntry(req: VaultEntryRequest): Observable<VaultEntry> {
    return this.http.post<VaultEntry>('/api/vault', req);
  }

  updateEntry(id: number, req: VaultEntryRequest): Observable<VaultEntry> {
    return this.http.put<VaultEntry>(`/api/vault/${id}`, req);
  }

  deleteEntry(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`/api/vault/${id}`);
  }

  bulkDelete(req: BulkDeleteRequest): Observable<MessageResponse> {
    return this.http.post<MessageResponse>('/api/vault/bulk-delete', req);
  }

  viewPassword(id: number, req: SensitiveViewRequest): Observable<ViewPasswordResponse> {
    return this.http.post<ViewPasswordResponse>(`/api/vault/${id}/password`, req);
  }

  toggleFavorite(id: number): Observable<VaultEntry> {
    return this.http.post<VaultEntry>(`/api/vault/${id}/favorite`, {});
  }

  getFavorites(): Observable<VaultEntry[]> {
    return this.http.get<VaultEntry[]>('/api/vault/favorites');
  }

  searchEntries(keyword: string): Observable<VaultEntry[]> {
    return this.http.get<VaultEntry[]>(`/api/vault/search?keyword=${encodeURIComponent(keyword)}`);
  }

  // ── Trash ──────────────────────────────────────────────────
  getTrash(): Observable<TrashEntry[]> {
    return this.http.get<TrashEntry[]>('/api/vault/trash');
  }

  getTrashCount(): Observable<TrashCountResponse> {
    return this.http.get<TrashCountResponse>('/api/vault/trash/count');
  }

  restoreEntry(id: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`/api/vault/trash/${id}/restore`, {});
  }

  permanentlyDelete(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`/api/vault/trash/${id}`);
  }

  emptyTrash(): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>('/api/vault/trash');
  }

  // ── Password History ───────────────────────────────────────
  getPasswordHistory(entryId: number): Observable<PasswordSnapshot[]> {
    return this.http.get<PasswordSnapshot[]>(`/api/vault/${entryId}/history`);
  }

  // ── Categories ─────────────────────────────────────────────
  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>('/api/categories');
  }

  createCategory(req: CategoryRequest): Observable<Category> {
    return this.http.post<Category>('/api/categories', req);
  }

  updateCategory(id: number, req: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`/api/categories/${id}`, req);
  }

  deleteCategory(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`/api/categories/${id}`);
  }

  // ── Folders ────────────────────────────────────────────────
  getFolders(): Observable<Folder[]> {
    return this.http.get<Folder[]>('/api/folders');
  }

  createFolder(name: string, parentFolderId?: number): Observable<Folder> {
    return this.http.post<Folder>('/api/folders', { name, parentFolderId });
  }

  updateFolder(id: number, name: string): Observable<Folder> {
    return this.http.put<Folder>(`/api/folders/${id}`, { name });
  }

  deleteFolder(id: number): Observable<MessageResponse> {
    return this.http.delete<MessageResponse>(`/api/folders/${id}`);
  }

  // ── Password Generator ─────────────────────────────────────
  generatePassword(req: GeneratorRequest): Observable<GeneratorResponse> {
    return this.http.post<GeneratorResponse>('/api/generator/generate', req);
  }

  generateMultiple(req: GeneratorRequest & { count: number }): Observable<GeneratorMultipleResponse> {
    return this.http.post<GeneratorMultipleResponse>('/api/generator/generate-multiple', req);
  }

  checkStrength(req: PasswordStrengthRequest): Observable<PasswordStrengthResponse> {
    return this.http.post<PasswordStrengthResponse>('/api/generator/strength', req);
  }

  // ── Expiry ─────────────────────────────────────────────────
  getExpiryStatus(): Observable<ExpiryStatusResponse> {
    return this.http.get<ExpiryStatusResponse>('/api/expiry/status');
  }

  getExpiryPolicy(): Observable<ExpiryPolicy> {
    return this.http.get<ExpiryPolicy>('/api/expiry/policy');
  }

  updateExpiryPolicy(policy: Partial<ExpiryPolicy>): Observable<ExpiryPolicy> {
    return this.http.put<ExpiryPolicy>('/api/expiry/policy', policy);
  }

  snoozeExpiry(entryId: number, days: number): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`/api/expiry/${entryId}/snooze`, { days });
  }
}
