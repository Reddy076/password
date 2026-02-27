# Comprehensive Manual API Testing Guide (Features 1-15)

This document provides a complete reference for manually testing **ALL** API endpoints implemented in Features 1-15 using Postman.

## 📋 Prerequisites
1.  **Application Running**: Ensure the Spring Boot application is running on `http://localhost:8080`.
2.  **Postman Environment**: Set up a Postman environment with a variable `accessToken` to easily reuse tokens.
3.  **Base URL**: `http://localhost:8080`

---

## 🔐 1. Authentication (AuthController)
**Base Path**: `/api/auth`
**Headers Required**: None (except Logout)

### 1.1 Register User
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/auth/register`
-   **Description**: Create a new user account.
-   **Body (JSON)**:
    ```json
    {
      "email": "user@example.com",
      "username": "testuser",
      "masterPassword": "StrongPassword123!",
      "securityQuestions": [
        { "question": "Question 1", "answer": "Answer 1" },
        { "question": "Question 2", "answer": "Answer 2" },
        { "question": "Question 3", "answer": "Answer 3" }
      ]
    }
    ```
-   **Expected Response (201 Created)**:
    ```json
    {
      "id": 1,
      "email": "user@example.com",
      "username": "testuser",
      "is2faEnabled": false,
      "createdAt": "2026-02-12T10:00:00"
    }
    ```

### 1.2 Login
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/auth/login`
-   **Description**: Authenticate with credentials to receive JWT tokens.
-   **Body (JSON)**:
    ```json
    {
      "username": "testuser",
      "masterPassword": "StrongPassword123!"
    }
    ```
-   **Expected Response (200 OK)**:
    ```json
    {
      "accessToken": "eyJh...",
      "refreshToken": "eyJh...",
      "tokenType": "Bearer",
      "username": "testuser",
      "expiresIn": 900,
      "requires2FA": false
    }
    ```
-   **Action**: Copy the `accessToken` from the response. Use it in the `Authorization` header as `Bearer <token>` for subsequent requests.

### 1.3 Refresh Token
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/auth/refresh-token`
-   **Description**: Get a new access token using a refresh token when the access token expires.
-   **Body (JSON)**:
    ```json
    {
      "refreshToken": "<YOUR_REFRESH_TOKEN_HERE>"
    }
    ```
-   **Expected Response (200 OK)**:
    ```json
    {
      "accessToken": "eyJh...",
      "refreshToken": "eyJh...",
      "tokenType": "Bearer",
      "username": "testuser",
      "expiresIn": 900,
      "requires2FA": false
    }
    ```

### 1.4 Logout
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/auth/logout`
-   **Headers**: `Authorization: Bearer <accessToken>`
-   **Description**: Invalidate the current session/token on the server.
-   **Expected Response (204 No Content)**: Empty body.

### 1.5 Get Security Questions (Public)
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/auth/security-questions/{username}`
-   **Description**: Retrieve security questions for a user (used during password recovery). No authentication needed.
-   **Path Variable**: Replace `{username}` with the actual username (e.g., `testuser`).
-   **Expected Response (200 OK)**:
    ```json
    [
      { "question": "Question 1", "answer": "" },
      { "question": "Question 2", "answer": "" },
      { "question": "Question 3", "answer": "" }
    ]
    ```

### 1.6 Verify OTP (Login 2FA)
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/auth/verify-otp`
-   **Description**: Verify 2FA code during login process if 2FA is enabled for the account.
-   **Query Parameters**:
    -   `username`: `testuser`
    -   `code`: `123456` (Current code from Authenticator App)
-   **Expected Response (200 OK)**: Similar to Login response (returns tokens).

### 1.7 Reset Password (Forgot Password)
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/auth/reset-password`
-   **Description**: Reset master password using security answers if password is forgotten.
-   **Body (JSON)**:
    ```json
    {
      "username": "testuser",
      "newMasterPassword": "NewStrongPassword456!",
      "securityAnswers": [
        { "question": "Question 1", "answer": "Answer 1" },
        { "question": "Question 2", "answer": "Answer 2" },
        { "question": "Question 3", "answer": "Answer 3" }
      ]
    }
    ```
-   **Expected Response (200 OK)**:
    ```text
    Password changed successfully
    ```

### 1.8 Send OTP (Email)
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/auth/send-otp`
-   **Description**: Send a one-time password to the user's registered email address.
-   **Query Parameters**:
    -   `username`: `testuser`
-   **Example URL**: `http://localhost:8080/api/auth/send-otp?username=testuser`
-   **Expected Response (200 OK)**: `OTP sent to your email.`

---

## 👤 2. User Management (UserController)
**Base Path**: `/api/users`
**Headers**: `Authorization: Bearer <accessToken>` (All endpoints)

