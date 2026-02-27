-- =============================================================================
-- Rev-PasswordManager Database Schema
-- Generated from DATABASE_AND_STRUCTURE.md
-- Database: MySQL / PostgreSQL compatible (adjust types as needed)
-- =============================================================================

-- =============================================================================
-- 1. USER & AUTHENTICATION (4 Tables)
-- =============================================================================

-- Table: users
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    master_password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    password_hint VARCHAR(255),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    is_2fa_enabled BOOLEAN DEFAULT FALSE,
    duress_password_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deletion_requested_at TIMESTAMP NULL,
    deletion_scheduled_at TIMESTAMP NULL
);

-- Table: security_questions
CREATE TABLE security_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_text VARCHAR(500) NOT NULL,
    answer_hash VARCHAR(255) NOT NULL,
    question_order INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: user_sessions
CREATE TABLE user_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(2048) NOT NULL UNIQUE,
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    location VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: recovery_codes
CREATE TABLE recovery_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    used_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================================
-- 2. TWO-FACTOR AUTHENTICATION (2 Tables)
-- =============================================================================

-- Table: two_factor_auth
CREATE TABLE two_factor_auth (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    secret_key VARCHAR(255) NOT NULL,
    is_enabled BOOLEAN DEFAULT FALSE,
    enabled_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: backup_codes (Element Collection for two_factor_auth)
CREATE TABLE backup_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    two_factor_auth_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    FOREIGN KEY (two_factor_auth_id) REFERENCES two_factor_auth(id) ON DELETE CASCADE
);

-- Table: otp_tokens
CREATE TABLE otp_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(10) NOT NULL,
    token_type VARCHAR(50),
    expiry_date TIMESTAMP NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================================
-- 3. VAULT & PASSWORDS (5 Tables)
-- =============================================================================

-- Table: categories
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    name VARCHAR(100) NOT NULL,
    icon VARCHAR(50),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: folders
CREATE TABLE folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    parent_folder_id BIGINT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Table: vault_entries
CREATE TABLE vault_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    category_id BIGINT,
    folder_id BIGINT,
    account_name VARCHAR(255) NOT NULL,
    website_url VARCHAR(2048),
    username VARCHAR(255),
    encrypted_password TEXT NOT NULL,
    notes TEXT,
    is_favorite BOOLEAN DEFAULT FALSE,
    is_highly_sensitive BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    password_strength INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP NULL,
    access_count INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY (folder_id) REFERENCES folders(id) ON DELETE SET NULL
);

-- Table: vault_trash
CREATE TABLE vault_trash (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vault_entry_id BIGINT NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    deleted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    permanent_delete_at TIMESTAMP NOT NULL,
    FOREIGN KEY (vault_entry_id) REFERENCES vault_entries(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: vault_snapshots (Password History)
CREATE TABLE vault_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vault_entry_id BIGINT NOT NULL,
    encrypted_password TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vault_entry_id) REFERENCES vault_entries(id) ON DELETE CASCADE
);

-- =============================================================================
-- 4. SECURITY & AUDIT (4 Tables)
-- =============================================================================

-- Table: audit_logs
CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Table: login_attempts
CREATE TABLE login_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    ip_address VARCHAR(45) NOT NULL,
    device_info VARCHAR(500),
    status VARCHAR(20) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Table: security_alerts
CREATE TABLE security_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: password_analysis
CREATE TABLE password_analysis (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    vault_entry_id BIGINT NOT NULL UNIQUE,
    strength_score INT,
    is_weak BOOLEAN DEFAULT FALSE,
    is_reused BOOLEAN DEFAULT FALSE,
    is_breached BOOLEAN DEFAULT FALSE,
    is_old BOOLEAN DEFAULT FALSE,
    analyzed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (vault_entry_id) REFERENCES vault_entries(id) ON DELETE CASCADE
);

-- =============================================================================
-- 5. SYSTEM & CONFIGURATION (3 Tables)
-- =============================================================================

-- Table: user_settings
CREATE TABLE user_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    theme VARCHAR(20) DEFAULT 'LIGHT',
    auto_lock_timeout INT DEFAULT 300,
    session_timeout INT DEFAULT 3600,
    password_generator_defaults JSON,
    notification_preferences JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: backup_exports
CREATE TABLE backup_exports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    export_format VARCHAR(20) NOT NULL,
    file_hash VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Table: notifications
CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =============================================================================
-- INDEXES FOR PERFORMANCE
-- =============================================================================

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token ON user_sessions(token(255));
CREATE INDEX idx_vault_entries_user_id ON vault_entries(user_id);
CREATE INDEX idx_vault_entries_category_id ON vault_entries(category_id);
CREATE INDEX idx_vault_entries_folder_id ON vault_entries(folder_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_login_attempts_user_id ON login_attempts(user_id);
CREATE INDEX idx_login_attempts_ip ON login_attempts(ip_address);
CREATE INDEX idx_otp_tokens_user_id ON otp_tokens(user_id);
CREATE INDEX idx_otp_tokens_expiry ON otp_tokens(expiry_date);

-- =============================================================================
-- END OF SCHEMA
-- =============================================================================

-- =============================================================================
-- TABLE RELATIONSHIPS REFERENCE
-- =============================================================================
--
-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │                         ONE-TO-ONE RELATIONSHIPS                            │
-- ├─────────────────────────────────────────────────────────────────────────────┤
-- │  users (1) ────────── (1) two_factor_auth       [user_id UNIQUE]            │
-- │  users (1) ────────── (1) user_settings         [user_id UNIQUE]            │
-- │  vault_entries (1) ── (1) vault_trash           [vault_entry_id UNIQUE]     │
-- │  vault_entries (1) ── (1) password_analysis     [vault_entry_id UNIQUE]     │
-- └─────────────────────────────────────────────────────────────────────────────┘
--
-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │                         ONE-TO-MANY RELATIONSHIPS                           │
-- ├─────────────────────────────────────────────────────────────────────────────┤
-- │  users (1) ────────── (*) security_questions                                │
-- │  users (1) ────────── (*) user_sessions                                     │
-- │  users (1) ────────── (*) recovery_codes                                    │
-- │  users (1) ────────── (*) otp_tokens                                        │
-- │  users (1) ────────── (*) categories                                        │
-- │  users (1) ────────── (*) folders                                           │
-- │  users (1) ────────── (*) vault_entries                                     │
-- │  users (1) ────────── (*) audit_logs                                        │
-- │  users (1) ────────── (*) login_attempts                                    │
-- │  users (1) ────────── (*) security_alerts                                   │
-- │  users (1) ────────── (*) backup_exports                                    │
-- │  users (1) ────────── (*) notifications                                     │
-- │                                                                             │
-- │  categories (1) ──── (*) vault_entries                                      │
-- │  folders (1) ─────── (*) vault_entries                                      │
-- │  folders (1) ─────── (*) folders               [Self-referential: parent]   │
-- │  vault_entries (1) ─ (*) vault_snapshots       [Password history]           │
-- │  two_factor_auth (1) (*) backup_codes          [Element collection]         │
-- └─────────────────────────────────────────────────────────────────────────────┘
--
-- ┌─────────────────────────────────────────────────────────────────────────────┐
-- │                         ENTITY RELATIONSHIP DIAGRAM                         │
-- ├─────────────────────────────────────────────────────────────────────────────┤
-- │                                                                             │
-- │                              ┌──────────┐                                   │
-- │                              │  users   │                                   │
-- │                              └────┬─────┘                                   │
-- │        ┌───────────────┬─────────┼─────────┬───────────────┐               │
-- │        │               │         │         │               │               │
-- │        ▼               ▼         ▼         ▼               ▼               │
-- │  ┌──────────┐   ┌──────────┐  ┌────────┐ ┌────────┐  ┌──────────┐          │
-- │  │ sessions │   │security_ │  │recovery│ │  2fa   │  │categories│          │
-- │  └──────────┘   │questions │  │ codes  │ └───┬────┘  └────┬─────┘          │
-- │                 └──────────┘  └────────┘     │            │                │
-- │                                              ▼            │                │
-- │                                        ┌──────────┐       │                │
-- │                                        │ backup_  │       │                │
-- │                                        │  codes   │       │                │
-- │                                        └──────────┘       │                │
-- │        ┌─────────────────────────────────────────────────┘                 │
-- │        │            ┌──────────────┐                                       │
-- │        │            │   folders    │◄────┐ (self-ref)                      │
-- │        │            └──────┬───────┘     │                                 │
-- │        │                   │             │                                 │
-- │        ▼                   ▼             │                                 │
-- │  ┌─────────────────────────────────┐     │                                 │
-- │  │         vault_entries           │─────┘                                 │
-- │  └───────────────┬─────────────────┘                                       │
-- │          ┌───────┼───────┐                                                 │
-- │          ▼       ▼       ▼                                                 │
-- │    ┌─────────┐ ┌─────┐ ┌──────────┐                                        │
-- │    │snapshots│ │trash│ │ analysis │                                        │
-- │    └─────────┘ └─────┘ └──────────┘                                        │
-- │                                                                             │
-- │  users (1) ──► audit_logs, login_attempts, security_alerts, notifications  │
-- │                backup_exports, otp_tokens, user_settings                   │
-- │                                                                             │
-- └─────────────────────────────────────────────────────────────────────────────┘
--
-- =============================================================================
-- FOREIGN KEY SUMMARY
-- =============================================================================
--
--  Parent Table       │ Child Table         │ FK Column          │ On Delete
-- ────────────────────┼─────────────────────┼────────────────────┼───────────
--  users              │ security_questions  │ user_id            │ CASCADE
--  users              │ user_sessions       │ user_id            │ CASCADE
--  users              │ recovery_codes      │ user_id            │ CASCADE
--  users              │ two_factor_auth     │ user_id            │ CASCADE
--  users              │ otp_tokens          │ user_id            │ CASCADE
--  users              │ categories          │ user_id            │ CASCADE
--  users              │ folders             │ user_id            │ CASCADE
--  users              │ vault_entries       │ user_id            │ CASCADE
--  users              │ audit_logs          │ user_id            │ SET NULL
--  users              │ login_attempts      │ user_id            │ SET NULL
--  users              │ security_alerts     │ user_id            │ CASCADE
--  users              │ user_settings       │ user_id            │ CASCADE
--  users              │ backup_exports      │ user_id            │ CASCADE
--  users              │ notifications       │ user_id            │ CASCADE
--  two_factor_auth    │ backup_codes        │ two_factor_auth_id │ CASCADE
--  categories         │ vault_entries       │ category_id        │ SET NULL
--  folders            │ vault_entries       │ folder_id          │ SET NULL
--  folders            │ folders             │ parent_folder_id   │ SET NULL
--  vault_entries      │ vault_trash         │ vault_entry_id     │ CASCADE
--  vault_entries      │ vault_snapshots     │ vault_entry_id     │ CASCADE
--  vault_entries      │ password_analysis   │ vault_entry_id     │ CASCADE
-- ────────────────────┴─────────────────────┴────────────────────┴───────────
