# 🔒 Feature 32: Password Hints & Read-Only Mode

## Overview
This feature enhances security and usability by adding:
1.  **Password Hints**: Optional hints displayed after failed login attempts.
2.  **Read-Only Mode**: A secure mode for public devices where users can view plain data but cannot modify, create, or delete vault entries.

## 📂 Internal Files
The following files are included in this folder as an archive of the feature implementation:

| File | Purpose |
|------|---------|
| `User.java` | Added `passwordHint` field |
| `UserSettings.java` | Added `readOnlyMode` boolean field |
| `AuthController.java` | Endpoints for setting/getting password hints |
| `UserSettingsController.java` | Endpoints for toggling read-only mode |
| `VaultService.java` | Logic to block write operations when read-only mode is active |
| `UserSettingsService.java` | Logic to persist read-only setting |
| `*.java` (DTOs/Tests) | Supporting data transfer objects and unit tests |

## 🛠️ Implementation Details

### 1. Read-Only Enforcement
**Location:** `VaultService.java`

We added a `checkReadOnlyMode(User)` helper method that is called at the start of every write operation (`create`, `update`, `delete`, `toggleFavorite`, `bulkDelete`).

```java
private void checkReadOnlyMode(User user) {
  userSettingsRepository.findByUserId(user.getId())
      .ifPresent(settings -> {
        if (Boolean.TRUE.equals(settings.getReadOnlyMode())) {
          throw new IllegalStateException(
              "Vault is in read-only mode. Disable read-only mode in settings to make changes.");
        }
      });
}
```

### 2. Password Hints
**Location:** `AuthController.java`

Simple GET/PUT endpoints allow users to manage their hint.

- `GET /api/auth/password-hint/{username}` (Public)
- `PUT /api/auth/password-hint` (Authenticated)

## ✅ Verification
We verified this feature with **8 new unit tests**:
- **6 tests in `VaultServiceTest`**: Confirming that all 5 write methods throw `IllegalStateException` when read-only mode is on.
- **2 tests in `UserSettingsServiceTest`**: Confirming the setting can be toggled and retrieved.
