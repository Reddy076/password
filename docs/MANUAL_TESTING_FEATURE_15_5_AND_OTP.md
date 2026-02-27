# Manual Testing Guide: Feature 15.5 & Email OTP

This guide provides step-by-step instructions to manually verify **Feature 15.5 (Highly Sensitive Entry Access)** and the **Email OTP** functionality using Postman or cURL.

## Prerequisites

1.  **Application Running**: Ensure the Spring Boot application is running (`mvn spring-boot:run`).
2.  **SMTP Configured**: Ensure `application.properties` has valid SMTP credentials (Gmail App Password).
3.  **User Created**: You need a registered user (e.g., `testuser`) with a master password (e.g., `password`).

---

## Part 1: Feature 15.5 - Highly Sensitive Entries

### 1. Login
Get an access token for your user.
*   **Method**: `POST`
*   **URL**: `http://localhost:8080/api/auth/login`
*   **Body** (JSON):
    ```json
    {
      "username": "testuser",
      "masterPassword": "password"
    }
    ```
*   **Response**: Copy the `accessToken`.

### 2. Create a Highly Sensitive Entry
Create a vault entry with the `isHighlySensitive` flag set to `true`.
*   **Method**: `POST`
*   **URL**: `http://localhost:8080/api/vault`
*   **Headers**: `Authorization: Bearer <accessToken>`
*   **Body** (JSON):
    ```json
    {
      "title": "My Bank",
      "username": "mybankuser",
      "password": "SuperSecretPassword123",
      "websiteUrl": "https://bank.com",
      "notes": "Bank account",
      "categoryId": 1,
      "folderId": null,
      "isFavorite": false,
      "isHighlySensitive": true
    }
    ```
*   **Verify**: Note the `id` from the response (e.g., `1`). Then call `GET /api/vault/1` to confirm `"isHighlySensitive": true` is present in the detail response.

### 3. Verify Masking (Standard Retrieval)
Try to view the entry using the standard GET endpoint. The sensitive fields should be masked.
*   **Method**: `GET`
*   **URL**: `http://localhost:8080/api/vault/1`
*   **Headers**: `Authorization: Bearer <accessToken>`
*   **Verify**:
    *   `password`: `******`
    *   `username`: `******`
    *   `notes`: `******` (if any)

### 4. Access Sensitive View (Password Only)
If 2FA is **NOT** enabled for the user, you only need the Master Password.
*   **Method**: `POST`
*   **URL**: `http://localhost:8080/api/vault/1/sensitive-view`
*   **Headers**: `Authorization: Bearer <accessToken>`
*   **Body** (JSON):
    ```json
    {
      "masterPassword": "password"
    }
    ```
*   **Verify**: Response should show **decrypted** `username` and `password`.

---

## Part 2: Email OTP Configuration & Testing

### 1. Enable 2FA (Optional but Recommended)
To fully test the OTP flow with Feature 15.5, enable 2FA for the user.
*   **Method**: `POST`
*   **URL**: `http://localhost:8080/api/2fa/setup`
*   **Headers**: `Authorization: Bearer <accessToken>`
*   **Verify**: App returns a Secret Key and QR URL.
*   **Confirm Setup**: Call `POST /api/2fa/verify-setup?code=123456` with a code from Google Authenticator to enable it. (Or skip this if you just want to test Email OTP as a standalone mechanism, but Feature 15.5 requires 2FA to be enabled on the user to request an OTP).

### 2. Request Email OTP
Trigger the email OTP.
*   **Method**: `POST`
*   **URL**: `http://localhost:8080/api/auth/send-otp?username=testuser`
*   **Verify**:
    *   Response: "OTP sent to your email."
    *   **Check Inbox**: You should receive an email with a 6-digit code (e.g., `123456`).

### 3. Access Sensitive View (Password + OTP)
Now try to access the sensitive entry again, assuming 2FA is enabled (or if the system enforces OTP for sensitive views regardless).
*   **Method**: `POST`
*   **URL**: `http://localhost:8080/api/vault/1/sensitive-view`
*   **Headers**: `Authorization: Bearer <accessToken>`
*   **Body** (JSON):
    ```json
    {
      "masterPassword": "password",
      "otpToken": "123456"
    }
    ```
    > **Note**: The `otpToken` value is the 6-digit code received from the email.
*   **Verify**: Response should show **decrypted** data.

---

## Troubleshooting
*   **500 Error on Send OTP**: Check server console. Likely authentication failed with SMTP server (wrong password) or connection timed out.
*   **"User not found"**: Ensure the username in the URL query parameter matches exactly.
*   **"Invalid credentials"**: Check Master Password.
*   **"Invalid OTP"**: Code matched neither TOTP nor Email OTP, or expired (15 mins).
