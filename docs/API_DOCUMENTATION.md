# 📡 API Documentation - Password Manager

## API Endpoints Estimate

Based on the feature requirements, this application requires approximately **100+ REST API endpoints** organized across 12 controllers.

---

## 📊 API Count Summary

| Controller | Endpoints | Category |
|------------|-----------|----------|
| Auth Controller | 15 | Authentication |
| User Controller | 9 | User Management |
| Vault Controller | 18 | Password Vault |
| Category Controller | 6 | Organization |
| Folder Controller | 7 | Organization |
| Password Generator Controller | 5 | Utilities |
| Security Controller | 10 | Security & Audit |
| Two-Factor Auth Controller | 6 | 2FA |
| Session Controller | 5 | Sessions |
| Backup Controller | 8 | Import/Export |
| Notification Controller | 5 | Alerts |
| Health Controller | 3 | System |
| **TOTAL** | **97** | |

---

## 🔐 1. Authentication Controller (`/api/auth`)
**Endpoints: 15**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | POST | `/api/auth/register` | Create new user account |
| 2 | POST | `/api/auth/login` | User login with credentials |
| 3 | POST | `/api/auth/logout` | Logout and invalidate session |
| 4 | POST | `/api/auth/refresh-token` | Refresh JWT access token |
| 5 | POST | `/api/auth/verify-otp` | Verify OTP for 2FA login |
| 6 | POST | `/api/auth/forgot-password` | Initiate password recovery |
| 7 | POST | `/api/auth/verify-security-questions` | Verify security question answers |
| 8 | POST | `/api/auth/reset-password` | Reset password after verification |
| 9 | POST | `/api/auth/verify-captcha` | Verify CAPTCHA response |
| 10 | POST | `/api/auth/duress-login` | Login with duress password (fake vault) 🆕 |
| 11 | GET | `/api/auth/validate-token` | Validate current JWT token |
| 12 | POST | `/api/auth/verify-master-password` | Re-verify master password for sensitive ops |
| 13 | GET | `/api/auth/password-hint` | Get password hint for email 🆕 |
| 14 | POST | `/api/auth/send-otp` | Send OTP for verification 🆕 |
| 15 | POST | `/api/auth/resend-otp` | Resend OTP code 🆕 |

---

## 👤 2. User Controller (`/api/users`)
**Endpoints: 9**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/users/profile` | Get current user profile |
| 2 | PUT | `/api/users/profile` | Update user profile |
| 3 | PUT | `/api/users/change-password` | Change master password |
| 4 | GET | `/api/users/dashboard` | Get dashboard summary stats |
| 5 | GET | `/api/users/security-questions` | Get user's security questions (not answers) |
| 6 | PUT | `/api/users/security-questions` | Update security question answers |
| 7 | DELETE | `/api/users/account` | Delete user account |
| 8 | GET | `/api/users/activity-heatmap` | Get vault access heatmap data |
| 9 | PUT | `/api/users/read-only-mode` | Toggle read-only mode 🆕 |

---

## 🗄️ 3. Vault Controller (`/api/vault`)
**Endpoints: 18**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/vault/entries` | Get all vault entries (paginated) |
| 2 | GET | `/api/vault/entries/{id}` | Get single entry details |
| 3 | POST | `/api/vault/entries` | Create new vault entry |
| 4 | PUT | `/api/vault/entries/{id}` | Update vault entry |
| 5 | DELETE | `/api/vault/entries/{id}` | Soft delete entry (move to trash) |
| 6 | DELETE | `/api/vault/entries/{id}/permanent` | Permanently delete entry |
| 7 | POST | `/api/vault/entries/{id}/view-password` | View decrypted password (requires master pw) |
| 8 | GET | `/api/vault/search` | Search entries by keyword |
| 9 | GET | `/api/vault/filter` | Filter entries by criteria |
| 10 | PUT | `/api/vault/entries/{id}/favorite` | Toggle favorite status |
| 11 | GET | `/api/vault/favorites` | Get all favorite entries |
| 12 | PUT | `/api/vault/entries/{id}/sensitive` | Mark as highly sensitive |
| 13 | GET | `/api/vault/trash` | Get trashed entries |
| 14 | POST | `/api/vault/trash/{id}/restore` | Restore entry from trash |
| 15 | DELETE | `/api/vault/trash/empty` | Empty trash permanently |
| 16 | GET | `/api/vault/recent` | Get recently added entries |
| 17 | GET | `/api/vault/recently-used` | Get recently accessed entries |
| 18 | POST | `/api/vault/entries/bulk-delete` | Bulk delete multiple entries |

---

