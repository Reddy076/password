-- ============================================================
-- Dummy Data for Rev-PasswordManager (user: Reddy)
-- Run this AFTER the application has started and tables exist.
-- Uses the existing "Reddy" user's ID from the users table.
-- ============================================================

-- Get Reddy's user ID into a variable
SET @reddy_id = (SELECT id FROM users WHERE username = 'Reddy');

-- ============================================================
-- 1. USER SETTINGS
-- ============================================================
INSERT IGNORE INTO user_settings (user_id, theme, language, auto_logout_minutes, read_only_mode, created_at, updated_at)
VALUES (@reddy_id, 'DARK', 'en-US', 30, false, NOW(), NOW());

-- ============================================================
-- 2. CATEGORIES
-- ============================================================
INSERT INTO categories (user_id, name, icon, is_default, created_at) VALUES
(@reddy_id, 'Social Media', '🌐', false, NOW()),
(@reddy_id, 'Banking', '🏦', false, NOW()),
(@reddy_id, 'Email', '📧', false, NOW()),
(@reddy_id, 'Shopping', '🛒', false, NOW()),
(@reddy_id, 'Work', '💼', false, NOW()),
(@reddy_id, 'Gaming', '🎮', false, NOW()),
(@reddy_id, 'Cloud Storage', '☁️', false, NOW()),
(@reddy_id, 'Development', '💻', false, NOW());

-- Get category IDs
SET @cat_social   = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Social Media' LIMIT 1);
SET @cat_banking  = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Banking' LIMIT 1);
SET @cat_email    = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Email' LIMIT 1);
SET @cat_shopping = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Shopping' LIMIT 1);
SET @cat_work     = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Work' LIMIT 1);
SET @cat_gaming   = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Gaming' LIMIT 1);
SET @cat_cloud    = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Cloud Storage' LIMIT 1);
SET @cat_dev      = (SELECT id FROM categories WHERE user_id = @reddy_id AND name = 'Development' LIMIT 1);

-- ============================================================
-- 3. FOLDERS (with nested sub-folders)
-- ============================================================
INSERT INTO folders (name, user_id, parent_folder_id, created_at, updated_at) VALUES
('Personal', @reddy_id, NULL, NOW(), NOW()),
('Work', @reddy_id, NULL, NOW(), NOW()),
('Finance', @reddy_id, NULL, NOW(), NOW()),
('Entertainment', @reddy_id, NULL, NOW(), NOW());

SET @folder_personal = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Personal' AND parent_folder_id IS NULL LIMIT 1);
SET @folder_work     = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Work' AND parent_folder_id IS NULL LIMIT 1);
SET @folder_finance  = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Finance' AND parent_folder_id IS NULL LIMIT 1);
SET @folder_entertainment = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Entertainment' AND parent_folder_id IS NULL LIMIT 1);

-- Sub-folders
INSERT INTO folders (name, user_id, parent_folder_id, created_at, updated_at) VALUES
('Social Accounts', @reddy_id, @folder_personal, NOW(), NOW()),
('Email Accounts', @reddy_id, @folder_personal, NOW(), NOW()),
('Project Alpha', @reddy_id, @folder_work, NOW(), NOW()),
('Credit Cards', @reddy_id, @folder_finance, NOW(), NOW());

SET @folder_social_accounts = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Social Accounts' LIMIT 1);
SET @folder_email_accounts  = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Email Accounts' LIMIT 1);
SET @folder_project_alpha   = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Project Alpha' LIMIT 1);
SET @folder_credit_cards    = (SELECT id FROM folders WHERE user_id = @reddy_id AND name = 'Credit Cards' LIMIT 1);

-- ============================================================
-- 4. VAULT ENTRIES (passwords are encrypted/hashed placeholders)
-- ============================================================
INSERT INTO vault_entries (user_id, category_id, folder_id, title, username, password, website_url, notes, is_favorite, is_highly_sensitive, is_deleted, deleted_at, created_at, updated_at) VALUES
-- Social Media
(@reddy_id, @cat_social, @folder_social_accounts, 'Facebook', 'reddy.dasthagiri@gmail.com', 'enc_Fb@Pass2024!', 'https://facebook.com', 'Personal Facebook account', true, false, false, NULL, DATE_SUB(NOW(), INTERVAL 90 DAY), NOW()),
(@reddy_id, @cat_social, @folder_social_accounts, 'Twitter / X', 'reddy_tech', 'enc_Tw!tter$2024', 'https://x.com', 'Tech Twitter account', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 75 DAY), NOW()),
(@reddy_id, @cat_social, @folder_social_accounts, 'LinkedIn', 'dasthagiri.reddy@outlook.com', 'enc_L1nk3d!nPr0', 'https://linkedin.com', 'Professional LinkedIn profile', true, false, false, NULL, DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
(@reddy_id, @cat_social, NULL, 'Instagram', 'reddy_photos', 'enc_Inst@gr4m!', 'https://instagram.com', 'Photography account', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 45 DAY), NOW()),

