# 🔐 Password Manager - Database Schema & File Structure

## 📊 Database Tables Overview

Based on the comprehensive feature requirements, the Password Manager application requires **18 database tables** organized into logical groups.

---

## 🗄️ Complete Database Tables (18 Tables)

### 1. User & Authentication (4 Tables)

| Table Name | Description | Key Fields |
|------------|-------------|------------|
| `users` | Core user account information | id, email, username, master_password_hash, salt, phone_number, password_hint, created_at, updated_at, status, is_2fa_enabled, duress_password_hash |
| `security_questions` | User's security Q&A for recovery | id, user_id, question, answer_hash, question_order, created_at |
| `user_sessions` | Active user sessions tracking | id, user_id, session_token, device_info, ip_address, location, created_at, expires_at, is_active, last_activity |
| `recovery_codes` | Backup recovery codes | id, user_id, code_hash, is_used, created_at, used_at |

### 2. Two-Factor Authentication (2 Tables)

| Table Name | Description | Key Fields |
|------------|-------------|------------|
| `two_factor_auth` | 2FA configuration per user | id, user_id, secret_key, is_enabled, enabled_at, backup_codes |
| `otp_tokens` | Temporary OTP verification codes | id, user_id, token_hash, purpose, created_at, expires_at, is_used |

### 3. Vault & Passwords (5 Tables)

| Table Name | Description | Key Fields |
|------------|-------------|------------|
| `vault_entries` | Core password vault entries | id, user_id, category_id, folder_id, account_name, website_url, username, encrypted_password, notes, is_favorite, is_highly_sensitive, is_deleted, password_strength, created_at, updated_at, last_accessed_at, access_count |
| `categories` | Entry categorization | id, name, icon, is_default, user_id, created_at |
| `folders` | Hierarchical folder organization | id, user_id, name, parent_folder_id, created_at, updated_at |
| `vault_trash` | Soft-deleted entries | id, vault_entry_id, user_id, deleted_at, permanent_delete_at |
| `vault_snapshots` | Password history/snapshots | id, vault_entry_id, encrypted_password, created_at |

### 4. Security & Audit (4 Tables)

| Table Name | Description | Key Fields |
|------------|-------------|------------|
| `audit_logs` | User activity audit trail | id, user_id, action, entity_type, entity_id, details, ip_address, user_agent, created_at |
| `login_attempts` | Failed login tracking | id, user_id, ip_address, device_info, status, failure_reason, created_at |
| `security_alerts` | Security notifications | id, user_id, alert_type, message, severity, is_read, created_at |
| `password_analysis` | Password health analysis | id, user_id, vault_entry_id, strength_score, is_weak, is_reused, is_breached, is_old, analyzed_at |

### 5. System & Configuration (3 Tables)

| Table Name | Description | Key Fields |
|------------|-------------|------------|
| `user_settings` | User preferences | id, user_id, theme, auto_lock_timeout, session_timeout, password_generator_defaults, notification_preferences |
| `backup_exports` | Export history tracking | id, user_id, export_format, file_hash, created_at, expires_at |
| `notifications` | User notifications | id, user_id, type, title, message, is_read, created_at |

---

