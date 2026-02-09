-- ===================================================
-- Advanced Auth System - Database Migration Script
-- ===================================================
-- Run this script AFTER first application startup
-- This ensures all tables exist and adds default values
-- ===================================================

-- Step 1: Add new columns to users table (if not already added by Hibernate)
-- These should be created automatically by Hibernate, but we ensure they exist

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_login DATETIME NULL,
ADD COLUMN IF NOT EXISTS account_status VARCHAR(20) DEFAULT 'ACTIVE',
ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS profile_picture_path VARCHAR(255) NULL,
ADD COLUMN IF NOT EXISTS theme VARCHAR(10) DEFAULT 'LIGHT';

-- Step 2: Update existing users with default values
UPDATE users 
SET created_at = COALESCE(created_at, NOW()),
    account_status = COALESCE(account_status, 'ACTIVE'),
    email_verified = COALESCE(email_verified, FALSE),
    theme = COALESCE(theme, 'LIGHT')
WHERE created_at IS NULL OR account_status IS NULL OR email_verified IS NULL OR theme IS NULL;

-- Step 3: Verify new tables exist (created by Hibernate)
-- If these DON'T exist, restart your Spring Boot app first!
-- These tables should be auto-created:
-- - email_verification_token
-- - password_reset_token  
-- - audit_log

-- Step 4: Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_account_status ON users(account_status);
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users(created_at);

CREATE INDEX IF NOT EXISTS idx_email_verification_token ON email_verification_token(token);
CREATE INDEX IF NOT EXISTS idx_password_reset_token ON password_reset_token(token);
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp ON audit_log(timestamp);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);

-- Step 5: Verify the schema
SELECT 'Users table structure:' AS info;
DESCRIBE users;

SELECT 'Email Verification Token table:' AS info;
DESCRIBE email_verification_token;

SELECT 'Password Reset Token table:' AS info;
DESCRIBE password_reset_token;

SELECT 'Audit Log table:' AS info;
DESCRIBE audit_log;

-- Step 6: Check existing data
SELECT 
    COUNT(*) AS total_users,
    SUM(CASE WHEN email_verified = TRUE THEN 1 ELSE 0 END) AS verified_users,
    SUM(CASE WHEN account_status = 'ACTIVE' THEN 1 ELSE 0 END) AS active_users,
    SUM(CASE WHEN role = 'ADMIN' THEN 1 ELSE 0 END) AS admin_users
FROM users;

-- ===================================================
-- OPTIONAL: Create an admin user (if needed)
-- ===================================================
-- Uncomment and modify these lines if you need an admin account
-- Password below is: admin123 (bcrypt hash)

/*
INSERT INTO users (username, email, password, role, account_status, email_verified, theme, created_at)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$10$xVc0dZ8EZLzLhCDqKJqJ2eKJ8Hj0qZ9vJKQxY5nXyR8K4x6wZLJ0G',
    'ADMIN',
    'ACTIVE',
    TRUE,
    'LIGHT',
    NOW()
)
ON DUPLICATE KEY UPDATE username = username;
*/

-- ===================================================
-- Migration Complete!
-- ===================================================