-- Banking (highly sensitive)
(@reddy_id, @cat_banking, @folder_finance, 'HDFC Bank', 'reddy.hdfc', 'enc_HdfC$B4nk!ng', 'https://netbanking.hdfcbank.com', 'Savings account - XXXX4521', false, true, false, NULL, DATE_SUB(NOW(), INTERVAL 120 DAY), NOW()),
(@reddy_id, @cat_banking, @folder_finance, 'SBI Net Banking', 'reddy_sbi2024', 'enc_SB1N3t@2024', 'https://onlinesbi.sbi', 'Salary account', true, true, false, NULL, DATE_SUB(NOW(), INTERVAL 100 DAY), NOW()),
(@reddy_id, @cat_banking, @folder_credit_cards, 'ICICI Credit Card', 'reddy.icici', 'enc_1C1C1@C4rd!', 'https://icicibank.com', 'Credit card ending 8832, CVV in notes: 442', false, true, false, NULL, DATE_SUB(NOW(), INTERVAL 80 DAY), NOW()),

-- Email
(@reddy_id, @cat_email, @folder_email_accounts, 'Gmail Personal', 'reddy.dasthagiri@gmail.com', 'enc_Gm4il@P3rs!', 'https://mail.google.com', 'Primary email account', true, false, false, NULL, DATE_SUB(NOW(), INTERVAL 150 DAY), NOW()),
(@reddy_id, @cat_email, @folder_email_accounts, 'Outlook Work', 'dasthagiri@company.com', 'enc_0utl00k@W0rk', 'https://outlook.office.com', 'Company email', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 130 DAY), NOW()),
(@reddy_id, @cat_email, @folder_email_accounts, 'ProtonMail', 'reddy_secure@protonmail.com', 'enc_Pr0t0n$ecur3!', 'https://mail.proton.me', 'Encrypted email for sensitive comms', false, true, false, NULL, DATE_SUB(NOW(), INTERVAL 30 DAY), NOW()),

-- Shopping
(@reddy_id, @cat_shopping, @folder_personal, 'Amazon India', 'reddy.dasthagiri@gmail.com', 'enc_Am4z0n!nd14', 'https://amazon.in', 'Prime member since 2022', true, false, false, NULL, DATE_SUB(NOW(), INTERVAL 200 DAY), NOW()),
(@reddy_id, @cat_shopping, @folder_personal, 'Flipkart', 'reddy_flip', 'enc_Fl1pk4rt#2024', 'https://flipkart.com', 'Supercoins balance: 4500', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 180 DAY), NOW()),

-- Work
(@reddy_id, @cat_work, @folder_project_alpha, 'Jira', 'dasthagiri.r', 'enc_J1r4@W0rk!', 'https://company.atlassian.net', 'Project Alpha board', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 50 DAY), NOW()),
(@reddy_id, @cat_work, @folder_project_alpha, 'Confluence', 'dasthagiri.r', 'enc_C0nflu3nc3!', 'https://company.atlassian.net/wiki', 'Documentation wiki', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 50 DAY), NOW()),
(@reddy_id, @cat_work, @folder_work, 'Slack', 'dasthagiri@company.com', 'enc_Sl4ck@T3am!', 'https://company.slack.com', 'Team communication', true, false, false, NULL, DATE_SUB(NOW(), INTERVAL 40 DAY), NOW()),

-- Development
(@reddy_id, @cat_dev, @folder_work, 'GitHub', 'Reddy076', 'enc_G1tHub@D3v!', 'https://github.com', 'Open source contributions', true, false, false, NULL, DATE_SUB(NOW(), INTERVAL 300 DAY), NOW()),
(@reddy_id, @cat_dev, @folder_work, 'AWS Console', 'reddy-admin', 'enc_AWS@C0ns0l3!', 'https://aws.amazon.com/console', 'Dev environment - us-east-1', false, true, false, NULL, DATE_SUB(NOW(), INTERVAL 60 DAY), NOW()),
(@reddy_id, @cat_dev, NULL, 'Docker Hub', 'reddydev', 'enc_D0ck3r@Hub!', 'https://hub.docker.com', 'Container registry', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW()),

-- Gaming
(@reddy_id, @cat_gaming, @folder_entertainment, 'Steam', 'ReddyGamer076', 'enc_St3am@G4m3!', 'https://store.steampowered.com', '250+ games library', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 365 DAY), NOW()),
(@reddy_id, @cat_gaming, @folder_entertainment, 'Epic Games', 'reddy_epic', 'enc_3p1c@G4m3s!', 'https://epicgames.com', 'Free games collection', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 200 DAY), NOW()),

