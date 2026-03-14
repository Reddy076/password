# Rev-PasswordManager — Backend Features Overview

> **Complete reference of all implemented backend features**  
> Spring Boot 3.2.3 · Java 21 · MySQL · JWT · AES-256-GCM  
> **170 endpoints** · **23 controllers** · **1032 tests** (0 failures) · **100% controller coverage**

---

## Table of Contents

1. [Tech Stack & Architecture](#tech-stack--architecture)
2. [Security Architecture](#security-architecture)
3. [Feature List](#feature-list)
   - [Feature 1 — User Registration & Email Verification](#feature-1--user-registration--email-verification)
   - [Feature 2 — User Authentication (JWT)](#feature-2--user-authentication-jwt)
   - [Feature 3 — Master Password Encryption](#feature-3--master-password-encryption)
   - [Feature 4 — User Profile Management](#feature-4--user-profile-management)
   - [Feature 5 — Password Login & Token Refresh](#feature-5--password-login--token-refresh)
   - [Feature 6 — Session Management](#feature-6--session-management)
   - [Feature 7 — Password Recovery (Security Questions + OTP)](#feature-7--password-recovery-security-questions--otp)
   - [Feature 8 — Account Deletion (Soft Delete with Grace Period)](#feature-8--account-deletion-soft-delete-with-grace-period)
   - [Feature 9 — Two-Factor Authentication (2FA / TOTP / OTP)](#feature-9--two-factor-authentication-2fa--totp--otp)
   - [Feature 10 — User Settings](#feature-10--user-settings)
   - [Feature 11 — Folder Management](#feature-11--folder-management)
   - [Feature 12 — Password Generator & Strength Checker](#feature-12--password-generator--strength-checker)
   - [Feature 13 — Vault (Password Entries)](#feature-13--vault-password-entries)
   - [Feature 14 — Category Management](#feature-14--category-management)
   - [Feature 15 — Highly Sensitive Entries (Re-auth Gate)](#feature-15--highly-sensitive-entries-re-auth-gate)
   - [Feature 15.5 — Vault Search & Filter](#feature-155--vault-search--filter)
   - [Feature 16 — Favorites & Recently Used](#feature-16--favorites--recently-used)
   - [Feature 17 — Trash / Soft Delete Vault Entries](#feature-17--trash--soft-delete-vault-entries)
   - [Feature 18 — Vault Snapshots (Change History)](#feature-18--vault-snapshots-change-history)
   - [Feature 19 — Secure Password Sharing](#feature-19--secure-password-sharing)
   - [Feature 20 — Login History](#feature-20--login-history)
   - [Feature 21 — Audit Logs](#feature-21--audit-logs)
   - [Feature 22 — Security Alerts](#feature-22--security-alerts)
   - [Feature 22.5 — Activity Heatmap](#feature-225--activity-heatmap)
   - [Feature 23 — Account Lockout (Adaptive Auth)](#feature-23--account-lockout-adaptive-auth)
   - [Feature 24 — CAPTCHA & Rate Limiting](#feature-24--captcha--rate-limiting)
   - [Feature 25 — Backup Export (JSON/CSV)](#feature-25--backup-export-jsoncsv)
   - [Feature 26 — Password Vault Dashboard](#feature-26--password-vault-dashboard)
   - [Feature 27 — Third-Party Import](#feature-27--third-party-import-chromefirefoxlastpass1password)
   - [Feature 28 — Notifications](#feature-28--notifications)
   - [Feature 29 — Adaptive Authentication (Device Fingerprint + Geo)](#feature-29--adaptive-authentication-device-fingerprint--geo)
   - [Feature 30 — Health Check](#feature-30--health-check)
   - [Feature 31 — Duress Mode (Panic Password)](#feature-31--duress-mode-panic-password)
   - [Feature 32 — Breach Monitor (Dark Web Scan)](#feature-32--breach-monitor-dark-web-scan)
   - [Feature 33 — Security Audit Report](#feature-33--security-audit-report)
   - [Feature 34 — Weak / Reused / Old Password Detection](#feature-34--weak--reused--old-password-detection)
   - [Feature 35 — Vault Entry History (Per-Entry)](#feature-35--vault-entry-history-per-entry)
   - [Feature 36 — Smart Password Autofill (Browser Extension API)](#feature-36--smart-password-autofill-browser-extension-api)
   - [Feature 37 — Vault Timeline Visualization](#feature-37--vault-timeline-visualization)
   - [Feature 38 — Password Expiration Tracker](#feature-38--password-expiration-tracker)
   - [Feature 39 — Emergency Access (Digital Legacy)](#feature-39--emergency-access-digital-legacy)
   - [Feature 40 — Secure File Storage Vault](#feature-40--secure-file-storage-vault)
   - [Feature 41 — AI Password Assistant](#feature-41--ai-password-assistant)
   - [Feature 42 — Team/Family Vault Sharing (RBAC)](#feature-42--teamfamily-vault-sharing-rbac)
4. [Endpoint Summary Table](#endpoint-summary-table)
5. [Test Coverage Summary](#test-coverage-summary)
6. [Database Tables](#database-tables)

---

## Tech Stack & Architecture

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.3 |
| Security | Spring Security 6 + JWT (JJWT 0.12) |
| Database | MySQL 8 + Spring Data JPA / Hibernate |
| Encryption | AES-256-GCM (vault passwords), BCrypt (master password), PBKDF2WithHmacSHA256 (key derivation) |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI at `/swagger-ui.html`) |
| Testing | JUnit 5, Mockito, Spring Boot Test, `@WebMvcTest`, `@ExtendWith(MockitoExtension.class)` |
| Build | Maven 3 |
| Lombok | `@Data`, `@Builder`, `@Builder.Default`, `@RequiredArgsConstructor` |

**Architecture pattern:** Layered (Controller → Service → Repository → Entity)  
**Auth pattern:** Stateless JWT with refresh token rotation  
**Encryption pattern:** Per-user AES key derived from master password; vault passwords encrypted at rest

---

## Security Architecture

### Authentication Flow
1. User submits `username` + `masterPassword` (+ optional `captchaToken`)
2. Rate limiter checks IP (Feature 24)
3. CAPTCHA validated if provided (Feature 24)
4. Account lockout check (Feature 23)
5. BCrypt master password verification
6. Adaptive auth check — new device/location triggers additional verification (Feature 29)
7. If 2FA enabled → return `requires2FA: true`, user must call `/api/auth/verify-otp` (Feature 9)
8. If duress password → silent alert + decoy vault (Feature 31)
9. JWT access token (15 min) + refresh token (7 days) issued

### Vault Encryption
- Master password → PBKDF2WithHmacSHA256 → 256-bit AES key
- Each vault password encrypted with AES-256-GCM (unique IV per entry)
- Encrypted ciphertext stored in database; decrypted only in memory on request
- Highly sensitive entries require master password re-verification before decryption

### Token Security
- Access token: 15-minute TTL, signed with HS256
- Refresh token: 7-day TTL, stored in database for revocation
- All sessions tracked; users can revoke individual or all sessions

---

## Feature List

---

### Feature 1 — User Registration & Email Verification

**Controller:** [`AuthController`](../src/main/java/com/revature/passwordmanager/controller/AuthController.java)  
**Base path:** `/api/auth`

Registers a new user with email, username, master password (BCrypt-hashed), exactly **3 security questions**, and an optional password hint. Sends an email OTP for verification before the account can be used.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register new account |
| POST | `/api/auth/verify-email` | No | Verify email with OTP (`?username=&code=`) |
| POST | `/api/auth/resend-verification-otp` | No | Resend verification OTP (`?username=`) |

**Request — `POST /api/auth/register`:**
```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "masterPassword": "MyStr0ng!Pass",
  "passwordHint": "My favorite color + year",
  "securityQuestions": [
    { "questionText": "What was your first pet's name?", "answer": "Fluffy" },
    { "questionText": "What city were you born in?", "answer": "London" },
    { "questionText": "What is your mother's maiden name?", "answer": "Smith" }
  ]
}
```

**Response 201 — `UserResponse`:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "name": null,
  "phoneNumber": null,
  "is2faEnabled": false,
  "createdAt": "2026-01-01T10:00:00",
  "deletionScheduledAt": null
}
```

**Key classes:** `RegistrationRequest`, `UserResponse`, `RegistrationService`, `OtpService`

---

### Feature 2 — User Authentication (JWT)

**Controller:** [`AuthController`](../src/main/java/com/revature/passwordmanager/controller/AuthController.java)

Issues JWT access tokens (15-minute TTL) and refresh tokens (7-day TTL) on successful login.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/login` | No | Login with username + master password |
| POST | `/api/auth/refresh-token` | No | Refresh expired access token |
| POST | `/api/auth/logout` | Yes | Invalidate token |
| GET | `/api/auth/validate-token` | Yes | Check if JWT is valid |

**Request — `POST /api/auth/login`:**
```json
{
  "username": "johndoe",
  "masterPassword": "MyStr0ng!Pass",
  "captchaToken": "optional-recaptcha-token"
}
```

**Response 200 — `AuthResponse`:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "johndoe",
  "expiresIn": 900000,
  "requires2FA": false,
  "message": null
}
```

**Key classes:** `AuthResponse`, `LoginRequest`, `JwtTokenProvider`, `AuthenticationService`

---

### Feature 3 — Master Password Encryption

**Service layer only** (no dedicated controller)

All vault passwords are encrypted with AES-256-GCM using a key derived from the user's master password via PBKDF2WithHmacSHA256. The master password itself is stored as a BCrypt hash and never in plaintext.

**Key classes:** `EncryptionService`, `EncryptionUtil`, `MasterPasswordValidator`

---

### Feature 4 — User Profile Management

**Controller:** [`UserController`](../src/main/java/com/revature/passwordmanager/controller/UserController.java)  
**Base path:** `/api/users`

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/users/profile` | Yes | Get current user profile |
| PUT | `/api/users/profile` | Yes | Update name / phone number |
| PUT | `/api/users/change-password` | Yes | Change master password |
| GET | `/api/users/security-questions` | Yes | Get security question prompts |
| PUT | `/api/users/security-questions` | Yes | Update security questions |
| PUT | `/api/users/read-only-mode` | Yes | Toggle read-only mode |
| GET | `/api/users/dashboard` | Yes | Get user dashboard summary |

**Response — `UserResponse`:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "name": "John Doe",
  "phoneNumber": "+1234567890",
  "is2faEnabled": false,
  "createdAt": "2026-01-01T10:00:00",
  "deletionScheduledAt": null
}
```

**Key classes:** `UserResponse`, `UpdateProfileRequest`, `ChangePasswordRequest`, `UpdateSecurityQuestionsRequest`

---

### Feature 5 — Password Login & Token Refresh

Covered under Feature 2. The login flow also handles:
- CAPTCHA token validation (Feature 24)
- 2FA challenge routing (Feature 9)
- Duress password detection (Feature 31)
- Adaptive auth (Feature 29)

---

### Feature 6 — Session Management

**Controller:** [`SessionController`](../src/main/java/com/revature/passwordmanager/controller/SessionController.java)  
**Base path:** `/api/sessions`

Tracks active user sessions with IP address, device info, location, and expiry. Users can view all sessions and revoke individual or all sessions.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/sessions` | Yes | List all active sessions |
| GET | `/api/sessions/current` | Yes | Get current session |
| POST | `/api/sessions/extend` | Yes | Extend current session |
| DELETE | `/api/sessions/{sessionId}` | Yes | Revoke a specific session |
| DELETE | `/api/sessions` | Yes | Revoke all sessions |

**Response — `SessionResponse`:**
```json
{
  "id": 1,
  "ipAddress": "192.168.1.1",
  "deviceInfo": "Chrome on Windows",
  "location": "London, UK",
  "isActive": true,
  "createdAt": "2026-01-15T10:00:00",
  "lastAccessedAt": "2026-01-15T12:00:00",
  "expiresAt": "2026-01-22T10:00:00"
}
```

**Key classes:** `SessionResponse`, `SessionService`, `UserSession`

---

### Feature 7 — Password Recovery (Security Questions + OTP)

**Controller:** [`AuthController`](../src/main/java/com/revature/passwordmanager/controller/AuthController.java)

Two recovery paths: (1) answer 3 security questions, (2) OTP sent to registered email.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/auth/security-questions/{username}` | No | Get security question prompts |
| POST | `/api/auth/verify-security-questions` | No | Verify answers |
| POST | `/api/auth/forgot-password` | No | Request OTP reset email |
| POST | `/api/auth/reset-password` | No | Reset password with OTP |

**Key classes:** `ForgotPasswordRequest`, `RecoveryRequest`, `VerifySecurityQuestionsRequest`, `SecurityQuestionService`

---

### Feature 8 — Account Deletion (Soft Delete with Grace Period)

**Controller:** [`UserController`](../src/main/java/com/revature/passwordmanager/controller/UserController.java)

Schedules account deletion with a 30-day grace period. Users can cancel deletion within the grace period. The `deletionScheduledAt` field is returned in `UserResponse`.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| DELETE | `/api/users/account` | Yes | Schedule account deletion |
| POST | `/api/users/account/cancel-deletion` | Yes | Cancel scheduled deletion |

**Key classes:** `AccountDeletionRequest`, `UserResponse.deletionScheduledAt`

---

### Feature 9 — Two-Factor Authentication (2FA / TOTP / OTP)

**Controller:** [`TwoFactorController`](../src/main/java/com/revature/passwordmanager/controller/TwoFactorController.java)  
**Base path:** `/api/2fa`

Supports both TOTP (Google Authenticator / Authy) and email OTP. Users can enable/disable 2FA, generate backup codes, and verify setup.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/2fa/status` | Yes | Check if 2FA is enabled |
| POST | `/api/2fa/setup` | Yes | Begin TOTP setup (returns QR code URI) |
| POST | `/api/2fa/verify-setup` | Yes | Confirm TOTP setup with code |
| POST | `/api/2fa/disable` | Yes | Disable 2FA |
| GET | `/api/2fa/backup-codes` | Yes | Get backup codes |
| POST | `/api/2fa/regenerate-codes` | Yes | Regenerate backup codes |
| POST | `/api/auth/verify-otp` | No | Verify OTP during login (`?username=&code=`) |
| POST | `/api/auth/send-otp` | No | Send OTP to email (`?username=`) |
| POST | `/api/auth/resend-otp` | No | Resend OTP (`?username=`) |

**Response — `TwoFactorSetupResponse`:**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeUri": "otpauth://totp/RevPasswordManager:johndoe?secret=...",
  "backupCodes": ["abc123", "def456", "..."]
}
```

**Key classes:** `TwoFactorSetupResponse`, `TwoFactorStatusResponse`, `RegenerateCodesResponse`, `TOTPUtil`, `OtpService`, `TwoFactorService`

---

### Feature 10 — User Settings

**Controller:** [`UserSettingsController`](../src/main/java/com/revature/passwordmanager/controller/UserSettingsController.java)  
**Base path:** `/api/settings`

Stores per-user preferences: UI theme, language, auto-logout timeout, and read-only mode.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/settings` | Yes | Get current settings |
| PUT | `/api/settings` | Yes | Update settings |

**Request/Response — `UserSettingsRequest` / `UserSettingsResponse`:**
```json
{
  "theme": "dark",
  "language": "en",
  "autoLogoutMinutes": 15,
  "readOnlyMode": false
}
```

**Key classes:** `UserSettingsRequest`, `UserSettingsResponse`, `UserSettingsService`

---

### Feature 11 — Folder Management

**Controller:** [`FolderController`](../src/main/java/com/revature/passwordmanager/controller/FolderController.java)  
**Base path:** `/api/folders`

Hierarchical folder structure for organizing vault entries. Supports nested folders (parent/child), renaming, and moving.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/folders` | Yes | List all folders (with subfolders) |
| GET | `/api/folders/{id}` | Yes | Get specific folder |
| POST | `/api/folders` | Yes | Create folder (`?name=&parentFolderId=`) |
| PUT | `/api/folders/{id}` | Yes | Rename folder (`?name=`) |
| PUT | `/api/folders/{id}/move` | Yes | Move folder to new parent (`?parentFolderId=`) |
| DELETE | `/api/folders/{id}` | Yes | Delete folder |
| GET | `/api/folders/{id}/entries` | Yes | Get entries in folder |

**Response — `FolderDTO`:**
```json
{
  "id": 1,
  "name": "Dev",
  "parentFolderId": null,
  "subfolders": [],
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-01-01T10:00:00"
}
```

**Key classes:** `FolderDTO`, `FolderService`, `Folder`

---

### Feature 12 — Password Generator & Strength Checker

**Controller:** [`PasswordGeneratorController`](../src/main/java/com/revature/passwordmanager/controller/PasswordGeneratorController.java)  
**Base path:** `/api/generator`

Generates cryptographically secure passwords with configurable options. Also checks password strength and provides actionable feedback.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/generator/generate` | No | Generate a single password |
| POST | `/api/generator/generate-multiple` | No | Generate multiple passwords |
| POST | `/api/generator/strength` | No | Check password strength |
| POST | `/api/generator/validate` | No | Validate a password |
| GET | `/api/generator/default-settings` | No | Get default generator settings |

**Request — `PasswordGeneratorRequest`:**
```json
{
  "length": 16,
  "includeUppercase": true,
  "includeLowercase": true,
  "includeNumbers": true,
  "includeSpecial": true,
  "excludeSimilar": false,
  "excludeAmbiguous": false,
  "count": 1
}
```

**Response — `PasswordStrengthResponse`:**
```json
{
  "score": 85,
  "label": "Strong",
  "feedback": ["Consider adding more special characters"]
}
```

**Strength labels:** `Very Weak` (0-20) · `Weak` (21-40) · `Fair` (41-60) · `Good` (61-80) · `Strong` (81-100)

**Key classes:** `PasswordGeneratorRequest`, `PasswordStrengthResponse`, `GeneratedPasswordResponse`, `GeneratedMultiplePasswordsResponse`, `PasswordStrengthCalculator`

---

### Feature 13 — Vault (Password Entries)

**Controller:** [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java)  
**Base path:** `/api/vault`

Core vault CRUD. Each entry stores title, username, password (AES-256-GCM encrypted), website URL, notes, category, folder, favorite flag, and highly-sensitive flag. Password strength is computed on save.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/vault` | Yes | Create vault entry |
| GET | `/api/vault` | Yes | List all entries |
| GET | `/api/vault/{id}` | Yes | Get entry detail (with decrypted password) |
| PUT | `/api/vault/{id}` | Yes | Update entry |
| DELETE | `/api/vault/{id}` | Yes | Soft-delete entry (to trash) |
| GET | `/api/vault/search` | Yes | Search with filters |
| GET | `/api/vault/filter` | Yes | Filter by category/folder/favorite |
| GET | `/api/vault/recent` | Yes | Recently created entries |
| GET | `/api/vault/recently-used` | Yes | Recently accessed entries |
| GET | `/api/vault/favorites` | Yes | Favorite entries |
| POST | `/api/vault/entries/bulk-delete` | Yes | Bulk delete entries |

**Request — `VaultEntryRequest`:**
```json
{
  "title": "GitHub",
  "username": "johndoe",
  "password": "MyGitHubPass!",
  "websiteUrl": "https://github.com",
  "notes": "Work account",
  "categoryId": 1,
  "folderId": 2,
  "isFavorite": false,
  "isHighlySensitive": false
}
```

**Response — `VaultEntryDetailResponse`:**
```json
{
  "id": 10,
  "title": "GitHub",
  "username": "johndoe",
  "password": "MyGitHubPass!",
  "websiteUrl": "https://github.com",
  "notes": "Work account",
  "categoryId": 1,
  "categoryName": "Work",
  "folderId": 2,
  "folderName": "Dev",
  "isFavorite": false,
  "isHighlySensitive": false,
  "requiresSensitiveAuth": false,
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-01-01T10:00:00",
  "strengthScore": 85,
  "strengthLabel": "Strong"
}
```

> If `isHighlySensitive: true`, password/username/notes are returned as `"******"` and `requiresSensitiveAuth: true`.

**Key classes:** `VaultEntryRequest`, `VaultEntryResponse`, `VaultEntryDetailResponse`, `VaultService`, `VaultEntry`

---

### Feature 14 — Category Management

**Controller:** [`CategoryController`](../src/main/java/com/revature/passwordmanager/controller/CategoryController.java)  
**Base path:** `/api/categories`

User-defined categories for organizing vault entries (e.g., Work, Personal, Finance). Includes default system categories.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/categories` | Yes | List all categories |
| GET | `/api/categories/{id}` | Yes | Get specific category |
| POST | `/api/categories` | Yes | Create category |
| PUT | `/api/categories/{id}` | Yes | Update category |
| DELETE | `/api/categories/{id}` | Yes | Delete category |
| GET | `/api/categories/{id}/entries` | Yes | Get entries in category |

**Response — `CategoryDTO`:**
```json
{
  "id": 1,
  "name": "Work",
  "icon": "briefcase",
  "isDefault": false,
  "createdAt": "2026-01-01T10:00:00",
  "entryCount": 5
}
```

**Key classes:** `CategoryDTO`, `CreateCategoryRequest`, `CategoryService`

---

### Feature 15 — Highly Sensitive Entries (Re-auth Gate)

**Controller:** [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java)

Entries marked `isHighlySensitive: true` return `"******"` for password/username/notes unless the user re-authenticates with their master password. The `requiresSensitiveAuth` flag in the response tells the frontend whether to prompt for re-auth.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/vault/entries/{id}/view-password` | Yes | View password after master password verification |
| PUT | `/api/vault/entries/{id}/sensitive` | Yes | Toggle highly-sensitive flag |
| POST | `/api/vault/{id}/sensitive-view` | Yes | View sensitive entry with master password |

**Key classes:** `SensitiveAccessRequest`, `ViewPasswordResponse`, `VerifyMasterPasswordRequest`

---

### Feature 15.5 — Vault Search & Filter

**Controller:** [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java)

Advanced search with keyword matching (title + URL), category/folder filtering, favorite filtering, highly-sensitive filtering, and sort options.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/vault/search` | Yes | Search with keyword, categoryId, folderId, isFavorite, isHighlySensitive, sortBy, sortDir |
| GET | `/api/vault/filter` | Yes | Filter by category, folder, or favorite |

**Query params for `/api/vault/search`:**
- `keyword` — search in title and URL
- `categoryId` — filter by category
- `folderId` — filter by folder
- `isFavorite` — boolean filter
- `isHighlySensitive` — boolean filter
- `sortBy` — `title`, `createdAt`, `updatedAt` (default: `title`)
- `sortDir` — `asc`, `desc` (default: `asc`)

---

### Feature 16 — Favorites & Recently Used

**Controller:** [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java)

Toggle favorite status on vault entries. Track last-accessed timestamps for "recently used" list.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| PUT | `/api/vault/{id}/favorite` | Yes | Toggle favorite |
| GET | `/api/vault/favorites` | Yes | Get all favorites |
| GET | `/api/vault/recently-used` | Yes | Get recently accessed entries |

---

### Feature 17 — Trash / Soft Delete Vault Entries

**Controller:** [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java)

Deleted entries go to trash instead of being permanently removed. Trash is auto-purged after 30 days by a scheduled job (`@Scheduled`).

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| DELETE | `/api/vault/{id}` | Yes | Move entry to trash |
| GET | `/api/vault/trash` | Yes | List trashed entries |
| GET | `/api/vault/trash/count` | Yes | Count trashed entries |

**Response — `TrashEntryResponse`:**
```json
{
  "id": 10,
  "title": "GitHub",
  "username": "johndoe",
  "websiteUrl": "https://github.com",
  "deletedAt": "2026-01-15T10:00:00",
  "purgeAt": "2026-02-14T10:00:00"
}
```

**Key classes:** `TrashEntryResponse`, `TrashCountResponse`, `TrashCleanupScheduler`, `VaultTrashService`

---

### Feature 18 — Vault Snapshots (Change History)

**Controller:** [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java)

Automatically creates a snapshot of a vault entry's previous state before each update. Users can view the full change history of any entry.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/vault/entries/{id}/history` | Yes | Get change history for an entry |
| GET | `/api/backup/snapshots` | Yes | List vault snapshots |
| POST | `/api/backup/snapshots/{id}/restore` | Yes | Restore from snapshot |

**Key classes:** `SnapshotResponse`, `VaultSnapshot`, `VaultSnapshotService`

---

### Feature 19 — Secure Password Sharing

**Controller:** [`SecureShareController`](../src/main/java/com/revature/passwordmanager/controller/SecureShareController.java)  
**Base path:** `/api/shares`

Creates time-limited, encrypted share links for vault entries. Recipients access the shared password via a one-time token without needing an account.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/shares` | Yes | Create a share link |
| GET | `/api/shares/{token}` | No | Access shared password (public) |
| GET | `/api/shares` | Yes | List my active shares |
| DELETE | `/api/shares/{id}` | Yes | Revoke a share |
| GET | `/api/shares/received` | Yes | View passwords shared with me |

**Request — `CreateShareRequest`:**
```json
{
  "vaultEntryId": 10,
  "expiresInHours": 24,
  "maxViews": 1,
  "password": "optional-share-password"
}
```

**Response — `ShareLinkResponse`:**
```json
{
  "id": 1,
  "token": "abc123xyz",
  "shareUrl": "https://app.example.com/share/abc123xyz",
  "expiresAt": "2026-01-16T10:00:00",
  "maxViews": 1,
  "viewCount": 0
}
```

**Key classes:** `CreateShareRequest`, `ShareLinkResponse`, `SharedPasswordResponse`, `ReceiveShareRequest`

---

### Feature 20 — Login History

**Controller:** [`SecurityController`](../src/main/java/com/revature/passwordmanager/controller/SecurityController.java)  
**Base path:** `/api/security`

Records every login attempt (successful and failed) with IP address, device info, location, and timestamp.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/security/login-history` | Yes | Get login history |

**Response — `LoginHistoryResponse`:**
```json
{
  "id": 1,
  "ipAddress": "192.168.1.1",
  "deviceInfo": "Chrome on Windows",
  "location": "London, UK",
  "successful": true,
  "failureReason": null,
  "timestamp": "2026-01-15T10:00:00"
}
```

**Key classes:** `LoginHistoryResponse`, `LoginAttemptService`, `LoginAttempt`

---

### Feature 21 — Audit Logs

**Controller:** [`SecurityController`](../src/main/java/com/revature/passwordmanager/controller/SecurityController.java)

Records all significant user actions (vault CRUD, settings changes, 2FA changes, etc.) with action type, details, IP address, and timestamp. Uses AOP (`@Around` advice) for automatic logging.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/security/audit-logs` | Yes | Get audit log entries |
| GET | `/api/security/audit-report` | Yes | Get full security audit report |

**Response — `AuditLogResponse`:**
```json
{
  "id": 1,
  "action": "VAULT_ENTRY_CREATED",
  "details": "Created entry: GitHub",
  "ipAddress": "192.168.1.1",
  "timestamp": "2026-01-15T10:00:00"
}
```

**Key classes:** `AuditLogResponse`, `AuditLogService`, `AuditLog`, `AuditLogAspect`

---

### Feature 22 — Security Alerts

**Controller:** [`SecurityController`](../src/main/java/com/revature/passwordmanager/controller/SecurityController.java)

Generates alerts for suspicious activity: new device logins, multiple failed attempts, password breaches, etc.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/security/alerts` | Yes | Get all security alerts |
| PUT | `/api/security/alerts/{id}/read` | Yes | Mark alert as read |
| DELETE | `/api/security/alerts/{id}` | Yes | Dismiss alert |

**Response — `SecurityAlertDTO`:**
```json
{
  "id": 1,
  "alertType": "NEW_DEVICE_LOGIN",
  "title": "New Device Detected",
  "message": "Login from Chrome on Windows",
  "severity": "MEDIUM",
  "isRead": false,
  "createdAt": "2026-01-15T10:00:00"
}
```

**Alert types:** `NEW_DEVICE_LOGIN`, `MULTIPLE_FAILED_ATTEMPTS`, `PASSWORD_BREACH`, `SUSPICIOUS_LOCATION`, `ACCOUNT_LOCKED`  
**Severity levels:** `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

**Key classes:** `SecurityAlertDTO`, `SecurityAlertService`, `SecurityAlert`

---

### Feature 22.5 — Activity Heatmap

**Controller:** [`UserController`](../src/main/java/com/revature/passwordmanager/controller/UserController.java)

Generates a GitHub-style activity heatmap showing vault access frequency by day over the past year.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/users/activity-heatmap` | Yes | Get activity heatmap data |

**Key classes:** `HeatmapResponse`, `AccessHeatmapService`

---

### Feature 23 — Account Lockout (Adaptive Auth)

**Service layer** (integrated into `AuthenticationService`)

Automatically locks accounts after configurable failed login attempts. Tracks login attempts per IP and per username. Integrates with adaptive authentication (Feature 29).

**Key classes:** `LoginAttemptService`, `LoginAttempt`, `AdaptiveAuthService`

---

### Feature 24 — CAPTCHA & Rate Limiting

**Filter/Config layer** (integrated into `AuthController`)

Validates Google reCAPTCHA tokens on login. Rate-limits login attempts per IP address using a sliding window algorithm. Returns HTTP 429 when rate limit exceeded.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/verify-captcha` | No | Verify CAPTCHA token |

**Key classes:** `CaptchaService`, `RateLimitService`, `RateLimitFilter`, `RateLimitConfig`, `RateLimitExceededException`

---

### Feature 25 — Backup Export (JSON/CSV)

**Controller:** [`BackupController`](../src/main/java/com/revature/passwordmanager/controller/BackupController.java)  
**Base path:** `/api/backup`

Exports the entire vault to JSON or CSV format, optionally encrypted with the master password. Supports preview before export and import validation.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/backup/export` | Yes | Export vault (`?format=JSON&encrypt=true`) |
| POST | `/api/backup/import` | Yes | Import vault from file |
| GET | `/api/backup/export/preview` | Yes | Preview export without downloading |
| POST | `/api/backup/import/validate` | Yes | Validate import file |
| GET | `/api/backup/snapshots` | Yes | List vault snapshots |
| POST | `/api/backup/snapshots/{id}/restore` | Yes | Restore from snapshot |

**Response — `ExportResponse`:**
```json
{
  "fileName": "vault-export-2026-01-15.json",
  "format": "JSON",
  "entryCount": 25,
  "encrypted": false,
  "data": "base64-encoded-content",
  "exportedAt": "2026-01-15T10:00:00"
}
```

**Response — `ImportResult`:**
```json
{
  "totalProcessed": 25,
  "successCount": 24,
  "failCount": 1,
  "message": "Import completed with 1 error"
}
```

**Key classes:** `ExportResponse`, `ImportResult`, `ExportService`, `ImportRequest`

---

### Feature 26 — Password Vault Dashboard

**Controller:** [`DashboardController`](../src/main/java/com/revature/passwordmanager/controller/DashboardController.java)  
**Base path:** `/api/dashboard`

Provides security metrics and analytics: overall security score, password health breakdown, reused passwords, password age distribution, and security trends over time.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/dashboard/security-score` | Yes | Overall security score (0-100) |
| GET | `/api/dashboard/password-health` | Yes | Password health metrics |
| GET | `/api/dashboard/reused-passwords` | Yes | List reused passwords |
| GET | `/api/dashboard/password-age` | Yes | Password age distribution |
| GET | `/api/dashboard/trends` | Yes | Security trends over time |

**Key classes:** `SecurityScoreResponse`, `PasswordHealthMetricsResponse`, `ReusedPasswordResponse`, `PasswordAgeResponse`, `SecurityTrendResponse`

---

### Feature 27 — Third-Party Import (Chrome/Firefox/LastPass/1Password)

**Controller:** [`BackupController`](../src/main/java/com/revature/passwordmanager/controller/BackupController.java)

Imports passwords from browser exports and popular password managers. Supports Chrome CSV, Firefox CSV, LastPass CSV, and 1Password 1PUX format.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/backup/import-external` | Yes | Import from third-party format |
| GET | `/api/backup/import-external/formats` | Yes | List supported import formats |

**Supported formats:** `CHROME`, `FIREFOX`, `LASTPASS`, `1PASSWORD`

**Key classes:** `ThirdPartyImportRequest`, `ImporterFactory`, `ChromeImporter`, `FirefoxImporter`, `LastPassImporter`, `OnePasswordImporter`

---

### Feature 28 — Notifications

**Controller:** [`NotificationController`](../src/main/java/com/revature/passwordmanager/controller/NotificationController.java)  
**Base path:** `/api/notifications`

In-app notification system for password expiry warnings, security alerts, breach detections, and system messages.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/notifications` | Yes | Get all notifications |
| GET | `/api/notifications/unread-count` | Yes | Get unread count |
| PUT | `/api/notifications/{id}/read` | Yes | Mark as read |
| PUT | `/api/notifications/mark-all-read` | Yes | Mark all as read |
| DELETE | `/api/notifications/{id}` | Yes | Delete notification |

**Response — `NotificationDTO`:**
```json
{
  "id": 1,
  "notificationType": "PASSWORD_EXPIRY",
  "title": "Password Expiring Soon: GitHub",
  "message": "Your GitHub password will expire in 5 days.",
  "isRead": false,
  "createdAt": "2026-01-15T10:00:00"
}
```

**Notification types:** `PASSWORD_EXPIRY`, `SECURITY_ALERT`, `BREACH_DETECTED`, `SYSTEM_MESSAGE`, `EMERGENCY_ACCESS_REQUEST`

**Key classes:** `NotificationDTO`, `UnreadCountResponse`, `NotificationService`, `Notification`

---

### Feature 29 — Adaptive Authentication (Device Fingerprint + Geo)

**Service layer** (integrated into `AuthenticationService`)

Detects logins from new devices or unusual locations and triggers additional verification. Uses device fingerprinting (User-Agent + IP hash) and geo-location lookup.

**Key classes:** `AdaptiveAuthService`, `DeviceFingerprintUtil`, `GeoLocationService`, `DateTimeUtil`

---

### Feature 30 — Health Check

**Controller:** [`HealthController`](../src/main/java/com/revature/passwordmanager/controller/HealthController.java)  
**Base path:** `/api/health`

Provides application health status for monitoring and load balancers. Checks database connectivity and service availability. **No authentication required.**

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/health` | No | Overall health status |
| GET | `/api/health/db` | No | Database connectivity check |
| GET | `/api/health/services` | No | Individual service health |

**Response — `HealthResponse`:**
```json
{
  "status": "UP",
  "database": "UP",
  "timestamp": "2026-01-15T10:00:00"
}
```

**Key classes:** `HealthResponse`, `HealthService`

---

### Feature 31 — Duress Mode (Panic Password)

**Controller:** [`AuthController`](../src/main/java/com/revature/passwordmanager/controller/AuthController.java)

Users can set a secondary "duress password." When used to log in, the vault appears normal but silently alerts the user's emergency contacts and optionally shows a decoy vault.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/duress-login` | No | Login with duress password |
| POST | `/api/auth/set-duress-password` | Yes | Set/update duress password |

**Key classes:** `DuressService`, `SetDuressPasswordRequest`

---

### Feature 32 — Breach Monitor (Dark Web Scan)

**Controller:** [`BreachMonitorController`](../src/main/java/com/revature/passwordmanager/controller/BreachMonitorController.java)  
**Base path:** `/api/security`

Checks vault passwords against known breach databases (HaveIBeenPwned API via k-anonymity SHA-1 prefix). Tracks breach history and allows resolving compromised credentials.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/security/breach-scan` | Yes | Scan all vault passwords for breaches |
| GET | `/api/security/breach-status` | Yes | Get current breach status |
| GET | `/api/security/compromised-credentials` | Yes | List compromised credentials |
| GET | `/api/security/breach-history` | Yes | Get breach scan history |
| PUT | `/api/security/compromised-credentials/{id}/resolve` | Yes | Mark as resolved |

**Response — `BreachScanResponse`:**
```json
{
  "scannedAt": "2026-01-15T10:00:00",
  "totalScanned": 25,
  "compromisedCount": 2,
  "compromisedEntries": [
    {
      "entryId": 10,
      "title": "GitHub",
      "breachCount": 1500000
    }
  ]
}
```

**Key classes:** `BreachScanResponse`, `BreachStatusResponse`, `CompromisedCredentialResponse`, `BreachHistoryResponse`

---

### Feature 33 — Security Audit Report

**Controller:** [`SecurityController`](../src/main/java/com/revature/passwordmanager/controller/SecurityController.java)

Generates a comprehensive security audit report combining audit logs, login history, alerts, and vault security metrics.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/security/audit-report` | Yes | Full security audit report |
| POST | `/api/security/analyze-vault` | Yes | Trigger vault security analysis |

**Key classes:** `SecurityAuditResponse`, `SecurityAuditService`

---

### Feature 34 — Weak / Reused / Old Password Detection

**Controller:** [`SecurityController`](../src/main/java/com/revature/passwordmanager/controller/SecurityController.java)

Identifies security risks in the vault: weak passwords (low strength score), reused passwords (same password across multiple entries), and old passwords (not changed in 90+ days).

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/security/weak-passwords` | Yes | List weak passwords |
| GET | `/api/security/reused-passwords` | Yes | List reused passwords |
| GET | `/api/security/old-passwords` | Yes | List old passwords |

---

### Feature 35 — Vault Entry History (Per-Entry)

**Controller:** [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java)

Tracks the full change history of each vault entry, including what changed and when. Snapshots are created automatically before each update.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/vault/entries/{id}/history` | Yes | Get change history for entry |

**Key classes:** `SnapshotResponse`, `VaultSnapshot`, `VaultSnapshotService`

---

### Feature 36 — Smart Password Autofill (Browser Extension API)

**Controller:** [`AutofillController`](../src/main/java/com/revature/passwordmanager/controller/AutofillController.java)  
**Base path:** `/api/autofill`

Backend API for browser extensions. Matches vault entries to the current page URL using exact, subdomain, and partial domain matching. **Passwords are never returned** — only metadata. The extension fetches the actual password separately after user confirmation.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/autofill/suggestions` | Yes | Get matching vault entries for a URL |
| GET | `/api/autofill/trusted-domains` | Yes | Get previously used domains |
| POST | `/api/autofill/log-usage` | Yes | Record autofill usage |

**Request — `AutofillSuggestionRequest`:**
```json
{ "url": "https://github.com/login" }
```

**Response — `AutofillSuggestionResponse`:**
```json
{
  "domain": "github.com",
  "totalCount": 2,
  "suggestions": [
    {
      "entryId": 10,
      "title": "GitHub",
      "username": "johndoe",
      "websiteUrl": "https://github.com",
      "matchType": "EXACT",
      "isFavorite": true
    }
  ]
}
```

**Match types:** `EXACT` > `SUBDOMAIN` > `PARTIAL` (sorted by relevance)

**Key classes:** `AutofillSuggestionRequest`, `AutofillSuggestionResponse`, `AutofillUsageRequest`, `DomainMatchingService`, `AutofillService`, `AutofillUsageLog`

---

### Feature 37 — Vault Timeline Visualization

**Controller:** [`VaultTimelineController`](../src/main/java/com/revature/passwordmanager/controller/VaultTimelineController.java)  
**Base path:** `/api/timeline`

Provides a chronological activity feed of all vault events (creates, updates, deletes, views) for visualization as a timeline or calendar.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/timeline` | Yes | Get full timeline |
| GET | `/api/timeline/summary` | Yes | Get timeline summary |
| GET | `/api/timeline/entry/{entryId}` | Yes | Get timeline for specific entry |
| GET | `/api/timeline/stats` | Yes | Get timeline statistics |

**Key classes:** `TimelineEventDTO`, `TimelineSummaryResponse`, `TimelineStatsResponse`, `VaultTimelineResponse`

---

### Feature 38 — Password Expiration Tracker

**Controller:** [`PasswordExpiryController`](../src/main/java/com/revature/passwordmanager/controller/PasswordExpiryController.java)  
**Base path:** `/api/expiry`

Tracks password age and sends reminders when passwords are approaching expiry. Configurable expiry policy per user. Supports snoozing reminders.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/expiry/status` | Yes | Get expiry status for all entries |
| GET | `/api/expiry/expiring-soon` | Yes | Get entries expiring within N days (`?days=7`) |
| GET | `/api/expiry/policy` | Yes | Get expiry policy |
| PUT | `/api/expiry/policy` | Yes | Update expiry policy |
| POST | `/api/expiry/snooze/{entryId}` | Yes | Snooze reminder (`?days=7`) |

**Response — `ExpiryStatusResponse`:**
```json
{
  "totalEntries": 25,
  "freshCount": 15,
  "agingCount": 5,
  "expiringSoonCount": 3,
  "expiredCount": 2,
  "entries": [
    {
      "entryId": 10,
      "title": "GitHub",
      "username": "johndoe",
      "websiteUrl": "https://github.com",
      "lastChangedAt": "2025-10-01T10:00:00",
      "expiresAt": "2026-01-01T10:00:00",
      "status": "EXPIRING_SOON",
      "daysUntilExpiry": 5,
      "reminderSent": false,
      "snoozed": false,
      "snoozedUntil": null
    }
  ]
}
```

**Status values:** `FRESH` (< 50% of expiry period) → `AGING` (50-80%) → `EXPIRING_SOON` (80-100%) → `EXPIRED`

**Key classes:** `ExpiryStatusResponse`, `ExpiryPolicyResponse`, `ExpiryPolicyRequest`

---

### Feature 39 — Emergency Access (Digital Legacy)

**Controller:** [`EmergencyAccessController`](../src/main/java/com/revature/passwordmanager/controller/EmergencyAccessController.java)  
**Base path:** `/api/emergency`

Allows users to designate trusted emergency contacts who can request access to the vault after a configurable waiting period (default 48 hours). The vault owner can approve or deny requests during the waiting period.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/emergency/contacts` | Yes | Add emergency contact |
| GET | `/api/emergency/contacts` | Yes | List emergency contacts |
| PUT | `/api/emergency/contacts/{contactId}` | Yes | Update contact |
| DELETE | `/api/emergency/contacts/{contactId}` | Yes | Remove contact |
| POST | `/api/emergency/request-access` | Yes | Request emergency access |
| GET | `/api/emergency/my-requests` | Yes | View my access requests |
| GET | `/api/emergency/requests` | Yes | View requests for my vault |
| POST | `/api/emergency/grant/{requestId}` | Yes | Approve access request |
| POST | `/api/emergency/deny/{requestId}` | Yes | Deny access request |
| GET | `/api/emergency/vault/{token}` | No | Access vault with emergency token |

**Response — `EmergencyContactResponse`:**
```json
{
  "id": 1,
  "contactEmail": "spouse@example.com",
  "contactName": "Jane Doe",
  "relationship": "Spouse",
  "waitingPeriodHours": 48,
  "verified": false,
  "active": true,
  "createdAt": "2026-01-01T10:00:00"
}
```

**Request status flow:** `PENDING` → `APPROVED` / `DENIED` / `EXPIRED`

**Key classes:** `EmergencyContactResponse`, `EmergencyAccessRequestResponse`, `EmergencyVaultResponse`, `AddEmergencyContactRequest`

---

### Feature 40 — Secure File Storage Vault

**Controller:** [`SecureFileController`](../src/main/java/com/revature/passwordmanager/controller/SecureFileController.java)  
**Base path:** `/api/files`

Encrypted file storage alongside passwords. Files are encrypted with AES-256-GCM before storage. Supports folder organization, search, and download.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/files/upload` | Yes | Upload encrypted file (multipart/form-data) |
| GET | `/api/files` | Yes | List files and folders |
| GET | `/api/files/search` | Yes | Search files (`?keyword=`) |
| GET | `/api/files/{id}/download` | Yes | Download decrypted file |
| DELETE | `/api/files/{id}` | Yes | Delete file |
| POST | `/api/files/folders` | Yes | Create file folder |
| DELETE | `/api/files/folders/{folderId}` | Yes | Delete file folder |

**Response — `SecureFileListResponse`:**
```json
{
  "folders": [
    { "id": 1, "name": "Documents", "parentId": null, "createdAt": "2026-01-01T10:00:00" }
  ],
  "files": [
    {
      "id": 1,
      "originalFilename": "passport.pdf",
      "fileSize": 524288,
      "mimeType": "application/pdf",
      "folderId": 1,
      "folderName": "Documents",
      "uploadedAt": "2026-01-15T10:00:00",
      "lastAccessedAt": "2026-01-15T12:00:00"
    }
  ],
  "totalFiles": 5,
  "totalStorageBytes": 2097152
}
```

**Key classes:** `SecureFileListResponse`, `FileUploadResponse`

---

### Feature 41 — AI Password Assistant

**Controller:** [`AIAssistantController`](../src/main/java/com/revature/passwordmanager/controller/AIAssistantController.java)  
**Base path:** `/api/ai`

Rule-based AI assistant (with optional LLM integration) that answers security questions, analyzes vault health, generates passwords on request, and provides personalized security recommendations. Maintains conversation history per session.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/ai/chat` | Yes | Send message to AI assistant |
| GET | `/api/ai/suggestions` | Yes | Get proactive security suggestions |
| POST | `/api/ai/generate-password` | Yes | AI-assisted password generation |
| GET | `/api/ai/security-insights` | Yes | Get AI security insights |
| DELETE | `/api/ai/session` | Yes | Clear conversation history |

**Request — `ChatRequest`:**
```json
{ "message": "How secure is my vault?" }
```

**Response — `ChatResponse`:**
```json
{
  "reply": "Based on your vault, you have 5 passwords older than 90 days...",
  "intent": "SECURITY_ANALYSIS",
  "generatedPassword": null,
  "suggestions": ["Update old passwords", "Enable 2FA"],
  "timestamp": "2026-01-15T10:00:00",
  "aiPowered": false,
  "history": [
    { "role": "user", "content": "How secure is my vault?" },
    { "role": "assistant", "content": "Based on your vault..." }
  ]
}
```

**Intent types:** `GENERATE_PASSWORD`, `SECURITY_ANALYSIS`, `SUGGESTIONS`, `GENERAL`

**Key classes:** `ChatRequest`, `ChatResponse`, `AIInsightResponse`

---

### Feature 42 — Team/Family Vault Sharing (RBAC)

**Controller:** [`TeamVaultController`](../src/main/java/com/revature/passwordmanager/controller/TeamVaultController.java)  
**Base path:** `/api/teams`

Shared vaults for teams or families with role-based access control. Roles: `OWNER` > `ADMIN` > `MEMBER` > `VIEWER`. Owners can share specific vault entries with the team.

**Endpoints:**
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/teams` | Yes | Create team |
| GET | `/api/teams` | Yes | List my teams |
| GET | `/api/teams/{teamId}` | Yes | Get team details |
| DELETE | `/api/teams/{teamId}` | Yes | Delete team |
| GET | `/api/teams/{teamId}/members` | Yes | List team members |
| POST | `/api/teams/{teamId}/members` | Yes | Invite member |
| PUT | `/api/teams/{teamId}/members/{userId}/role` | Yes | Change member role |
| DELETE | `/api/teams/{teamId}/members/{userId}` | Yes | Remove member |
| POST | `/api/teams/{teamId}/share` | Yes | Share vault entry with team |
| GET | `/api/teams/{teamId}/vault` | Yes | View team vault |
| DELETE | `/api/teams/{teamId}/vault/{vaultEntryId}` | Yes | Remove entry from team vault |
| GET | `/api/teams/{teamId}/activity` | Yes | Team activity log |

**Response — `TeamResponse`:**
```json
{
  "id": 1,
  "name": "Dev Team",
  "description": "Development team shared vault",
  "createdByUsername": "johndoe",
  "createdAt": "2026-01-01T10:00:00",
  "memberCount": 3,
  "sharedEntryCount": 5,
  "currentUserRole": "OWNER",
  "members": [
    {
      "memberId": 1,
      "userId": 1,
      "username": "johndoe",
      "email": "john@example.com",
      "role": "OWNER",
      "joinedAt": "2026-01-01T10:00:00"
    }
  ]
}
```

**Roles:** `OWNER` (full control) · `ADMIN` (manage members) · `MEMBER` (read/write) · `VIEWER` (read-only)

**Key classes:** `TeamResponse`, `TeamMemberResponse`, `CreateTeamRequest`, `InviteMemberRequest`

---

## Endpoint Summary Table

| Controller | Base Path | Endpoints | Test Methods |
|---|---|---|---|
| [`VaultController`](../src/main/java/com/revature/passwordmanager/controller/VaultController.java) | `/api/vault` | 22 | 23 |
| [`AuthController`](../src/main/java/com/revature/passwordmanager/controller/AuthController.java) | `/api/auth` | 20 | 23 |
| [`TeamVaultController`](../src/main/java/com/revature/passwordmanager/controller/TeamVaultController.java) | `/api/teams` | 12 | 20 |
| [`UserController`](../src/main/java/com/revature/passwordmanager/controller/UserController.java) | `/api/users` | 10 | 10 |
| [`SecurityController`](../src/main/java/com/revature/passwordmanager/controller/SecurityController.java) | `/api/security` | 10 | 10 |
| [`EmergencyAccessController`](../src/main/java/com/revature/passwordmanager/controller/EmergencyAccessController.java) | `/api/emergency` | 10 | 21 |
| [`BackupController`](../src/main/java/com/revature/passwordmanager/controller/BackupController.java) | `/api/backup` | 8 | 10 |
| [`SecureFileController`](../src/main/java/com/revature/passwordmanager/controller/SecureFileController.java) | `/api/files` | 7 | 23 |
| [`FolderController`](../src/main/java/com/revature/passwordmanager/controller/FolderController.java) | `/api/folders` | 7 | 7 |
| [`TwoFactorController`](../src/main/java/com/revature/passwordmanager/controller/TwoFactorController.java) | `/api/2fa` | 6 | 6 |
| [`CategoryController`](../src/main/java/com/revature/passwordmanager/controller/CategoryController.java) | `/api/categories` | 6 | 23 |
| [`SessionController`](../src/main/java/com/revature/passwordmanager/controller/SessionController.java) | `/api/sessions` | 5 | 8 |
| [`SecureShareController`](../src/main/java/com/revature/passwordmanager/controller/SecureShareController.java) | `/api/shares` | 5 | 24 |
| [`PasswordGeneratorController`](../src/main/java/com/revature/passwordmanager/controller/PasswordGeneratorController.java) | `/api/generator` | 5 | 6 |
| [`PasswordExpiryController`](../src/main/java/com/revature/passwordmanager/controller/PasswordExpiryController.java) | `/api/expiry` | 5 | 20 |
| [`NotificationController`](../src/main/java/com/revature/passwordmanager/controller/NotificationController.java) | `/api/notifications` | 5 | 6 |
| [`DashboardController`](../src/main/java/com/revature/passwordmanager/controller/DashboardController.java) | `/api/dashboard` | 5 | 20 |
| [`BreachMonitorController`](../src/main/java/com/revature/passwordmanager/controller/BreachMonitorController.java) | `/api/security` | 5 | 22 |
| [`AIAssistantController`](../src/main/java/com/revature/passwordmanager/controller/AIAssistantController.java) | `/api/ai` | 5 | 16 |
| [`VaultTimelineController`](../src/main/java/com/revature/passwordmanager/controller/VaultTimelineController.java) | `/api/timeline` | 4 | 15 |
| [`HealthController`](../src/main/java/com/revature/passwordmanager/controller/HealthController.java) | `/api/health` | 3 | 4 |
| [`AutofillController`](../src/main/java/com/revature/passwordmanager/controller/AutofillController.java) | `/api/autofill` | 3 | 12 |
| [`UserSettingsController`](../src/main/java/com/revature/passwordmanager/controller/UserSettingsController.java) | `/api/settings` | 2 | 2 |
| **TOTAL** | | **170** | **331** |

---

## Test Coverage Summary

| Test Type | Count |
|-----------|-------|
| Controller tests (`@WebMvcTest`) | 331 |
| Service / unit tests (`@ExtendWith(MockitoExtension.class)`) | 701 |
| **Total tests** | **1032** |
| Failures | **0** |
| Controllers with tests | **23 / 23 (100%)** |

Each controller test covers:
- ✅ Happy path (correct request → expected response body + status)
- ✅ Unauthenticated access (401)
- ✅ Invalid input / validation errors (400)
- ✅ Not found scenarios (404)
- ✅ Edge cases (empty results, null fields, etc.)

---

## Database Tables

| Table | Feature | Description |
|-------|---------|-------------|
| `users` | 1-4 | User accounts |
| `security_questions` | 7 | User security questions |
| `recovery_codes` | 7 | OTP recovery codes |
| `user_sessions` | 6 | Active JWT sessions |
| `user_settings` | 10 | Per-user preferences |
| `otp_tokens` | 9 | Email OTP tokens |
| `two_factor_auth` | 9 | TOTP secrets and backup codes |
| `vault_entries` | 13 | Encrypted password entries |
| `vault_snapshots` | 18, 35 | Entry change history |
| `categories` | 14 | Vault entry categories |
| `folders` | 11 | Vault entry folders |
| `login_attempts` | 20, 23 | Login attempt tracking |
| `audit_logs` | 21 | User action audit trail |
| `security_alerts` | 22 | Security alert records |
| `notifications` | 28 | In-app notifications |
| `backup_exports` | 25 | Export history |
| `secure_shares` | 19 | Password share links |
| `emergency_contacts` | 39 | Emergency access contacts |
| `emergency_access_requests` | 39 | Emergency access requests |
| `expiry_policies` | 38 | Per-user expiry configuration |
| `autofill_usage_logs` | 36 | Autofill usage tracking |
| `vault_timeline_events` | 37 | Timeline event log |
| `secure_files` | 40 | Encrypted file metadata |
| `secure_file_folders` | 40 | File folder structure |
| `teams` | 42 | Team definitions |
| `team_members` | 42 | Team membership + roles |
| `team_vault_entries` | 42 | Shared vault entries |
| `team_invitations` | 42 | Pending team invitations |
| `breach_scan_results` | 32 | Breach scan history |
| `compromised_credentials` | 32 | Compromised credential records |
