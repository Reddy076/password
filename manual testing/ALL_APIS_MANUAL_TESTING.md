# 🧪 Comprehensive API Manual Testing Guide

This guide contains all the available API endpoints in the Password Manager application, designed to make manual testing via Postman or curl straightforward. All JSON bodies have been precisely mapped to their corresponding backend DTOs.

## 🔐 Authentication Guide

**Base URL**: `http://localhost:8080` (or your configured port)

Most endpoints require authentication. To authenticate requests:
1. Send a login request to `POST /api/auth/login`.
2. Extract the `accessToken` from the response.
3. Include it in the header for subsequent requests:
   `Authorization: Bearer <your_access_token>`

---

## 🚀 1. Authentication & Onboarding (`/api/auth`)

### 1.1 Register User
- **Method**: `POST`
- **URL**: `/api/auth/register`
- **Auth**: None
- **Body** (`RegistrationRequest`):
```json
{
  "email": "test@example.com",
  "username": "testuser",
  "masterPassword": "StrongPassword123!",
  "passwordHint": "My favorite color",
  "securityQuestions": [
    { "question": 1, "answer": "Answer 1" },
    { "question": 2, "answer": "Answer 2" },
    { "question": 3, "answer": "Answer 3" }
  ]
}
```
**Response** (`UserResponse`):
```json
{
  "id": 1,
  "email": "test@example.com",
  "username": "testuser",
  "name": null,
  "phoneNumber": null,
  "is2faEnabled": false,
  "createdAt": "2026-02-22T10:00:00"
}
```

### 1.2 Login
- **Method**: `POST`
- **URL**: `/api/auth/login`
- **Auth**: None
- **Body** (`LoginRequest`):
```json
{
  "username": "testuser",
  "masterPassword": "StrongPassword123!",
  "captchaToken": "" 
}
```
**Response** (`AuthResponse`):
```json
{
  "accessToken": "eyJhb...",
  "refreshToken": "eyJhb...",
  "tokenType": "Bearer",
  "username": "testuser",
  "expiresIn": 900000,
  "requires2FA": false,
  "message": null
}
```

### 1.3 Refresh Token
- **Method**: `POST`
- **URL**: `/api/auth/refresh-token`
- **Auth**: None
- **Body** (`RefreshTokenRequest`):
```json
{
  "refreshToken": "<your_refresh_token>"
}
```
**Response** (`AuthResponse`):
```json
{
  "accessToken": "eyJhb...",
  "refreshToken": "eyJhb...",
  "tokenType": "Bearer",
  "username": "testuser",
  "expiresIn": 900000,
  "requires2FA": false,
  "message": null
}
```

### 1.4 Logout
- **Method**: `POST`
- **URL**: `/api/auth/logout`
- **Auth**: Bearer Token
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Logged out successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.5 Get Security Questions (Password Reset)
- **Method**: `GET`
- **URL**: `/api/auth/security-questions/{username}`
- **Auth**: None
- **Body**: None required
**Response** (`List<SecurityQuestionDTO>`):
```json
[
  { "id": 1, "question": "What was the name of your first pet?" },
  { "id": 2, "question": "What is your mother's maiden name?" }
]
```

