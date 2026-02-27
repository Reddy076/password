// ============================================================
// SECURITY MODELS — TypeScript interfaces for security API responses
// ============================================================

// ── Audit Logs ────────────────────────────────────────────────

export interface AuditLog {
  id: number;
  action: string;
  details: string;
  ipAddress: string;
  createdAt: string;
}

// ── Login History ─────────────────────────────────────────────

export interface LoginAttempt {
  id: number;
  ipAddress: string;
  deviceInfo: string;
  status: 'SUCCESS' | 'FAILED';
  failureReason: string | null;
  location: string | null;
  createdAt: string;
}

// ── Security Alerts ───────────────────────────────────────────

export type AlertSeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface SecurityAlert {
  id: number;
  alertType: string;
  title: string;
  message: string;
  severity: AlertSeverity;
  isRead: boolean;
  createdAt: string;
}

// ── Security Audit Report ─────────────────────────────────────

export interface SecurityAuditReport {
  overallScore: number;
  weakPasswords: VaultEntrySummary[];
  reusedPasswords: VaultEntrySummary[];
  oldPasswords: VaultEntrySummary[];
  generatedAt: string;
}

export interface VaultEntrySummary {
  id: number;
  title: string;
  username: string | null;
  websiteUrl: string | null;
  strengthScore?: number;
  strengthLabel?: string;
}

// ── Breach Monitor ────────────────────────────────────────────

export type BreachStatusType = 'SAFE' | 'AT_RISK';

export interface BreachStatus {
  status: BreachStatusType;
  compromisedCount: number;
  lastScanAt: string | null;
}

export interface BreachScanResult {
  scanId: number;
  status: 'COMPLETED' | 'RUNNING' | 'FAILED';
  entriesScanned: number;
  compromisedFound: number;
  scannedAt: string;
}

export interface CompromisedCredential {
  id: number;
  vaultEntryId: number;
  vaultEntryTitle: string;
  pwnedCount: number;
  isResolved: boolean;
  detectedAt: string;
}

export interface BreachHistoryResponse {
  scans: BreachScanRecord[];
}

export interface BreachScanRecord {
  id: number;
  triggerType: 'MANUAL' | 'SCHEDULED';
  entriesScanned: number;
  compromisedFound: number;
  status: string;
  scannedAt: string;
}

// ── Dashboard ─────────────────────────────────────────────────

export interface SecurityScore {
  overallScore: number;
  scoreLabel: string;
  totalPasswords: number;
  strongPasswords: number;
  fairPasswords: number;
  weakPasswords: number;
  reusedPasswords: number;
  oldPasswords: number;
  recommendation: string;
}

export interface PasswordHealth {
  totalPasswords: number;
  strongCount: number;
  goodCount: number;
  fairCount: number;
  weakCount: number;
  veryWeakCount: number;
  averageStrengthScore: number;
  categoryBreakdowns: CategoryBreakdown[];
}

export interface CategoryBreakdown {
  categoryName: string;
  count: number;
  averageScore: number;
  weakCount: number;
}

export interface ReusedPasswordsResponse {
  totalReusedGroups: number;
  totalAffectedEntries: number;
  reusedGroups: ReusedGroup[];
}

export interface ReusedGroup {
  reuseCount: number;
  entries: VaultEntrySummary[];
}

export interface PasswordAgeResponse {
  totalPasswords: number;
  freshCount: number;
  agingCount: number;
  oldCount: number;
  ancientCount: number;
  averageAgeInDays: number;
  distribution: AgeDistribution[];
}

export interface AgeDistribution {
  label: string;
  count: number;
  minDays: number;
  maxDays: number;
}

export interface SecurityTrends {
  trendPoints: TrendPoint[];
  scoreChange: number;
  trendDirection: 'IMPROVING' | 'DECLINING' | 'STABLE';
  periodLabel: string;
}

export interface TrendPoint {
  recordedAt: string;
  overallScore: number;
  weakPasswordsCount: number;
  reusedPasswordsCount: number;
  oldPasswordsCount: number;
}

// ── Activity Heatmap ──────────────────────────────────────────

export interface ActivityHeatmap {
  heatmapData: HeatmapDataPoint[];
  totalAccesses: number;
  mostActiveDay: string;
  mostActiveHour: number;
}

export interface HeatmapDataPoint {
  date: string;
  count: number;
}

// ── Dashboard Summary ─────────────────────────────────────────

export interface DashboardSummary {
  totalPasswords: number;
  weakPasswords: number;
  reusedPasswords: number;
  oldPasswords: number;
  securityScore: number;
  lastLogin: string;
}

// ── Notifications ─────────────────────────────────────────────

export interface Notification {
  id: number;
  notificationType: string;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  count: number;
}