### 2.1 Get Profile
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/users/profile`
-   **Description**: Get current user details.
-   **Expected Response (200 OK)**:
    ```json
    {
      "id": 1,
      "email": "user@example.com",
      "username": "testuser",
      "is2faEnabled": false,
      "createdAt": "2026-02-12T10:00:00"
    }
    ```

### 2.2 Change Master Password
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/users/change-password`
-   **Description**: Change the master password while logged in.
-   **Body (JSON)**:
    ```json
    {
      "oldPassword": "StrongPassword123!",
      "newPassword": "NewStrongPassword789!"
    }
    ```
-   **Expected Response (200 OK)**: `Password changed successfully`

### 2.3 Get My Security Questions
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/users/security-questions`
-   **Description**: View current security questions configured for the account.
-   **Expected Response (200 OK)**: LIST of Question objects (without answers).

### 2.4 Update Security Questions
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/users/security-questions`
-   **Description**: Update security questions. Requires current master password for verification.
-   **Body (JSON)**:
    ```json
    {
      "masterPassword": "StrongPassword123!",
      "securityQuestions": [
        { "question": "New Q1", "answer": "New Ans1" },
        { "question": "New Q2", "answer": "New Ans2" },
        { "question": "New Q3", "answer": "New Ans3" }
      ]
    }
    ```
-   **Expected Response (200 OK)**: Empty body.

### 2.5 Request Account Deletion
-   **Method**: `DELETE`
-   **URL**: `http://localhost:8080/api/users/account`
-   **Description**: Schedule account for deletion.
-   **Body (JSON)**:
    ```json
    {
      "masterPassword": "StrongPassword123!",
      "confirmation": true
    }
    ```
-   **Expected Response (200 OK)**: `Account scheduled for deletion in 30 days...`

### 2.6 Cancel Account Deletion
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/users/account/cancel-deletion`
-   **Description**: Cancel a pending account deletion request.
-   **Expected Response (200 OK)**: `Account deletion cancelled.`

---

## 📱 3. Session Management (SessionController)
**Base Path**: `/api/sessions`
**Headers**: `Authorization: Bearer <accessToken>` (All endpoints)

### 3.1 Get All Active Sessions
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/sessions`
-   **Description**: List all active sessions for the user.
-   **Expected Response (200 OK)**:
    ```json
    [
      {
        "id": 1,
        "ipAddress": "127.0.0.1",
        "deviceInfo": "Chrome on Windows",
        "location": "localhost",
        "isActive": true,
        "createdAt": "2026-02-12T10:00:00",
        "lastAccessedAt": "2026-02-12T10:00:00",
        "expiresAt": "2026-02-12T10:15:00"
      }
    ]
    ```

### 3.2 Get Current Session
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/sessions/current`
-   **Description**: Get details of the current session associated with the token.

### 3.3 Terminate Session
-   **Method**: `DELETE`
-   **URL**: `http://localhost:8080/api/sessions/{sessionId}`
-   **Description**: Log out a specific session by ID.
-   **Path Variable**: `sessionId` (e.g., `1`)
-   **Expected Response (204 No Content)**: Empty body.

### 3.4 Terminate ALL Sessions
-   **Method**: `DELETE`
-   **URL**: `http://localhost:8080/api/sessions`
-   **Description**: Log out from all devices immediately.
-   **Expected Response (204 No Content)**: Empty body.

---

## 🛡️ 4. Two-Factor Authentication (TwoFactorController)
**Base Path**: `/api/2fa`
**Headers**: `Authorization: Bearer <accessToken>` (All endpoints)

### 4.1 Get 2FA Status
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/2fa/status`
-   **Description**: Check if 2FA is enabled for the current user.
-   **Expected Response (200 OK)**:
    ```json
    {
      "enabled": false
    }
    ```

### 4.2 Initialize Setup (Step 1)
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/2fa/setup`
-   **Description**: Generate a new secret key and QR code URL to be scanned by Authenticator App.
-   **Expected Response (200 OK)**:
    ```json
    {
      "secretKey": "JBSWY3DPEHPK3PXP",
      "qrCodeUrl": "otpauth://totp/RevPasswordManager:testuser?secret=JBSWY3DPEHPK3PXP&issuer=RevPasswordManager"
    }
    ```

### 4.3 Verify & Enable (Step 2)
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/2fa/verify-setup`
-   **Description**: Confirm setup with a code from the authenticator app to enable 2FA.
-   **Query Parameters**:
    -   `code`: `123456` (The 6-digit code from your app)
-   **Expected Response (200 OK)**:
    ```json
    {
      "success": true,
      "message": "2FA enabled successfully",
      "backupCodes": ["code1", "code2", "code3"...]
    }
    ```

### 4.4 Disable 2FA
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/2fa/disable`
-   **Description**: Turn off Two-Factor Authentication for the account.
-   **Query Parameters**: None.
-   **Body**: None.
-   **Expected Response (200 OK)**:
    ```json
    {
      "message": "2FA disabled successfully"
    }
    ```