### 1.6 Reset Password
- **Method**: `POST`
- **URL**: `/api/auth/reset-password`
- **Auth**: None
- **Body** (`RecoveryRequest`):
```json
{
  "username": "testuser",
  "securityAnswers": [
    { "question": "Question 1?", "answer": "Answer 1" },
    { "question": "Question 2?", "answer": "Answer 2" },
    { "question": "Question 3?", "answer": "Answer 3" }
  ],
  "newMasterPassword": "NewStrongPassword123!"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "Password changed successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.7 Request Email OTP
- **Method**: `POST`
- **URL**: `/api/auth/send-otp?username=testuser`
- **Auth**: None
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "OTP sent to your email.",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.8 Verify Email OTP
- **Method**: `POST`
- **URL**: `/api/auth/verify-otp?username=testuser&code=123456`
- **Auth**: None
- **Body**: None required
**Response** (`AuthResponse`):
```json
{
  "accessToken": "eyJhb...",
  "refreshToken": "eyJhb...",
  "tokenType": "Bearer",
  "username": "testuser",
  "expiresIn": 900000,
  "requires2FA": false,
  "message": null
}
```

### 1.9 Set Password Hint
- **Method**: `PUT`
- **URL**: `/api/auth/password-hint`
- **Auth**: Bearer Token
- **Body** (`Map<String, String>`):
```json
{
  "masterPassword": "CurrentPassword123!",
  "hint": "My new favorite color"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "Password hint updated successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.10 Set Duress Password
- **Method**: `POST`
- **URL**: `/api/auth/set-duress-password`
- **Auth**: Bearer Token
- **Body** (`Map<String, String>`):
```json
{
  "duressPassword": "FakePassword123!"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "Duress password set successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.11 Duress Login
- **Method**: `POST`
- **URL**: `/api/auth/duress-login`
- **Auth**: None
- **Body** (`LoginRequest`):
```json
{
  "username": "testuser",
  "masterPassword": "FakePassword123!"
}
```
**Response** (`AuthResponse`):
```json
{
  "accessToken": "eyJhb...",
  "refreshToken": "eyJhb...",
  "tokenType": "Bearer",
  "username": "testuser",
  "expiresIn": 900000,
  "requires2FA": false,
  "message": null
}
```

### 1.12 Forgot Password (Init Recovery)
- **Method**: `POST`
- **URL**: `/api/auth/forgot-password`
- **Auth**: None
- **Body** (`ForgotPasswordRequest`):
```json
{
  "username": "testuser"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "If the account exists, password recovery has been initiated.",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.13 Verify Security Questions
- **Method**: `POST`
- **URL**: `/api/auth/verify-security-questions`
- **Auth**: None
- **Body** (`VerifySecurityQuestionsRequest`):
```json
{
  "username": "testuser",
  "securityAnswers": [
    { "question": "Question 1?", "answer": "Answer 1" },
    { "question": "Question 2?", "answer": "Answer 2" },
    { "question": "Question 3?", "answer": "Answer 3" }
  ]
}
```
**Response** (`MessageResponse`): 
```json
{
  "message": "Security questions verified successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.14 Verify CAPTCHA (Explicit)
- **Method**: `POST`
- **URL**: `/api/auth/verify-captcha`
- **Auth**: None
- **Body** (`VerifyCaptchaRequest`):
```json
{
  "captchaToken": "your-captcha-response-token"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "CAPTCHA verified successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.15 Validate JWT Token
- **Method**: `GET`
- **URL**: `/api/auth/validate-token`
- **Auth**: Bearer Token
- **Body**: None required
**Response**:
```json
{
  "valid": true
}
```

### 1.16 Verify Master Password Mid-Session
- **Method**: `POST`
- **URL**: `/api/auth/verify-master-password`
- **Auth**: Bearer Token
- **Body** (`VerifyMasterPasswordRequest`):
```json
{
  "masterPassword": "CurrentPassword123!"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "Master password verified successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 1.17 Resend OTP
- **Method**: `POST`
- **URL**: `/api/auth/resend-otp?username=testuser`
- **Auth**: None
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "OTP resent to your email.",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 🛡️ 2. Two-Factor Authentication (`/api/2fa`)

*(Requires Bearer Token for all)*

### 2.1 Get 2FA Status
- **Method**: `GET`
- **URL**: `/api/2fa/status`
- **Body**: None required
**Response** (`Map<String, Boolean>`):
```json
{
  "enabled": true
}
```

### 2.2 Initialize 2FA Setup
- **Method**: `POST`
- **URL**: `/api/2fa/setup`
- **Body**: None required
**Response** (`TwoFactorSetupResponse`): 
```json
{
  "secretKey": "JBSW...",
  "qrCodeUrl": "otpauth://totp/...",
  "backupCodes": [
    "abcd1234efgh5678"
  ]
}
```

### 2.3 Verify and Enable 2FA
- **Method**: `POST`
- **URL**: `/api/2fa/verify-setup?code=123456`
- **Body**: None required
**Response** (`Map<String, Object>`):
```json
{
  "success": true,
  "message": "2FA enabled successfully",
  "backupCodes": [
    "abcd1234efgh5678"
  ],
  "timestamp": "2026-02-22T10:00:00"
}
```

### 2.4 Disable 2FA
- **Method**: `POST`
- **URL**: `/api/2fa/disable`
- **Body**: None required
**Response** (`Map<String, String>`):
```json
{
  "message": "2FA disabled successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 2.5 Get Backup Codes
- **Method**: `GET`
- **URL**: `/api/2fa/backup-codes`
- **Body**: None required
**Response** (`List<String>`):
```json
[
  "abcd1234efgh5678",
  "ijkl9012mnop3456"
]
```

### 2.6 Regenerate Recovery Codes
- **Method**: `POST`
- **URL**: `/api/2fa/regenerate-codes`
- **Body**: None required
**Response** (`Map<String, Object>`): 
```json
{
  "success": true,
  "message": "Backup codes regenerated successfully",
  "backupCodes": [
    "newabcd1234efgh5"
  ],
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 🧑 3. User & Profile (`/api/users`)

*(Requires Bearer Token for all)*

### 3.1 Get Profile
- **Method**: `GET`
- **URL**: `/api/users/profile`
- **Body**: None required
**Response** (`UserResponse`):
```json
{
  "id": 1,
  "email": "test@example.com",
  "username": "testuser",
  "name": "John Doe",
  "phoneNumber": "123-456-7890",
  "is2faEnabled": true,
  "createdAt": "2026-02-22T10:00:00"
}
```

### 3.2 Update Profile
- **Method**: `PUT`
- **URL**: `/api/users/profile`
- **Body** (`UpdateProfileRequest`):
```json
{
  "email": "newemail@example.com",
  "name": "John Doe",
  "phoneNumber": "123-456-7890"
}
```
**Response** (`UserResponse`):
```json
{
  "id": 1,
  "email": "newemail@example.com",
  "username": "testuser",
  "name": "John Doe",
  "phoneNumber": "123-456-7890",
  "is2faEnabled": true,
  "createdAt": "2026-02-22T10:00:00"
}
```

### 3.3 Change Master Password
- **Method**: `PUT`
- **URL**: `/api/users/change-password`
- **Body** (`ChangePasswordRequest`):
```json
{
  "oldPassword": "CurrentPassword123!",
  "newPassword": "NewPassword123!@#"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "Password changed successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 3.4 Schedule Account Deletion
- **Method**: `DELETE`
- **URL**: `/api/users/account`
- **Body** (`AccountDeletionRequest`):
```json
{
  "masterPassword": "CurrentPassword123!"
}
```
**Response** (`MessageResponse`):
```json
{
  "message": "Account scheduled for deletion in 30 days. You can cancel this action by logging in and using the cancel endpoint.",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 3.5 Cancel Account Deletion
- **Method**: `POST`
- **URL**: `/api/users/account/cancel-deletion`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Account deletion cancelled.",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## ⚙️ 4. User Settings (`/api/settings`)

*(Requires Bearer Token for all)*

### 4.1 Get Settings
- **Method**: `GET`
- **URL**: `/api/settings`
- **Body**: None required
**Response** (`UserSettingsResponse`):
```json
{
  "autoLockTimeout": 15,
  "defaultPasswordLength": 16,
  "requireMfaForSensitive": true,
  "enableRiskAlerts": true,
  "readOnlyMode": false
}
```

### 4.2 Update Settings
- **Method**: `PUT`
- **URL**: `/api/settings`
- **Body** (`UserSettingsRequest`):
```json
{
  "autoLockTimeout": 15,
  "defaultPasswordLength": 16,
  "requireMfaForSensitive": true,
  "enableRiskAlerts": true,
  "readOnlyMode": false
}
```
**Response** (`UserSettingsResponse`):
```json
{
  "autoLockTimeout": 15,
  "defaultPasswordLength": 16,
  "requireMfaForSensitive": true,
  "enableRiskAlerts": true,
  "readOnlyMode": false
}
```

---

## 📂 5. Vault Items (`/api/vault`)

*(Requires Bearer Token for all)*

### 5.1 Create Entry
- **Method**: `POST`
- **URL**: `/api/vault`
- **Body** (`VaultEntryRequest`):
```json
{
  "title": "My Bank",
  "username": "johndoe",
  "password": "SecretPassword123",
  "websiteUrl": "https://bank.com",
  "notes": "Savings account",
  "categoryId": 1,
  "folderId": 1,
  "isFavorite": true,
  "isHighlySensitive": false
}
```
**Response** (`VaultEntryResponse`):
```json
{
  "id": 1,
  "title": "My Bank",
  "username": "johndoe",
  "websiteUrl": "https://bank.com",
  "notes": "Savings account",
  "categoryId": 1,
  "folderId": 1,
  "isFavorite": true,
  "isHighlySensitive": false,
  "createdAt": "2026-02-22T10:00:00",
  "updatedAt": "2026-02-22T10:00:00"
}
```

### 5.2 Get All Entries
- **Method**: `GET`
- **URL**: `/api/vault`
- **Body**: None required
**Response** (`List<VaultEntryResponse>`):
```json
[
  {
    "id": 1,
    "title": "My Bank",
    "username": "johndoe",
    "websiteUrl": "https://bank.com",
    "isFavorite": true,
    "isHighlySensitive": false
  }
]
```

### 5.3 Search & Filter Entries
- **Method**: `GET`
- **URL**: `/api/vault/search?keyword=bank&categoryId=1&folderId=1&isFavorite=true&isHighlySensitive=false&sortBy=title&sortDir=asc`
- **Body**: None required
**Response** (`List<VaultEntryResponse>`): Returns filtered list format identical to **5.2**.

### 5.3.1 Filter Entries Formally
- **Method**: `GET`
- **URL**: `/api/vault/filter?keyword=bank&categoryId=1&folderId=1&isFavorite=true&isHighlySensitive=false&sortBy=title&sortDir=asc`
- **Body**: None required
**Response** (`List<VaultEntryResponse>`): Returns filtered list format identical to **5.2**.

### 5.3.2 Get Recent Entries
- **Method**: `GET`
- **URL**: `/api/vault/recent?limit=10`
- **Body**: None required
**Response** (`List<VaultEntryResponse>`): Returns list format identical to **5.2**.

### 5.3.3 Get Recently Used
- **Method**: `GET`
- **URL**: `/api/vault/recently-used?limit=5`
- **Body**: None required
**Response** (`List<VaultEntryResponse>`): Returns list format identical to **5.2**.

### 5.4 Get Single Entry
- **Method**: `GET`
- **URL**: `/api/vault/{id}`
- **Body**: None required
**Response** (`VaultEntryDetailResponse`):
```json
{
  "id": 1,
  "title": "My Bank",
  "username": "johndoe",
  "password": "••••••••••••",
  "websiteUrl": "https://bank.com",
  "notes": "Savings account",
  "isFavorite": true,
  "isHighlySensitive": false,
  "isMasked": true
}
```

### 5.5 View Password (Unmasked)
- **Method**: `POST`
- **URL**: `/api/vault/entries/{id}/view-password`
- **Body**: None required
**Response** (`Map<String, String>`):
```json
{
  "password": "SecretPassword123"
}
```

### 5.6 Sensitive View (Highly Sensitive Items)
- **Method**: `POST`
- **URL**: `/api/vault/{id}/sensitive-view`
- **Body** (`SensitiveAccessRequest`):
```json
{
  "masterPassword": "CurrentPassword123!",
  "twoFactorCode": ""
}
```
**Response** (`VaultEntryDetailResponse`):
```json
{
  "id": 1,
  "title": "My Bank",
  "username": "johndoe",
  "password": "SecretPassword123",
  "websiteUrl": "https://bank.com",
  "notes": "Savings account",
  "isFavorite": true,
  "isHighlySensitive": true,
  "isMasked": false
}
```

### 5.7 Update Entry
- **Method**: `PUT`
- **URL**: `/api/vault/{id}`
- **Body** (`VaultEntryRequest`): Same format as **5.1 Create Entry**.
**Response** (`VaultEntryResponse`): Returns format identical to **5.1**.

### 5.8 Delete Entry (Move to trash)
- **Method**: `DELETE`
- **URL**: `/api/vault/{id}`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Entry deleted successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 5.8.1 Toggle Highly Sensitive Property
- **Method**: `PUT`
- **URL**: `/api/vault/entries/{id}/sensitive`
- **Body**: None required
**Response** (`VaultEntryResponse`): Returns format identical to **5.1**.

### 5.9 Password History for Entry
- **Method**: `GET`
- **URL**: `/api/vault/entries/{id}/history`
- **Body**: None required
**Response** (`List<SnapshotResponse>`):
```json
[
  {
    "id": 1,
    "timestamp": "2026-02-20T12:00:00",
    "changeType": "UPDATE"
  }
]
```

---

## 🗑️ 6. Trash Management (`/api/vault/trash`)

*(Requires Bearer Token for all)*

### 6.1 Get Trash
- **Method**: `GET`
- **URL**: `/api/vault/trash`
- **Body**: None required
**Response** (`List<TrashEntryResponse>`):
```json
[
  {
    "id": 1,
    "vaultEntryId": 1,
    "title": "My Bank",
    "deletedAt": "2026-02-22T10:00:00",
    "expiresAt": "2026-03-24T10:00:00"
  }
]
```

### 6.2 Restore Entry
- **Method**: `POST`
- **URL**: `/api/vault/trash/{id}/restore`
- **Body**: None required
**Response** (`TrashEntryResponse`): Returns format identical to **6.1**.

### 6.3 Permanent Delete
- **Method**: `DELETE`
- **URL**: `/api/vault/trash/{id}`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Entry permanently deleted",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 6.4 Empty All Trash
- **Method**: `DELETE`
- **URL**: `/api/vault/trash/empty`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Trash emptied successfully",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 📁 7. Folders (`/api/folders`)

*(Requires Bearer Token for all)*

### 7.1 Create Folder
- **Method**: `POST`
- **URL**: `/api/folders?name=Finance&parentFolderId=1` (parent optional)
- **Body**: None required
**Response** (`FolderDTO`):
```json
{
  "id": 1,
  "name": "Finance",
  "parentFolderId": null,
  "subfolders": [],
  "createdAt": "2026-02-22T10:00:00",
  "updatedAt": "2026-02-22T10:00:00"
}
```

### 7.2 Get All Folders
- **Method**: `GET`
- **URL**: `/api/folders`
- **Body**: None required
**Response** (`List<FolderDTO>`): Returns format identical to **7.1**.

### 7.3 Get Entries in Folder
- **Method**: `GET`
- **URL**: `/api/folders/{id}/entries`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "This endpoint will return vault entries when the Vault feature is implemented",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 7.4 Update Folder
- **Method**: `PUT`
- **URL**: `/api/folders/{id}?name=NewFinanceName`
- **Body**: None required
**Response** (`FolderDTO`): Returns format identical to **7.1**.

### 7.5 Move Folder
- **Method**: `PUT`
- **URL**: `/api/folders/{id}/move?parentId=2`
- **Body**: None required
**Response** (`FolderDTO`): Returns format identical to **7.1**.

### 7.6 Delete Folder
- **Method**: `DELETE`
- **URL**: `/api/folders/{id}`
- **Body**: None required
**Response**: `204 No Content`

---

## 🏷️ 8. Categories (`/api/categories`)

*(Requires Bearer Token for all)*

### 8.1 Create Category
- **Method**: `POST`
- **URL**: `/api/categories`
- **Body** (`CreateCategoryRequest`):
```json
{
  "name": "Social Media",
  "icon": "fa-users"
}
```
**Response** (`CategoryDTO`):
```json
{
  "id": 1,
  "name": "Social Media",
  "icon": "fa-users",
  "isDefault": false,
  "createdAt": "2026-02-22T10:00:00"
}
```

### 8.2 Get All Categories
- **Method**: `GET`
- **URL**: `/api/categories`
- **Body**: None required
**Response** (`List<CategoryDTO>`): Returns format identical to **8.1**.

### 8.3 Update Category
- **Method**: `PUT`
- **URL**: `/api/categories/{id}`
- **Body** (`CreateCategoryRequest`): Same structure as **8.1**.
**Response** (`CategoryDTO`): Returns format identical to **8.1**.

### 8.4 Delete Category
- **Method**: `DELETE`
- **URL**: `/api/categories/{id}`
- **Body**: None required
**Response**: `204 No Content`

### 8.5 Get Entries in Category
- **Method**: `GET`
- **URL**: `/api/categories/{id}/entries`
**Response** (`MessageResponse`):
```json
{
  "message": "This endpoint will return vault entries when the Vault feature is implemented",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 🔐 9. Password Generator (`/api/generator`)

*(Bearer Token Optional but recommended for custom defaults)*

### 9.1 Generate Single Password
- **Method**: `POST`
- **URL**: `/api/generator/generate`
- **Body** (`PasswordGeneratorRequest`):
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
**Response** (`Map<String, String>`):
```json
{
  "password": "R4nd0mP@ssw0rd!"
}
```

### 9.2 Check Password Strength
- **Method**: `POST`
- **URL**: `/api/generator/strength`
- **Body** (`Map<String, String>`):
```json
{
  "password": "MyTestPassword123!"
}
```
**Response** (`PasswordStrengthResponse`):
```json
{
  "score": 85,
  "label": "Strong",
  "feedback": "Good password!",
  "isExposed": false,
  "crackTimeDisplay": "cents"
}
```

### 9.3 Generate Multiple Passwords
- **Method**: `POST`
- **URL**: `/api/generator/generate-multiple`
- **Body** (`PasswordGeneratorRequest`): Same structure as **9.1** with `"count": 5`
**Response** (`Map<String, List<String>>`):
```json
{
  "passwords": [
    "R4nd0mP@ssw0rd!",
    "An0th3rP@ss^8",
    "Y3t@notherP@ss"
  ]
}
```

---

## 📡 10. Active Sessions (`/api/sessions`)

*(Requires Bearer Token for all)*

### 10.1 Get All Active Sessions
- **Method**: `GET`
- **URL**: `/api/sessions`
- **Body**: None required
**Response** (`List<SessionResponse>`):
```json
[
  {
    "id": 1,
    "deviceInfo": "Chrome on Windows",
    "ipAddress": "192.168.1.1",
    "lastActiveAt": "2026-02-22T10:00:00",
    "isCurrentSession": true
  }
]
```

### 10.2 Terminate Specific Session
- **Method**: `DELETE`
- **URL**: `/api/sessions/{sessionId}`
- **Body**: None required
**Response**: `204 No Content`

### 10.3 Terminate All Sessions (Logout Everywhere)
- **Method**: `DELETE`
- **URL**: `/api/sessions`
- **Body**: None required
**Response**: `204 No Content`

### 10.4 Extend Session Expiration
- **Method**: `POST`
- **URL**: `/api/sessions/extend`
- **Auth**: Bearer Token
- **Body**: None required
**Response** (`SessionResponse`): Contains updated session data identical to **10.1**.

---

## 🔔 11. Notifications (`/api/notifications`)

*(Requires Bearer Token for all)*

### 11.1 Get All Notifications
- **Method**: `GET`
- **URL**: `/api/notifications`
- **Body**: None required
**Response** (`List<NotificationDTO>`):
```json
[
  {
    "id": 1,
    "title": "Security Alert",
    "message": "New login detected.",
    "isRead": false,
    "createdAt": "2026-02-22T10:00:00"
  }
]
```

### 11.2 Get Unread Count
- **Method**: `GET`
- **URL**: `/api/notifications/unread-count`
- **Body**: None required
**Response** (`Map<String, Long>`):
```json
{
  "unreadCount": 3,
  "timestamp": "2026-02-22T10:00:00"
}
```

### 11.3 Mark as Read
- **Method**: `PUT`
- **URL**: `/api/notifications/{id}/read`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Notification marked as read",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 11.4 Mark All Contextual As Read
- **Method**: `PUT`
- **URL**: `/api/notifications/mark-all-read`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "All notifications marked as read",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 11.5 Delete Notification
- **Method**: `DELETE`
- **URL**: `/api/notifications/{id}`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Notification deleted",
  "timestamp": "2026-02-22T10:00:00"
}
```

---

## 🚨 12. Security & Audit (`/api/security`)

*(Requires Bearer Token for all)*

### 12.1 Get Security Alerts
- **Method**: `GET`
- **URL**: `/api/security/alerts`
- **Body**: None required
**Response** (`List<SecurityAlertDTO>`):
```json
[
  {
    "id": 1,
    "alertType": "NEW_LOGIN",
    "message": "New login from unfamiliar device.",
    "isRead": false,
    "createdAt": "2026-02-22T10:00:00"
  }
]
```

### 12.1.1 Mark Alert As Read
- **Method**: `PUT`
- **URL**: `/api/security/alerts/{id}/read`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Alert marked as read",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 12.1.2 Delete Alert
- **Method**: `DELETE`
- **URL**: `/api/security/alerts/{id}`
- **Body**: None required
**Response** (`MessageResponse`):
```json
{
  "message": "Alert deleted",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 12.2 Get Login History
- **Method**: `GET`
- **URL**: `/api/security/login-history`
- **Body**: None required
**Response** (`List<LoginHistoryResponse>`): 
```json
[
  {
    "id": 1,
    "ipAddress": "192.168.1.1",
    "deviceInfo": "Chrome on Windows",
    "success": true,
    "timestamp": "2026-02-22T10:00:00"
  }
]
```

### 12.3 Get Audit Logs
- **Method**: `GET`
- **URL**: `/api/security/audit-logs`
- **Body**: None required
**Response** (`List<AuditLogResponse>`): 
```json
[
  {
    "id": 1,
    "action": "MASTER_PASSWORD_CHANGED",
    "details": "User changed master password",
    "timestamp": "2026-02-22T10:00:00"
  }
]
```

### 12.4 Generate Full Audit Report
- **Method**: `GET`
- **URL**: `/api/security/audit-report`
- **Body**: None required
**Response** (`SecurityAuditResponse`): 
```json
{
  "vaultHealthScore": 85,
  "weakPasswordsCount": 2,
  "reusedPasswordsCount": 1,
  "oldPasswordsCount": 0
}
```

### 12.5 Get Weak Passwords
- **Method**: `GET`
- **URL**: `/api/security/weak-passwords`
- **Body**: None required
**Response** (`List<VaultEntrySummary>`): Includes entry id and general flags.

### 12.6 Get Reused Passwords
- **Method**: `GET`
- **URL**: `/api/security/reused-passwords`
- **Body**: None required
**Response** (`List<VaultEntrySummary>`): Includes entry id and general flags.

### 12.7 Get Old Passwords
- **Method**: `GET`
- **URL**: `/api/security/old-passwords`
- **Body**: None required
**Response** (`List<VaultEntrySummary>`): Includes entry id and general flags.

### 12.8 Analyze Vault Security
- **Method**: `POST`
- **URL**: `/api/security/analyze-vault`
- **Body**: None required
**Response** (`SecurityAuditResponse`): Returns explicitly calculated metrics mirroring **12.4**.

---

## 📦 13. Backup & Export (`/api/backup`)

*(Requires Bearer Token for all)*

### 13.1 Export Vault
- **Method**: `GET`
- **URL**: `/api/backup/export?format=JSON&password=OptionalEncryptionPass`
- **Body**: None required
**Response** (`ExportResponse`):
```json
{
  "data": "eyJiYXNlNjQiOiAibWFzc2l2ZSBkYXRhIHN0cmluZyJ9",
  "format": "JSON",
  "timestamp": "2026-02-22T10:00:00"
}
```

### 13.2 Import Vault (Native)
- **Method**: `POST`
- **URL**: `/api/backup/import`
- **Body** (`ImportRequest`):
```json
{
  "data": "base64_encoded_json_or_csv_string",
  "format": "JSON",
  "encryptionPassword": "OptionalDecryptionPass",
  "targetFolderId": 1
}
```
**Response** (`ImportResult`):
```json
{
  "success": true,
  "importedCount": 42,
  "failedCount": 0,
  "message": "Import completed successfully"
}
```

### 13.3 Third-Party Import (1Password/LastPass)
- **Method**: `POST`
- **URL**: `/api/backup/import-external`
- **Body** (`ThirdPartyImportRequest`):
```json
{
  "source": "ONEPASSWORD",
  "data": "base64_encoded_csv",
  "defaultFolderId": 1
}
```
**Response** (`ImportResult`): Returns format identical to **13.2**.

### 13.4 Preview Export
- **Method**: `GET`
- **URL**: `/api/backup/export/preview?format=JSON`
- **Body**: None required
**Response** (`ExportResponse`): Returns format identical to **13.1**.

### 13.5 Validate Import Config
- **Method**: `POST`
- **URL**: `/api/backup/import/validate`
- **Body** (`ImportRequest`):
```json
{
  "data": "{}",
  "format": "JSON"
}
```
**Response** (`ImportResult`): Returns validation status identical to **13.2**.

### 13.6 Retrieve Supported Formats
- **Method**: `GET`
- **URL**: `/api/backup/import-external/formats`
- **Body**: None required
**Response** (`List<String>`):
```json
[
  "ONEPASSWORD",
  "LASTPASS",
  "BITWARDEN",
  "CSV"
]
```

### 13.7 Get Snapshot Backup Checkpoints 
- **Method**: `GET`
- **URL**: `/api/backup/snapshots`
- **Body**: None required
**Response** (`List<SnapshotResponse>`):
```json
[
  {
    "id": 1,
    "timestamp": "2026-02-22T10:00:00",
    "changeType": "FULL_BACKUP"
  }
]
```

### 13.8 Restore Snapshot Configuration
- **Method**: `POST`
- **URL**: `/api/backup/snapshots/{id}/restore`
- **Body**: None required
**Response**: `200 OK`

---

## 🩺 14. Server Health

### 14.1 Health Check (Public)
- **Method**: `GET`
- **URL**: `/api/health`
- **Auth**: None
- **Body**: None required
**Response** (`HealthResponse`):
```json
{
  "status": "UP",
  "version": "1.0.0",
  "uptime": "2 days, 4 hours",
  "components": {
    "database": {
      "status": "UP",
      "details": "PostgreSQL connected"
    }
  }
}
```

### 14.2 Database Health (Public)
- **Method**: `GET`
- **URL**: `/api/health/db`
- **Auth**: None
- **Body**: None required
**Response** (`ComponentHealth`):
```json
{
  "status": "UP",
  "details": "PostgreSQL running at 15ms latency"
}
```

### 14.3 External Services Health (Public)
- **Method**: `GET`
- **URL**: `/api/health/services`
- **Auth**: None
- **Body**: None required
**Response** (`ComponentHealth`):
```json
{
  "status": "UP",
  "details": "All external services reachable"
}
```
