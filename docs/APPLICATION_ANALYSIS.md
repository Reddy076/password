# Rev-PasswordManager — Comprehensive Application Analysis

> Generated: 2026-02-27  
> Analyst: Kilo Code

---

## 1. Executive Summary

**Rev-PasswordManager** is a full-stack, enterprise-grade password management application built with:

| Layer | Technology |
|-------|-----------|
| Backend | Java 21 + Spring Boot 3.2.3 |
| Frontend | Angular 18 (standalone components) |
| Database | MySQL (H2 for tests) |
| Auth | JWT (JJWT 0.12.5) + BCrypt + TOTP 2FA |
| Encryption | AES-256-GCM + PBKDF2WithHmacSHA256 |
| API Docs | SpringDoc OpenAPI 2.3.0 (Swagger UI) |
| Code Quality | SonarQube + JaCoCo |
| Testing | JUnit 5 + Mockito + Testcontainers |

The application provides a secure vault for storing credentials, with a rich security feature set including 2FA, duress mode, audit logging, password health analysis, and backup/restore capabilities.

---

## 2. Project Structure

```
Rev-PasswordManager/
├── pom.xml                          # Maven build descriptor
├── src/
│   ├── main/java/com/revature/passwordmanager/
│   │   ├── PasswordManagerApplication.java   # Entry point (@EnableScheduling, @EnableAsync)
│   │   ├── aspect/                           # AOP: audit + logging
│   │   ├── config/                           # Security, JWT, CORS, Encryption, OpenAPI
│   │   ├── controller/                       # 13 REST controllers
│   │   ├── dto/                              # Request/Response DTOs
│   │   ├── exception/                        # Global exception handler
│   │   ├── model/                            # JPA entities (auth, vault, security, user)
│   │   ├── repository/                       # Spring Data JPA repositories
│   │   ├── scheduler/                        # Scheduled tasks (trash cleanup, account deletion)
│   │   ├── security/                         # JWT filter, rate limit filter, user details
│   │   ├── service/                          # Business logic (auth, vault, security, backup)
│   │   └── util/                             # Encryption, password strength, TOTP, device fingerprint
│   ├── main/resources/
│   │   ├── application.properties            # Main config
│   │   ├── application-dev.properties        # Dev profile
│   │   └── application-prod.properties       # Prod profile
│   └── test/                                 # ~35 test classes
├── frontend/                                 # Angular 18 SPA
│   ├── src/app/
│   │   ├── core/api/                         # OpenAPI-generated TypeScript clients
│   │   ├── core/guards/                      # Auth guard
│   │   ├── core/interceptors/                # Auth + error interceptors
│   │   ├── core/services/                    # Idle service, notification event service
│   │   ├── features/auth/                    # Login, Register, Forgot Password
│   │   ├── features/dashboard/               # Dashboard
│   │   ├── features/vault/                   # Vault, modals, category sidebar
│   │   ├── features/user/profile/            # Profile management
│   │   └── layout/                           # Shell layout + notification bell
│   └── package.json
├── database/schema.sql                       # Full MySQL schema
└── docs/                                     # Extensive documentation (30+ files)
```

---

## 3. Backend Architecture

### 3.1 Technology Stack

- **Spring Boot 3.2.3** on **Java 21**
- **Spring Security** — stateless JWT-based authentication
- **Spring Data JPA** — Hibernate ORM with MySQL
- **Spring AOP** — cross-cutting concerns (audit logging, method logging)
- **Spring Mail** — email notifications and OTP delivery
- **Spring Actuator** — health, info, metrics endpoints
- **Spring Scheduling** — automated cleanup tasks
- **Lombok** — boilerplate reduction
- **MapStruct** — DTO mapping
- **JJWT 0.12.5** — JWT generation and validation
- **dev.samstevens.totp** — TOTP-based 2FA
- **SpringDoc OpenAPI 2.3.0** — Swagger UI at `/swagger-ui.html`
- **JaCoCo + SonarQube** — code coverage and quality

### 3.2 Controller Layer (13 Controllers)