## 📈 Database Relationships Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USER MANAGEMENT                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌───────────┐       ┌───────────────────┐       ┌──────────────────┐     │
│    │   users   │───────│ security_questions │       │  recovery_codes  │     │
│    │           │       │                   │       │                  │     │
│    │  (1)      │◄──────│       (*)         │       │       (*)        │     │
│    └─────┬─────┘       └───────────────────┘       └──────────────────┘     │
│          │                                                                   │
│          │ 1:N                                                              │
│          ▼                                                                   │
│    ┌─────────────┐     ┌───────────────────┐                                │
│    │user_sessions│     │  two_factor_auth  │                                │
│    │     (*)     │     │       (1)         │                                │
│    └─────────────┘     └───────────────────┘                                │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              PASSWORD VAULT                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌───────────┐                                                            │
│    │   users   │                                                            │
│    └─────┬─────┘                                                            │
│          │ 1:N                                                              │
│          ▼                                                                   │
│    ┌─────────────────┐     ┌────────────┐     ┌───────────┐                │
│    │  vault_entries  │────►│ categories │     │  folders  │                │
│    │                 │     └────────────┘     └───────────┘                │
│    │  account_name   │           ▲                  ▲                       │
│    │  encrypted_pass │           │                  │                       │
│    │  website_url    │───────────┴──────────────────┘                       │
│    └────────┬────────┘                                                      │
│             │ 1:N                                                            │
│             ▼                                                                │
│    ┌─────────────────┐     ┌────────────────┐                               │
│    │ vault_snapshots │     │   vault_trash  │                               │
│    │  (history)      │     │ (soft delete)  │                               │
│    └─────────────────┘     └────────────────┘                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                            SECURITY & AUDIT                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    ┌───────────┐                                                            │
│    │   users   │                                                            │
│    └─────┬─────┘                                                            │
│          │ 1:N                                                              │
│          ▼                                                                   │
│    ┌────────────┐  ┌────────────────┐  ┌─────────────────┐                  │
│    │ audit_logs │  │ login_attempts │  │ security_alerts │                  │
│    └────────────┘  └────────────────┘  └─────────────────┘                  │
│                                                                              │
│    ┌───────────────────┐                                                    │
│    │ password_analysis │◄─── Links to vault_entries                         │
│    │  (health check)   │                                                    │
│    └───────────────────┘                                                    │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 📁 Detailed Project File Structure

