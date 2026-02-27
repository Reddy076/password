# 🧪 Manual Testing Guide (Postman)

Use this guide to verify the implemented features using Postman or any HTTP client.

## 1️⃣ Feature: User Registration
**Endpoint:** `POST http://localhost:8080/api/auth/register`
**Auth:** None

**Body (JSON):**
```json
{
  "username": "testuser",
  "email": "test@example.com",
  "masterPassword": "StrongPassword123!",
  "securityQuestions": [
    { "question": "City?", "answer": "Paris" },
    { "question": "Pet?", "answer": "Dog" },
    { "question": "Color?", "answer": "Blue" }
  ]
}
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "is2faEnabled": false,
  "createdAt": "..."
}
```

---

## 2️⃣ Feature: User Login
**Endpoint:** `POST http://localhost:8080/api/auth/login`
**Auth:** None

**Body (JSON):**
```json
{
  "username": "testuser",
  "masterPassword": "StrongPassword123!"
}
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "ey...",
  "refreshToken": "ey...",
  "tokenType": "Bearer",
  "username": "testuser",
  "expiresIn": 900000,
  "requires2FA": false
}
```

> [!IMPORTANT]
> **Copy the `accessToken`** from the response. You will need it for the next steps.

---

## 3️⃣ Feature: Account Deletion (Schedule)
**Endpoint:** `DELETE http://localhost:8080/api/users/account`
**Auth:** Bearer Token (Paste `accessToken`)

**Body (JSON):**
```json
{
  "masterPassword": "StrongPassword123!",
  "confirmation": true
}
```

**Expected Response (200 OK):**
```text
Account scheduled for deletion in 30 days. You can cancel this action by logging in and using the cancel endpoint.
```

---

## 4️⃣ Feature: Cancel Account Deletion
**Endpoint:** `POST http://localhost:8080/api/users/account/cancel-deletion`
**Auth:** Bearer Token (Paste `accessToken`)

**Body:** None

**Expected Response (200 OK):**
```text
Account deletion cancelled.
```

---

## 5️⃣ Feature: Refresh Token
**Endpoint:** `POST http://localhost:8080/api/auth/refresh-token`
**Auth:** None

**Body (JSON):**
```json
{
  "refreshToken": "YOUR_REFRESH_TOKEN"
}
```

**Expected Response (200 OK):**
```json
{
  "accessToken": "ey...",
  "refreshToken": "ey...",
  "tokenType": "Bearer",
  "username": "testuser",
  "expiresIn": 900000,
  "requires2FA": false
}
```

---

## 6️⃣ Feature: User Profile
**Endpoint:** `GET http://localhost:8080/api/users/profile`
**Auth:** Bearer Token

**Body:** None

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
    "is2faEnabled": false,
  "createdAt": "..."
}
```

---

## 7️⃣ Feature: Change Password (Maintenance)
> **Use Case**: You are logged in and want to change your password.
> **Requires**: `oldPassword` and Bearer Token.

**Endpoint:** `PUT http://localhost:8080/api/users/change-password`
**Auth:** Bearer Token

**Body (JSON):**
```json
{
  "oldPassword": "OldStrongPassword123!",
  "newPassword": "NewStrongPassword456!"
}
```

**Expected Response (200 OK):**
```text
Password changed successfully
```
> **Verify:** Logout and try logging in with the NEW password.

---

## 8️⃣ Feature: Current Session
**Endpoint:** `GET http://localhost:8080/api/sessions/current`
**Auth:** Bearer Token

**Body:** None

**Expected Response (200 OK):**
```json
{
  "id": 101,
  "deviceInfo": "PostmanRuntime/7.29.2",
  "ipAddress": "127.0.0.1",
  "lastAccessedAt": "...",
  "active": true
}
```

---

## 9️⃣ Feature: Logout
**Endpoint:** `POST http://localhost:8080/api/auth/logout`
**Auth:** Bearer Token

**Body:** None

**Expected Response (204 No Content):**
```text
(Empty Body)
```
> **Verify:** Try to use the `accessToken` again (e.g., Get Profile). It should fail (401 or 403).

---

## 🔟 Feature: Security Questions (Management)
**Endpoint:** `GET http://localhost:8080/api/users/security-questions`
**Auth:** Bearer Token

**Body:** None

**Expected Response (200 OK):**
```json
[
  { "question": "City?", "answer": "" },
  { "question": "Pet?", "answer": "" },
  { "question": "Color?", "answer": "" }
]
```

**Endpoint:** `PUT http://localhost:8080/api/users/security-questions`
**Auth:** Bearer Token

**Body (JSON):**
```json
{
  "masterPassword": "StrongPassword123!",
  "securityQuestions": [
    { "question": "New City?", "answer": "London" },
    { "question": "New Pet?", "answer": "Cat" },
    { "question": "New Color?", "answer": "Red" }
  ]
}
```

**Expected Response (200 OK):**
```text
(Empty Body)
```

---

## 1️⃣1️⃣ Feature: Password Reset (Recovery)
> **Use Case**: You forgot your password and are locked out.
> **Requires**: Security Answers. No Token required (Public Endpoint).

**Endpoint:** `GET http://localhost:8080/api/auth/security-questions/testuser`
**Auth:** None (Public)

**Body:** None

**Expected Response (200 OK):**
```json
[
  { "question": "City?", "answer": "" },
  { "question": "Pet?", "answer": "" },
  { "question": "Color?", "answer": "" }
]
```

**Endpoint:** `POST http://localhost:8080/api/auth/reset-password`
**Auth:** None (Public)

**Body (JSON):**
```json
{
  "username": "testuser",
  "newMasterPassword": "NewStrongPassword456!",
  "securityAnswers": [
    { "question": "City?", "answer": "Paris" },
    { "question": "Pet?", "answer": "Dog" },
    { "question": "Color?", "answer": "Blue" }
  ]
}
```

**Expected Response (200 OK):**
```text
Password changed successfully
```
> **Verify:** Try logging in with the old password (should fail) and new password (should succeed).

