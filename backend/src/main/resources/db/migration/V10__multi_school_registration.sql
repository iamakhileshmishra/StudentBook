-- ═══════════════════════════════════════════════════════════════════════════
-- V10 — Multi-school self-registration
-- ═══════════════════════════════════════════════════════════════════════════

-- Login no longer knows the school up front, so usernames must be unique
-- across the whole platform (case-insensitive).
CREATE UNIQUE INDEX uq_users_username_global ON users (LOWER(username));

-- Accounts created with an emailed temporary password must set their own
-- password on first login.
ALTER TABLE users
    ADD COLUMN must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

-- One school account per contact email.
CREATE UNIQUE INDEX uq_schools_email ON schools (LOWER(email)) WHERE email IS NOT NULL;
