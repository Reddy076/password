# Rev-PasswordManager — Complete Backend API Reference

> **For Frontend Engineers**  
> Base URL: `http://localhost:8080`  
> All authenticated endpoints require: `Authorization: Bearer <JWT_TOKEN>`  
> Content-Type: `application/json` (unless noted)

---

## Table of Contents

1. [Authentication & Registration](#1-authentication--registration)
2. [Two-Factor Authentication (2FA)](#2-two-factor-authentication-2fa)
3. [Vault (Password Entries)](#3-vault-password-entries)
4. [Categories](#4-categories)
5. [Folders](#5-folders)
6. [Password Generator](#6-password-generator)
7. [User Profile & Settings](#7-user-profile--settings)
8. [Sessions](#8-sessions)
9. [Security (Audit Logs, Alerts, Login History)](#9-security-audit-logs-alerts-login-history)
10. [Backup & Import/Export](#10-backup--importexport)
11. [Notifications](#11-notifications)
12. [Dashboard (Password Strength)](#12-dashboard-password-strength)
13. [Breach Monitor](#13-breach-monitor)
14. [Secure Password Sharing](#14-secure-password-sharing)
15. [Vault Timeline](#15-vault-timeline)
16. [Health Check](#16-health-check)
17. [Feature 36 — Smart Password Autofill](#17-feature-36--smart-password-autofill)
18. [Feature 38 — Password Expiration Tracker](#18-feature-38--password-expiration-tracker)
19. [Feature 39 — Emergency Access (Digital Legacy)](#19-feature-39--emergency-access-digital-legacy)
20. [Feature 40 — Secure File Storage Vault](#20-feature-40--secure-file-storage-vault)
21. [Feature 41 — AI Password Assistant](#21-feature-41--ai-password-assistant)
22. [Feature 42 — Team/Family Vault Sharing](#22-feature-42--teamfamily-vault-sharing)
23. [Error Responses](#23-error-responses)
24. [Authentication Flow](#24-authentication-flow)

---

## 1. Authentication & Registration

**Base path:** `/api/auth`  
**Auth required:** No (unless noted)

### POST `/api/auth/register`
Register a new user account.

**Request:**
```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "masterPassword": "MyStr0ng!Pass",
  "passwordHint": "My favorite color + year",
  "securityQuestions": [
    { "questionText": "What was your first pet's name?", "answer": "Fluffy" },
    { "questionText": "What city were you born in?", "answer": "London" }
  ]
}
```

**Response 201:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "is2faEnabled": false,
  "createdAt": "2026-01-01T10:00:00"
}
```
> After registration, an email OTP is sent. User must verify email before logging in.

---

### POST `/api/auth/verify-email`
Verify email with OTP sent during registration.

**Query params:** `username=johndoe&code=123456`

**Response 200:**
```json
{ "message": "Email verified successfully. You can now log in." }
```

---

### POST `/api/auth/resend-verification-otp`
Resend email verification OTP.

**Query params:** `username=johndoe`

**Response 200:**
```json
{ "message": "Verification code resent to your email." }
```

---

### POST `/api/auth/login`
Authenticate and receive JWT tokens.

**Request:**
```json
{
  "username": "johndoe",
  "masterPassword": "MyStr0ng!Pass",
  "captchaToken": "optional-recaptcha-token"
}
```

**Response 200 (success):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "johndoe",
  "expiresIn": 900000,
  "requires2FA": false,
  "message": null
}
```

**Response 200 (2FA required):**
```json
{
  "requires2FA": true,
  "username": "johndoe",
  "accessToken": null
}
```
> If `requires2FA: true`, call `POST /api/auth/verify-otp` next.

---

### POST `/api/auth/verify-otp`
Verify OTP code during 2FA login.

**Query params:** `username=johndoe&code=123456`

**Response 200:** Same as login success response.

---

### POST `/api/auth/send-otp`
Send OTP to user's email.

**Query params:** `username=johndoe`

**Response 200:**
```json
{ "message": "OTP sent to your email." }
```

---

### POST `/api/auth/resend-otp`
Resend OTP to user's email.

**Query params:** `username=johndoe`

**Response 200:**
```json
{ "message": "OTP resent to your email." }
```

---

### POST `/api/auth/refresh-token`
Refresh an expired access token.

**Request:**
```json
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
```

**Response 200:** Same as login success response.

---

### POST `/api/auth/logout`
**Auth required: Yes**  
Logout and invalidate token.

**Headers:** `Authorization: Bearer <token>`

**Response 200:**
```json
{ "message": "Logged out successfully" }
```

---

### GET `/api/auth/validate-token`
**Auth required: Yes**  
Check if current JWT is valid.

**Response 200:**
```json
{ "valid": true }
```

---

### POST `/api/auth/verify-master-password`
**Auth required: Yes**  
Re-authenticate master password for sensitive actions.

**Request:**
```json
{ "masterPassword": "MyStr0ng!Pass" }
```

**Response 200:**
```json
{ "message": "Master password verified successfully" }
```

---

### GET `/api/auth/security-questions/{username}`
Get security questions for a username (for account recovery).

**Response 200:**
```json
[
  { "questionText": "What was your first pet's name?", "answer": "" },
  { "questionText": "What city were you born in?", "answer": "" }
]
```

---

### POST `/api/auth/reset-password`
Reset master password using security questions.

**Request:**
```json
{
  "username": "johndoe",
  "securityQuestions": [
    { "questionText": "What was your first pet's name?", "answer": "Fluffy" }
  ],
  "newPassword": "NewStr0ng!Pass"
}
```

**Response 200:**
```json
{ "message": "Password changed successfully" }
```

---

### POST `/api/auth/forgot-password`
Initiate account recovery via email link.

**Request:**
```json
{ "email": "user@example.com" }
```

**Response 200:**
```json
{ "message": "If the account exists, password recovery has been initiated." }
```

---

### POST `/api/auth/verify-security-questions`
Validate security question answers during recovery.

**Request:**
```json
{
  "username": "johndoe",
  "securityQuestions": [
    { "questionText": "What was your first pet's name?", "answer": "Fluffy" }
  ]
}
```

**Response 200:**
```json
{ "message": "Security questions verified successfully" }
```

---

### POST `/api/auth/verify-captcha`
Verify a CAPTCHA token.

**Request:**
```json
{ "captchaToken": "recaptcha-token" }
```

**Response 200:**
```json
{ "message": "CAPTCHA verified successfully" }
```

---

### GET `/api/auth/password-hint/{username}`
Get password hint for a username.

**Response 200:**
```json
{ "hint": "My favorite color + year" }
```

---

### PUT `/api/auth/password-hint`
**Auth required: Yes**  
Set or update the password hint.

**Request:**
```json
{
  "hint": "My favorite color + year",
  "masterPassword": "MyStr0ng!Pass"
}
```

**Response 200:**
```json
{ "message": "Password hint updated" }
```

---

### POST `/api/auth/duress-login`
Login using a duress password (shows dummy vault, blocks writes).

**Request:** Same as `/api/auth/login`

**Response 200:** Same as login success response (token has `duress: true` claim).

---

### POST `/api/auth/set-duress-password`
**Auth required: Yes**  
Configure a duress password for emergency data wiping.

**Request:**
```json
{ "duressPassword": "FakePass123!" }
```

**Response 200:**
```json
{ "message": "Duress password set successfully" }
```

---

## 2. Two-Factor Authentication (2FA)

**Base path:** `/api/2fa`  
**Auth required:** Yes

### GET `/api/2fa/status`
Get 2FA enabled status.

**Response 200:**
```json
{ "enabled": false }
```

---

### POST `/api/2fa/setup`
Initialize 2FA setup — returns QR code for authenticator app.

**Response 200:**
```json
{
  "secret": "JBSWY3DPEHPK3PXP",
  "qrCodeUrl": "otpauth://totp/RevPasswordManager:johndoe?secret=...",
  "qrCodeImage": "data:image/png;base64,..."
}
```

---

### POST `/api/2fa/verify-setup`
Verify and enable 2FA with the code from the authenticator app.

**Query params:** `code=123456`

**Response 200:**
```json
{
  "success": true,
  "message": "2FA enabled successfully",
  "backupCodes": ["abc123", "def456", "ghi789", ...]
}
```

---

### POST `/api/2fa/disable`
Disable 2FA.

**Response 200:**
```json
{ "message": "2FA disabled successfully" }
```

---

### GET `/api/2fa/backup-codes`
Get backup recovery codes.

**Response 200:**
```json
["abc123", "def456", "ghi789", ...]
```

---

### POST `/api/2fa/regenerate-codes`
Regenerate backup recovery codes.

**Response 200:**
```json
{
  "success": true,
  "message": "Backup codes regenerated successfully",
  "codes": ["new123", "new456", ...]
}
```

---

## 3. Vault (Password Entries)

**Base path:** `/api/vault`  
**Auth required:** Yes

### POST `/api/vault`
Create a new vault entry.

**Request:**
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

**Response 201:**
```json
{
  "id": 10,
  "title": "GitHub",
  "username": "johndoe",
  "websiteUrl": "https://github.com",
  "categoryId": 1,
  "categoryName": "Work",
  "folderId": 2,
  "folderName": "Dev",
  "isFavorite": false,
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-01-01T10:00:00",
  "strengthScore": 85,
  "strengthLabel": "Strong"
}
```

---

### GET `/api/vault`
Get all vault entries.

**Response 200:** Array of vault entry objects (same as above).

---

### GET `/api/vault/search`
Search vault entries with filters.

**Query params:**
- `keyword` (optional) — search in title and URL
- `categoryId` (optional)
- `folderId` (optional)
- `isFavorite` (optional, boolean)
- `isHighlySensitive` (optional, boolean)
- `sortBy` (default: `title`) — `title`, `createdAt`, `updatedAt`
- `sortDir` (default: `asc`) — `asc`, `desc`

**Response 200:** Array of vault entry objects.

---

### GET `/api/vault/filter`
Filter vault entries by category, folder, or favorite.

**Query params:** `categoryId`, `folderId`, `isFavorite`

**Response 200:** Array of vault entry objects.

---

### GET `/api/vault/recent`
Get 10 most recently created entries.

**Response 200:** Array of vault entry objects.

---

### GET `/api/vault/recently-used`
Get 10 most recently updated entries.

**Response 200:** Array of vault entry objects.

---

### GET `/api/vault/favorites`
Get all favorited entries.

**Response 200:** Array of vault entry objects.

---

### GET `/api/vault/{id}`
Get a specific vault entry with decrypted details.

**Response 200:**
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
  "createdAt": "2026-01-01T10:00:00",
  "updatedAt": "2026-01-01T10:00:00",
  "strengthScore": 85,
  "strengthLabel": "Strong"
}
```
> If `isHighlySensitive: true`, password/username/notes are returned as `"******"`.

---

### PUT `/api/vault/{id}`
Update a vault entry. Only non-null fields are updated.

**Request:** Same as POST (all fields optional).

**Response 200:** Updated vault entry object.

---

### PUT `/api/vault/{id}/favorite`
Toggle favorite status.

**Response 200:** Updated vault entry object.

---

### PUT `/api/vault/entries/{id}/sensitive`
Toggle highly sensitive flag.

**Response 200:** Updated vault entry object.

---

### POST `/api/vault/{id}/sensitive-view`
Access a highly sensitive entry (requires master password re-verification).

**Request:**
```json
{ "masterPassword": "MyStr0ng!Pass" }
```

**Response 200:** Full vault entry detail response with decrypted fields.

---

### POST `/api/vault/entries/{id}/view-password`
Reveal the plain text password for a specific entry.

**Response 200:**
```json
{ "password": "MyGitHubPass!" }
```

---

### DELETE `/api/vault/{id}`
Move entry to trash (soft delete).

**Response 200:**
```json
{ "message": "Entry deleted successfully" }
```

---

### POST `/api/vault/entries/bulk-delete`
Move multiple entries to trash.

**Request:**
```json
[1, 2, 3, 4]
```

**Response 200:**
```json
{ "message": "Entries deleted successfully" }
```

---

### GET `/api/vault/entries/{id}/history`
Get password history snapshots for an entry.

**Response 200:**
```json
[
  {
    "id": 1,
    "vaultEntryId": 10,
    "encryptedPassword": "...",
    "changedAt": "2026-01-01T10:00:00"
  }
]
```

---

### GET `/api/vault/trash`
List all entries in the trash.

**Response 200:**
```json
[
  {
    "id": 10,
    "title": "GitHub",
    "websiteUrl": "https://github.com",
    "categoryName": "Work",
    "folderName": "Dev",
    "deletedAt": "2026-01-15T10:00:00",
    "expiresAt": "2026-02-14T10:00:00",
    "daysRemaining": 20
  }
]
```

---

### GET `/api/vault/trash/count`
Get total count of items in trash.

**Response 200:**
```json
{ "count": 5 }
```

---

### POST `/api/vault/trash/{id}/restore`
Restore a specific entry from trash.

**Response 200:** Trash entry response object.

---

### POST `/api/vault/trash/restore-all`
Restore all entries from trash.

**Response 200:**
```json
{ "message": "All trash entries restored successfully" }
```

---

### DELETE `/api/vault/trash/{id}`
Permanently delete a specific entry.

**Response 200:**
```json
{ "message": "Entry permanently deleted" }
```

---

### DELETE `/api/vault/trash/empty`
Permanently delete all items in trash.

**Response 200:**
```json
{ "message": "Trash emptied successfully" }
```

---

## 4. Categories

**Base path:** `/api/categories`  
**Auth required:** Yes

### GET `/api/categories`
List all categories.

**Response 200:**
```json
[
  { "id": 1, "name": "Work", "icon": "briefcase", "isDefault": false }
]
```

---

### GET `/api/categories/{id}`
Get a specific category.

**Response 200:** Category object.

---

### POST `/api/categories`
Create a category.

**Request:**
```json
{ "name": "Work", "icon": "briefcase" }
```

**Response 201:** Category object.

---

### PUT `/api/categories/{id}`
Update a category.

**Request:** Same as POST.

**Response 200:** Updated category object.

---

### DELETE `/api/categories/{id}`
Delete a category.

**Response 200:**
```json
{ "message": "Category deleted successfully" }
```

---

### GET `/api/categories/{id}/entries`
Get all vault entries in a category.

**Response 200:** Array of vault entry objects.

---

## 5. Folders

**Base path:** `/api/folders`  
**Auth required:** Yes

### GET `/api/folders`
List all folders.

**Response 200:**
```json
[
  {
    "id": 1,
    "name": "Dev",
    "parentFolderId": null,
    "createdAt": "2026-01-01T10:00:00"
  }
]
```

---

### GET `/api/folders/{id}`
Get a specific folder.

**Response 200:** Folder object.

---

### POST `/api/folders`
Create a folder.

**Query params:** `name=Dev&parentFolderId=1` (parentFolderId optional)

**Response 201:** Folder object.

---

### PUT `/api/folders/{id}`
Rename a folder.

**Query params:** `name=NewName`

**Response 200:** Updated folder object.

---

### PUT `/api/folders/{id}/move`
Move a folder to a new parent.

**Query params:** `parentId=2` (null = move to root)

**Response 200:** Updated folder object.

---

### DELETE `/api/folders/{id}`
Delete a folder.

**Response 200:**
```json
{ "message": "Folder deleted successfully" }
```

---

### GET `/api/folders/{id}/entries`
Get all vault entries in a folder.

**Response 200:** Array of vault entry objects.

---

## 6. Password Generator

**Base path:** `/api/generator`  
**Auth required:** No

### POST `/api/generator/generate`
Generate a single password.

**Request:**
```json
{
  "length": 16,
  "includeUppercase": true,
  "includeLowercase": true,
  "includeNumbers": true,
  "includeSpecial": true,
  "excludeSimilar": false,
  "excludeAmbiguous": false
}
```

**Response 200:**
```json
{ "password": "Xk9#mP2@nQ5!vR7$" }
```

---

### POST `/api/generator/generate-multiple`
Generate multiple passwords.

**Request:** Same as above (add `count: 5`).

**Response 200:**
```json
{ "passwords": ["Xk9#mP2@nQ5!vR7$", "..."] }
```

---

### POST `/api/generator/strength`
Check password strength.

**Request:**
```json
{ "password": "MyPassword123!" }
```

**Response 200:**
```json
{
  "score": 75,
  "label": "Good",
  "feedback": ["Add special characters", "Increase length"],
  "entropy": 52.3,
  "crackTime": "3 years"
}
```

---

### POST `/api/generator/validate`
Validate a password (same as strength check).

**Request:**
```json
{ "password": "MyPassword123!" }
```

**Response 200:** Same as strength response.

---

### GET `/api/generator/default-settings`
Get default password generator settings.

**Response 200:** Password generator request object with defaults.

---

## 7. User Profile & Settings

**Base path:** `/api/users`  
**Auth required:** Yes

### GET `/api/users/profile`
Get current user profile.

**Response 200:**
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "name": "John Doe",
  "phoneNumber": "+1234567890",
  "is2faEnabled": false,
  "createdAt": "2026-01-01T10:00:00"
}
```

---

### PUT `/api/users/profile`
Update user profile.

**Request:**
```json
{
  "name": "John Doe",
  "phoneNumber": "+1234567890"
}
```

**Response 200:** Updated user response.

---

### PUT `/api/users/change-password`
Change master password.

**Request:**
```json
{
  "currentPassword": "OldPass!",
  "newPassword": "NewStr0ng!Pass"
}
```

**Response 200:**
```json
{ "message": "Password changed successfully" }
```

---

### DELETE `/api/users/account`
Schedule account deletion (30-day grace period).

**Request:**
```json
{ "masterPassword": "MyStr0ng!Pass" }
```

**Response 200:**
```json
{ "message": "Account scheduled for deletion in 30 days..." }
```

---

### POST `/api/users/account/cancel-deletion`
Cancel scheduled account deletion.

**Response 200:**
```json
{ "message": "Account deletion cancelled." }
```

---

### GET `/api/users/security-questions`
Get current user's security questions (answers hidden).

**Response 200:**
```json
[
  { "questionText": "What was your first pet's name?", "answer": "" }
]
```

---

### PUT `/api/users/security-questions`
Update security questions.

**Request:**
```json
{
  "masterPassword": "MyStr0ng!Pass",
  "securityQuestions": [
    { "questionText": "New question?", "answer": "New answer" }
  ]
}
```

**Response 200:** (empty body, 200 status)

---

### GET `/api/users/activity-heatmap`
Get vault access heatmap data.

**Response 200:**
```json
{
  "heatmapData": [
    { "date": "2026-01-01", "count": 5 }
  ],
  "totalAccesses": 150,
  "mostActiveDay": "Monday",
  "mostActiveHour": 14
}
```

---

### GET `/api/users/dashboard`
Get dashboard summary data.

**Response 200:**
```json
{
  "totalPasswords": 25,
  "weakPasswords": 3,
  "reusedPasswords": 2,
  "oldPasswords": 5,
  "securityScore": 72,
  "lastLogin": "2026-01-15T10:00:00"
}
```

---

### PUT `/api/users/read-only-mode`
Toggle read-only mode (prevents vault modifications).

**Request:**
```json
{ "readOnlyMode": true }
```

**Response 200:** User settings response.

---

### GET `/api/settings`
Get user settings.

**Response 200:**
```json
{
  "id": 1,
  "theme": "DARK",
  "language": "en-US",
  "autoLogoutMinutes": 15,
  "readOnlyMode": false
}
```

---

### PUT `/api/settings`
Update user settings.

**Request:**
```json
{
  "theme": "DARK",
  "language": "en-US",
  "autoLogoutMinutes": 30
}
```

**Response 200:** Updated settings object.

---

## 8. Sessions

**Base path:** `/api/sessions`  
**Auth required:** Yes

### GET `/api/sessions`
List all active sessions.

**Response 200:**
```json
[
  {
    "id": 1,
    "deviceInfo": "Chrome/120 on Windows",
    "ipAddress": "192.168.1.1",
    "location": "New York, US",
    "isActive": true,
    "createdAt": "2026-01-01T10:00:00",
    "lastAccessedAt": "2026-01-15T10:00:00",
    "expiresAt": "2026-01-22T10:00:00"
  }
]
```

---

### GET `/api/sessions/current`
Get current session details.

**Headers:** `Authorization: Bearer <token>`

**Response 200:** Session object.

---

### POST `/api/sessions/extend`
Extend the current session.

**Headers:** `Authorization: Bearer <token>`

**Response 200:** Updated session object.

---

### DELETE `/api/sessions/{sessionId}`
Terminate a specific session.

**Response 200:**
```json
{ "message": "Session terminated successfully" }
```

---

### DELETE `/api/sessions`
Terminate all sessions.

**Response 200:**
```json
{ "message": "All sessions terminated successfully" }
```

---

## 9. Security (Audit Logs, Alerts, Login History)

**Base path:** `/api/security`  
**Auth required:** Yes

### GET `/api/security/audit-logs`
Get audit log history.

**Response 200:**
```json
[
  {
    "id": 1,
    "action": "LOGIN",
    "details": "Successful login",
    "ipAddress": "192.168.1.1",
    "createdAt": "2026-01-15T10:00:00"
  }
]
```

---

### GET `/api/security/login-history`
Get login attempt history.

**Response 200:**
```json
[
  {
    "id": 1,
    "ipAddress": "192.168.1.1",
    "deviceInfo": "Chrome/120",
    "status": "SUCCESS",
    "failureReason": null,
    "location": "New York, US",
    "createdAt": "2026-01-15T10:00:00"
  }
]
```

---

### GET `/api/security/alerts`
Get security alerts.

**Response 200:**
```json
[
  {
    "id": 1,
    "alertType": "NEW_DEVICE_LOGIN",
    "title": "New Device Detected",
    "message": "Login from Chrome on Windows",
    "severity": "MEDIUM",
    "isRead": false,
    "createdAt": "2026-01-15T10:00:00"
  }
]
```

---

### PUT `/api/security/alerts/{id}/read`
Mark an alert as read.

**Response 200:**
```json
{ "message": "Alert marked as read" }
```

---

### DELETE `/api/security/alerts/{id}`
Delete an alert.

**Response 200:**
```json
{ "message": "Alert deleted" }
```

---

### GET `/api/security/audit-report`
Get full security audit report.

**Response 200:**
```json
{
  "overallScore": 72,
  "weakPasswords": [...],
  "reusedPasswords": [...],
  "oldPasswords": [...],
  "generatedAt": "2026-01-15T10:00:00"
}
```

---

### GET `/api/security/weak-passwords`
Get list of weak passwords.

**Response 200:** Array of vault entry summaries.

---

### GET `/api/security/reused-passwords`
Get list of reused passwords.

**Response 200:** Array of vault entry summaries.

---

### GET `/api/security/old-passwords`
Get list of old passwords (>90 days).

**Response 200:** Array of vault entry summaries.

---

### POST `/api/security/analyze-vault`
Trigger vault security analysis.

**Response 200:** Security audit response.

---

### POST `/api/security/breach-scan`
Trigger manual breach scan (checks against HaveIBeenPwned).

**Response 200:**
```json
{
  "scanId": 1,
  "status": "COMPLETED",
  "entriesScanned": 25,
  "compromisedFound": 2,
  "scannedAt": "2026-01-15T10:00:00"
}
```

---

### GET `/api/security/breach-status`
Get current breach status.

**Response 200:**
```json
{
  "status": "AT_RISK",
  "compromisedCount": 2,
  "lastScanAt": "2026-01-15T10:00:00"
}
```

---

### GET `/api/security/compromised-credentials`
List compromised credentials.

**Response 200:**
```json
[
  {
    "id": 1,
    "vaultEntryId": 10,
    "vaultEntryTitle": "GitHub",
    "pwnedCount": 15234,
    "isResolved": false,
    "detectedAt": "2026-01-15T10:00:00"
  }
]
```

---

### GET `/api/security/breach-history`
Get historical breach scan records.

**Response 200:**
```json
{
  "scans": [
    {
      "id": 1,
      "triggerType": "MANUAL",
      "entriesScanned": 25,
      "compromisedFound": 2,
      "status": "COMPLETED",
      "scannedAt": "2026-01-15T10:00:00"
    }
  ]
}
```

---

### PUT `/api/security/compromised-credentials/{id}/resolve`
Mark a compromised credential as resolved.

**Response 200:** Updated compromised credential object.

---

## 10. Backup & Import/Export

**Base path:** `/api/backup`  
**Auth required:** Yes

### GET `/api/backup/export`
Export vault data.

**Query params:** `format=JSON` (or `CSV`), `password=optional-encryption-password`

**Response 200:**
```json
{
  "format": "JSON",
  "data": "base64-encoded-export-data",
  "encryptedWithPassword": false,
  "exportedAt": "2026-01-15T10:00:00",
  "entryCount": 25
}
```

---

### GET `/api/backup/export/preview`
Preview export without downloading.

**Query params:** `format=JSON`

**Response 200:** Export response object.

---

### POST `/api/backup/import`
Import vault data.

**Request:**
```json
{
  "format": "JSON",
  "data": "base64-encoded-data",
  "password": "optional-decryption-password"
}
```

**Response 200:**
```json
{
  "imported": 20,
  "skipped": 2,
  "failed": 0,
  "errors": []
}
```

---

### POST `/api/backup/import/validate`
Validate import data without importing.

**Request:** Same as import.

**Response 200:** Import result object.

---

### POST `/api/backup/import-external`
Import from third-party password managers.

**Request:**
```json
{
  "source": "CHROME",
  "data": "csv-content-as-string"
}
```
> Supported sources: `CHROME`, `FIREFOX`, `LASTPASS`, `1PASSWORD`

**Response 200:** Import result object.

---

### GET `/api/backup/import-external/formats`
Get list of supported import formats.

**Response 200:**
```json
["CHROME", "FIREFOX", "LASTPASS", "1PASSWORD"]
```

---

### GET `/api/backup/snapshots`
Get all vault snapshots.

**Response 200:**
```json
[
  {
    "id": 1,
    "vaultEntryId": 10,
    "encryptedPassword": "...",
    "changedAt": "2026-01-01T10:00:00"
  }
]
```

---

### POST `/api/backup/snapshots/{id}/restore`
Restore a vault snapshot.

**Response 200:** (empty body, 200 status)

---

## 11. Notifications

**Base path:** `/api/notifications`  
**Auth required:** Yes

### GET `/api/notifications`
Get all notifications.

**Response 200:**
```json
[
  {
    "id": 1,
    "notificationType": "PASSWORD_EXPIRY",
    "title": "Password Expiring Soon: GitHub",
    "message": "Your GitHub password will expire in 5 days.",
    "isRead": false,
    "createdAt": "2026-01-15T10:00:00"
  }
]
```

---

### GET `/api/notifications/unread-count`
Get unread notification count.

**Response 200:**
```json
{ "count": 3 }
```

---

### PUT `/api/notifications/{id}/read`
Mark a notification as read.

**Response 200:**
```json
{ "message": "Notification marked as read" }
```

---

### PUT `/api/notifications/mark-all-read`
Mark all notifications as read.

**Response 200:**
```json
{ "message": "All notifications marked as read" }
```

---

### DELETE `/api/notifications/{id}`
Delete a notification.

**Response 200:**
```json
{ "message": "Notification deleted" }
```

---

## 12. Dashboard (Password Strength)

**Base path:** `/api/dashboard`  
**Auth required:** Yes

### GET `/api/dashboard/security-score`
Get overall security score (0-100).

**Response 200:**
```json
{
  "overallScore": 82,
  "scoreLabel": "Good",
  "totalPasswords": 25,
  "strongPasswords": 18,
  "fairPasswords": 4,
  "weakPasswords": 3,
  "reusedPasswords": 2,
  "oldPasswords": 5,
  "recommendation": "Update 5 old passwords to improve your score."
}
```

---

### GET `/api/dashboard/password-health`
Get password health breakdown by strength category.

**Response 200:**
```json
{
  "totalPasswords": 25,
  "strongCount": 10,
  "goodCount": 8,
  "fairCount": 4,
  "weakCount": 2,
  "veryWeakCount": 1,
  "averageStrengthScore": 68.5,
  "categoryBreakdowns": [
    {
      "categoryName": "Work",
      "count": 10,
      "averageScore": 75.0,
      "weakCount": 1
    }
  ]
}
```

---

### GET `/api/dashboard/reused-passwords`
Get groups of reused passwords.

**Response 200:**
```json
{
  "totalReusedGroups": 2,
  "totalAffectedEntries": 5,
  "reusedGroups": [
    {
      "reuseCount": 3,
      "entries": [
        { "entryId": 1, "title": "Gmail", "username": "user@gmail.com", "websiteUrl": "https://gmail.com" }
      ]
    }
  ]
}
```

---

### GET `/api/dashboard/password-age`
Get password age distribution.

**Response 200:**
```json
{
  "totalPasswords": 25,
  "freshCount": 10,
  "agingCount": 8,
  "oldCount": 5,
  "ancientCount": 2,
  "averageAgeInDays": 45.3,
  "distribution": [
    { "label": "< 30 days", "count": 10, "minDays": 0, "maxDays": 29 },
    { "label": "30-90 days", "count": 8, "minDays": 30, "maxDays": 90 },
    { "label": "90-180 days", "count": 5, "minDays": 91, "maxDays": 180 },
    { "label": "> 180 days", "count": 2, "minDays": 181, "maxDays": 2147483647 }
  ]
}
```

---

### GET `/api/dashboard/trends`
Get historical security score trends.

**Query params:** `days=30` (default 30)

**Response 200:**
```json
{
  "trendPoints": [
    {
      "recordedAt": "2026-01-01T00:00:00",
      "overallScore": 75,
      "weakPasswordsCount": 5,
      "reusedPasswordsCount": 3,
      "oldPasswordsCount": 8
    }
  ],
  "scoreChange": 7,
  "trendDirection": "IMPROVING",
  "periodLabel": "30-day trend"
}
```

---

## 13. Breach Monitor

See [Section 9](#9-security-audit-logs-alerts-login-history) — breach endpoints are under `/api/security`.

---

## 14. Secure Password Sharing

**Base path:** `/api/shares`  
**Auth required:** Yes (except GET `/{token}`)

### POST `/api/shares`
Create a new secure share.

**Request:**
```json
{
  "vaultEntryId": 10,
  "recipientEmail": "friend@example.com",
  "permission": "VIEW_ONCE",
  "maxViews": 1,
  "expiresInHours": 24
}
```
> Permissions: `VIEW_ONCE`, `VIEW_MULTIPLE`, `TEMPORARY_ACCESS`

**Response 201:**
```json
{
  "shareId": 1,
  "shareToken": "abc123token",
  "shareUrl": "/api/shares/abc123token",
  "encryptionKey": "base64KeyHere==",
  "vaultEntryTitle": "GitHub",
  "permission": "VIEW_ONCE",
  "maxViews": 1,
  "viewCount": 0,
  "expiresAt": "2026-01-16T10:00:00",
  "recipientEmail": "friend@example.com",
  "createdAt": "2026-01-15T10:00:00"
}
```
> **Important:** The `encryptionKey` must be embedded in the share URL fragment (e.g., `https://app.com/share/abc123token#base64KeyHere==`). The key is never stored on the server.

---

### GET `/api/shares/{token}`
**Auth required: No**  
Retrieve a shared password by token.

**Response 200:**
```json
{
  "title": "GitHub",
  "encryptedPassword": "base64-encrypted-data",
  "encryptionIv": "base64-iv",
  "username": "johndoe",
  "websiteUrl": "https://github.com",
  "expiresAt": "2026-01-16T10:00:00",
  "viewCount": 1,
  "maxViews": 1
}
```
> The frontend must decrypt `encryptedPassword` using the `encryptionKey` from the URL fragment.

---

### GET `/api/shares`
List all active shares created by the user.

**Response 200:** Array of share link response objects.

---

### DELETE `/api/shares/{id}`
Revoke a share.

**Response 200:** Updated share link response (with `isRevoked: true`).

---

### GET `/api/shares/received`
List shares received by the user (targeted by their email).

**Response 200:** Array of share link response objects.

---

## 15. Vault Timeline

**Base path:** `/api/timeline`  
**Auth required:** Yes

### GET `/api/timeline`
Get full vault activity timeline.

**Query params:** `days=N` (optional), `category=VAULT` (optional)  
Categories: `VAULT`, `AUTH`, `BREACH`, `SHARING`, `BACKUP`, `SECURITY`

**Response 200:**
```json
{
  "events": [
    {
      "id": 1,
      "action": "ENTRY_CREATED",
      "category": "VAULT",
      "description": "Created entry: GitHub",
      "actorUsername": "johndoe",
      "ipAddress": "192.168.1.1",
      "createdAt": "2026-01-15T10:00:00"
    }
  ],
  "categoryBreakdown": {
    "VAULT": 15,
    "AUTH": 8,
    "BREACH": 2
  }
}
```

---

### GET `/api/timeline/summary`
Get activity summary statistics.

**Response 200:**
```json
{
  "totalEvents": 150,
  "entriesCreated": 25,
  "entriesDeleted": 3,
  "passwordChanges": 10,
  "sharesCreated": 5,
  "breachDetections": 2,
  "mostActiveDay": "Monday",
  "mostActiveHour": 14,
  "topAccessedEntries": [...],
  "weeklyHistogram": [...]
}
```

---

### GET `/api/timeline/entry/{entryId}`
Get timeline for a specific vault entry.

**Response 200:**
```json
{
  "entryId": 10,
  "entryTitle": "GitHub",
  "events": [...],
  "totalEvents": 8,
  "createdAt": "2026-01-01T10:00:00",
  "lastModifiedAt": "2026-01-15T10:00:00"
}
```

---

### GET `/api/timeline/stats`
Get chart-ready activity statistics.

**Query params:** `days=N` (optional)

**Response 200:**
```json
{
  "dailyActivity": [...],
  "monthlyTotals": [...],
  "eventCountsByType": {...},
  "peakActivityDate": "2026-01-10",
  "averageEventsPerDay": 5.2
}
```

---

## 16. Health Check

**Base path:** `/api/health`  
**Auth required:** No

### GET `/api/health`
Get overall system health.

**Response 200:**
```json
{
  "status": "UP",
  "components": {
    "database": { "status": "UP" },
    "services": { "status": "UP" }
  },
  "timestamp": "2026-01-15T10:00:00"
}
```

---

### GET `/api/health/db`
Get database health.

**Response 200:**
```json
{ "status": "UP", "details": "MySQL connection OK" }
```

---

### GET `/api/health/services`
Get services health.

**Response 200:**
```json
{ "status": "UP", "details": "All services operational" }
```

---

## 17. Feature 36 — Smart Password Autofill

**Base path:** `/api/autofill`  
**Auth required:** Yes  
**Purpose:** Backend API for browser extensions. Passwords are NEVER returned — only metadata.

### POST `/api/autofill/suggestions`
Get vault entries matching the current page URL.

**Request:**
```json
{ "url": "https://github.com/login" }
```

**Response 200:**
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
    },
    {
      "entryId": 11,
      "title": "GitHub App",
      "username": "johndoe",
      "websiteUrl": "https://app.github.com",
      "matchType": "SUBDOMAIN",
      "isFavorite": false
    }
  ]
}
```
> Match types: `EXACT` > `SUBDOMAIN` > `PARTIAL` (sorted by relevance)  
> To get the actual password, call `POST /api/vault/entries/{entryId}/view-password`

---

### GET `/api/autofill/trusted-domains`
Get domains previously used with autofill (most recent first).

**Response 200:**
```json
["github.com", "google.com", "bank.com"]
```

---

### POST `/api/autofill/log-usage`
Record autofill usage for analytics.

**Request:**
```json
{
  "url": "https://github.com/login",
  "vaultEntryId": 10,
  "applied": true
}
```

**Response 200:**
```json
{ "message": "Usage logged successfully" }
```

---

## 18. Feature 38 — Password Expiration Tracker

**Base path:** `/api/expiry`  
**Auth required:** Yes

### GET `/api/expiry/status`
Get expiry status for all vault entries.

**Response 200:**
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
> Status values: `FRESH`, `AGING`, `EXPIRING_SOON`, `EXPIRED`

---

### GET `/api/expiry/expiring-soon`
Get entries expiring within N days.

**Query params:** `days=7` (default 7)

**Response 200:** Same structure as `/status` but filtered.

---

### GET `/api/expiry/policy`
Get current expiry policy.

**Response 200:**
```json
{
  "id": 1,
  "defaultExpiryDays": 90,
  "criticalExpiryDays": 180,
  "reminderDaysBefore": 7,
  "enabled": true
}
```

---

### PUT `/api/expiry/policy`
Update expiry policy.

**Request:**
```json
{
  "defaultExpiryDays": 60,
  "criticalExpiryDays": 120,
  "reminderDaysBefore": 5,
  "enabled": true
}
```

**Response 200:** Updated policy object.

---

### POST `/api/expiry/snooze/{entryId}`
Snooze expiry reminder for an entry.

**Query params:** `days=7` (default 7)

**Response 200:** Updated entry expiry detail object.

---

## 19. Feature 39 — Emergency Access (Digital Legacy)

**Base path:** `/api/emergency`  
**Auth required:** Yes (except `GET /vault/{token}`)

### POST `/api/emergency/contacts`
Add an emergency contact.

**Request:**
```json
{
  "contactEmail": "spouse@example.com",
  "contactName": "Jane Doe",
  "relationship": "Spouse",
  "waitingPeriodHours": 48
}
```

**Response 201:**
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

---

### GET `/api/emergency/contacts`
List all active emergency contacts.

**Response 200:** Array of contact objects.

---

### PUT `/api/emergency/contacts/{contactId}`
Update an emergency contact.

**Request:** Same as POST (all fields optional, email ignored).

**Response 200:** Updated contact object.

---

### DELETE `/api/emergency/contacts/{contactId}`
Remove an emergency contact.

**Response 200:**
```json
{ "message": "Emergency contact removed successfully" }
```

---

### POST `/api/emergency/request-access`
Request emergency access to a vault owner's vault.

**Request:**
```json
{
  "ownerUsername": "johndoe",
  "requestMessage": "Medical emergency"
}
```

**Response 201:**
```json
{
  "id": 1,
  "contactId": 5,
  "contactEmail": "spouse@example.com",
  "contactName": "Jane Doe",
  "ownerUsername": "johndoe",
  "status": "PENDING",
  "requestedAt": "2026-01-15T10:00:00",
  "waitingPeriodEndsAt": "2026-01-17T10:00:00",
  "hoursUntilAutoApproval": 48,
  "decidedAt": null,
  "accessToken": null,
  "expiresAt": null,
  "requestMessage": "Medical emergency"
}
```
> Status values: `PENDING`, `APPROVED`, `DENIED`, `EXPIRED`

---

### GET `/api/emergency/my-requests`
List all requests made by the authenticated contact.

**Response 200:** Array of request objects.

---

### GET `/api/emergency/requests`
List all incoming requests for the vault owner.

**Response 200:** Array of request objects.

---

### POST `/api/emergency/grant/{requestId}`
Owner explicitly grants access before waiting period ends.

**Response 200:** Updated request object with `accessToken` and `expiresAt`.

---

### POST `/api/emergency/deny/{requestId}`
Owner denies an emergency access request.

**Response 200:** Updated request object with `status: "DENIED"`.

---

### GET `/api/emergency/vault/{token}`
**Auth required: No**  
Access vault metadata using an approved access token.

**Response 200:**
```json
{
  "ownerUsername": "johndoe",
  "expiresAt": "2026-01-19T10:00:00",
  "hoursUntilExpiry": 48,
  "entries": [
    {
      "entryId": 10,
      "title": "GitHub",
      "websiteUrl": "https://github.com",
      "categoryName": "Work",
      "folderName": "Dev",
      "lastUpdatedAt": "2026-01-15T10:00:00"
    }
  ]
}
```
> **Note:** Passwords are NOT included. This is read-only metadata access.

---

## 20. Feature 40 — Secure File Storage Vault

**Base path:** `/api/files`  
**Auth required:** Yes  
**Max file size:** 50 MB  
**Max total storage:** 100 MB per user

### POST `/api/files/upload`
Upload an encrypted file.

**Content-Type:** `multipart/form-data`  
**Form fields:** `file` (required), `folderId` (optional)

**Response 201:**
```json
{
  "id": 1,
  "originalFilename": "passport.pdf",
  "fileSize": 524288,
  "mimeType": "application/pdf",
  "checksum": "sha256hash...",
  "folderId": 2,
  "folderName": "Documents",
  "uploadedAt": "2026-01-15T10:00:00"
}
```

---

### GET `/api/files`
List files and folders at a level.

**Query params:** `folderId` (optional, null = root)

**Response 200:**
```json
{
  "folders": [
    {
      "id": 2,
      "name": "Documents",
      "parentId": null,
      "createdAt": "2026-01-01T10:00:00"
    }
  ],
  "files": [
    {
      "id": 1,
      "originalFilename": "passport.pdf",
      "fileSize": 524288,
      "mimeType": "application/pdf",
      "folderId": 2,
      "folderName": "Documents",
      "uploadedAt": "2026-01-15T10:00:00",
      "lastAccessedAt": "2026-01-15T11:00:00"
    }
  ],
  "totalFiles": 5,
  "totalStorageBytes": 2097152
}
```

---

### GET `/api/files/search`
Search files by filename.

**Query params:** `keyword=passport`

**Response 200:** Same structure as list response.

---

### GET `/api/files/{id}/download`
Download a decrypted file.

**Response 200:** Binary file bytes  
**Headers:**
- `Content-Disposition: attachment; filename="passport.pdf"`
- `Content-Type: application/pdf`
- `Content-Length: 524288`

---

### DELETE `/api/files/{id}`
Delete a file permanently.

**Response 200:**
```json
{ "message": "File deleted successfully" }
```

---

### POST `/api/files/folders`
Create a folder.

**Request:**
```json
{
  "name": "Documents",
  "parentId": null
}
```

**Response 201:**
```json
{
  "id": 2,
  "name": "Documents",
  "parentId": null,
  "createdAt": "2026-01-15T10:00:00"
}
```

---

### DELETE `/api/files/folders/{folderId}`
Delete a folder and all its contents recursively.

**Response 200:**
```json
{ "message": "Folder deleted successfully" }
```

---

## 21. Feature 41 — AI Password Assistant

**Base path:** `/api/ai`  
**Auth required:** Yes  
**Note:** When `ai.openai.api-key` is configured, uses GPT-3.5-turbo. Otherwise uses rule-based fallback.

### POST `/api/ai/chat`
Send a message to the AI assistant.

**Request:**
```json
{ "message": "Which passwords should I update?" }
```

**Response 200:**
```json
{
  "reply": "Based on your vault, you have 5 passwords older than 90 days...",
  "intent": "SECURITY_ANALYSIS",
  "generatedPassword": null,
  "suggestions": [
    "View security score",
    "Check weak passwords",
    "View reused passwords"
  ],
  "timestamp": "2026-01-15T10:00:00",
  "aiPowered": false,
  "history": [
    { "role": "user", "content": "Which passwords should I update?", "createdAt": "..." },
    { "role": "assistant", "content": "Based on your vault...", "createdAt": "..." }
  ]
}
```
> Intent values: `GENERATE_PASSWORD`, `BREACH_CHECK`, `SECURITY_ANALYSIS`, `TWO_FACTOR`, `GENERAL`  
> When intent is `GENERATE_PASSWORD`, `generatedPassword` is populated.

---

### GET `/api/ai/suggestions`
Get contextual quick-action suggestions.

**Response 200:**
```json
{
  "reply": "Here are some suggestions based on your vault:",
  "intent": "SUGGESTIONS",
  "suggestions": [
    "Update 3 password(s) older than 90 days",
    "Generate a strong password",
    "Check for breached passwords",
    "Enable 2FA for better security"
  ],
  "timestamp": "2026-01-15T10:00:00",
  "aiPowered": false,
  "history": []
}
```

---

### POST `/api/ai/generate-password`
Generate a password with AI explanation.

**Query params:** `context=for my bank account` (optional)

**Response 200:**
```json
{
  "reply": "Here's a strong 20-character password for your bank account...",
  "intent": "GENERATE_PASSWORD",
  "generatedPassword": "Xk9#mP2@nQ5!vR7$wZ3&",
  "suggestions": ["Save this password to your vault", "Generate another password"],
  "timestamp": "2026-01-15T10:00:00",
  "aiPowered": false,
  "history": []
}
```

---

### GET `/api/ai/security-insights`
Get AI-generated security insights for the vault.

**Response 200:**
```json
{
  "overallAssessment": "Your vault security is GOOD. Keep up the good work!",
  "securityScore": 85,
  "aiPowered": false,
  "insights": [
    {
      "severity": "MEDIUM",
      "title": "Outdated Passwords",
      "description": "3 passwords haven't been changed in over 90 days.",
      "affectedCount": 3
    },
    {
      "severity": "INFO",
      "title": "Enable Breach Monitoring",
      "description": "Run a breach scan to check for compromised passwords.",
      "affectedCount": 25
    }
  ]
}
```
> Severity values: `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO`

---

### DELETE `/api/ai/session`
Clear chat history.

**Response 200:**
```json
{ "message": "Chat history cleared successfully" }
```

---

## 22. Feature 42 — Team/Family Vault Sharing

**Base path:** `/api/teams`  
**Auth required:** Yes  
**Roles:** `OWNER` > `ADMIN` > `MEMBER` > `VIEWER`

### POST `/api/teams`
Create a new team (creator becomes OWNER).

**Request:**
```json
{
  "name": "Dev Team",
  "description": "Development team shared vault"
}
```

**Response 201:**
```json
{
  "id": 1,
  "name": "Dev Team",
  "description": "Development team shared vault",
  "createdByUsername": "johndoe",
  "createdAt": "2026-01-01T10:00:00",
  "memberCount": 1,
  "sharedEntryCount": 0,
  "currentUserRole": "OWNER",
  "members": [
    {
      "memberId": 1,
      "userId": 1,
      "username": "johndoe",
      "email": "johndoe@example.com",
      "role": "OWNER",
      "joinedAt": "2026-01-01T10:00:00"
    }
  ]
}
```

---

### GET `/api/teams`
List all teams the user belongs to.

**Response 200:** Array of team objects.

---

### GET `/api/teams/{teamId}`
Get a specific team with members.

**Response 200:** Team object with members list.

---

### DELETE `/api/teams/{teamId}`
Delete a team (OWNER only).

**Response 200:**
```json
{ "message": "Team deleted successfully" }
```

---

### GET `/api/teams/{teamId}/members`
List all team members.

**Response 200:** Array of member objects.

---

### POST `/api/teams/{teamId}/members`
Invite a member by email (OWNER/ADMIN only).

**Request:**
```json
{
  "email": "colleague@example.com",
  "role": "MEMBER"
}
```
> Roles: `ADMIN`, `MEMBER`, `VIEWER`  
> If the user is already registered, they are added immediately. Otherwise, an invitation email is sent.

**Response 201:** Member object.

---

### PUT `/api/teams/{teamId}/members/{userId}/role`
Change a member's role (OWNER only).

**Query params:** `role=ADMIN`

**Response 200:** Updated member object.

---

### DELETE `/api/teams/{teamId}/members/{userId}`
Remove a member (OWNER/ADMIN can remove others; members can remove themselves).

**Response 200:**
```json
{ "message": "Member removed from team" }
```

---

### POST `/api/teams/{teamId}/share`
Share a vault entry with the team (MEMBER role or above).

**Query params:** `vaultEntryId=10`

**Response 201:** Vault entry response object.
> Highly sensitive entries cannot be shared.

---

### GET `/api/teams/{teamId}/vault`
Get all vault entries shared with the team.

**Response 200:** Array of vault entry objects.

---

### DELETE `/api/teams/{teamId}/vault/{vaultEntryId}`
Remove a vault entry from the team vault.

**Response 200:**
```json
{ "message": "Entry removed from team vault" }
```

---

### GET `/api/teams/{teamId}/activity`
Get team activity log.

**Response 200:**
```json
[
  {
    "action": "ENTRY_SHARED",
    "actorUsername": "johndoe",
    "description": "Shared 'GitHub' with the team",
    "timestamp": "2026-01-15T10:00:00"
  }
]
```

---

## 23. Error Responses

All errors follow this format:

```json
{
  "timestamp": "2026-01-15T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found",
  "path": "/api/users/profile"
}
```

| HTTP Status | When |
|---|---|
| `400 Bad Request` | Validation error, illegal argument |
| `401 Unauthorized` | Missing/invalid JWT, authentication failure |
| `403 Forbidden` | Access denied (e.g., not team member) |
| `404 Not Found` | Resource not found |
| `409 Conflict` | Duplicate resource |
| `429 Too Many Requests` | Rate limit exceeded |
| `500 Internal Server Error` | Unexpected server error |

---

## 24. Authentication Flow

### Standard Login Flow
```
1. POST /api/auth/login
   → If requires2FA: false → store accessToken + refreshToken
   → If requires2FA: true → proceed to step 2

2. POST /api/auth/verify-otp?username=X&code=Y
   → store accessToken + refreshToken

3. Include in all requests:
   Authorization: Bearer <accessToken>

4. When accessToken expires (15 min):
   POST /api/auth/refresh-token { refreshToken: "..." }
   → get new accessToken + refreshToken
```

### Token Storage (Frontend)
- Store `accessToken` in memory (not localStorage for security)
- Store `refreshToken` in httpOnly cookie or secure storage
- Access token expires in **15 minutes** (`expiresIn: 900000` ms)
- Refresh token expires in **7 days**

### CAPTCHA
- After 3+ failed login attempts, `captchaToken` is required in the login request
- Check if CAPTCHA is needed: the login response will include `message: "CAPTCHA verification required"`

### Duress Mode
- If logged in with duress password, the vault shows dummy entries
- All write operations are blocked
- The JWT has a `duress: true` claim (not visible to frontend, handled server-side)

---

## Quick Reference — All Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Register |
| POST | `/api/auth/verify-email` | No | Verify email OTP |
| POST | `/api/auth/login` | No | Login |
| POST | `/api/auth/verify-otp` | No | Verify 2FA OTP |
| POST | `/api/auth/refresh-token` | No | Refresh JWT |
| POST | `/api/auth/logout` | Yes | Logout |
| GET | `/api/auth/validate-token` | Yes | Validate token |
| POST | `/api/auth/verify-master-password` | Yes | Re-auth |
| GET | `/api/auth/security-questions/{username}` | No | Get security questions |
| POST | `/api/auth/reset-password` | No | Reset password |
| POST | `/api/auth/forgot-password` | No | Forgot password |
| POST | `/api/auth/duress-login` | No | Duress login |
| POST | `/api/auth/set-duress-password` | Yes | Set duress password |
| GET | `/api/auth/password-hint/{username}` | No | Get hint |
| PUT | `/api/auth/password-hint` | Yes | Set hint |
| GET | `/api/2fa/status` | Yes | 2FA status |
| POST | `/api/2fa/setup` | Yes | Setup 2FA |
| POST | `/api/2fa/verify-setup` | Yes | Enable 2FA |
| POST | `/api/2fa/disable` | Yes | Disable 2FA |
| GET | `/api/2fa/backup-codes` | Yes | Get backup codes |
| POST | `/api/2fa/regenerate-codes` | Yes | Regenerate codes |
| POST | `/api/vault` | Yes | Create entry |
| GET | `/api/vault` | Yes | List entries |
| GET | `/api/vault/search` | Yes | Search entries |
| GET | `/api/vault/{id}` | Yes | Get entry |
| PUT | `/api/vault/{id}` | Yes | Update entry |
| DELETE | `/api/vault/{id}` | Yes | Trash entry |
| PUT | `/api/vault/{id}/favorite` | Yes | Toggle favorite |
| PUT | `/api/vault/entries/{id}/sensitive` | Yes | Toggle sensitive |
| POST | `/api/vault/{id}/sensitive-view` | Yes | View sensitive |
| POST | `/api/vault/entries/{id}/view-password` | Yes | View password |
| GET | `/api/vault/entries/{id}/history` | Yes | Password history |
| GET | `/api/vault/trash` | Yes | List trash |
| POST | `/api/vault/trash/{id}/restore` | Yes | Restore entry |
| DELETE | `/api/vault/trash/{id}` | Yes | Permanent delete |
| DELETE | `/api/vault/trash/empty` | Yes | Empty trash |
| GET | `/api/categories` | Yes | List categories |
| POST | `/api/categories` | Yes | Create category |
| PUT | `/api/categories/{id}` | Yes | Update category |
| DELETE | `/api/categories/{id}` | Yes | Delete category |
| GET | `/api/folders` | Yes | List folders |
| POST | `/api/folders` | Yes | Create folder |
| PUT | `/api/folders/{id}` | Yes | Rename folder |
| DELETE | `/api/folders/{id}` | Yes | Delete folder |
| POST | `/api/generator/generate` | No | Generate password |
| POST | `/api/generator/strength` | No | Check strength |
| GET | `/api/users/profile` | Yes | Get profile |
| PUT | `/api/users/profile` | Yes | Update profile |
| PUT | `/api/users/change-password` | Yes | Change password |
| DELETE | `/api/users/account` | Yes | Delete account |
| GET | `/api/settings` | Yes | Get settings |
| PUT | `/api/settings` | Yes | Update settings |
| GET | `/api/sessions` | Yes | List sessions |
| DELETE | `/api/sessions/{id}` | Yes | Terminate session |
| GET | `/api/security/audit-logs` | Yes | Audit logs |
| GET | `/api/security/login-history` | Yes | Login history |
| GET | `/api/security/alerts` | Yes | Security alerts |
| POST | `/api/security/breach-scan` | Yes | Trigger breach scan |
| GET | `/api/security/breach-status` | Yes | Breach status |
| GET | `/api/backup/export` | Yes | Export vault |
| POST | `/api/backup/import` | Yes | Import vault |
| POST | `/api/backup/import-external` | Yes | Import from 3rd party |
| GET | `/api/notifications` | Yes | List notifications |
| PUT | `/api/notifications/mark-all-read` | Yes | Mark all read |
| GET | `/api/dashboard/security-score` | Yes | Security score |
| GET | `/api/dashboard/password-health` | Yes | Password health |
| GET | `/api/dashboard/reused-passwords` | Yes | Reused passwords |
| GET | `/api/dashboard/password-age` | Yes | Password age |
| GET | `/api/dashboard/trends` | Yes | Security trends |
| POST | `/api/shares` | Yes | Create share |
| GET | `/api/shares/{token}` | **No** | Get shared password |
| GET | `/api/shares` | Yes | List shares |
| DELETE | `/api/shares/{id}` | Yes | Revoke share |
| GET | `/api/timeline` | Yes | Full timeline |
| GET | `/api/timeline/summary` | Yes | Timeline summary |
| GET | `/api/health` | **No** | Health check |
| POST | `/api/autofill/suggestions` | Yes | Autofill suggestions |
| GET | `/api/autofill/trusted-domains` | Yes | Trusted domains |
| POST | `/api/autofill/log-usage` | Yes | Log autofill usage |
| GET | `/api/expiry/status` | Yes | Expiry status |
| GET | `/api/expiry/expiring-soon` | Yes | Expiring soon |
| GET | `/api/expiry/policy` | Yes | Expiry policy |
| PUT | `/api/expiry/policy` | Yes | Update policy |
| POST | `/api/expiry/snooze/{id}` | Yes | Snooze reminder |
| POST | `/api/emergency/contacts` | Yes | Add contact |
| GET | `/api/emergency/contacts` | Yes | List contacts |
| PUT | `/api/emergency/contacts/{id}` | Yes | Update contact |
| DELETE | `/api/emergency/contacts/{id}` | Yes | Remove contact |
| POST | `/api/emergency/request-access` | Yes | Request access |
| GET | `/api/emergency/requests` | Yes | List requests |
| POST | `/api/emergency/grant/{id}` | Yes | Grant access |
| POST | `/api/emergency/deny/{id}` | Yes | Deny access |
| GET | `/api/emergency/vault/{token}` | **No** | Access vault |
| POST | `/api/files/upload` | Yes | Upload file |
| GET | `/api/files` | Yes | List files |
| GET | `/api/files/search` | Yes | Search files |
| GET | `/api/files/{id}/download` | Yes | Download file |
| DELETE | `/api/files/{id}` | Yes | Delete file |
| POST | `/api/files/folders` | Yes | Create folder |
| DELETE | `/api/files/folders/{id}` | Yes | Delete folder |
| POST | `/api/ai/chat` | Yes | AI chat |
| GET | `/api/ai/suggestions` | Yes | AI suggestions |
| POST | `/api/ai/generate-password` | Yes | AI generate password |
| GET | `/api/ai/security-insights` | Yes | AI insights |
| DELETE | `/api/ai/session` | Yes | Clear chat |
| POST | `/api/teams` | Yes | Create team |
| GET | `/api/teams` | Yes | List teams |
| GET | `/api/teams/{id}` | Yes | Get team |
| DELETE | `/api/teams/{id}` | Yes | Delete team |
| GET | `/api/teams/{id}/members` | Yes | List members |
| POST | `/api/teams/{id}/members` | Yes | Invite member |
| PUT | `/api/teams/{id}/members/{uid}/role` | Yes | Change role |
| DELETE | `/api/teams/{id}/members/{uid}` | Yes | Remove member |
| POST | `/api/teams/{id}/share` | Yes | Share entry |
| GET | `/api/teams/{id}/vault` | Yes | Team vault |
| DELETE | `/api/teams/{id}/vault/{eid}` | Yes | Unshare entry |
| GET | `/api/teams/{id}/activity` | Yes | Team activity |
