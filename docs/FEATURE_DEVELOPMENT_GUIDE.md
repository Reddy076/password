# 🛠️ Feature Development Guide

## File Requirements per Feature

This document lists which files to **create** and **update** for each feature, organized in **optimal development order** based on dependencies.

---

## 📊 Quick Reference

| Symbol | Meaning |
|--------|---------|
| 🆕 | New file to create |
| 📝 | Existing file to update |
| 📁 | Directory path |

---

## 🎯 Development Phases Overview

| Phase | Features | Description |
|-------|----------|-------------|
| **Phase 1: Foundation** | 1-4 | Core setup, User, Config, Security base |
| **Phase 2: Authentication** | 5-9 | Login, Sessions, 2FA, Recovery |
| **Phase 3: Vault Core** | 10-14 | Vault CRUD, Categories, Folders, Password Generator |
| **Phase 4: Vault Features** | 15-18 | Favorites, Search, Trash, Snapshots |
| **Phase 5: Security** | 19-24 | Audit, Alerts, Lockout, CAPTCHA, Rate Limiting |
| **Phase 6: Backup & Export** | 25-27 | Export, Import, Third-Party Import |
| **Phase 7: User Experience** | 28-30 | Settings, Notifications, Health Check |
| **Phase 8: Advanced** | 31-32 | Duress Mode, Read-Only Mode |

---

# 📦 PHASE 1: FOUNDATION (Features 1-4)

## 1️⃣ PROJECT SETUP & CONFIGURATION

### Feature Description
Set up the Spring Boot project structure, database connection, security configuration, and base classes that all other features depend on.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `PasswordManagerApplication.java` | 📁 `root package/` | Main Spring Boot entry point |
| `SecurityConfig.java` | 📁 `config/` | Spring Security configuration |
| `JwtConfig.java` | 📁 `config/` | JWT token settings |
| `EncryptionConfig.java` | 📁 `config/` | AES-256 encryption setup |
| `GlobalExceptionHandler.java` | 📁 `exception/` | Global error handling |
| `AuthenticationException.java` | 📁 `exception/` | Custom auth exception |
| `ResourceNotFoundException.java` | 📁 `exception/` | 404 exception |

### Files to Update 📝
| File | Changes |
|------|---------|
| `application.properties` | Database, JWT, logging configuration |
| `pom.xml` | All required dependencies |

---

## 2️⃣ USER ENTITY & REPOSITORY

### Feature Description
Create the core User entity and repository that all authentication and vault features depend on.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `User.java` | 📁 `model/user/` | Entity class with all user fields |
| `UserRepository.java` | 📁 `repository/` | User database operations |
| `UserResponse.java` | 📁 `dto/response/` | Safe user output (no password) |

### Files to Update 📝
| File | Changes |
|------|---------|
| `SecurityConfig.java` | Permit registration endpoint |

---

## 3️⃣ ENCRYPTION SERVICE

### Feature Description
Core encryption/decryption service using AES-256. Required before any password storage.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `EncryptionService.java` | 📁 `service/security/` | AES-256 encrypt/decrypt |
| `EncryptionUtil.java` | 📁 `util/` | Helper methods for encryption |
| `MasterPasswordValidator.java` | 📁 `security/` | Validates master password strength |

---

## 4️⃣ USER REGISTRATION

### Feature Description
New users create account with email, username, master password. Password is hashed with unique salt.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `RegistrationService.java` | 📁 `service/auth/` | Registration logic, validation |
| `AuthController.java` | 📁 `controller/` | REST endpoint `/api/auth/register` |
| `RegistrationRequest.java` | 📁 `dto/request/` | Input with email, username, password |

### Files to Update 📝
| File | Changes |
|------|---------|
| `SecurityConfig.java` | Permit `/api/auth/register` |

---

## 4️⃣.5️⃣ ACCOUNT DELETION

### Feature Description
Users can permanently delete their account with all associated data. Includes multi-step confirmation, 30-day grace period for recovery, and complete data wipe after grace period.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `AccountDeletionService.java` | 📁 `service/user/` | Handle deletion logic and grace period |
| `AccountDeletionRequest.java` | 📁 `dto/request/` | Master password + confirmation |
| `AccountDeletionScheduler.java` | 📁 `scheduler/` | Permanent deletion after 30 days |