-- Cloud Storage
(@reddy_id, @cat_cloud, @folder_personal, 'Google Drive', 'reddy.dasthagiri@gmail.com', 'enc_GDr1v3@2024', 'https://drive.google.com', '15GB free storage', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 100 DAY), NOW()),
(@reddy_id, @cat_cloud, @folder_work, 'Dropbox', 'dasthagiri@company.com', 'enc_Dr0pb0x@Pr0!', 'https://dropbox.com', 'Work file sharing', false, false, false, NULL, DATE_SUB(NOW(), INTERVAL 50 DAY), NOW()),

-- DELETED entries (soft-deleted, in trash)
(@reddy_id, @cat_social, NULL, 'Old MySpace', 'reddy_myspace', 'enc_MySpac3@0ld', 'https://myspace.com', 'Abandoned account', false, false, true, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 500 DAY), NOW()),
(@reddy_id, @cat_email, NULL, 'Yahoo Mail (old)', 'reddy_yahoo@yahoo.com', 'enc_Y4h00@0ld!', 'https://mail.yahoo.com', 'Migrated to Gmail', false, false, true, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 400 DAY), NOW());

-- ============================================================
-- 5. VAULT SNAPSHOTS (password history)
-- ============================================================
SET @entry_facebook = (SELECT id FROM vault_entries WHERE user_id = @reddy_id AND title = 'Facebook' LIMIT 1);
SET @entry_hdfc     = (SELECT id FROM vault_entries WHERE user_id = @reddy_id AND title = 'HDFC Bank' LIMIT 1);
SET @entry_github   = (SELECT id FROM vault_entries WHERE user_id = @reddy_id AND title = 'GitHub' LIMIT 1);

INSERT INTO vault_snapshots (vault_entry_id, password, changed_at) VALUES
(@entry_facebook, 'enc_OldFb@Pass2023', DATE_SUB(NOW(), INTERVAL 180 DAY)),
(@entry_facebook, 'enc_OlderFb@2022!', DATE_SUB(NOW(), INTERVAL 365 DAY)),
(@entry_hdfc, 'enc_OldHdfc@2023', DATE_SUB(NOW(), INTERVAL 200 DAY)),
(@entry_github, 'enc_OldGit@2023!', DATE_SUB(NOW(), INTERVAL 150 DAY)),
(@entry_github, 'enc_OlderGit@2022', DATE_SUB(NOW(), INTERVAL 300 DAY));

