# Database Schema Documentation

This document describes the complete database schema for the **Rev-PasswordManager** application, generated from the JPA entity classes.

---

## Entity Relationship Diagram (ERD)

```mermaid
erDiagram
    users ||--o{ user_sessions : "has many"
    users ||--o{ security_questions : "has many"
    users ||--o{ recovery_codes : "has many"
    users ||--o| two_factor_auth : "has one"
    users ||--o{ otp_tokens : "has many"
    two_factor_auth ||--o{ backup_codes : "has many"

    users {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR username UK
        VARCHAR master_password_hash
        VARCHAR salt
        BOOLEAN is_2fa_enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
        TIMESTAMP deletion_requested_at
        TIMESTAMP deletion_scheduled_at
    }

    user_sessions {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR token UK
        VARCHAR ip_address
        VARCHAR device_info
        VARCHAR location
        BOOLEAN is_active
        TIMESTAMP created_at
        TIMESTAMP last_accessed_at
        TIMESTAMP expires_at
    }

    security_questions {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR question_text
        VARCHAR answer_hash
    }

    recovery_codes {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR code_hash
        BOOLEAN is_used
        TIMESTAMP created_at
    }

    two_factor_auth {
        BIGINT id PK
        BIGINT user_id FK_UK
        VARCHAR secret_key
        BOOLEAN is_enabled
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }

    backup_codes {
        BIGINT two_factor_auth_id FK
        VARCHAR code
    }

    otp_tokens {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR token
        TIMESTAMP expiry_date
        BOOLEAN is_used
        VARCHAR token_type
    }
```

---

## Table Definitions

### 1. `users`
The central table storing user account information.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Unique user identifier |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE | User's email address |
| `username` | VARCHAR(255) | NOT NULL, UNIQUE | User's login name |
| `master_password_hash` | VARCHAR(255) | NOT NULL | BCrypt hash of master password |
| `salt` | VARCHAR(255) | NOT NULL | Unique salt for encryption |
| `is_2fa_enabled` | BOOLEAN | DEFAULT FALSE | Whether 2FA is active |
| `created_at` | TIMESTAMP | AUTO (Hibernate) | Account creation time |
| `updated_at` | TIMESTAMP | AUTO (Hibernate) | Last update time |
| `deletion_requested_at` | TIMESTAMP | NULLABLE | When deletion was requested |
| `deletion_scheduled_at` | TIMESTAMP | NULLABLE | When deletion will occur |

---

### 2. `user_sessions`
Tracks active login sessions for each user.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Session identifier |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Associated user |
| `token` | VARCHAR(2048) | NOT NULL, UNIQUE | JWT token (or hash) |
| `ip_address` | VARCHAR(255) | NULLABLE | Client IP address |
| `device_info` | VARCHAR(255) | NULLABLE | Browser/device info |
| `location` | VARCHAR(255) | NULLABLE | Geolocation (if available) |
| `is_active` | BOOLEAN | DEFAULT TRUE | Is session valid? |
| `created_at` | TIMESTAMP | AUTO (Hibernate) | Session start time |
| `last_accessed_at` | TIMESTAMP | NULLABLE | Last activity time |
| `expires_at` | TIMESTAMP | NULLABLE | Session expiration |

**Relationship:** Many-to-One with `users`

---

### 3. `security_questions`
Stores user security questions for account recovery.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Question identifier |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Associated user |
| `question_text` | VARCHAR(255) | NOT NULL | The security question |
| `answer_hash` | VARCHAR(255) | NOT NULL | Hashed answer |

**Relationship:** Many-to-One with `users`

---

### 4. `recovery_codes`
One-time backup codes for account recovery.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Code identifier |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Associated user |
| `code_hash` | VARCHAR(255) | NOT NULL | Hashed recovery code |
| `is_used` | BOOLEAN | NOT NULL | Has code been used? |
| `created_at` | TIMESTAMP | NOT NULL | Generation time |

**Relationship:** Many-to-One with `users`

---

### 5. `two_factor_auth`
Stores 2FA configuration for each user.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | 2FA config identifier |
| `user_id` | BIGINT | FK → users(id), NOT NULL, UNIQUE | One per user |
| `secret_key` | VARCHAR(255) | NOT NULL | TOTP secret key |
| `is_enabled` | BOOLEAN | NOT NULL, DEFAULT FALSE | Is 2FA active? |
| `created_at` | TIMESTAMP | NOT NULL | Setup time |
| `updated_at` | TIMESTAMP | AUTO (Hibernate) | Last update |

**Relationship:** One-to-One with `users`

---

### 6. `backup_codes`
Collection table for 2FA backup codes.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `two_factor_auth_id` | BIGINT | FK → two_factor_auth(id) | Parent 2FA config |
| `code` | VARCHAR(255) | | Backup code value |

**Relationship:** Element collection of `two_factor_auth`

---

### 7. `otp_tokens`
Temporary tokens for email verification, password reset, etc.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Token identifier |
| `user_id` | BIGINT | FK → users(id), NOT NULL | Associated user |
| `token` | VARCHAR(255) | NOT NULL | The OTP code |
| `expiry_date` | TIMESTAMP | NOT NULL | When token expires |
| `is_used` | BOOLEAN | NOT NULL, DEFAULT FALSE | Has been consumed? |
| `token_type` | VARCHAR(255) | NULLABLE | Purpose (EMAIL_VERIFY, etc.) |

**Relationship:** Many-to-One with `users`

---

## Relationship Summary

| Parent Table | Child Table | Relationship | Cardinality |
|--------------|-------------|--------------|-------------|
| `users` | `user_sessions` | One-to-Many | 1:N |
| `users` | `security_questions` | One-to-Many | 1:N |
| `users` | `recovery_codes` | One-to-Many | 1:N |
| `users` | `two_factor_auth` | One-to-One | 1:1 |
| `users` | `otp_tokens` | One-to-Many | 1:N |
| `two_factor_auth` | `backup_codes` | Element Collection | 1:N |

---

## Notes
- All tables use **auto-increment BIGINT** primary keys.
- **Hibernate** manages timestamps via `@CreationTimestamp` and `@UpdateTimestamp`.
- The `two_factor_auth` ↔ `users` relationship is **one-to-one** (enforced by `UNIQUE` on `user_id`).
- Sensitive data like passwords, answers, and codes are stored as **hashes**, not plaintext.