### Files to Update 📝
| File | Changes |
|------|---------|
| `User.java` | Add `deletionRequestedAt`, `deletionScheduledAt` fields |
| `UserController.java` | Add `DELETE /api/users/account` endpoint |
| `UserRepository.java` | Add `findByDeletionScheduledAtBefore()` query |
| `AuditLogService.java` | Log account deletion events |

### Implementation Flow
```
1. User requests deletion → verify master password
2. Set deletion_scheduled_at = NOW + 30 days
3. Send confirmation email
4. User can cancel during grace period
5. Scheduler checks daily and permanently deletes expired accounts
6. CASCADE DELETE removes all user data from all tables
```

---

# 🔐 PHASE 2: AUTHENTICATION (Features 5-9)

## 5️⃣ USER LOGIN

### Feature Description
Users authenticate with username/email and master password. System generates JWT tokens.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `AuthenticationService.java` | 📁 `service/auth/` | Login logic, token generation |
| `LoginRequest.java` | 📁 `dto/request/` | Login credentials |
| `AuthResponse.java` | 📁 `dto/response/` | Access token, refresh token |
| `JwtTokenProvider.java` | 📁 `security/` | JWT generation and validation |
| `JwtAuthenticationFilter.java` | 📁 `security/` | Token validation filter |
| `CustomUserDetailsService.java` | 📁 `security/` | Load user for Spring Security |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthController.java` | Add `/api/auth/login` endpoint |
| `SecurityConfig.java` | Configure JWT filter chain |

---

## 6️⃣ SESSION MANAGEMENT

### Feature Description
Track active user sessions with device info, IP, location. Users can view and terminate sessions.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `UserSession.java` | 📁 `model/user/` | Session entity |
| `UserSessionRepository.java` | 📁 `repository/` | Session database operations |
| `SessionService.java` | 📁 `service/auth/` | Create, validate, terminate |
| `SessionController.java` | 📁 `controller/` | `/api/sessions/*` endpoints |
| `SessionResponse.java` | 📁 `dto/response/` | Session details |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthenticationService.java` | Create session on login |
| `JwtAuthenticationFilter.java` | Validate session is active |

---

## 7️⃣ SECURITY QUESTIONS

### Feature Description
Users set up 3 security questions during registration for account recovery.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `SecurityQuestion.java` | 📁 `model/user/` | Security question entity |
| `SecurityQuestionRepository.java` | 📁 `repository/` | Database operations |
| `SecurityQuestionService.java` | 📁 `service/auth/` | Save, verify questions |
| `SecurityQuestionDTO.java` | 📁 `dto/` | Question + answer transfer |

### Files to Update 📝
| File | Changes |
|------|---------|
| `RegistrationRequest.java` | Add security questions list |
| `RegistrationService.java` | Call SecurityQuestionService |
| `UserController.java` | Add view/update endpoints |

---

## 8️⃣ ACCOUNT RECOVERY

### Feature Description
Password reset via email verification and security questions.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `AccountRecoveryService.java` | 📁 `service/auth/` | Recovery flow logic |
| `RecoveryCode.java` | 📁 `model/user/` | Backup recovery codes |
| `RecoveryCodeRepository.java` | 📁 `repository/` | Database operations |
| `RecoveryRequest.java` | 📁 `dto/request/` | Email and security answers |
| `PasswordResetRequest.java` | 📁 `dto/request/` | New password input |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthController.java` | Add `/api/auth/forgot-password`, `/api/auth/reset-password` |
| `SecurityQuestionService.java` | Add answer verification |

---

## 9️⃣ TWO-FACTOR AUTHENTICATION (2FA)

### Feature Description
OTP-based 2FA using Google Authenticator. Includes backup codes.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `TwoFactorAuth.java` | 📁 `model/auth/` | 2FA configuration entity |
| `TwoFactorAuthRepository.java` | 📁 `repository/` | Database operations |
| `TwoFactorService.java` | 📁 `service/auth/` | Setup, verify, enable/disable |
| `TwoFactorController.java` | 📁 `controller/` | `/api/2fa/*` endpoints |
| `TwoFactorSetupResponse.java` | 📁 `dto/response/` | QR code URL and secret |
| `OtpToken.java` | 📁 `model/auth/` | OTP token entity |
| `OtpTokenRepository.java` | 📁 `repository/` | OTP database operations |
| `OtpService.java` | 📁 `service/auth/` | Generate, validate tokens |
| `TOTPUtil.java` | 📁 `util/` | TOTP generation utilities |

### Files to Update 📝
| File | Changes |
|------|---------|
| `User.java` | Add `is2faEnabled` field |
| `AuthenticationService.java` | Check 2FA status during login |
| `AuthResponse.java` | Add `requires2FA` flag |

---

# 🗄️ PHASE 3: VAULT CORE (Features 10-14)

## 1️⃣0️⃣ CATEGORIES

### Feature Description
Organize vault entries by category (Social Media, Banking, Email, etc.). **Create before VaultEntry since it references Category.**

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `Category.java` | 📁 `model/vault/` | Category entity |
| `CategoryRepository.java` | 📁 `repository/` | Database operations |
| `CategoryService.java` | 📁 `service/vault/` | CRUD for categories |
| `CategoryController.java` | 📁 `controller/` | `/api/categories/*` endpoints |
| `CategoryDTO.java` | 📁 `dto/` | Category transfer object |

### Files to Update 📝
| File | Changes |
|------|---------|
| `schema.sql` | Insert default categories |

---

## 1️⃣1️⃣ FOLDERS

### Feature Description
Hierarchical folder organization for vault entries. **Create before VaultEntry since it references Folder.**

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `Folder.java` | 📁 `model/vault/` | Folder entity (self-referential) |
| `FolderRepository.java` | 📁 `repository/` | Database operations |
| `FolderService.java` | 📁 `service/vault/` | CRUD, move, tree structure |
| `FolderController.java` | 📁 `controller/` | `/api/folders/*` endpoints |
| `FolderDTO.java` | 📁 `dto/` | Folder with children |

---

## 1️⃣2️⃣ PASSWORD GENERATOR

### Feature Description
Generate secure random passwords. **Create before VaultEntry to calculate strength on save.**

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `PasswordGeneratorService.java` | 📁 `service/security/` | Random password generation |
| `PasswordStrengthService.java` | 📁 `service/security/` | Strength calculation |
| `PasswordGeneratorController.java` | 📁 `controller/` | `/api/generator/*` endpoints |
| `PasswordGeneratorRequest.java` | 📁 `dto/request/` | Generation options |
| `PasswordStrengthResponse.java` | 📁 `dto/response/` | Score, label, feedback |
| `PasswordValidator.java` | 📁 `util/` | Password validation rules |
| `PasswordStrengthCalculator.java` | 📁 `util/` | Strength scoring algorithm |

---

## 1️⃣3️⃣ VAULT ENTRY MANAGEMENT (CRUD)

### Feature Description
Core password vault functionality. Create, view, update, delete password entries.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `VaultEntry.java` | 📁 `model/vault/` | Password entry entity |
| `VaultEntryRepository.java` | 📁 `repository/` | Database operations |
| `VaultService.java` | 📁 `service/vault/` | CRUD operations |
| `VaultController.java` | 📁 `controller/` | `/api/vault/*` endpoints |
| `VaultEntryRequest.java` | 📁 `dto/request/` | Create/update input |
| `VaultEntryResponse.java` | 📁 `dto/response/` | Masked password output |
| `VaultEntryDetailResponse.java` | 📁 `dto/response/` | Decrypted password output |
| `VaultAccessException.java` | 📁 `exception/` | Access denied exception |

### Files to Update 📝
| File | Changes |
|------|---------|
| `SecurityConfig.java` | Protect `/api/vault/*` endpoints |
| `PasswordStrengthService.java` | Calculate strength on entry save |

---

## 1️⃣4️⃣ USER SETTINGS

### Feature Description
User preferences (theme, auto-lock timeout, password generator defaults). **Create early as other features use settings.**

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `UserSettings.java` | 📁 `model/system/` | User preferences entity |
| `UserSettingsRepository.java` | 📁 `repository/` | Database operations |
| `UserSettingsService.java` | 📁 `service/user/` | Get and update settings |
| `UserSettingsDTO.java` | 📁 `dto/` | Settings transfer object |

### Files to Update 📝
| File | Changes |
|------|---------|
| `UserController.java` | Add settings endpoints |
| `PasswordGeneratorService.java` | Use user's default settings |
| `RegistrationService.java` | Create default settings on registration |

---

# 🔍 PHASE 4: VAULT FEATURES (Features 15-18)

## 1️⃣5️⃣ FAVORITES

### Feature Description
Mark entries as favorites for quick access.

### Files to Create 🆕
*No new files needed*

### Files to Update 📝
| File | Changes |
|------|---------|
| `VaultEntry.java` | Add `isFavorite` field (should exist) |
| `VaultEntryRepository.java` | Add query to find favorites |
| `VaultService.java` | Add `toggleFavorite()`, `getFavorites()` |
| `VaultController.java` | Add `/api/vault/favorites` endpoint |

---

## 1️⃣5️⃣.5️⃣ HIGHLY SENSITIVE ENTRY ACCESS

### Feature Description
Entries marked as "Highly Sensitive" require both OTP verification and master password re-entry to view. This provides extra protection for critical credentials like banking or admin passwords.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `SensitiveAccessService.java` | 📁 `service/vault/` | Verify OTP + master password for access |
| `SensitiveAccessRequest.java` | 📁 `dto/request/` | Contains OTP, master password, entry ID |
| `SensitiveAccessResponse.java` | 📁 `dto/response/` | Decrypted entry data |

### Files to Update 📝
| File | Changes |
|------|---------|
| `VaultEntry.java` | Add `isHighlySensitive` field (should exist) |
| `VaultService.java` | Check sensitivity flag before returning password |
| `VaultController.java` | Add `/api/vault/entries/{id}/sensitive-view` endpoint |
| `OtpService.java` | Add `verifyForSensitiveAccess()` method |
| `AuditLogService.java` | Log all sensitive access attempts |

### Implementation Flow
```
1. User requests to view highly sensitive entry
2. Check if entry.isHighlySensitive == true
3. If true → return 403 with "requiresSensitiveAuth" flag
4. User must submit:
   - Current session JWT
   - Master password (re-verification)
   - Valid OTP from authenticator app
5. Verify all three factors
6. If valid → return decrypted password (5-second timeout)
7. Log access in audit_logs with action = 'SENSITIVE_PASSWORD_VIEW'
```

---

## 1️⃣6️⃣ SEARCH & FILTER

### Feature Description
Search vault entries by name, URL, username. Filter by category, strength, favorites.

### Files to Create 🆕
*No new files needed*

### Files to Update 📝
| File | Changes |
|------|---------|
| `VaultEntryRepository.java` | Add custom search queries with `@Query` |
| `VaultService.java` | Add `search()`, `filter()`, `sort()` methods |
| `VaultController.java` | Add `/api/vault/search` with query params |

---

## 1️⃣7️⃣ TRASH & RESTORE

### Feature Description
Soft delete for vault entries. Recover within 30 days. Auto-cleanup after expiry.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `VaultTrash.java` | 📁 `model/vault/` | Deleted entry storage |
| `VaultTrashRepository.java` | 📁 `repository/` | Database operations |
| `VaultTrashService.java` | 📁 `service/vault/` | Trash, restore, cleanup |
| `TrashCleanupScheduler.java` | 📁 `scheduler/` | Auto-delete expired items |

### Files to Update 📝
| File | Changes |
|------|---------|
| `VaultEntry.java` | Add `isDeleted` field |
| `VaultService.java` | Modify delete to soft-delete |
| `VaultController.java` | Add `/api/vault/trash/*` endpoints |

---

## 1️⃣8️⃣ VAULT SNAPSHOTS

### Feature Description
Password history per entry. Track password changes over time.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `VaultSnapshot.java` | 📁 `model/vault/` | Password history entity |
| `VaultSnapshotRepository.java` | 📁 `repository/` | Database operations |
| `VaultSnapshotService.java` | 📁 `service/vault/` | Create and view snapshots |

### Files to Update 📝
| File | Changes |
|------|---------|
| `VaultService.java` | Create snapshot on password change |
| `VaultController.java` | Add `/api/vault/entries/{id}/history` |

---

# 🛡️ PHASE 5: SECURITY (Features 19-24)

## 1️⃣9️⃣ AUDIT LOGGING

### Feature Description
Track all user activities. Append-only for security compliance.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `AuditLog.java` | 📁 `model/security/` | Audit log entity |
| `AuditLogRepository.java` | 📁 `repository/` | Insert-only operations |
| `AuditLogService.java` | 📁 `service/security/` | Log various events |
| `AuditLogAspect.java` | 📁 `aspect/` | AOP for automatic logging |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthenticationService.java` | Log login/logout events |
| `VaultService.java` | Log password views and modifications |
| `SecurityController.java` | Add `/api/security/audit-logs` endpoint |

---

## 2️⃣0️⃣ LOGIN HISTORY

### Feature Description
History of login attempts (successful and failed) with device info.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `LoginAttempt.java` | 📁 `model/security/` | Login attempt entity |
| `LoginAttemptRepository.java` | 📁 `repository/` | Database operations |
| `LoginAttemptService.java` | 📁 `service/security/` | Record attempts |
| `LoginHistoryResponse.java` | 📁 `dto/response/` | Formatted history |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthenticationService.java` | Record each login attempt |
| `SecurityController.java` | Add `/api/security/login-history` endpoint |

---

## 2️⃣1️⃣ SECURITY ALERTS

### Feature Description
Notify users of security events (new device login, failed attempts, etc.)

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `SecurityAlert.java` | 📁 `model/security/` | Alert entity |
| `SecurityAlertRepository.java` | 📁 `repository/` | Database operations |
| `SecurityAlertService.java` | 📁 `service/security/` | Create and manage alerts |
| `SecurityAlertDTO.java` | 📁 `dto/` | Alert transfer object |
| `SecurityController.java` | 📁 `controller/` | `/api/security/*` endpoints |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthenticationService.java` | Trigger alerts on suspicious login |
| `TwoFactorService.java` | Trigger alert on 2FA changes |

---

## 2️⃣2️⃣ SECURITY AUDIT REPORT

### Feature Description
Password health report showing weak, reused, old passwords.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `PasswordAnalysis.java` | 📁 `model/security/` | Analysis results entity |
| `PasswordAnalysisRepository.java` | 📁 `repository/` | Database operations |
| `SecurityAuditService.java` | 📁 `service/security/` | Analyze vault and generate report |
| `SecurityAuditResponse.java` | 📁 `dto/response/` | Audit report data |

### Files to Update 📝
| File | Changes |
|------|---------|
| `PasswordStrengthService.java` | Add reuse detection |
| `VaultService.java` | Call analysis after entry changes |
| `SecurityController.java` | Add `/api/security/audit-report` endpoint |

---

## 2️⃣2️⃣.5️⃣ DATA ACCESS HEATMAP

### Feature Description
Visual analytics showing vault access patterns over time. Helps users identify unusual access patterns and understand their password usage habits.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `AccessHeatmapService.java` | 📁 `service/analytics/` | Aggregate access data for heatmap |
| `HeatmapResponse.java` | 📁 `dto/response/` | Hour/day access counts |

### Files to Update 📝
| File | Changes |
|------|---------|
| `VaultEntry.java` | Already has `accessCount`, `lastAccessedAt` |
| `AuditLogRepository.java` | Add `countByUserIdGroupByHourAndDay()` query |
| `UserController.java` | Add `GET /api/users/activity-heatmap` endpoint |

### Implementation Details
```java
// HeatmapResponse structure
{
  "accessByHour": [12, 45, 32, ...], // 24 values (00:00-23:00)
  "accessByDay": [120, 89, 134, ...], // 7 values (Sun-Sat)
  "peakHour": 14,
  "peakDay": "Wednesday",
  "totalAccesses": 847,
  "period": "LAST_30_DAYS"
}
```

---

## 2️⃣3️⃣ ACCOUNT LOCKOUT

### Feature Description
Lock account after failed login attempts. Progressive lockout duration.

### Files to Create 🆕
*No new files needed*

### Files to Update 📝
| File | Changes |
|------|---------|
| `User.java` | Add `failedLoginAttempts`, `lockedUntil`, `lockoutCount` |
| `AuthenticationService.java` | Implement lockout logic |
| `UserRepository.java` | Add method to increment failed attempts |

---

## 2️⃣4️⃣ CAPTCHA & RATE LIMITING

### Feature Description
CAPTCHA after failed attempts. Rate limiting to prevent abuse.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `CaptchaService.java` | 📁 `service/security/` | Generate/verify CAPTCHA |
| `CaptchaConfig.java` | 📁 `config/` | CAPTCHA provider config |
| `RateLimitFilter.java` | 📁 `security/` | Rate limit filter |
| `RateLimitConfig.java` | 📁 `config/` | Limits per endpoint |
| `RateLimitService.java` | 📁 `service/` | Track and enforce limits |
| `RateLimitExceededException.java` | 📁 `exception/` | 429 exception |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthController.java` | Add CAPTCHA verification |
| `LoginRequest.java` | Add captchaToken field |
| `SecurityConfig.java` | Add rate limit filter |
| `GlobalExceptionHandler.java` | Handle rate limit exception |

---

# 📦 PHASE 6: BACKUP & EXPORT (Features 25-27)

## 2️⃣5️⃣ VAULT EXPORT

### Feature Description
Export encrypted backup of vault. AES-256 encrypted file.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `BackupExport.java` | 📁 `model/system/` | Export tracking entity |
| `BackupExportRepository.java` | 📁 `repository/` | Database operations |
| `ExportService.java` | 📁 `service/backup/` | Generate encrypted export |
| `BackupController.java` | 📁 `controller/` | `/api/backup/*` endpoints |
| `ExportResponse.java` | 📁 `dto/response/` | Export metadata |

### Files to Update 📝
| File | Changes |
|------|---------|
| `EncryptionService.java` | Add file encryption methods |
| `AuditLogService.java` | Log export events |

---

## 2️⃣6️⃣ VAULT IMPORT

### Feature Description
Import from encrypted backup. Supports merge or replace.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `ImportService.java` | 📁 `service/backup/` | Parse and import backups |
| `ImportRequest.java` | 📁 `dto/request/` | Import options |
| `ImportResult.java` | 📁 `dto/response/` | Import statistics |

### Files to Update 📝
| File | Changes |
|------|---------|
| `BackupController.java` | Add `/api/backup/import` endpoint |
| `VaultService.java` | Add bulk insert method |

---

## 2️⃣7️⃣ THIRD-PARTY IMPORT

### Feature Description
Import from Chrome, Firefox, LastPass, 1Password CSV exports.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `ThirdPartyImportService.java` | 📁 `service/backup/` | Parse various formats |
| `ChromeImporter.java` | 📁 `service/backup/importers/` | Chrome CSV parser |
| `LastPassImporter.java` | 📁 `service/backup/importers/` | LastPass CSV parser |
| `ImporterFactory.java` | 📁 `service/backup/importers/` | Importer selection |
| `ThirdPartyImportRequest.java` | 📁 `dto/request/` | Import source and data |

### Files to Update 📝
| File | Changes |
|------|---------|
| `BackupController.java` | Add `/api/backup/import-external` endpoint |

---

# 🎨 PHASE 7: USER EXPERIENCE (Features 28-30)

## 2️⃣8️⃣ NOTIFICATIONS

### Feature Description
In-app notification center for alerts and reminders.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `Notification.java` | 📁 `model/system/` | Notification entity |
| `NotificationRepository.java` | 📁 `repository/` | Database operations |
| `NotificationService.java` | 📁 `service/` | Create, send, manage |
| `NotificationController.java` | 📁 `controller/` | `/api/notifications/*` |
| `NotificationDTO.java` | 📁 `dto/` | Notification data |

### Files to Update 📝
| File | Changes |
|------|---------|
| `SecurityAlertService.java` | Create notification when alert triggered |

---

## 2️⃣9️⃣ ADAPTIVE AUTHENTICATION

### Feature Description
Detect suspicious logins and trigger additional verification.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `AdaptiveAuthService.java` | 📁 `service/auth/` | Risk analysis |
| `DeviceFingerprintUtil.java` | 📁 `util/` | Generate device fingerprints |
| `GeoLocationService.java` | 📁 `service/` | IP to location lookup |
| `DateTimeUtil.java` | 📁 `util/` | Time-based analysis |

### Files to Update 📝
| File | Changes |
|------|---------|
| `AuthenticationService.java` | Call adaptive auth before login |
| `UserSession.java` | Store device fingerprint |
| `LoginAttempt.java` | Add location and risk score |

---

## 3️⃣0️⃣ HEALTH CHECK

### Feature Description
System health monitoring endpoints.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `HealthController.java` | 📁 `controller/` | `/api/health/*` endpoints |
| `HealthService.java` | 📁 `service/` | Check system components |
| `HealthResponse.java` | 📁 `dto/response/` | Health status |

### Files to Update 📝
| File | Changes |
|------|---------|
| `SecurityConfig.java` | Permit `/api/health` without auth |

---

# 🚀 PHASE 8: ADVANCED (Features 31-32)

## 3️⃣1️⃣ DURESS MODE (DUMMY VAULT)

### Feature Description
Emergency fake vault when logging in with duress password.

### Files to Create 🆕

| File | Location | Purpose |
|------|----------|---------|
| `DuressService.java` | 📁 `service/security/` | Handle duress login |
| `DummyVaultGenerator.java` | 📁 `util/` | Generate fake entries |

### Files to Update 📝
| File | Changes |
|------|---------|
| `User.java` | Add `duressPasswordHash` field |
| `AuthenticationService.java` | Check for duress password |
| `AuthController.java` | Add `/api/auth/duress-login` |
| `VaultService.java` | Return dummy data in duress mode |

---

## 3️⃣2️⃣ PASSWORD HINTS & READ-ONLY MODE

### Feature Description
Optional password hint displayed after failed attempts. Read-only mode for public devices.

### Files to Create 🆕
*No new files needed*

### Files to Update 📝
| File | Changes |
|------|---------|
| `User.java` | Add `passwordHint` field |
| `UserSettings.java` | Add `readOnlyMode` field |
| `AuthController.java` | Add `/api/auth/password-hint` endpoint |
| `VaultService.java` | Check read-only mode before writes |
| `RegistrationService.java` | Validate hint doesn't contain password |

---

# 📊 SUMMARY

## Development Order Rationale

```
Phase 1: Foundation
    ↓ (provides base config, User entity, encryption)
Phase 2: Authentication  
    ↓ (provides login, sessions, security - needed to protect vault)
Phase 3: Vault Core
    ↓ (Categories & Folders FIRST, then VaultEntry, then Password Generator)
Phase 4: Vault Features
    ↓ (builds on vault CRUD)
Phase 5: Security
    ↓ (adds audit, alerts, lockout - uses vault and auth)
Phase 6: Backup & Export
    ↓ (needs vault fully working)
Phase 7: User Experience
    ↓ (polish features)
Phase 8: Advanced
    ↓ (optional features last)
```

## Total Files to Create

| Layer | Count |
|-------|-------|
| **Models** | 18 |
| **Repositories** | 18 |
| **Services** | 26 |
| **Controllers** | 12 |
| **DTOs** | 26 |
| **Config** | 5 |
| **Security** | 6 |
| **Utilities** | 6 |
| **Exceptions** | 5 |
| **Importers** | 3 |
| **TOTAL** | **~127 files** |

---

## 📋 Features by Priority

| Priority | Features |
|----------|----------|
| **Critical** | 1-14 (Foundation + Auth + Vault Core) |
| **Required** | 15-27 (Vault Features + Security + Backup) |
| **Important** | 28-30 (Notifications, Adaptive Auth, Health) |
| **Optional** | 31-32 (Duress Mode, Password Hints, Read-Only) |

---

> **Tip:** Always develop in this order to avoid circular dependencies and missing entities!