```
Rev-PasswordManager/
│
├── 📄 README.md                          # Project overview & setup instructions
├── 📄 pom.xml                            # Maven dependencies configuration
├── 📄 .gitignore                         # Git ignore rules
├── 📄 DATABASE_AND_STRUCTURE.md          # This documentation file
│
├── 📁 docs/                              # Documentation folder
│   ├── 📄 ERD.md                         # Entity Relationship Diagram
│   ├── 📄 ARCHITECTURE.md                # Application Architecture Diagram
│   ├── 📄 API_DOCUMENTATION.md           # REST API endpoints documentation
│   ├── 📄 SECURITY_DOCUMENTATION.md      # Security implementation details
│   └── 📄 TESTING_ARTIFACTS.md           # Testing documentation
│
├── 📁 database/                          # Database scripts
│   ├── 📄 schema.sql                     # Complete database schema
│   ├── 📄 seed_data.sql                  # Initial seed data
│   └── 📄 migrations/                    # Database migration scripts
│       ├── 📄 V1__initial_schema.sql
│       └── 📄 V2__add_audit_tables.sql
│
├── 📁 src/
│   ├── 📁 main/
│   │   ├── 📁 java/
│   │   │   └── 📁 com/
│   │   │       └── 📁 revature/
│   │   │           └── 📁 passwordmanager/
│   │   │               │
│   │   │               ├── 📄 PasswordManagerApplication.java   # Main entry point
│   │   │               │
│   │   │               ├── 📁 config/                           # Configuration classes
│   │   │               │   ├── 📄 SecurityConfig.java           # Security configuration
│   │   │               │   ├── 📄 JwtConfig.java                # JWT settings
│   │   │               │   ├── 📄 EncryptionConfig.java         # AES-256 encryption setup
│   │   │               │   ├── 📄 CaptchaConfig.java            # CAPTCHA configuration
│   │   │               │   └── 📄 RateLimitConfig.java          # Rate limiting settings
│   │   │               │
│   │   │               ├── 📁 model/                            # Entity classes (18 entities)
│   │   │               │   ├── 📁 user/
│   │   │               │   │   ├── 📄 User.java
│   │   │               │   │   ├── 📄 SecurityQuestion.java
│   │   │               │   │   ├── 📄 UserSession.java
│   │   │               │   │   └── 📄 RecoveryCode.java
│   │   │               │   ├── 📁 auth/
│   │   │               │   │   ├── 📄 TwoFactorAuth.java
│   │   │               │   │   └── 📄 OtpToken.java
│   │   │               │   ├── 📁 vault/
│   │   │               │   │   ├── 📄 VaultEntry.java
│   │   │               │   │   ├── 📄 Category.java
│   │   │               │   │   ├── 📄 Folder.java
│   │   │               │   │   ├── 📄 VaultTrash.java
│   │   │               │   │   └── 📄 VaultSnapshot.java
│   │   │               │   ├── 📁 security/
│   │   │               │   │   ├── 📄 AuditLog.java
│   │   │               │   │   ├── 📄 LoginAttempt.java
│   │   │               │   │   ├── 📄 SecurityAlert.java
│   │   │               │   │   └── 📄 PasswordAnalysis.java
│   │   │               │   └── 📁 system/
│   │   │               │       ├── 📄 UserSettings.java
│   │   │               │       ├── 📄 BackupExport.java
│   │   │               │       └── 📄 Notification.java
│   │   │               │
│   │   │               ├── 📁 dto/                              # Data Transfer Objects
│   │   │               │   ├── 📁 request/
│   │   │               │   │   ├── 📄 RegistrationRequest.java
│   │   │               │   │   ├── 📄 LoginRequest.java
│   │   │               │   │   ├── 📄 VaultEntryRequest.java
│   │   │               │   │   ├── 📄 PasswordGeneratorRequest.java
│   │   │               │   │   ├── 📄 ChangePasswordRequest.java
│   │   │               │   │   └── 📄 TwoFactorSetupRequest.java
│   │   │               │   └── 📁 response/
│   │   │               │       ├── 📄 AuthResponse.java
│   │   │               │       ├── 📄 VaultEntryResponse.java
│   │   │               │       ├── 📄 DashboardResponse.java
│   │   │               │       ├── 📄 SecurityAuditResponse.java
│   │   │               │       └── 📄 PasswordStrengthResponse.java
│   │   │               │
│   │   │               ├── 📁 repository/                       # Data Access Layer
│   │   │               │   ├── 📄 UserRepository.java
│   │   │               │   ├── 📄 SecurityQuestionRepository.java
│   │   │               │   ├── 📄 UserSessionRepository.java
│   │   │               │   ├── 📄 RecoveryCodeRepository.java
│   │   │               │   ├── 📄 TwoFactorAuthRepository.java
│   │   │               │   ├── 📄 OtpTokenRepository.java
│   │   │               │   ├── 📄 VaultEntryRepository.java
│   │   │               │   ├── 📄 CategoryRepository.java
│   │   │               │   ├── 📄 FolderRepository.java
│   │   │               │   ├── 📄 VaultTrashRepository.java
│   │   │               │   ├── 📄 VaultSnapshotRepository.java
│   │   │               │   ├── 📄 AuditLogRepository.java
│   │   │               │   ├── 📄 LoginAttemptRepository.java
│   │   │               │   ├── 📄 SecurityAlertRepository.java
│   │   │               │   ├── 📄 PasswordAnalysisRepository.java
│   │   │               │   ├── 📄 UserSettingsRepository.java
│   │   │               │   ├── 📄 BackupExportRepository.java
│   │   │               │   └── 📄 NotificationRepository.java
│   │   │               │
│   │   │               ├── 📁 service/                          # Business Logic Layer
│   │   │               │   ├── 📁 auth/
│   │   │               │   │   ├── 📄 AuthenticationService.java
│   │   │               │   │   ├── 📄 RegistrationService.java
│   │   │               │   │   ├── 📄 TwoFactorService.java
│   │   │               │   │   ├── 📄 SessionService.java
│   │   │               │   │   └── 📄 AccountRecoveryService.java
│   │   │               │   ├── 📁 vault/
│   │   │               │   │   ├── 📄 VaultService.java
│   │   │               │   │   ├── 📄 CategoryService.java
│   │   │               │   │   ├── 📄 FolderService.java
│   │   │               │   │   ├── 📄 VaultTrashService.java
│   │   │               │   │   └── 📄 VaultSnapshotService.java
│   │   │               │   ├── 📁 security/
│   │   │               │   │   ├── 📄 EncryptionService.java
│   │   │               │   │   ├── 📄 PasswordGeneratorService.java
│   │   │               │   │   ├── 📄 PasswordStrengthService.java
│   │   │               │   │   ├── 📄 SecurityAuditService.java
│   │   │               │   │   ├── 📄 AuditLogService.java
│   │   │               │   │   └── 📄 AdaptiveAuthService.java
│   │   │               │   ├── 📁 user/
│   │   │               │   │   ├── 📄 UserService.java
│   │   │               │   │   ├── 📄 UserSettingsService.java
│   │   │               │   │   └── 📄 ProfileService.java
│   │   │               │   └── 📁 backup/
│   │   │               │       ├── 📄 ExportService.java
│   │   │               │       └── 📄 ImportService.java
│   │   │               │
│   │   │               ├── 📁 controller/                       # REST Controllers
│   │   │               │   ├── 📄 AuthController.java           # /api/auth/*
│   │   │               │   ├── 📄 UserController.java           # /api/users/*
│   │   │               │   ├── 📄 VaultController.java          # /api/vault/*
│   │   │               │   ├── 📄 CategoryController.java       # /api/categories/*
│   │   │               │   ├── 📄 FolderController.java         # /api/folders/*
│   │   │               │   ├── 📄 PasswordGeneratorController.java  # /api/generator/*
│   │   │               │   ├── 📄 SecurityController.java       # /api/security/*
│   │   │               │   ├── 📄 TwoFactorController.java      # /api/2fa/*
│   │   │               │   ├── 📄 BackupController.java         # /api/backup/*
│   │   │               │   ├── 📄 NotificationController.java   # /api/notifications/*
│   │   │               │   ├── 📄 SessionController.java        # /api/sessions/*
│   │   │               │   └── 📄 HealthController.java         # /api/health/*
│   │   │               │
│   │   │               ├── 📁 security/                         # Security Components
│   │   │               │   ├── 📄 JwtTokenProvider.java
│   │   │               │   ├── 📄 JwtAuthenticationFilter.java
│   │   │               │   ├── 📄 CustomUserDetailsService.java
│   │   │               │   ├── 📄 RateLimitFilter.java
│   │   │               │   ├── 📄 CaptchaValidator.java
│   │   │               │   └── 📄 MasterPasswordValidator.java
│   │   │               │
│   │   │               ├── 📁 exception/                        # Custom Exceptions
│   │   │               │   ├── 📄 GlobalExceptionHandler.java
│   │   │               │   ├── 📄 AuthenticationException.java
│   │   │               │   ├── 📄 VaultAccessException.java
│   │   │               │   ├── 📄 RateLimitExceededException.java
│   │   │               │   └── 📄 ResourceNotFoundException.java
│   │   │               │
│   │   │               └── 📁 util/                             # Utility Classes
│   │   │                   ├── 📄 EncryptionUtil.java
│   │   │                   ├── 📄 PasswordValidator.java
│   │   │                   ├── 📄 PasswordStrengthCalculator.java
│   │   │                   └── 📄 DateTimeUtil.java
│   │   │
│   │   ├── 📁 resources/
│   │   │   ├── 📄 application.properties                        # Main configuration
│   │   │   ├── 📄 application-dev.properties                    # Dev environment
│   │   │   ├── 📄 application-prod.properties                   # Production environment
│   │   │   └── 📁 templates/                                    # Email templates
│   │   │       ├── 📄 otp-email.html
│   │   │       └── 📄 security-alert.html
│   │   │
│   │   └── 📁 webapp/                                           # Frontend (if any)
│   │       ├── 📁 static/
│   │       │   ├── 📁 css/
│   │       │   ├── 📁 js/
│   │       │   └── 📁 images/
│   │       └── 📁 WEB-INF/
│   │           └── 📁 views/
│   │
│   └── 📁 test/                                                 # Test classes
│       └── 📁 java/
│           └── 📁 com/
│               └── 📁 revature/
│                   └── 📁 passwordmanager/
│                       ├── 📁 controller/
│                       │   ├── 📄 AuthControllerTest.java
│                       │   ├── 📄 VaultControllerTest.java
│                       │   └── 📄 PasswordGeneratorControllerTest.java
│                       ├── 📁 service/
│                       │   ├── 📄 AuthenticationServiceTest.java
│                       │   ├── 📄 VaultServiceTest.java
│                       │   ├── 📄 EncryptionServiceTest.java
│                       │   └── 📄 PasswordGeneratorServiceTest.java
│                       ├── 📁 repository/
│                       │   └── 📄 VaultEntryRepositoryTest.java
│                       └── 📁 integration/
│                           └── 📄 VaultIntegrationTest.java
│
└── 📁 frontend/                                                 # Angular Frontend (Optional)
    ├── 📄 package.json
    ├── 📄 angular.json
    ├── 📁 src/
    │   ├── 📁 app/
    │   │   ├── 📁 core/                                         # Core modules
    │   │   │   ├── 📁 auth/
    │   │   │   ├── 📁 guards/
    │   │   │   ├── 📁 interceptors/
    │   │   │   └── 📁 services/
    │   │   ├── 📁 shared/                                       # Shared components
    │   │   │   ├── 📁 components/
    │   │   │   └── 📁 directives/
    │   │   ├── 📁 features/                                     # Feature modules
    │   │   │   ├── 📁 dashboard/
    │   │   │   ├── 📁 vault/
    │   │   │   ├── 📁 generator/
    │   │   │   ├── 📁 settings/
    │   │   │   └── 📁 security/
    │   │   └── 📄 app.module.ts
    │   ├── 📁 assets/
    │   └── 📁 environments/
    └── 📁 e2e/
```

