# 🚨 API Coverage Gap Report

Based on a cross-reference between the original `docs/API_DOCUMENTATION.md` (which lists 97 planned endpoints) and the actual 82 endpoints implemented in the codebase, here is a detailed breakdown of the discrepancies.

---

## 🛑 APIs Documented but MISSING from Codebase

These endpoints were planned in the design document but have not been implemented in the Java controllers:

### 1. Authentication (`/api/auth`)
- `POST /api/auth/forgot-password` *(Logic merged directly into reset-password)*
- `POST /api/auth/verify-security-questions` *(Logic merged directly into reset-password)*
- `POST /api/auth/verify-captcha` *(CAPTCHA verification merged into login logic)*
- `GET /api/auth/validate-token`
- `POST /api/auth/verify-master-password`
- `POST /api/auth/resend-otp`

### 2. User Management (`/api/users`)
- `GET /api/users/dashboard`
- `PUT /api/users/read-only-mode` *(State managed via UserSettings instead)*

### 3. Vault & Trash (`/api/vault`)
- `GET /api/vault/filter` *(Logic merged into generic `/api/vault/search`)*
- `PUT /api/vault/entries/{id}/sensitive` *(Property toggled via standard PUT update)*
- `GET /api/vault/recent`
- `GET /api/vault/recently-used`

### 4. Security & Auditing (`/api/security`)
- `GET /api/security/weak-passwords`
- `GET /api/security/reused-passwords`
- `GET /api/security/old-passwords`
- `POST /api/security/analyze-vault`

### 5. Two-Factor Authentication (`/api/2fa`)
- `POST /api/2fa/regenerate-codes`

### 6. Session Management (`/api/sessions`)
- `POST /api/sessions/extend`

### 7. Backup & Export (`/api/backup`)
- `GET /api/backup/export/preview`
- `POST /api/backup/import/validate`
- `GET /api/backup/snapshots` *(History is currently scoped per-entry in VaultController)*
- `POST /api/backup/snapshots/{id}/restore`
- `GET /api/backup/import-external/formats`

### 8. System Health (`/api/health`)
- `GET /api/health/db` *(Handled by default `/api/health` response Object)*
- `GET /api/health/services` *(Handled by default `/api/health` response Object)*

---

## 🆕 APIs Implemented but MISSING from Documentation

These endpoints are fully functional in the Java controllers but were never added to `API_DOCUMENTATION.md`:

### 1. AuthController
- `PUT /api/auth/password-hint` (Update hint)
- `POST /api/auth/set-duress-password` (Set the fake vault password)
- `GET /api/auth/security-questions/{username}` (Fetch questions before reset)

### 2. UserController
- `POST /api/users/account/cancel-deletion` (Cancel scheduled account wiping)

### 3. VaultController
- `POST /api/vault/{id}/sensitive-view` (Unlock highly sensitive items)
- `GET /api/vault/entries/{id}/history` (Get snapshot history for a specific entry)
- `GET /api/vault/trash/count` (Get number of items in trash)
- `POST /api/vault/trash/restore-all` (Restore everything in trash at once)

### 4. UserSettingsController (Entire Controller completely missing from docs)
- `GET /api/settings`
- `PUT /api/settings`

---

## 💡 Summary

*   **Total Documented APIs:** 97
*   **Total Actual Implemented APIs:** 82
*   **"Missing" Planned APIs:** ~24 (Note: Many of these were consolidated or merged into other endpoints to make the API cleaner).
*   **"Bonus" Implemented APIs:** ~10 (Endpoints created to support features like User Settings and Duress Mode).
