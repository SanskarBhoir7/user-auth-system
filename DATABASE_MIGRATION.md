# Database Migration Guide

## 🔄 Automatic Migration (Recommended)

Your application is configured with `spring.jpa.hibernate.ddl-auto=update` in `application.properties`, which means:

1. **New tables** will be created automatically when you start the app
2. **New columns** will be added automatically to existing tables

### Steps:
1. **Restart your Spring Boot application**
2. Hibernate will automatically create:
   - `email_verification_token` table
   - `password_reset_token` table
   - `audit_log` table
   - New columns in `users` table

3. **After restart**, run this SQL to update existing users:

```sql
UPDATE users 
SET created_at = COALESCE(created_at, NOW()),
    account_status = COALESCE(account_status, 'ACTIVE'),
    email_verified = COALESCE(email_verified, FALSE),
    theme = COALESCE(theme, 'LIGHT')
WHERE created_at IS NULL OR account_status IS NULL;
```

---

## 📋 Manual Migration (If Needed)

If you prefer manual control, run the complete migration script:

```bash
mysql -u root -p userauth < database-migration.sql
```

Or execute it in MySQL Workbench/CLI.

---

## ✅ Verify Migration

Check if all new columns exist:

```sql
DESCRIBE users;
```

Expected columns:
- `id`, `username`, `email`, `password`, `role`
- **NEW**: `created_at`, `last_login`, `account_status`, `email_verified`, `profile_picture_path`, `theme`

Check new tables:

```sql
SHOW TABLES;
```

Expected tables:
- `users`
- **NEW**: `email_verification_token`
- **NEW**: `password_reset_token`
- **NEW**: `audit_log`

---

## 🔧 Troubleshooting

**If columns are missing:**
1. Check `application.properties` has `spring.jpa.hibernate.ddl-auto=update`
2. Restart the application
3. Check logs for Hibernate SQL statements

**If you get "column already exists" errors:**
- This is fine! It means Hibernate already created the columns

**If existing users can't login:**
Run the UPDATE query above to set default values for existing users

---

## 🎯 Create Admin User

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your-username';
```

Or create a new admin:

```sql
INSERT INTO users (username, email, password, role, account_status, email_verified, theme, created_at)
VALUES (
    'admin',
    'admin@example.com',
    '$2a$10$dXJ3SW6G7P3R92U/Iwzbbe.c0VvZnx2P8FKR/BqW9TCt8TFt.UJHa',  -- password: admin123
    'ADMIN',
    'ACTIVE',
    TRUE,
    'LIGHT',
    NOW()
);
```

---

## 📊 Database Schema Overview

### Users Table
- Authentication fields
- Account status (ACTIVE/SUSPENDED/DISABLED)
- Email verification status
- Theme preference (LIGHT/DARK)
- Timestamps (created_at, last_login)

### Email Verification Token
- Token, user reference, expiry date, verified status

### Password Reset Token
- Token, user reference, expiry date, used status

### Audit Log
- User actions, timestamps, IP addresses, details

---

**✅ Migration is simple**: Just restart your app and run one UPDATE query!