| Controller | Base Path | Responsibility |
|-----------|-----------|---------------|
| `AuthController` | `/api/auth` | Registration, login, logout, OTP, duress, password hint |
| `VaultController` | `/api/vault` | CRUD vault entries, trash, snapshots, sensitive access |
| `UserController` | `/api/user` | Profile, password change, account deletion, read-only mode |
| `SecurityController` | `/api/security` | Audit logs, login history, alerts, vault security audit |
| `TwoFactorController` | `/api/2fa` | TOTP setup, verify, disable, backup codes |
| `SessionController` | `/api/sessions` | Active session management |
| `BackupController` | `/api/backup` | Export/import vault, third-party import, snapshots |
| `CategoryController` | `/api/categories` | Category CRUD |
| `FolderController` | `/api/folders` | Folder CRUD (nested folders supported) |
| `PasswordGeneratorController` | `/api/generator` | Password generation and strength check |
| `NotificationController` | `/api/notifications` | In-app notifications |
| `UserSettingsController` | `/api/settings` | User preferences |
| `HealthController` | `/api/health` | System health status |

### 3.3 Service Layer

Services are organized into domain packages:

- **`service/auth/`** — `AuthenticationService`, `RegistrationService`, `SessionService`, `TwoFactorService`, `AccountRecoveryService`
- **`service/vault/`** — `VaultService`, `VaultTrashService`, `VaultSnapshotService`, `CategoryService`, `FolderService`
- **`service/security/`** — `EncryptionService`, `SecurityAuditService`, `AuditLogService`, `LoginAttemptService`, `SecurityAlertService`, `DuressService`, `RateLimitService`, `CaptchaService`, `AdaptiveAuthService`, `GeoLocationService`, `PasswordGeneratorService`, `PasswordStrengthService`
- **`service/user/`** — `UserService`, `UserSettingsService`, `AccountDeletionService`
- **`service/backup/`** — `ExportService`, `ImportService`, `ThirdPartyImportService`
- **`service/email/`** — `EmailService`
- **`service/notification/`** — `NotificationService`
- **`service/system/`** — `HealthService`

### 3.4 Data Model (JPA Entities)

| Package | Entities |
|---------|---------|
| `model/user/` | `User`, `UserSession`, `UserSettings`, `SecurityQuestion`, `RecoveryCode` |
| `model/vault/` | `VaultEntry`, `Category`, `Folder`, `VaultSnapshot` |
| `model/auth/` | `TwoFactorAuth`, `OtpToken` |
| `model/security/` | `AuditLog`, `LoginAttempt`, `SecurityAlert`, `PasswordAnalysis` |
| `model/backup/` | `BackupExport` |
| `model/notification/` | `Notification` |

### 3.5 Scheduled Tasks

| Scheduler | Task |
|-----------|------|
| `TrashCleanupScheduler` | Auto-permanently-deletes trash entries after retention period |
| `AccountDeletionScheduler` | Processes scheduled account deletions |

---

## 4. Frontend Architecture

### 4.1 Technology Stack

- **Angular 18** with standalone components (no NgModules)
- **Angular Material 18** — UI component library
- **Lucide Angular** — icon library
- **RxJS 7.8** — reactive programming
- **OpenAPI Generator** — auto-generated TypeScript API clients from backend OpenAPI spec

### 4.2 Application Structure

```
app/
├── app.config.ts          # Root providers (router, HTTP, interceptors, icons)
├── app.routes.ts          # Route definitions
├── core/
│   ├── api/               # Auto-generated OpenAPI clients (13 service files)
│   ├── guards/auth.guard  # Route protection
│   ├── interceptors/
│   │   ├── auth.interceptor    # Attaches Bearer token to requests
│   │   └── error.interceptor   # Handles 401 → token refresh → retry
│   └── services/
│       ├── idle.service        # Auto-logout on inactivity
│       └── notification-event.service
├── features/
│   ├── auth/              # Login, Register, Forgot Password
│   ├── dashboard/         # Dashboard overview
│   ├── vault/             # Main vault UI + modals
│   └── user/profile/      # Profile, 2FA, sessions, account deletion
└── layout/                # Shell with sidebar + notification bell
```

