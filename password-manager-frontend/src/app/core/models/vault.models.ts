// ============================================================
// VAULT MODELS — TypeScript interfaces for vault API responses
// ============================================================

export interface VaultEntry {
  id: number;
  title: string;
  username: string | null;
  password?: string;
  websiteUrl: string | null;
  notes: string | null;
  categoryId: number | null;
  categoryName: string | null;
  folderId: number | null;
  folderName: string | null;
  isFavorite: boolean;
  isHighlySensitive: boolean;
  createdAt: string;
  updatedAt: string;
  strengthScore: number;
  strengthLabel: string;
}

export interface VaultEntryRequest {
  title: string;
  username?: string;
  password?: string;
  websiteUrl?: string;
  notes?: string;
  categoryId?: number;
  folderId?: number;
  isFavorite?: boolean;
  isHighlySensitive?: boolean;
}

export interface VaultSearchParams {
  keyword?: string;
  categoryId?: number;
  folderId?: number;
  isFavorite?: boolean;
  isHighlySensitive?: boolean;
  sortBy?: 'title' | 'createdAt' | 'updatedAt';
  sortDir?: 'asc' | 'desc';
}

export interface ViewPasswordResponse {
  password: string;
}

export interface SensitiveViewRequest {
  masterPassword: string;
}

export interface BulkDeleteRequest {
  ids: number[];
}

// ── Trash ─────────────────────────────────────────────────────

export interface TrashEntry {
  id: number;
  title: string;
  websiteUrl: string | null;
  categoryName: string | null;
  folderName: string | null;
  deletedAt: string;
  expiresAt: string;
  daysRemaining: number;
}

export interface TrashCountResponse {
  count: number;
}

// ── Password History ──────────────────────────────────────────

export interface PasswordSnapshot {
  id: number;
  vaultEntryId: number;
  encryptedPassword: string;
  changedAt: string;
}

// ── Categories ────────────────────────────────────────────────

export interface Category {
  id: number;
  name: string;
  icon: string | null;
  isDefault: boolean;
}

export interface CategoryRequest {
  name: string;
  icon?: string;
}

// ── Folders ───────────────────────────────────────────────────

export interface Folder {
  id: number;
  name: string;
  parentFolderId: number | null;
  createdAt: string;
}

// ── Password Generator ────────────────────────────────────────

export interface GeneratorRequest {
  length: number;
  includeUppercase: boolean;
  includeLowercase: boolean;
  includeNumbers: boolean;
  includeSpecial: boolean;
  excludeSimilar: boolean;
  excludeAmbiguous: boolean;
  count?: number;
}

export interface GeneratorResponse {
  password: string;
}

export interface GeneratorMultipleResponse {
  passwords: string[];
}

export interface PasswordStrengthRequest {
  password: string;
}

export interface PasswordStrengthResponse {
  score: number;
  label: string;
  feedback: string[];
  entropy: number;
  crackTime: string;
}

// ── Expiry ────────────────────────────────────────────────────

export type ExpiryStatus = 'FRESH' | 'AGING' | 'EXPIRING_SOON' | 'EXPIRED';

export interface ExpiryEntry {
  entryId: number;
  title: string;
  username: string | null;
  websiteUrl: string | null;
  lastChangedAt: string;
  expiresAt: string;
  status: ExpiryStatus;
  daysUntilExpiry: number;
  reminderSent: boolean;
  snoozed: boolean;
  snoozedUntil: string | null;
}

export interface ExpiryStatusResponse {
  totalEntries: number;
  freshCount: number;
  agingCount: number;
  expiringSoonCount: number;
  expiredCount: number;
  entries: ExpiryEntry[];
}

export interface ExpiryPolicy {
  id: number;
  defaultExpiryDays: number;
  criticalExpiryDays: number;
  reminderDaysBefore: number;
  enabled: boolean;
}

// ── Sharing ───────────────────────────────────────────────────

export type SharePermission = 'VIEW_ONCE' | 'VIEW_MULTIPLE' | 'TEMPORARY_ACCESS';

export interface CreateShareRequest {
  vaultEntryId: number;
  recipientEmail: string;
  permission: SharePermission;
  maxViews?: number;
  expiresInHours: number;
}

export interface ShareLinkResponse {
  shareId: number;
  shareToken: string;
  shareUrl: string;
  encryptionKey: string;
  vaultEntryTitle: string;
  permission: SharePermission;
  maxViews: number | null;
  viewCount: number;
  expiresAt: string;
  recipientEmail: string;
  createdAt: string;
  isRevoked?: boolean;
}

export interface SharedPasswordResponse {
  title: string;
  encryptedPassword: string;
  encryptionIv: string;
  username: string | null;
  websiteUrl: string | null;
  expiresAt: string;
  viewCount: number;
  maxViews: number | null;
}

// ── Backup ────────────────────────────────────────────────────

export type BackupFormat = 'JSON' | 'CSV';
export type ImportSource = 'CHROME' | 'FIREFOX' | 'LASTPASS' | '1PASSWORD';

export interface ExportResponse {
  format: BackupFormat;
  data: string;
  encryptedWithPassword: boolean;
  exportedAt: string;
  entryCount: number;
}

export interface ImportRequest {
  format: BackupFormat;
  data: string;
  password?: string;
}

export interface ImportResult {
  imported: number;
  skipped: number;
  failed: number;
  errors: string[];
}

export interface ExternalImportRequest {
  source: ImportSource;
  data: string;
}

// ── Files ─────────────────────────────────────────────────────

export interface FileEntry {
  id: number;
  originalFilename: string;
  fileSize: number;
  mimeType: string;
  folderId: number | null;
  folderName: string | null;
  uploadedAt: string;
  lastAccessedAt: string | null;
  checksum?: string;
}

export interface FileFolder {
  id: number;
  name: string;
  parentId: number | null;
  createdAt: string;
}

export interface FilesListResponse {
  folders: FileFolder[];
  files: FileEntry[];
  totalFiles: number;
  totalStorageBytes: number;
}

export interface CreateFileFolderRequest {
  name: string;
  parentId?: number;
}