### 4.5 Get Backup Codes
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/2fa/backup-codes`
-   **Description**: Retrieve the list of backup recovery codes. These are used if you lose access to your Authenticator App.
-   **Query Parameters**: None.
-   **Expected Response (200 OK)**:
    ```json
    [
      "abc1-def2",
      "ghi3-jkl4",
      "mno5-pqr6",
      ...
    ]
    ```

---

## 🗄️ 5. Vault Management (VaultController)
**Base Path**: `/api/vault`
**Headers**: `Authorization: Bearer <accessToken>` (All endpoints)

### 5.1 Create Vault Entry
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/vault`
-   **Body (JSON)**:
    ```json
    {
      "title": "Google",
      "username": "myemail@gmail.com",
      "password": "MySuperSecretPassword",
      "websiteUrl": "https://google.com",
      "notes": "Main account",
      "categoryId": 1,
      "folderId": null,
      "isFavorite": true
    }
    ```
-   **Expected Response (201 Created)**: Returns the created entry object (password masked).

### 5.2 Get All Entries
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/vault`
-   **Description**: List all entries. Passwords are masked/encrypted.
-   **Expected Response (200 OK)**:
    ```json
    [
      { "id": 1, "title": "Google", "username": "myemail@gmail.com", "websiteUrl": "https://google.com", "categoryId": 1 ... },
      ...
    ]
    ```

### 5.3 Get Single Entry
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/vault/{id}`
-   **Description**: Get full details of a specific entry (includes decrypted password and notes).
-   **Path Variable**: `id` (e.g., `1`)
-   **Expected Response (200 OK)**:
    ```json
    {
      "id": 1,
      "title": "Google",
      "username": "myemail@gmail.com",
      "password": "MySuperSecretPassword",
      "websiteUrl": "https://google.com",
      "notes": "Main account",
      "categoryId": 1,
      "categoryName": "Social Media",
      "folderId": null,
      "folderName": null,
      "isFavorite": true,
      "isHighlySensitive": false,
      "requiresSensitiveAuth": false,
      "createdAt": "2026-02-12T10:00:00",
      "updatedAt": "2026-02-12T10:00:00"
    }
    ```
-   **Note**: If the entry is highly sensitive (`isHighlySensitive: true`), the `password`, `username`, and `notes` fields will be masked as `"******"`. Use the `POST /api/vault/{id}/sensitive-view` endpoint to access the unmasked data.

### 5.4 Update Entry
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/vault/{id}`
-   **Body (JSON)**: Update desired fields (same structure as Create).
-   **Expected Response (200 OK)**: Updated entry object.

### 5.5 Delete Entry
-   **Method**: `DELETE`
-   **URL**: `http://localhost:8080/api/vault/{id}`
-   **Expected Response (204 No Content)**: Empty body.

### 5.6 View Raw Password
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/vault/entries/{id}/view-password`
-   **Description**: Retrieve the clear-text password for an entry.
-   **Expected Response (200 OK)**:
    ```json
    {
      "password": "MySuperSecretPassword"
    }
    ```

### 5.7 Toggle Favorite
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/vault/{id}/favorite`
-   **Description**: Toggle the favorite status of an entry.
-   **Expected Response (200 OK)**: Updated entry object with `isFavorite` flipped.

### 5.8 Get Favorites
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/vault/favorites`
-   **Description**: List all entries marked as favorite.

### 5.9 Bulk Delete
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/vault/entries/bulk-delete`
-   **Description**: Delete multiple entries at once.
-   **Body (JSON)**:
    ```json
    [1, 2, 3]
    ```
-   **Expected Response (204 No Content)**: Empty body.

---

## 📂 6. Categories (CategoryController)
**Base Path**: `/api/categories`
**Headers**: `Authorization: Bearer <accessToken>` (All endpoints)

### 6.1 Create Category
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/categories`
-   **Body (JSON)**:
    ```json
    {
      "name": "Social Media",
      "icon": "share"
    }
    ```
-   **Expected Response (201 Created)**: Created category object.

### 6.2 Get All Categories
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/categories`
-   **Expected Response (200 OK)**:
    ```json
    [
      { "id": 1, "name": "Social Media", "icon": "share", "isDefault": false, "createdAt": "2026-02-12T10:00:00", "entryCount": 0 },
      { "id": 2, "name": "Work", "icon": "briefcase", "isDefault": false, "createdAt": "2026-02-12T10:00:00", "entryCount": 0 }
    ]
    ```