### 4.3 Routing

| Route | Component | Guard |
|-------|-----------|-------|
| `/login` | `LoginComponent` | None |
| `/register` | `RegisterComponent` | None |
| `/forgot-password` | `ForgotPasswordComponent` | None |
| `/dashboard` | `DashboardComponent` | `authGuard` |
| `/vault` | `VaultComponent` | `authGuard` |
| `/profile` | `ProfileComponent` | `authGuard` |
| `**` | Redirect to `/login` | — |

### 4.4 Key Frontend Features

- **Lazy loading** — all feature components loaded on demand
- **Token refresh** — `error.interceptor` automatically refreshes expired access tokens using the refresh token, queuing concurrent requests
- **Idle detection** — `idle.service` auto-logs out inactive users
- **Notification bell** — real-time unread notification count in layout
- **Vault filtering** — filter by category, folder, favorites, trash via query params
- **Sensitive entry unlock** — master password modal for highly sensitive entries
- **Password generator widget** — inline password generation in vault

---

## 5. Database Schema

The schema has **18 tables** organized into 5 domains:

### 5.1 User & Authentication (4 tables)
- **`users`** — core user record with master password hash, salt, 2FA flag, duress password hash, lockout tracking, deletion scheduling
- **`security_questions`** — hashed answers for account recovery
- **`user_sessions`** — active JWT sessions with device/IP/location info
- **`recovery_codes`** — one-time recovery codes

### 5.2 Two-Factor Authentication (3 tables)
- **`two_factor_auth`** — TOTP secret key per user
- **`backup_codes`** — element collection of TOTP backup codes
- **`otp_tokens`** — email OTP tokens with expiry

### 5.3 Vault & Passwords (5 tables)
- **`vault_entries`** — encrypted passwords with category/folder/favorite/sensitive flags, access tracking
- **`categories`** — user-defined categories with icons
- **`folders`** — hierarchical folders (self-referencing `parent_folder_id`)
- **`vault_trash`** — soft-deleted entries with scheduled permanent deletion
- **`vault_snapshots`** — password history per entry

### 5.4 Security & Audit (4 tables)
- **`audit_logs`** — action audit trail with IP/user-agent
- **`login_attempts`** — login history with success/failure status
- **`security_alerts`** — user-facing security notifications
- **`password_analysis`** — per-entry strength/reuse/breach/age analysis

### 5.5 System & Configuration (3 tables)
- **`user_settings`** — theme, auto-lock timeout, session timeout, JSON preferences
- **`backup_exports`** — export history with format and file hash
- **`notifications`** — in-app notification records

### 5.6 Performance Indexes
Indexes on: `users(email)`, `users(username)`, `user_sessions(user_id)`, `user_sessions(token)`, `vault_entries(user_id)`, `vault_entries(category_id)`, `vault_entries(folder_id)`, `audit_logs(user_id)`, `audit_logs(action)`, `login_attempts(user_id)`, `login_attempts(ip_address)`, `otp_tokens(user_id)`, `otp_tokens(expiry_date)`

---

## 6. Security Architecture

### 6.1 Authentication Flow

```
Client → POST /api/auth/login
  → RateLimitFilter (IP-based rate limiting)
  → CaptchaService (required after N failed attempts)
  → AuthenticationService.login()
    → BCrypt password verification
    → DuressService.isDuressLogin() check
    → If 2FA enabled → return partial token, send OTP
    → If normal → generate access + refresh JWT tokens
    → LoginAttemptService.recordAttempt()
    → SessionService.createSession()
  → Return AuthResponse (accessToken, refreshToken)
```

### 6.2 Encryption

| Mechanism | Usage |
|-----------|-------|
| **AES-256-GCM** | Vault password encryption (authenticated encryption) |
| **PBKDF2WithHmacSHA256** (100,000 iterations) | Key derivation from master password + salt |
| **BCrypt** | Master password hashing, security question answer hashing |
| **HMAC-SHA256** | JWT signing |
| **SecureRandom** | IV generation (12 bytes for GCM) |

