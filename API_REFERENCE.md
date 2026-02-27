# Rev-PasswordManager — Complete API Reference

**Base URL:** `http://localhost:8080`

> [!IMPORTANT]
> **Authorization:** Most endpoints require a Bearer JWT token in the `Authorization` header.
> Public endpoints (no token needed): `/api/auth/**`, `/api/generator/**`, `/api/health/**`
>
> ```
> Authorization: Bearer <your_jwt_token>
> ```

---

## Table of Contents

1. [Auth Controller](#1-auth-controller) — 12 endpoints (🔓 Public)
2. [Backup Controller](#2-backup-controller) — 3 endpoints (🔒 Auth Required)
3. [Category Controller](#3-category-controller) — 6 endpoints (🔒 Auth Required)
4. [Folder Controller](#4-folder-controller) — 7 endpoints (🔒 Auth Required)
5. [Health Controller](#5-health-controller) — 1 endpoint (🔓 Public)
6. [Notification Controller](#6-notification-controller) — 5 endpoints (🔒 Auth Required)
7. [Password Generator Controller](#7-password-generator-controller) — 5 endpoints (🔓 Public)
8. [Security Controller](#8-security-controller) — 6 endpoints (🔒 Auth Required)
9. [Session Controller](#9-session-controller) — 4 endpoints (🔒 Auth Required)
10. [Two-Factor Controller](#10-two-factor-controller) — 5 endpoints (🔒 Auth Required)
11. [User Controller](#11-user-controller) — 7 endpoints (🔒 Auth Required)
12. [User Settings Controller](#12-user-settings-controller) — 2 endpoints (🔒 Auth Required)
13. [Vault Controller](#13-vault-controller) — 19 endpoints (🔒 Auth Required)

---

## 1. Auth Controller

**Base Path:** `/api/auth` | **Auth:** 🔓 Public (no token needed)

---

### 1.1 POST `/api/auth/register`

**Description:** Register a new user

**Request Body:**
```json
{
  "email": "user@example.com",
  "username": "johndoe",
  "masterPassword": "MySecurePass123!",
  "securityQuestions": [
    { "question": "What is your pet's name?", "answer": "Buddy" },
    { "question": "What city were you born in?", "answer": "NYC" },
    { "question": "What is your favorite book?", "answer": "1984" }
  ]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| email | String | ✅ | Must be valid email |
| username | String | ✅ | 3–50 characters |
| masterPassword | String | ✅ | Min 12 characters |
| securityQuestions | Array | ✅ | Exactly 3 items |
| securityQuestions[].question | String | ✅ | Not blank |
| securityQuestions[].answer | String | ✅ | Not blank |

**Response:** `201 Created`
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "is2faEnabled": false,
  "createdAt": "2026-02-15T10:30:00"
}
```

---

### 1.2 POST `/api/auth/login`

**Description:** Login with username and master password

**Request Body:**
```json
{
  "username": "johndoe",
  "masterPassword": "MySecurePass123!"
}
```

| Field | Type | Required |
|-------|------|----------|
| username | String | ✅ |
| masterPassword | String | ✅ |

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "johndoe",
  "expiresIn": 3600,
  "requires2FA": false
}
```

> [!NOTE]
> If `requires2FA` is `true`, you must call `/api/auth/verify-otp` with the OTP code to get the actual access token.

---

### 1.3 POST `/api/auth/refresh-token`

**Description:** Refresh an expired access token

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "new-access-token...",
  "refreshToken": "new-refresh-token...",
  "tokenType": "Bearer",
  "username": "johndoe",
  "expiresIn": 3600,
  "requires2FA": false
}
```

---

### 1.4 POST `/api/auth/logout`

**Description:** Logout (invalidate token)

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Request Body:** None

**Response:** `204 No Content`

---

### 1.5 GET `/api/auth/security-questions/{username}`

**Description:** Get security questions for a user (for account recovery)

**Path Params:** `username` — The user's username

**Request Body:** None

**Response:** `200 OK`
```json
[
  { "question": "What is your pet's name?", "answer": "" },
  { "question": "What city were you born in?", "answer": "" },
  { "question": "What is your favorite book?", "answer": "" }
]
```

---

### 1.6 POST `/api/auth/reset-password`

**Description:** Reset password using security questions

**Request Body:**
```json
{
  "username": "johndoe",
  "securityAnswers": [
    { "question": "What is your pet's name?", "answer": "Buddy" },
    { "question": "What city were you born in?", "answer": "NYC" },
    { "question": "What is your favorite book?", "answer": "1984" }
  ],
  "newMasterPassword": "NewSecurePass456!"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| username | String | ✅ | Not blank |
| securityAnswers | Array | ✅ | Exactly 3 items |
| newMasterPassword | String | ✅ | Min 12 characters |

**Response:** `200 OK`
```json
"Password changed successfully"
```

---

### 1.7 POST `/api/auth/verify-otp`

**Description:** Verify OTP code during 2FA login

**Query Params:**
| Param | Type | Required |
|-------|------|----------|
| username | String | ✅ |
| code | String | ✅ |

**Example:** `POST /api/auth/verify-otp?username=johndoe&code=123456`

**Request Body:** None

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "username": "johndoe",
  "expiresIn": 3600,
  "requires2FA": false
}
```

---

### 1.8 POST `/api/auth/send-otp`

**Description:** Send OTP code to user's email

**Query Params:**
| Param | Type | Required |
|-------|------|----------|
| username | String | ✅ |

**Example:** `POST /api/auth/send-otp?username=johndoe`

**Request Body:** None

**Response:** `200 OK`
```json
"OTP sent to your email."
```

---

### 1.9 POST `/api/auth/duress-login`

**Description:** Login with duress password (fake login under threat)

**Request Body:**
```json
{
  "username": "johndoe",
  "masterPassword": "duress-password-here"
}
```

**Response (Success):** `200 OK`
```json
{
  "accessToken": "duress-mode-active",
  "refreshToken": null,
  "tokenType": "Bearer",
  "username": "johndoe",
  "expiresIn": 0,
  "requires2FA": false
}
```

**Response (Failure):** `401 Unauthorized`

---

### 1.10 POST `/api/auth/set-duress-password`

**Description:** Set a duress password

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "duressPassword": "my-panic-password"
}
```

**Response:** `200 OK`
```json
"Duress password set successfully"
```

---

### 1.11 GET `/api/auth/password-hint/{username}`

**Description:** Get the password hint for a user

**Path Params:** `username` — The user's username

**Request Body:** None

**Response:** `200 OK`
```json
{
  "hint": "My childhood pet name + year"
}
```

---

### 1.12 PUT `/api/auth/password-hint`

**Description:** Set/update password hint

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "hint": "My childhood pet name + year"
}
```

**Response:** `200 OK`
```json
"Password hint updated"
```

---

## 2. Backup Controller

**Base Path:** `/api/backup` | **Auth:** 🔒 Bearer Token Required

---

### 2.1 GET `/api/backup/export`

**Description:** Export all vault entries

**Query Params:**
| Param | Type | Default |
|-------|------|---------|
| format | String | `"JSON"` |

**Example:** `GET /api/backup/export?format=CSV`

**Request Body:** None

**Response:** `200 OK`
```json
{
  "fileName": "vault_export_20260215.json",
  "format": "JSON",
  "entryCount": 25,
  "encrypted": true,
  "data": "<exported vault data>",
  "exportedAt": "2026-02-15T10:30:00"
}
```

---

### 2.2 POST `/api/backup/import`

**Description:** Import vault entries from data

**Request Body:**
```json
{
  "data": "<vault entries JSON string>",
  "format": "JSON"
}
```

**Response:** `200 OK`
```json
{
  "totalProcessed": 25,
  "successCount": 23,
  "failCount": 2,
  "message": "Import completed with 2 errors"
}
```

---

### 2.3 POST `/api/backup/import/external`

**Description:** Import from third-party password manager

**Request Body:**
```json
{
  "source": "LastPass",
  "data": "<exported CSV/JSON data from third party>"
}
```

**Response:** `200 OK`
```json
{
  "totalProcessed": 50,
  "successCount": 48,
  "failCount": 2,
  "message": "Import from LastPass completed"
}
```

---

## 3. Category Controller

**Base Path:** `/api/categories` | **Auth:** 🔒 Bearer Token Required

---

### 3.1 GET `/api/categories`

**Description:** Get all categories for the authenticated user

**Request Body:** None

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Social Media",
    "icon": "social",
    "isDefault": true,
    "createdAt": "2026-02-15T10:30:00",
    "entryCount": 5
  }
]
```

---

### 3.2 GET `/api/categories/{id}`

**Description:** Get a specific category by ID

**Path Params:** `id` — Category ID (Long)

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "Social Media",
  "icon": "social",
  "isDefault": true,
  "createdAt": "2026-02-15T10:30:00",
  "entryCount": 5
}
```

---

### 3.3 POST `/api/categories`

**Description:** Create a new custom category

**Request Body:**
```json
{
  "name": "Banking",
  "icon": "bank"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| name | String | ✅ | 1–100 characters |
| icon | String | ❌ | Max 50 characters |

**Response:** `201 Created`
```json
{
  "id": 10,
  "name": "Banking",
  "icon": "bank",
  "isDefault": false,
  "createdAt": "2026-02-15T10:30:00",
  "entryCount": 0
}
```

---

### 3.4 PUT `/api/categories/{id}`

**Description:** Update an existing category

**Path Params:** `id` — Category ID (Long)

**Request Body:**
```json
{
  "name": "Updated Banking",
  "icon": "bank-updated"
}
```

**Response:** `200 OK` — Same structure as GET `/api/categories/{id}`

---

### 3.5 DELETE `/api/categories/{id}`

**Description:** Delete a category (entries will have category set to null)

**Path Params:** `id` — Category ID (Long)

**Response:** `204 No Content`

---

### 3.6 GET `/api/categories/{id}/entries`

**Description:** Get vault entries in a category (placeholder)

**Path Params:** `id` — Category ID (Long)

**Response:** `200 OK`
```json
"This endpoint will return vault entries when the Vault feature is implemented"
```

---

## 4. Folder Controller

**Base Path:** `/api/folders` | **Auth:** 🔒 Bearer Token Required

---

### 4.1 GET `/api/folders`

**Description:** Get all folders for the authenticated user

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "Work",
    "parentFolderId": null,
    "subfolders": [
      {
        "id": 2,
        "name": "Engineering",
        "parentFolderId": 1,
        "subfolders": [],
        "createdAt": "2026-02-15T10:30:00",
        "updatedAt": "2026-02-15T10:30:00"
      }
    ],
    "createdAt": "2026-02-15T10:30:00",
    "updatedAt": "2026-02-15T10:30:00"
  }
]
```

---

### 4.2 GET `/api/folders/{id}`

**Description:** Get a specific folder by ID

**Response:** `200 OK` — Same structure as a single folder object above

---

### 4.3 POST `/api/folders`

**Description:** Create a new folder

**Query Params:**
| Param | Type | Required |
|-------|------|----------|
| name | String | ✅ |
| parentFolderId | Long | ❌ |

**Example:** `POST /api/folders?name=Work&parentFolderId=1`

**Response:** `201 Created` — FolderDTO object

---

### 4.4 PUT `/api/folders/{id}`

**Description:** Rename a folder

**Query Params:**
| Param | Type | Required |
|-------|------|----------|
| name | String | ✅ |

**Example:** `PUT /api/folders/1?name=Updated+Work`

**Response:** `200 OK` — FolderDTO object

---

### 4.5 PUT `/api/folders/{id}/move`

**Description:** Move a folder to a new parent

**Query Params:**
| Param | Type | Required | Notes |
|-------|------|----------|-------|
| parentId | Long | ❌ | null = move to root |

**Example:** `PUT /api/folders/2/move?parentId=5`

**Response:** `200 OK` — FolderDTO object

---

### 4.6 DELETE `/api/folders/{id}`

**Description:** Delete a folder

**Response:** `204 No Content`

---

### 4.7 GET `/api/folders/{id}/entries`

**Description:** Get vault entries in a folder

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Gmail",
    "username": "john@gmail.com",
    "websiteUrl": "https://gmail.com",
    "categoryId": 1,
    "categoryName": "Email",
    "folderId": 1,
    "folderName": "Work",
    "isFavorite": false,
    "createdAt": "2026-02-15T10:30:00",
    "updatedAt": "2026-02-15T10:30:00"
  }
]
```

---

## 5. Health Controller

**Base Path:** `/api/health` | **Auth:** 🔓 Public (no token needed)

---

### 5.1 GET `/api/health`

**Description:** System health check

**Response:** `200 OK`
```json
{
  "status": "UP",
  "version": "1.0.0",
  "timestamp": "2026-02-15T10:30:00",
  "components": {
    "database": { "status": "UP", "details": "H2 connected" },
    "memory": { "status": "UP", "details": "85% used" },
    "diskSpace": { "status": "UP", "details": "50GB free" }
  }
}
```

---

## 6. Notification Controller

**Base Path:** `/api/notifications` | **Auth:** 🔒 Bearer Token Required

---

### 6.1 GET `/api/notifications`

**Description:** Get all notifications

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "notificationType": "SECURITY_ALERT",
    "title": "New login detected",
    "message": "A new login from Chrome on Windows",
    "isRead": false,
    "createdAt": "2026-02-15T10:30:00"
  }
]
```

---

### 6.2 GET `/api/notifications/unread-count`

**Description:** Get unread notification count

**Response:** `200 OK`
```json
{
  "unreadCount": 5
}
```

---

### 6.3 PUT `/api/notifications/{id}/read`

**Description:** Mark a notification as read

**Path Params:** `id` — Notification ID (Long)

**Response:** `200 OK`
```json
"Notification marked as read"
```

---

### 6.4 PUT `/api/notifications/mark-all-read`

**Description:** Mark all notifications as read

**Response:** `200 OK`
```json
"All notifications marked as read"
```

---

### 6.5 DELETE `/api/notifications/{id}`

**Description:** Delete a notification

**Response:** `200 OK`
```json
"Notification deleted"
```

---

## 7. Password Generator Controller

**Base Path:** `/api/generator` | **Auth:** 🔓 Public (no token needed)

---

### 7.1 POST `/api/generator/generate`

**Description:** Generate a single password

**Request Body:**
```json
{
  "length": 20,
  "includeUppercase": true,
  "includeLowercase": true,
  "includeNumbers": true,
  "includeSpecial": true,
  "excludeSimilar": false,
  "excludeAmbiguous": false,
  "count": 1
}
```

| Field | Type | Default |
|-------|------|---------|
| length | int | 16 |
| includeUppercase | boolean | true |
| includeLowercase | boolean | true |
| includeNumbers | boolean | true |
| includeSpecial | boolean | true |
| excludeSimilar | boolean | false |
| excludeAmbiguous | boolean | false |
| count | int | 1 |

**Response:** `200 OK`
```json
{
  "password": "xK9#mP2$nL7@wQ4!"
}
```

---

### 7.2 POST `/api/generator/strength`

**Description:** Check password strength

**Request Body:**
```json
{
  "password": "MyPassword123!"
}
```

**Response:** `200 OK`
```json
{
  "score": 75,
  "label": "Good",
  "feedback": ["Consider adding more special characters"]
}
```

---

### 7.3 POST `/api/generator/generate-multiple`

**Description:** Generate multiple passwords

**Request Body:** Same as `/generate` (the `count` field controls how many)

**Response:** `200 OK`
```json
{
  "passwords": [
    "xK9#mP2$nL7@wQ4!",
    "aB3%hT8*rU5&yI1!",
    "fG6@jN4#dW9$eR7!"
  ]
}
```

---

### 7.4 POST `/api/generator/validate`

**Description:** Validate a password (returns strength analysis)

**Request Body:**
```json
{
  "password": "TestPassword123"
}
```

**Response:** `200 OK` — Same as `/strength` response

---

### 7.5 GET `/api/generator/default-settings`

**Description:** Get default password generator settings

**Response:** `200 OK`
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

---

## 8. Security Controller

**Base Path:** `/api/security` | **Auth:** 🔒 Bearer Token Required

---

### 8.1 GET `/api/security/audit-logs`

**Description:** Get audit logs for the authenticated user

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "action": "LOGIN",
    "details": "Successful login from Chrome",
    "ipAddress": "192.168.1.1",
    "timestamp": "2026-02-15T10:30:00"
  }
]
```

---

### 8.2 GET `/api/security/login-history`

**Description:** Get login history

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "ipAddress": "192.168.1.1",
    "deviceInfo": "Chrome on Windows",
    "location": "New York, US",
    "successful": true,
    "failureReason": null,
    "timestamp": "2026-02-15T10:30:00"
  }
]
```

---

### 8.3 GET `/api/security/alerts`

**Description:** Get security alerts

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "alertType": "SUSPICIOUS_LOGIN",
    "title": "Unusual login detected",
    "message": "Login from a new device in unknown location",
    "severity": "HIGH",
    "isRead": false,
    "createdAt": "2026-02-15T10:30:00"
  }
]
```

---

### 8.4 PUT `/api/security/alerts/{id}/read`

**Description:** Mark a security alert as read

**Path Params:** `id` — Alert ID (Long)

**Response:** `200 OK`
```json
"Alert marked as read"
```

---

### 8.5 DELETE `/api/security/alerts/{id}`

**Description:** Delete a security alert

**Path Params:** `id` — Alert ID (Long)

**Response:** `200 OK`
```json
"Alert deleted"
```

---

### 8.6 GET `/api/security/audit-report`

**Description:** Get comprehensive security audit report

**Response:** `200 OK`
```json
{
  "totalEntries": 50,
  "weakCount": 5,
  "reusedCount": 3,
  "oldCount": 8,
  "securityScore": 72,
  "recommendations": [
    "Update 5 weak passwords",
    "Change 3 reused passwords"
  ],
  "weakPasswords": [
    { "id": 1, "title": "Old Gmail", "websiteUrl": "gmail.com", "issue": "Too short" }
  ],
  "reusedPasswords": [
    { "id": 2, "title": "Facebook", "websiteUrl": "facebook.com", "issue": "Same as Gmail" }
  ],
  "oldPasswords": [
    { "id": 3, "title": "Twitter", "websiteUrl": "twitter.com", "issue": "Not changed in 180 days" }
  ]
}
```

---

## 9. Session Controller

**Base Path:** `/api/sessions` | **Auth:** 🔒 Bearer Token Required

---

### 9.1 GET `/api/sessions`

**Description:** Get all active sessions

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "ipAddress": "192.168.1.1",
    "deviceInfo": "Chrome on Windows",
    "location": "New York, US",
    "isActive": true,
    "createdAt": "2026-02-15T10:30:00",
    "lastAccessedAt": "2026-02-15T11:00:00",
    "expiresAt": "2026-02-16T10:30:00"
  }
]
```

---

### 9.2 GET `/api/sessions/current`

**Description:** Get current session details

**Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response:** `200 OK` — Single SessionResponse object (same fields as above)

---

### 9.3 DELETE `/api/sessions/{sessionId}`

**Description:** Terminate a specific session

**Path Params:** `sessionId` — Session ID (Long)

**Response:** `204 No Content`

---

### 9.4 DELETE `/api/sessions`

**Description:** Terminate all sessions (logs out everywhere)

**Response:** `204 No Content`

---

## 10. Two-Factor Controller

**Base Path:** `/api/2fa` | **Auth:** 🔒 Bearer Token Required

---

### 10.1 GET `/api/2fa/status`

**Description:** Get 2FA status for the authenticated user

**Response:** `200 OK`
```json
{
  "enabled": false
}
```

---

### 10.2 POST `/api/2fa/setup`

**Description:** Initialize 2FA setup (get QR code)

**Request Body:** None

**Response:** `200 OK`
```json
{
  "secretKey": "JBSWY3DPEHPK3PXP",
  "qrCodeUrl": "otpauth://totp/RevPM:johndoe?secret=JBSWY3DPEHPK3PXP"
}
```

---

### 10.3 POST `/api/2fa/verify-setup`

**Description:** Verify and enable 2FA

**Query Params:**
| Param | Type | Required |
|-------|------|----------|
| code | String | ✅ |

**Example:** `POST /api/2fa/verify-setup?code=123456`

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "2FA enabled successfully",
  "backupCodes": ["abc123", "def456", "ghi789"]
}
```

---

### 10.4 POST `/api/2fa/disable`

**Description:** Disable 2FA

**Request Body:** None

**Response:** `200 OK`
```json
{
  "message": "2FA disabled successfully"
}
```

---

### 10.5 GET `/api/2fa/backup-codes`

**Description:** Get backup recovery codes

**Response:** `200 OK`
```json
["abc123", "def456", "ghi789", "jkl012"]
```

---

## 11. User Controller

**Base Path:** `/api/users` | **Auth:** 🔒 Bearer Token Required

---

### 11.1 GET `/api/users/profile`

**Description:** Get current user's profile

**Response:** `200 OK`
```json
{
  "id": 1,
  "email": "user@example.com",
  "username": "johndoe",
  "is2faEnabled": false,
  "createdAt": "2026-02-15T10:30:00"
}
```

---

### 11.2 PUT `/api/users/change-password`

**Description:** Change master password

**Request Body:**
```json
{
  "oldPassword": "OldPassword123!",
  "newPassword": "NewSecurePass456!"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| oldPassword | String | ✅ | Not blank |
| newPassword | String | ✅ | Min 12 characters |

**Response:** `200 OK`
```json
"Password changed successfully"
```

---

### 11.3 DELETE `/api/users/account`

**Description:** Schedule account for deletion (30-day grace period)

**Request Body:**
```json
{
  "masterPassword": "MySecurePass123!",
  "confirmation": true
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| masterPassword | String | ✅ | Not blank |
| confirmation | boolean | ✅ | Must be `true` |

**Response:** `200 OK`
```json
"Account scheduled for deletion in 30 days. You can cancel this action by logging in and using the cancel endpoint."
```

---

### 11.4 POST `/api/users/account/cancel-deletion`

**Description:** Cancel a scheduled account deletion

**Request Body:** None

**Response:** `200 OK`
```json
"Account deletion cancelled."
```

---

### 11.5 GET `/api/users/security-questions`

**Description:** Get security questions for authenticated user

**Response:** `200 OK`
```json
[
  { "question": "What is your pet's name?", "answer": "" },
  { "question": "What city were you born in?", "answer": "" },
  { "question": "What is your favorite book?", "answer": "" }
]
```

> [!NOTE]
> The `answer` field is always empty for security — only question text is returned.

---

### 11.6 PUT `/api/users/security-questions`

**Description:** Update security questions

**Request Body:**
```json
{
  "masterPassword": "MySecurePass123!",
  "securityQuestions": [
    { "question": "New question 1?", "answer": "Answer 1" },
    { "question": "New question 2?", "answer": "Answer 2" },
    { "question": "New question 3?", "answer": "Answer 3" }
  ]
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| masterPassword | String | ✅ | Must match current password |
| securityQuestions | Array | ✅ | Exactly 3 items |

**Response:** `200 OK` (empty body)

---

### 11.7 GET `/api/users/activity-heatmap`

**Description:** Get access activity heatmap data

**Response:** `200 OK`
```json
{
  "accessByHour": [0, 0, 0, 0, 0, 1, 3, 8, 15, 12, 10, 7, 5, 6, 8, 10, 12, 14, 9, 6, 4, 2, 1, 0],
  "accessByDay": [5, 12, 15, 14, 13, 8, 3],
  "peakHour": 8,
  "peakDay": "Wednesday",
  "totalAccesses": 150,
  "period": "LAST_30_DAYS"
}
```

---

## 12. User Settings Controller

**Base Path:** `/api/settings` | **Auth:** 🔒 Bearer Token Required

---

### 12.1 GET `/api/settings`

**Description:** Get user settings

**Response:** `200 OK`
```json
{
  "theme": "dark",
  "language": "en",
  "autoLogoutMinutes": 15
}
```

---

### 12.2 PUT `/api/settings`

**Description:** Update user settings

**Request Body:**
```json
{
  "theme": "light",
  "language": "es",
  "autoLogoutMinutes": 30
}
```

| Field | Type | Required |
|-------|------|----------|
| theme | String | ❌ |
| language | String | ❌ |
| autoLogoutMinutes | Integer | ❌ |

**Response:** `200 OK` — Same structure as GET `/api/settings`

---

## 13. Vault Controller

**Base Path:** `/api/vault` | **Auth:** 🔒 Bearer Token Required

---

### 13.1 POST `/api/vault`

**Description:** Create a new vault entry

**Request Body:**
```json
{
  "title": "Gmail",
  "username": "john@gmail.com",
  "password": "SecurePass123!",
  "websiteUrl": "https://gmail.com",
  "notes": "Personal email account",
  "categoryId": 1,
  "folderId": 2,
  "isFavorite": true,
  "isHighlySensitive": false
}
```

| Field | Type | Required |
|-------|------|----------|
| title | String | ❌ |
| username | String | ❌ |
| password | String | ❌ |
| websiteUrl | String | ❌ |
| notes | String | ❌ |
| categoryId | Long | ❌ |
| folderId | Long | ❌ |
| isFavorite | Boolean | ❌ |
| isHighlySensitive | Boolean | ❌ |

**Response:** `201 Created`
```json
{
  "id": 1,
  "title": "Gmail",
  "username": "john@gmail.com",
  "websiteUrl": "https://gmail.com",
  "categoryId": 1,
  "categoryName": "Email",
  "folderId": 2,
  "folderName": "Work",
  "isFavorite": true,
  "createdAt": "2026-02-15T10:30:00",
  "updatedAt": "2026-02-15T10:30:00"
}
```

---

### 13.2 GET `/api/vault`

**Description:** Get all vault entries

**Response:** `200 OK` — Array of VaultEntryResponse objects

---

### 13.3 GET `/api/vault/search`

**Description:** Search/filter vault entries

**Query Params:**
| Param | Type | Required | Default |
|-------|------|----------|---------|
| keyword | String | ❌ | — |
| categoryId | Long | ❌ | — |
| folderId | Long | ❌ | — |
| isFavorite | Boolean | ❌ | — |
| isHighlySensitive | Boolean | ❌ | — |
| sortBy | String | ❌ | `"title"` |
| sortDir | String | ❌ | `"asc"` |

**Example:** `GET /api/vault/search?keyword=gmail&isFavorite=true&sortBy=title&sortDir=desc`

**Response:** `200 OK` — Array of VaultEntryResponse objects

---

### 13.4 GET `/api/vault/{id}`

**Description:** Get vault entry details (with decrypted data)

**Response:** `200 OK`
```json
{
  "id": 1,
  "title": "Gmail",
  "username": "john@gmail.com",
  "password": "decrypted-password-here",
  "websiteUrl": "https://gmail.com",
  "notes": "Decrypted notes here",
  "categoryId": 1,
  "categoryName": "Email",
  "folderId": 2,
  "folderName": "Work",
  "isFavorite": true,
  "isHighlySensitive": false,
  "requiresSensitiveAuth": false,
  "createdAt": "2026-02-15T10:30:00",
  "updatedAt": "2026-02-15T10:30:00"
}
```

---

### 13.5 PUT `/api/vault/{id}`

**Description:** Update a vault entry

**Request Body:** Same as POST `/api/vault`

**Response:** `200 OK` — VaultEntryResponse object

---

### 13.6 PUT `/api/vault/{id}/favorite`

**Description:** Toggle favorite status

**Request Body:** None

**Response:** `200 OK` — VaultEntryResponse object with updated `isFavorite`

---

### 13.7 GET `/api/vault/favorites`

**Description:** Get all favorite entries

**Response:** `200 OK` — Array of VaultEntryResponse objects

---

### 13.8 POST `/api/vault/entries/bulk-delete`

**Description:** Bulk delete vault entries (moves to trash)

**Request Body:**
```json
[1, 2, 3, 5]
```

**Response:** `204 No Content`

---

### 13.9 POST `/api/vault/entries/{id}/view-password`

**Description:** View a specific entry's password

**Path Params:** `id` — Entry ID (Long)

**Request Body:** None

**Response:** `200 OK`
```json
{
  "password": "decrypted-password-here"
}
```

---

### 13.10 DELETE `/api/vault/{id}`

**Description:** Delete a vault entry (moves to trash)

**Response:** `204 No Content`

---

### 13.11 POST `/api/vault/{id}/sensitive-view`

**Description:** Access a highly sensitive entry (requires re-authentication)

**Request Body:**
```json
{
  "masterPassword": "MySecurePass123!",
  "otpToken": "123456"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| masterPassword | String | ✅ | Not blank |
| otpToken | String | ❌ | Required only if 2FA is enabled |

**Response:** `200 OK` — VaultEntryDetailResponse (same as GET `/api/vault/{id}`)

---

### 13.12 GET `/api/vault/entries/{id}/history`

**Description:** Get password change history for an entry

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "password": "******",
    "changedAt": "2026-02-15T10:30:00"
  },
  {
    "id": 2,
    "password": "******",
    "changedAt": "2026-01-15T10:30:00"
  }
]
```

---

### 13.13 GET `/api/vault/trash`

**Description:** Get all trashed entries

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "title": "Old Gmail",
    "websiteUrl": "https://gmail.com",
    "categoryName": "Email",
    "folderName": "Work",
    "deletedAt": "2026-02-10T10:30:00",
    "expiresAt": "2026-03-12T10:30:00",
    "daysRemaining": 25
  }
]
```

---

### 13.14 GET `/api/vault/trash/count`

**Description:** Get number of items in trash

**Response:** `200 OK`
```json
{
  "count": 3
}
```

---

### 13.15 POST `/api/vault/trash/{id}/restore`

**Description:** Restore a trashed entry

**Path Params:** `id` — Trash entry ID (Long)

**Response:** `200 OK` — TrashEntryResponse object

---

### 13.16 POST `/api/vault/trash/restore-all`

**Description:** Restore all trashed entries

**Response:** `204 No Content`

---

### 13.17 DELETE `/api/vault/trash/{id}`

**Description:** Permanently delete a trashed entry

**Path Params:** `id` — Trash entry ID (Long)

**Response:** `204 No Content`

---

### 13.18 DELETE `/api/vault/trash/empty`

**Description:** Permanently delete all trashed entries

**Response:** `204 No Content`