## 📂 4. Category Controller (`/api/categories`)
**Endpoints: 6**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/categories` | Get all categories |
| 2 | GET | `/api/categories/{id}` | Get category by ID |
| 3 | POST | `/api/categories` | Create custom category |
| 4 | PUT | `/api/categories/{id}` | Update category |
| 5 | DELETE | `/api/categories/{id}` | Delete category |
| 6 | GET | `/api/categories/{id}/entries` | Get entries in category |

---

## 📁 5. Folder Controller (`/api/folders`)
**Endpoints: 7**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/folders` | Get all folders (tree structure) |
| 2 | GET | `/api/folders/{id}` | Get folder by ID |
| 3 | POST | `/api/folders` | Create new folder |
| 4 | PUT | `/api/folders/{id}` | Update folder |
| 5 | DELETE | `/api/folders/{id}` | Delete folder |
| 6 | GET | `/api/folders/{id}/entries` | Get entries in folder |
| 7 | PUT | `/api/folders/{id}/move` | Move folder to new parent |

---

## 🔑 6. Password Generator Controller (`/api/generator`)
**Endpoints: 5**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | POST | `/api/generator/generate` | Generate random password |
| 2 | POST | `/api/generator/generate-multiple` | Generate multiple passwords |
| 3 | POST | `/api/generator/strength` | Check password strength |
| 4 | POST | `/api/generator/validate` | Validate password against rules |
| 5 | GET | `/api/generator/default-settings` | Get default generator settings |

---

## 🛡️ 7. Security Controller (`/api/security`)
**Endpoints: 10**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/security/audit-report` | Get security audit report |
| 2 | GET | `/api/security/weak-passwords` | Get list of weak passwords |
| 3 | GET | `/api/security/reused-passwords` | Get reused password alerts |
| 4 | GET | `/api/security/old-passwords` | Get old passwords needing rotation |
| 5 | GET | `/api/security/audit-logs` | Get user activity audit logs |
| 6 | GET | `/api/security/login-history` | Get login attempt history |
| 7 | GET | `/api/security/alerts` | Get security alerts |
| 8 | PUT | `/api/security/alerts/{id}/read` | Mark alert as read |
| 9 | DELETE | `/api/security/alerts/{id}` | Dismiss security alert |
| 10 | POST | `/api/security/analyze-vault` | Run full vault security analysis |

---

## 🔒 8. Two-Factor Auth Controller (`/api/2fa`)
**Endpoints: 6**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/2fa/status` | Get 2FA status |
| 2 | POST | `/api/2fa/setup` | Initialize 2FA setup (get QR code) |
| 3 | POST | `/api/2fa/verify-setup` | Verify and enable 2FA |
| 4 | POST | `/api/2fa/disable` | Disable 2FA |
| 5 | GET | `/api/2fa/backup-codes` | Get backup recovery codes |
| 6 | POST | `/api/2fa/regenerate-codes` | Regenerate backup codes |

---

## 📱 9. Session Controller (`/api/sessions`)
**Endpoints: 5**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/sessions` | Get all active sessions |
| 2 | GET | `/api/sessions/current` | Get current session details |
| 3 | DELETE | `/api/sessions/{id}` | Terminate specific session |
| 4 | DELETE | `/api/sessions/all` | Terminate all sessions except current |
| 5 | POST | `/api/sessions/extend` | Extend current session |

---

## 💾 10. Backup Controller (`/api/backup`)
**Endpoints: 8**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | POST | `/api/backup/export` | Export encrypted vault backup |
| 2 | GET | `/api/backup/export/preview` | Preview export (entry count) |
| 3 | POST | `/api/backup/import` | Import encrypted backup |
| 4 | POST | `/api/backup/import/validate` | Validate backup file before import |
| 5 | GET | `/api/backup/snapshots` | Get vault snapshots |
| 6 | POST | `/api/backup/snapshots/{id}/restore` | Restore vault from snapshot |
| 7 | POST | `/api/backup/import-external` | Import from Chrome/Firefox/LastPass 🆕 |
| 8 | GET | `/api/backup/import-external/formats` | Get supported import formats 🆕 |

---

## 🔔 11. Notification Controller (`/api/notifications`)
**Endpoints: 5**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/notifications` | Get all notifications |
| 2 | GET | `/api/notifications/unread-count` | Get unread notification count |
| 3 | PUT | `/api/notifications/{id}/read` | Mark notification as read |
| 4 | PUT | `/api/notifications/read-all` | Mark all as read |
| 5 | DELETE | `/api/notifications/{id}` | Delete notification |

---

## ❤️ 12. Health Controller (`/api/health`)
**Endpoints: 3**

| # | Method | Endpoint | Description |
|---|--------|----------|-------------|
| 1 | GET | `/api/health` | Application health status |
| 2 | GET | `/api/health/db` | Database connectivity check |
| 3 | GET | `/api/health/services` | All services health check |

---

## 📈 API Statistics