The encryption key is derived per-user from `masterPasswordHash + salt` using PBKDF2, meaning the server never stores the raw encryption key — it is re-derived on each operation.

### 6.3 JWT Configuration

| Token | Expiry |
|-------|--------|
| Access Token | 15 minutes (900,000 ms) |
| Refresh Token | 7 days (604,800,000 ms) |

JWT tokens include a `duress` claim for duress mode detection.

### 6.4 Security Features Matrix

| Feature | Implementation |
|---------|---------------|
| **Rate Limiting** | `RateLimitFilter` + `RateLimitService` (IP-based, Guava RateLimiter) |
| **CAPTCHA** | `CaptchaService` — triggered after repeated failed logins |
| **Account Lockout** | `failedLoginAttempts` + `lockedUntil` on `User` entity |
| **2FA (TOTP)** | `TwoFactorService` + `TOTPUtil` (dev.samstevens.totp library) |
| **Email OTP** | `OtpToken` entity + `EmailService` (Gmail SMTP) |
| **Duress Mode** | Separate `duressPasswordHash`; returns dummy vault data on duress login |
| **Sensitive Entries** | `isHighlySensitive` flag; requires master password re-verification to view |
| **Audit Logging** | `AuditLogAspect` (AOP) + `AuditLogService` — logs all vault operations |
| **Security Alerts** | `SecurityAlertService` — generates alerts for suspicious activity |
| **Password Analysis** | `SecurityAuditService` — detects weak, reused, old, breached passwords |
| **Session Management** | `UserSession` entity; sessions tracked with device/IP/location |
| **Adaptive Auth** | `AdaptiveAuthService` — risk-based authentication adjustments |
| **Geo-Location** | `GeoLocationService` — IP-based location detection |
| **Device Fingerprint** | `DeviceFingerprintUtil` — device identification |
| **Read-Only Mode** | `ToggleReadOnlyRequest` — prevents vault modifications |
| **Password Hint** | Stored hint (validated to not contain the actual password) |
| **CORS** | `CorsConfig` — configurable allowed origins |
| **CSRF** | Disabled (stateless JWT API) |

### 6.5 Security Filter Chain

```
Request → RateLimitFilter → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → Controller
```

Public endpoints (no auth required):
- `/api/auth/**` — all auth endpoints
- `/api/generator/**` — password generator (public utility)
- `/api/health/**` — health checks
- `/v3/api-docs/**`, `/swagger-ui/**` — API documentation

---

## 7. API Endpoints Summary

### Authentication (`/api/auth`)
- `POST /register` — user registration
- `POST /verify-email` — email OTP verification
- `POST /resend-verification-otp` — resend verification OTP
- `POST /login` — login with optional CAPTCHA
- `POST /refresh-token` — JWT refresh
- `POST /logout` — invalidate session
- `GET /security-questions/{username}` — get security questions
- `POST /reset-password` — reset via security questions
- `POST /verify-otp` — verify 2FA OTP
- `POST /send-otp` / `POST /resend-otp` — OTP delivery
- `POST /forgot-password` — initiate email recovery
- `POST /verify-security-questions` — validate recovery answers
- `POST /verify-captcha` — standalone CAPTCHA check
- `GET /validate-token` — token validity check
- `POST /verify-master-password` — re-auth for sensitive actions
- `POST /duress-login` — duress mode login
- `POST /set-duress-password` — configure duress password
- `GET /password-hint/{username}` — retrieve hint
- `PUT /password-hint` — set/update hint