-- ============================================================
-- 6. SECURITY QUESTIONS (answers are bcrypt hashed)
-- Plaintext answers: "mumbai", "fluffy", "revature"
-- ============================================================
INSERT INTO security_questions (user_id, question_text, answer_hash) VALUES
(@reddy_id, 'What city were you born in?', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjqQBz.IiudXQbz.IRSQHC.5a1N1S6'),
(@reddy_id, 'What was the name of your first pet?', '$2a$10$dSx4WJqKqJtlZkCmbUjsieHlGKy2JN/3QGv3dLQ2YPkQVqR5Xq1nO'),
(@reddy_id, 'What company did you first work for?', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhi6M5QEK3/q.OoMl0D3TrS5ydmzq1ni');

-- ============================================================
-- 7. AUDIT LOGS
-- ============================================================
INSERT INTO audit_logs (user_id, action, details, ip_address, timestamp) VALUES
(@reddy_id, 'LOGIN', 'Successful login', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(@reddy_id, 'LOGIN', 'Successful login (2FA)', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(@reddy_id, 'ENTRY_CREATED', 'Created vault entry: Docker Hub', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(@reddy_id, 'ENTRY_UPDATED', 'Updated vault entry: Facebook', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(@reddy_id, 'PASSWORD_VIEWED', 'Viewed password: HDFC Bank', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 3 DAY)),
(@reddy_id, 'ENTRY_DELETED', 'Deleted vault entry: Old MySpace', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 5 DAY)),
(@reddy_id, 'LOGIN_FAILED', 'Failed login attempt', '203.0.113.50', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(@reddy_id, 'LOGIN_FAILED', 'Failed login attempt', '203.0.113.50', DATE_SUB(NOW(), INTERVAL 10 DAY)),
(@reddy_id, 'VAULT_EXPORTED', 'Vault exported as JSON', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 15 DAY)),
(@reddy_id, 'ENTRY_RESTORED', 'Restored vault entry from trash', '192.168.1.100', DATE_SUB(NOW(), INTERVAL 2 DAY));

-- ============================================================
-- 8. SECURITY ALERTS
-- ============================================================
INSERT INTO security_alerts (user_id, alert_type, title, message, severity, is_read, created_at) VALUES
(@reddy_id, 'NEW_DEVICE_LOGIN', 'New Device Detected', 'Login detected from a new device: Mozilla/5.0 Chrome/120.0', 'MEDIUM', false, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(@reddy_id, 'MULTIPLE_FAILED_LOGINS', 'Multiple Failed Login Attempts', '3 failed login attempts detected from IP: 203.0.113.50', 'HIGH', false, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(@reddy_id, 'TWO_FA_ENABLED', 'Two-Factor Authentication Enabled', '2FA has been successfully enabled for your account', 'LOW', true, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@reddy_id, 'PASSWORD_CHANGED', 'Password Updated', 'Your master password was changed successfully', 'MEDIUM', true, DATE_SUB(NOW(), INTERVAL 60 DAY)),
(@reddy_id, 'SENSITIVE_ACCESS', 'Sensitive Entry Accessed', 'Highly sensitive entry "HDFC Bank" was accessed', 'HIGH', false, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(@reddy_id, 'NEW_LOCATION_LOGIN', 'Login from New Location', 'Login detected from a new location: Hyderabad, IN', 'MEDIUM', true, DATE_SUB(NOW(), INTERVAL 20 DAY));

-- ============================================================
-- 9. NOTIFICATIONS
-- ============================================================
INSERT INTO notifications (user_id, notification_type, title, message, is_read, created_at) VALUES
(@reddy_id, 'PASSWORD_EXPIRY', 'Password Expiring Soon', 'Your password for "HDFC Bank" is 120 days old. Consider updating it for security.', false, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(@reddy_id, 'SECURITY_ALERT', 'Unusual Login Activity', 'We detected login attempts from an unfamiliar IP address. Please verify your account security.', false, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(@reddy_id, 'BACKUP_REMINDER', 'Time for a Backup', 'You haven''t exported your vault in 15 days. Regular backups help protect your data.', false, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(@reddy_id, 'SYSTEM_UPDATE', 'New Feature Available', 'Password strength analysis is now available! Check the security dashboard for details.', true, DATE_SUB(NOW(), INTERVAL 30 DAY)),
(@reddy_id, 'BREACH_DETECTED', 'Data Breach Alert', 'A data breach was reported at linkedin.com. We recommend changing your LinkedIn password immediately.', false, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(@reddy_id, 'ACCOUNT_ACTIVITY', 'Vault Entry Updated', 'Your vault entry "Facebook" was updated successfully.', true, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- ============================================================
-- 10. LOGIN ATTEMPTS
-- ============================================================
INSERT INTO login_attempts (username, success, failure_reason, ip_address, device_info, location, risk_score, attempted_at) VALUES
('Reddy', true, NULL, '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0', 'Hyderabad, IN', 0, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
('Reddy', true, NULL, '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0', 'Hyderabad, IN', 0, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('Reddy', false, 'Invalid credentials', '203.0.113.50', 'Mozilla/5.0 (Linux; Android 12) Mobile Safari', 'Unknown', 75, DATE_SUB(NOW(), INTERVAL 10 DAY)),
('Reddy', false, 'Invalid credentials', '203.0.113.50', 'Mozilla/5.0 (Linux; Android 12) Mobile Safari', 'Unknown', 80, DATE_SUB(NOW(), INTERVAL 10 DAY)),
('Reddy', false, 'Invalid credentials', '203.0.113.50', 'curl/7.88.1', 'Unknown', 95, DATE_SUB(NOW(), INTERVAL 10 DAY)),
('Reddy', true, NULL, '10.0.0.1', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605', 'Bangalore, IN', 15, DATE_SUB(NOW(), INTERVAL 20 DAY)),
('Reddy', true, NULL, '192.168.1.100', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Edge/120.0', 'Hyderabad, IN', 0, DATE_SUB(NOW(), INTERVAL 30 DAY));

-- ============================================================
-- Done! Summary of inserted data:
-- ============================================================
-- User Settings:      1 record (dark theme, 30min auto-logout)
-- Categories:         8 categories
-- Folders:            8 folders (4 root + 4 sub-folders)
-- Vault Entries:     24 entries (22 active + 2 soft-deleted)
-- Vault Snapshots:    5 password history records
-- Security Questions: 3 questions
-- Audit Logs:        10 entries
-- Security Alerts:    6 alerts (3 unread)
-- Notifications:      6 notifications (3 unread)
-- Login Attempts:     7 records (4 successful, 3 failed)
-- ============================================================

SELECT 'Dummy data inserted successfully for user Reddy!' AS result;