---

## 📊 Table Count Summary

| Category | Tables | Table Names |
|----------|--------|-------------|
| User & Authentication | 4 | users, security_questions, user_sessions, recovery_codes |
| Two-Factor Auth | 2 | two_factor_auth, otp_tokens |
| Vault & Passwords | 5 | vault_entries, categories, folders, vault_trash, vault_snapshots |
| Security & Audit | 4 | audit_logs, login_attempts, security_alerts, password_analysis |
| System & Config | 3 | user_settings, backup_exports, notifications |
| **TOTAL** | **18** | |

---

## 📁 File Count Summary

| Layer | Count | Description |
|-------|-------|-------------|
| Models | 18 | One per database table |
| Repositories | 18 | One per model |
| Services | 26 | Business logic classes |
| Controllers | 12 | REST API endpoints |
| DTOs | 26 | Request/Response objects |
| Config | 5 | Configuration classes |
| Security | 6 | Security components |
| Utilities | 6 | Helper classes |
| Exceptions | 5 | Custom exceptions |
| **TOTAL (Backend)** | **~127** | Java files |

---

## 🔗 Key Relationships

### 1:1 Relationships (UNIQUE constraint enforced)

| Parent Table | Child Table | Foreign Key | Constraint |
|-------------|-------------|-------------|------------|
| users | two_factor_auth | user_id | UNIQUE |
| users | user_settings | user_id | UNIQUE |
| vault_entries | vault_trash | vault_entry_id | UNIQUE |
| vault_entries | password_analysis | vault_entry_id | UNIQUE |

### 1:N Relationships

| Parent Table | Child Table | Foreign Key |
|-------------|-------------|-------------|
| users | security_questions | user_id |
| users | user_sessions | user_id |
| users | recovery_codes | user_id |
| users | otp_tokens | user_id |
| users | categories | user_id |
| users | folders | user_id |
| users | vault_entries | user_id |
| users | audit_logs | user_id |
| users | login_attempts | user_id |
| users | security_alerts | user_id |
| users | backup_exports | user_id |
| users | notifications | user_id |
| categories | vault_entries | category_id |
| folders | vault_entries | folder_id |
| folders | folders | parent_folder_id (self-ref) |
| vault_entries | vault_snapshots | vault_entry_id |

---

> **Note:** This structure supports all 58+ features defined in the Feature Specifications document, with a focus on security, scalability, and maintainability.