### By HTTP Method
| Method | Count | Percentage |
|--------|-------|------------|
| GET | 45 | 46% |
| POST | 35 | 36% |
| PUT | 13 | 13% |
| DELETE | 9 | 9% |
| **Total** | **102** | 100% |

### By Feature Area
```
Vault Management      ███████████████████░░ 18 APIs (18%)
Authentication        ███████████████░░░░░░ 15 APIs (15%) +3 🆕
Security & Audit      ████████████░░░░░░░░░ 10 APIs (10%)
User Management       ███████████░░░░░░░░░░  9 APIs (9%)  +1 🆕
Backup/Export         ██████████░░░░░░░░░░░  8 APIs (8%)  +2 🆕
Folder Management     █████████░░░░░░░░░░░░  7 APIs (7%)
Category Management   ███████░░░░░░░░░░░░░░  6 APIs (6%)
Two-Factor Auth       ███████░░░░░░░░░░░░░░  6 APIs (6%)
Password Generator    ██████░░░░░░░░░░░░░░░  5 APIs (5%)
Sessions              ██████░░░░░░░░░░░░░░░  5 APIs (5%)
Notifications         ██████░░░░░░░░░░░░░░░  5 APIs (5%)
Health Check          ████░░░░░░░░░░░░░░░░░  3 APIs (3%)
```

---

## 🔒 Authorization Matrix

| Endpoint Group | Auth Required | 2FA Required | Master PW Re-entry |
|----------------|---------------|--------------|-------------------|
| `/api/auth/login` | ❌ | ❌ | ❌ |
| `/api/auth/register` | ❌ | ❌ | ❌ |
| `/api/vault/**` | ✅ | ⚡ | ⚡ |
| `/api/users/**` | ✅ | ❌ | ⚡ |
| `/api/security/**` | ✅ | ❌ | ❌ |
| `/api/backup/export` | ✅ | ✅ | ✅ |
| `/api/2fa/**` | ✅ | ❌ | ✅ |
| `/api/health/**` | ❌ | ❌ | ❌ |

**Legend:** ✅ Always | ⚡ Conditional | ❌ Never

---

## 🔖 API Versioning

> **Current Version:** v1 (implied in `/api/` prefix)
> 
> **Future Recommendation:** Consider migrating to `/api/v1/` prefix for backward compatibility when v2 is released.

---

## 📥 Request Examples

### Authentication - Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "username": "john_doe",
  "masterPassword": "SecureP@ss123!",
  "passwordHint": "My favorite pet name",
  "securityQuestions": [
    {
      "question": "What is your mother's maiden name?",
      "answer": "Smith"
    },
    {
      "question": "What was the name of your first pet?",
      "answer": "Buddy"
    },
    {
      "question": "What city were you born in?",
      "answer": "New York"
    }
  ]
}
```

### Authentication - Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "identifier": "user@example.com",
  "masterPassword": "SecureP@ss123!",
  "rememberDevice": true,
  "captchaToken": "optional-captcha-response"
}
```

### Vault - Create Entry
```http
POST /api/vault/entries
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "accountName": "Gmail Account",
  "websiteUrl": "https://gmail.com",
  "username": "john.doe@gmail.com",
  "password": "MyGm@ilP@ss123",
  "categoryId": 1,
  "folderId": null,
  "notes": "Personal email account",
  "isFavorite": true,
  "isSensitive": false
}
```

### Password Generator
```http
POST /api/generator/generate
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "length": 20,
  "includeUppercase": true,
  "includeLowercase": true,
  "includeNumbers": true,
  "includeSpecial": true,
  "excludeSimilar": true,
  "excludeAmbiguous": false,
  "count": 1
}
```

### 2FA Setup
```http
POST /api/2fa/setup
Authorization: Bearer <jwt_token>
X-Master-Password: <base64_encoded_password>
```

### Backup Export
```http
POST /api/backup/export
Authorization: Bearer <jwt_token>
X-2FA-Code: 123456
X-Master-Password: <base64_encoded_password>
Content-Type: application/json

{
  "format": "JSON",
  "encryptionPassword": "BackupP@ss123!"
}
```

---

## 📤 Response Format

### Success Response
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "id": 1,
    "accountName": "Gmail Account",
    "websiteUrl": "https://gmail.com"
  },
  "timestamp": "2026-02-06T18:44:00Z",
  "errors": []
}
```

### Paginated Response
```json
{
  "success": true,
  "message": "Entries retrieved successfully",
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "timestamp": "2026-02-06T18:44:00Z",
  "errors": []
}
```

### Authentication Response
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "dGhpcyBpcyBhIHJlZnJl...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "requires2FA": false,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "john_doe"
    }
  },
  "timestamp": "2026-02-06T18:44:00Z",
  "errors": []
}
```

---

## ❌ Error Response Format