// ── Timeline ──────────────────────────────────────────────────

export type TimelineCategory = 'VAULT' | 'AUTH' | 'BREACH' | 'SHARING' | 'BACKUP' | 'SECURITY';

export interface TimelineEvent {
  id: number;
  action: string;
  category: TimelineCategory;
  description: string;
  actorUsername: string;
  ipAddress: string | null;
  createdAt: string;
}

export interface TimelineResponse {
  events: TimelineEvent[];
  categoryBreakdown: Record<TimelineCategory, number>;
}

export interface TimelineSummary {
  totalEvents: number;
  entriesCreated: number;
  entriesDeleted: number;
  passwordChanges: number;
  sharesCreated: number;
  breachDetections: number;
  mostActiveDay: string;
  mostActiveHour: number;
  topAccessedEntries: VaultEntrySummary[];
  weeklyHistogram: number[];
}

// ── Health Check ──────────────────────────────────────────────

export interface HealthStatus {
  status: 'UP' | 'DOWN' | 'DEGRADED';
  components: {
    database: { status: string };
    services: { status: string };
  };
  timestamp: string;
}

// ── Teams ─────────────────────────────────────────────────────

export type TeamRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';

export interface Team {
  id: number;
  name: string;
  description: string | null;
  createdByUsername: string;
  createdAt: string;
  memberCount: number;
  sharedEntryCount: number;
  currentUserRole: TeamRole;
  members: TeamMember[];
}

export interface TeamMember {
  memberId: number;
  userId: number;
  username: string;
  email: string;
  role: TeamRole;
  joinedAt: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface InviteMemberRequest {
  email: string;
  role: Exclude<TeamRole, 'OWNER'>;
}

export interface TeamActivity {
  action: string;
  actorUsername: string;
  description: string;
  timestamp: string;
}

// ── Emergency Access ──────────────────────────────────────────

export type EmergencyRequestStatus = 'PENDING' | 'APPROVED' | 'DENIED' | 'EXPIRED';

export interface EmergencyContact {
  id: number;
  contactEmail: string;
  contactName: string;
  relationship: string;
  waitingPeriodHours: number;
  verified: boolean;
  active: boolean;
  createdAt: string;
}

export interface CreateEmergencyContactRequest {
  contactEmail: string;
  contactName: string;
  relationship: string;
  waitingPeriodHours: number;
}

export interface EmergencyAccessRequest {
  id: number;
  contactId: number;
  contactEmail: string;
  contactName: string;
  ownerUsername: string;
  status: EmergencyRequestStatus;
  requestedAt: string;
  waitingPeriodEndsAt: string;
  hoursUntilAutoApproval: number;
  decidedAt: string | null;
  accessToken: string | null;
  expiresAt: string | null;
  requestMessage: string;
}

export interface RequestEmergencyAccessRequest {
  ownerUsername: string;
  requestMessage: string;
}

export interface EmergencyVaultResponse {
  ownerUsername: string;
  expiresAt: string;
  hoursUntilExpiry: number;
  entries: EmergencyVaultEntry[];
}

export interface EmergencyVaultEntry {
  entryId: number;
  title: string;
  websiteUrl: string | null;
  categoryName: string | null;
  folderName: string | null;
  lastUpdatedAt: string;
}

// ── AI Assistant ──────────────────────────────────────────────

export type AiIntent = 'GENERATE_PASSWORD' | 'BREACH_CHECK' | 'SECURITY_ANALYSIS' | 'TWO_FACTOR' | 'GENERAL' | 'SUGGESTIONS';

export interface AiChatRequest {
  message: string;
}

export interface AiChatResponse {
  reply: string;
  intent: AiIntent;
  generatedPassword: string | null;
  suggestions: string[];
  timestamp: string;
  aiPowered: boolean;
  history: AiMessage[];
}

export interface AiMessage {
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

export interface AiInsight {
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
  title: string;
  description: string;
  affectedCount: number;
}

export interface AiSecurityInsights {
  overallAssessment: string;
  securityScore: number;
  aiPowered: boolean;
  insights: AiInsight[];
}

// ── Autofill ──────────────────────────────────────────────────

export type AutofillMatchType = 'EXACT' | 'SUBDOMAIN' | 'PARTIAL';

export interface AutofillSuggestion {
  entryId: number;
  title: string;
  username: string | null;
  websiteUrl: string;
  matchType: AutofillMatchType;
  isFavorite: boolean;
}

export interface AutofillSuggestionsResponse {
  domain: string;
  totalCount: number;
  suggestions: AutofillSuggestion[];
}

export interface AutofillSuggestionsRequest {
  url: string;
}

export interface LogAutofillUsageRequest {
  url: string;
  vaultEntryId: number;
  applied: boolean;
}