### 6.3 Get Category by ID
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/categories/{id}`
-   **Path Variable**: `id`

### 6.4 Update Category
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/categories/{id}`
-   **Body (JSON)**:
    ```json
    {
      "name": "Socials Updated",
      "icon": "share-alt"
    }
    ```

### 6.5 Delete Category
-   **Method**: `DELETE`
-   **URL**: `http://localhost:8080/api/categories/{id}`
-   **Expected Response (204 No Content)**: Empty body.

### 6.6 Get Entries in Category
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/categories/{id}/entries`
-   **Description**: Get all vault entries belonging to a specific category.
-   **Path Variable**: `id` (e.g., `1`)
-   **Expected Response (200 OK)**: Placeholder — will return vault entries when fully wired.

---

## 📁 7. Folders (FolderController)
**Base Path**: `/api/folders`
**Headers**: `Authorization: Bearer <accessToken>` (All endpoints)

### 7.1 Create Folder
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/folders`
-   **Query Parameters**:
    -   `name`: `My Folder`
    -   `parentFolderId`: `1` (Optional, for nested folders)
-   **Example URL**: `http://localhost:8080/api/folders?name=Finance`
-   **Expected Response (201 Created)**: Created folder object.

### 7.2 Get Folders
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/folders`
-   **Expected Response (200 OK)**:
    ```json
    [
      { "id": 1, "name": "Finance", "parentFolderId": null, "subfolders": [], "createdAt": "2026-02-12T10:00:00", "updatedAt": "2026-02-12T10:00:00" }
    ]
    ```

### 7.3 Get Folder by ID
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/folders/{id}`

### 7.4 Update Folder Name
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/folders/{id}`
-   **Query Parameters**: `name=New Name`
-   **Example URL**: `http://localhost:8080/api/folders/1?name=My Finances`

### 7.5 Move Folder
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/folders/{id}/move`
-   **Query Parameters**: `parentId=2` (Use null or omit to move to root)

### 7.6 Delete Folder
-   **Method**: `DELETE`
-   **URL**: `http://localhost:8080/api/folders/{id}`

### 7.7 Get Entries in Folder
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/folders/{id}/entries`
-   **Description**: Get all vault entries contained within a specific folder.

---

## 🔑 8. Password Generator (PasswordGeneratorController)
**Base Path**: `/api/generator`
**Headers**: `Authorization: Bearer <accessToken>`

### 8.1 Generate Single Password
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/generator/generate`
-   **Body (JSON)**:
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
-   **Expected Response (200 OK)**:
    ```json
    {
      "password": "aBc1!2@3dEf4#5$6"
    }
    ```

### 8.2 Generate Multiple Passwords
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/generator/generate-multiple`
-   **Body (JSON)**:
    ```json
    {
      "length": 12,
      "count": 5,
      "includeUppercase": true,
      "includeLowercase": true,
      "includeNumbers": true,
      "includeSpecial": true,
      "excludeSimilar": false,
      "excludeAmbiguous": false
    }
    ```
-   **Expected Response (200 OK)**:
    ```json
    {
      "passwords": [
        "pass1...",
        "pass2...",
        "pass3...",
        "pass4...",
        "pass5..."
      ]
    }
    ```

### 8.3 Check Strength
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/generator/strength`
-   **Body (JSON)**:
    ```json
    { "password": "CheckThisPassword123!" }
    ```
-   **Expected Response (200 OK)**:
    ```json
    {
      "score": 85,
      "label": "Strong",
      "feedback": []
    }
    ```

### 8.4 Validate Password
-   **Method**: `POST`
-   **URL**: `http://localhost:8080/api/generator/validate`
-   **Body (JSON)**: same as Check Strength
-   **Description**: Validates if a password meets policy requirements.

### 8.5 Get Default Settings
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/generator/default-settings`
-   **Description**: Get user's default password generation preferences.

---

## ⚙️ 9. User Settings (UserSettingsController)
**Base Path**: `/api/settings`
**Headers**: `Authorization: Bearer <accessToken>`

### 9.1 Get Settings
-   **Method**: `GET`
-   **URL**: `http://localhost:8080/api/settings`
-   **Expected Response (200 OK)**:
    ```json
    {
      "theme": "DARK",
      "language": "en",
      "autoLogoutMinutes": 15
    }
    ```

### 9.2 Update Settings
-   **Method**: `PUT`
-   **URL**: `http://localhost:8080/api/settings`
-   **Body (JSON)**:
    ```json
    {
      "theme": "LIGHT",
      "language": "fr",
      "autoLogoutMinutes": 30
    }
    ```