### Error Response Structure
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "timestamp": "2026-02-06T18:44:00Z",
  "errors": [
    {
      "field": "email",
      "code": "INVALID_FORMAT",
      "message": "Email format is invalid"
    }
  ]
}
```

### HTTP Status Codes

| Code | Name | Description | When Used |
|------|------|-------------|-----------|
| 200 | OK | Request successful | GET, PUT, DELETE success |
| 201 | Created | Resource created | POST success |
| 204 | No Content | Success, no body | DELETE success |
| 400 | Bad Request | Invalid request data | Validation errors |
| 401 | Unauthorized | Not authenticated | Missing/invalid token |
| 403 | Forbidden | Not authorized | Insufficient permissions |
| 404 | Not Found | Resource not found | Invalid ID |
| 409 | Conflict | Resource conflict | Duplicate email/username |
| 422 | Unprocessable | Business logic error | Password too weak |
| 429 | Too Many Requests | Rate limit exceeded | Too many API calls |
| 500 | Internal Error | Server error | Unexpected error |

### Error Codes Reference

| Code | Description |
|------|-------------|
| `AUTH_001` | Invalid credentials |
| `AUTH_002` | Account locked |
| `AUTH_003` | 2FA required |
| `AUTH_004` | Invalid OTP |
| `AUTH_005` | Session expired |
| `VAULT_001` | Entry not found |
| `VAULT_002` | Decryption failed |
| `VAULT_003` | Entry in trash |
| `USER_001` | User not found |
| `USER_002` | Email already exists |
| `USER_003` | Username already exists |
| `SEC_001` | Master password required |
| `SEC_002` | Weak password |
| `SEC_003` | Rate limit exceeded |
| `BACKUP_001` | Invalid backup format |
| `BACKUP_002` | Decryption password wrong |

---

## 📄 Pagination Parameters

All list endpoints support pagination via query parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Items per page (max: 100) |
| `sort` | string | `createdAt,desc` | Sort field and direction |

### Example
```http
GET /api/vault/entries?page=0&size=20&sort=accountName,asc
```

### Paginated Endpoints
- `GET /api/vault/entries`
- `GET /api/vault/trash`
- `GET /api/vault/favorites`
- `GET /api/security/audit-logs`
- `GET /api/security/login-history`
- `GET /api/notifications`

---

## 🔍 Query Parameters

### Vault Search
```http
GET /api/vault/search?q=gmail&field=all
```
| Parameter | Type | Description |
|-----------|------|-------------|
| `q` | string | Search keyword |
| `field` | string | Field to search: `all`, `name`, `url`, `username` |

### Vault Filter
```http
GET /api/vault/filter?categoryId=1&strength=weak&favorite=true
```
| Parameter | Type | Description |
|-----------|------|-------------|
| `categoryId` | long | Filter by category |
| `folderId` | long | Filter by folder |
| `strength` | string | `weak`, `medium`, `strong`, `very_strong` |
| `favorite` | boolean | Filter favorites only |
| `age` | int | Filter passwords older than X days |

### Security Audit Logs
```http
GET /api/security/audit-logs?action=LOGIN&startDate=2026-01-01&endDate=2026-02-06
```
| Parameter | Type | Description |
|-----------|------|-------------|
| `action` | string | `LOGIN`, `LOGOUT`, `VIEW_PASSWORD`, `CREATE`, `UPDATE`, `DELETE` |
| `startDate` | date | Start date (YYYY-MM-DD) |
| `endDate` | date | End date (YYYY-MM-DD) |

---

## 🔐 Request Headers

### Required Headers

| Header | Description | Required For |
|--------|-------------|--------------|
| `Authorization` | Bearer JWT token | All protected endpoints |
| `Content-Type` | `application/json` | POST, PUT requests |

### Optional Security Headers

| Header | Description | When Required |
|--------|-------------|---------------|
| `X-Master-Password` | Base64 encoded master password | Sensitive operations |
| `X-2FA-Code` | 6-digit TOTP code | When 2FA is enabled |
| `X-Device-Fingerprint` | Device identifier | Session tracking |
| `X-Request-ID` | Unique request ID | Debugging/tracing |

---

## ⏱️ Rate Limiting

| Endpoint Group | Requests/Minute | Burst |
|----------------|-----------------|-------|
| `/api/auth/login` | 5 | 3 |
| `/api/auth/register` | 3 | 2 |
| `/api/auth/forgot-password` | 3 | 2 |
| `/api/vault/**` | 60 | 30 |
| `/api/generator/**` | 30 | 15 |
| `/api/backup/export` | 5 | 2 |
| Other endpoints | 100 | 50 |

### Rate Limit Headers in Response
```http
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1707235200
```

---

> **Total APIs: 97 endpoints** across 12 controllers  
> **Documentation Version:** 1.0.0  
> **Last Updated:** 2026-02-06

