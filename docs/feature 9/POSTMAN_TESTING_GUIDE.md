# Feature 9: Two-Factor Authentication (2FA) - Postman Testing Guide

This guide details how to manually test the 2FA flow using Postman.

## Environment Setup
- **Base URL:** `http://localhost:8080`
- **Auth Header:** Most endpoints require `Authorization: Bearer <your_token>`

## Prerequisites
- User must be registered and logged in.
- Google Authenticator (or similar app) installed on your phone.

---

## 1. Login (Initial)
**Endpoint:** `POST http://localhost:8080/api/auth/login`
**Body:**
```json
{
  "username": "testuser",
  "masterPassword": "password123"
}
```
**Response:**
- `accessToken`: (JWT Token) - *Save this for standard requests*
- `requires2FA`: `false`

---

## 2. Setup 2FA
**Endpoint:** `POST http://localhost:8080/api/2fa/setup`
**Header:** `Authorization: Bearer <access_token>`
**Body:** *None*
**Response:**
```json
{
    "secretKey": "JBSWY3DPEHPK3PXP...",
    "qrCodeUrl": "data:image/png;base64,..."
}
```
**Action:**
1.  **Method A (Easiest):** Copy the entire `qrCodeUrl` string (starting with `data:image/png;base64...`) and paste it directly into your browser's address bar. The QR code will appear. Scan it with your app.
2.  **Method B (Manual):** Enter the `secretKey` manually into your authenticator app (select "Enter setup key").
3.  **Method C (Online Tool):** Copy the Base64 part (after the comma) and use an online "Base64 to Image" converter.

---

## 3. Verify & Enable 2FA
**Endpoint:** `POST http://localhost:8080/api/2fa/verify-setup?code=<code>`
**Header:** `Authorization: Bearer <access_token>`
**Params:**
- `code`: The **6-digit code** from your **Google Authenticator App** (e.g., `123456`). Do NOT use a backup code here.
**Response:**
```json
{
    "success": true,
    "message": "2FA enabled successfully",
    "backupCodes": [
        "ABC1234567",
        "XYZ9876543",
        ...
    ]
}
```
**Important:** The response contains your **Recovery Codes**. Save these list of codes safely! You use them only if you lose your phone.

---

## 4. Test 2FA Login Flow
Now that 2FA is enabled, try logging in again.

### Step 4a: Attempt Standard Login
**Endpoint:** `POST http://localhost:8080/api/auth/login`
**Body:**
```json
{
  "username": "testuser",
  "masterPassword": "password123"
}
```
**Response:**
```json
{
    "accessToken": null,
    "refreshToken": null,
    "username": "testuser",
    "requires2FA": true
}
```
**Observation:** No token is returned. The app waits for the 2FA code.

### Step 4b: Verify OTP (Complete Login)
**Endpoint:** `POST http://localhost:8080/api/auth/verify-otp`

**How to add Query Parameters in Postman:**
1.  Click the **"Params"** tab (next to "Auth", "Headers").
2.  Add these Key-Value pairs:
    *   **Key:** `username` | **Value:** `testuser` (your username)
    *   **Key:** `code`     | **Value:** `123456` (from your Google Authenticator app)
*(Alternatively, you can just type them in the URL like this: `.../verify-otp?username=testuser&code=123456`)*

**Response:**
```json
{
    "accessToken": "eyJhbGciOiJIUzI...",
    "refreshToken": "...",
    "username": "testuser",
    "requires2FA": false
}
```
**Success:** You now have a valid `accessToken`.

---

## 5. View 2FA Status
**Endpoint:** `GET http://localhost:8080/api/2fa/status`
**Header:** `Authorization: Bearer <access_token>`
**Response:**
```json
{
    "enabled": true
}
```

---

## 6. Get Backup Codes
**Endpoint:** `GET http://localhost:8080/api/2fa/backup-codes`
**Header:** `Authorization: Bearer <access_token>`
**Response:**
```json
[
    "ABC1234567",
    "XYZ9876543",
    ...
]
```

---

## 7. Disable 2FA
**Endpoint:** `POST http://localhost:8080/api/2fa/disable`
**Header:** `Authorization: Bearer <access_token>`
**Response:**
```json
{
    "message": "2FA disabled successfully"
}
```

## Troubleshooting
- **Invalid Code?** Ensure your phone's time is synced with the server time (NTP). TOTP relies on accurate time.
- **401 Unauthorized?** Ensure you are sending the `Authorization: Bearer <token>` header for setup/disable endpoints.
- **405 Method Not Allowed?** Ensure you are using **POST** for `/api/2fa/disable`, `/verify-setup`, etc. Do NOT use **GET**.