### Vault (`/api/vault`)
- `POST /` — create entry
- `GET /` — list all entries
- `GET /search` — search with filters (keyword, category, folder, favorite, sensitive, sort)
- `GET /filter` — filter by category/folder/favorite
- `GET /recent` — recently created entries
- `GET /recently-used` — recently accessed entries
- `GET /{id}` — get entry detail
- `PUT /{id}` — update entry
- `PUT /{id}/favorite` — toggle favorite
- `GET /favorites` — list favorites
- `POST /entries/bulk-delete` — bulk soft-delete
- `POST /entries/{id}/view-password` — reveal decrypted password
- `DELETE /{id}` — soft-delete (move to trash)
- `PUT /entries/{id}/sensitive` — toggle sensitive flag
- `POST /{id}/sensitive-view` — access sensitive entry with master password
- `GET /entries/{id}/history` — password history snapshots
- `GET /trash` — list trash
- `GET /trash/count` — trash item count
- `POST /trash/{id}/restore` — restore from trash
- `POST /trash/restore-all` — restore all trash
- `DELETE /trash/{id}` — permanent delete
- `DELETE /trash/empty` — empty trash

### Security (`/api/security`)
- `GET /audit-logs` — user audit trail
- `GET /login-history` — login attempt history
- `GET /alerts` — security alerts
- `PUT /alerts/{id}/read` — mark alert read
- `DELETE /alerts/{id}` — delete alert
- `GET /audit-report` — full security audit report
- `GET /weak-passwords` — weak password list
- `GET /reused-passwords` — reused password list
- `GET /old-passwords` — old password list
- `POST /analyze-vault` — trigger vault analysis

### Two-Factor Auth (`/api/2fa`)
- `GET /status` — 2FA enabled status
- `POST /setup` — initialize TOTP setup (QR code)
- `POST /verify-setup` — verify and enable 2FA
- `POST /disable` — disable 2FA
- `GET /backup-codes` — list backup codes
- `POST /regenerate-codes` — regenerate backup codes

### Backup (`/api/backup`)
- `GET /export` — export vault (JSON/CSV, optional password encryption)
- `POST /import` — import vault data
- `POST /import-external` — import from third-party (LastPass, 1Password, etc.)
- `GET /export/preview` — preview export
- `POST /import/validate` — validate import data
- `GET /import-external/formats` — supported import formats
- `GET /snapshots` — all vault snapshots
- `POST /snapshots/{id}/restore` — restore from snapshot

### Other Controllers
- **User** (`/api/user`): profile, change password, account deletion, read-only mode, dashboard
- **Sessions** (`/api/sessions`): list, revoke, revoke-all
- **Categories** (`/api/categories`): CRUD
- **Folders** (`/api/folders`): CRUD with nesting
- **Password Generator** (`/api/generator`): generate, generate-multiple, check-strength, validate
- **Notifications** (`/api/notifications`): list, mark-read, mark-all-read, delete, unread-count
- **Settings** (`/api/settings`): get, update
- **Health** (`/api/health`): status, detailed

---

## 8. Testing Strategy

### Backend Tests (~35 test classes)
- **Unit tests** for all service classes using Mockito
- **Integration tests** using Testcontainers (MySQL) and H2 in-memory
- **Security tests** — `JwtAuthenticationIntegrationTest`, `MasterPasswordValidatorTest`
- **Coverage** — JaCoCo configured; SonarQube integration for quality gates
- **Test suite** — `AllTestsSuite` aggregates all tests

### Frontend Tests
- **Karma + Jasmine** test runner
- Spec files for all components and services
- Coverage for: auth components, vault component, profile, layout, interceptors, guards

---

## 9. Notable Design Patterns & Decisions

| Pattern | Where Used |
|---------|-----------|
| **Repository Pattern** | Spring Data JPA repositories |
| **DTO Pattern** | Separate request/response DTOs for all endpoints |
| **Builder Pattern** | Lombok `@Builder` on all entities and DTOs |
| **AOP** | Audit logging and method-level logging aspects |
| **Strategy Pattern** | Multiple import format handlers in `ThirdPartyImportService` |
| **Observer Pattern** | `NotificationEventService` for cross-component events |
| **Interceptor Pattern** | Angular HTTP interceptors for auth and error handling |
| **Guard Pattern** | Angular route guards for authentication |
| **Lazy Loading** | Angular standalone components loaded on demand |
| **Token Refresh Queue** | `BehaviorSubject` in error interceptor queues requests during refresh |

---

## 10. Configuration & Deployment

