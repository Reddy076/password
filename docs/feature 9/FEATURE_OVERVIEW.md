# Feature 9: Two-Factor Authentication (2FA) Overview

## What is it?
Two-Factor Authentication (2FA) adds an extra layer of security to your Password Manager account. instead of only entering a password to log in, you will also be required to enter a code that is generated on your mobile device.

This implementation uses **Time-based One-Time Passwords (TOTP)**, which is the industry standard used by Google Authenticator, Authy, Microsoft Authenticator, etc.

## Why is it useful?
- **Enhanced Security:** Even if a hacker steals your Master Password, they cannot access your account without your phone.
- **Industry Standard:** Uses secure, rotating codes that change every 30 seconds.
- **Account Recovery:** Includes "Backup Codes" in case you lose your phone, ensuring you never get locked out completely.

## How to Access & Use

### 1. Enabling 2FA
1.  Log in to the application.
2.  Navigate to **Settings** > **Security** (or access the API endpoint `/api/2fa/setup`).
3.  The system will generate a **QR Code**.
4.  Open your authenticator app (e.g., Google Authenticator) and scan the QR code.
5.  Enter the 6-digit code displayed on your phone into the application to verify and enable 2FA.

### 2. Logging In
1.  Enter your Username and Master Password as usual.
2.  The system will detect 2FA is enabled and ask for a verification code.
3.  Open your authenticator app, find the code for `Rev-PasswordManager`, and enter it.
4.  You will be granted access only if the code is correct.

### 3. Disabling 2FA
1.  Log in (requires 2FA).
2.  Navigate to **Settings** (or call `/api/2fa/disable`).
3.  Confirm you want to turn off 2FA.
4.  Future logins will only require your password.

## Backup Codes
When you enable 2FA, you are given a set of **10 Backup Codes**.
- **Save these codes** in a safe place (e.g., printed out or in a different secure location).
- If you lose your phone, you can use one of these codes instead of the 6-digit TOTP code to log in.
- Each code can be used only once.
