-- ═══════════════════════════════════════════════════════════════════════════
-- V12 — One-time codes are now used for more than password resets
-- (also confirming a password change in Settings), so tag each code with
-- its purpose; a code issued for one purpose can't be used for another.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE password_reset_otps RENAME TO user_otps;
ALTER INDEX idx_password_reset_otps_user RENAME TO idx_user_otps_user;

ALTER TABLE user_otps
    ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'PASSWORD_RESET';

ALTER TABLE user_otps ALTER COLUMN purpose DROP DEFAULT;

DROP INDEX idx_user_otps_user;
CREATE INDEX idx_user_otps_user_purpose ON user_otps (user_id, purpose, created_at DESC);