### Application Properties
- **Port**: 8080
- **Database**: MySQL at `localhost:3306/rev_password_manager`
- **JWT Secret**: Configurable via `JWT_SECRET` env variable (with default)
- **Encryption**: AES/GCM/NoPadding, 256-bit keys
- **Email**: Gmail SMTP (credentials in properties — **security concern in production**)
- **Logging**: Rolling file logs at `logs/password-manager.log` (10MB, 7-day retention)
- **Actuator**: health, info, metrics exposed

### Profiles
- `application-dev.properties` — development overrides
- `application-prod.properties` — production overrides

---

## 11. Identified Strengths

1. **Comprehensive security feature set** — 2FA, duress mode, rate limiting, CAPTCHA, adaptive auth, audit logging
2. **Strong encryption** — AES-256-GCM with PBKDF2 key derivation (100K iterations)
3. **Well-organized codebase** — clear separation of concerns across layers and domain packages
4. **Auto-generated API clients** — OpenAPI spec drives TypeScript client generation, keeping frontend/backend in sync
5. **Extensive documentation** — 30+ documentation files covering features, APIs, testing
6. **Comprehensive test coverage** — unit + integration tests for all major services
7. **Soft-delete with trash** — recoverable deletion with scheduled permanent cleanup
8. **Password health analysis** — detects weak, reused, old passwords across the vault
9. **Backup/restore** — export/import with optional encryption, third-party format support
10. **Hierarchical organization** — nested folders + categories for vault organization

---

## 12. Identified Concerns & Recommendations

| Concern | Severity | Recommendation |
|---------|----------|---------------|
| **Email credentials in `application.properties`** | HIGH | Move to environment variables or secrets manager |
| **Default JWT secret in properties** | HIGH | Enforce `JWT_SECRET` env variable in production; remove default |
| **Database password in properties** | HIGH | Use environment variables or Spring Cloud Config |
| **CSRF disabled** | MEDIUM | Acceptable for stateless JWT API, but document the decision |
| **Token stored in `localStorage`**  | MEDIUM | Consider `httpOnly` cookies to mitigate XSS token theft |
| **`application.properties` has `spring.jpa.show-sql=true`** | LOW | Disable in production to avoid SQL leakage in logs |
| **`spring.jpa.hibernate.ddl-auto=update`** | MEDIUM | Use `validate` in production; manage schema with Flyway/Liquibase |
| **`DuressService` uses `Random` (not `SecureRandom`)** | LOW | Use `SecureRandom` for dummy vault generation |
| **Encryption key derived from `masterPasswordHash + salt`** | MEDIUM | The key derivation input is the *hash* of the password, not the password itself — this is non-standard; typically PBKDF2 takes the raw password |
| **No refresh token rotation** | MEDIUM | Implement refresh token rotation to limit refresh token reuse |
| **`isRefreshing` is module-level state in error interceptor** | LOW | Can cause issues in SSR or multi-tab scenarios; consider service-level state |

---

## 13. Feature Coverage Summary

The application implements **26+ documented features**:

| # | Feature |
|---|---------|
| 1-5 | Core auth (register, login, logout, JWT, email verification) |
| 6 | Session management |
| 7 | Security questions |
| 8 | Account recovery |
| 9 | Two-factor authentication (TOTP + OTP) |
| 10 | Password hints |
| 11 | Folder management |
| 12 | Password generator + strength checker |
| 13 | Vault CRUD |
| 14 | User settings |
| 15 | Vault search & filter |
| 16 | Favorites + sensitive entries |
| 17 | Trash + restore |
| 18 | Password history (snapshots) |
| 19 | Audit logging |
| 20 | Login history |
| 21 | Security alerts |
| 22 | Security audit report (weak/reused/old passwords) |
| 22.5 | Access heatmap |
| 23 | Account deletion (scheduled) |
| 24 | Rate limiting + CAPTCHA |
| 25 | Vault export |
| 26 | Vault import (native + third-party) |
| 27 | Notifications |
| 28 | Categories |
| 29 | Duress mode |
| 30 | Read-only mode |
| 31 | Adaptive authentication |

---

*End of Analysis*
