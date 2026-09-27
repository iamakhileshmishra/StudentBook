-- ═══════════════════════════════════════════════════════════════════════════
-- V13 — Session revocation (test&bug.md SEC-04)
-- ═══════════════════════════════════════════════════════════════════════════

-- Embedded in every token as "tv". Bumping it (password change/reset)
-- invalidates all existing access and refresh tokens for the user.
ALTER TABLE users
    ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

-- Refresh tokens revoked by logout, keyed by the token's jti.
-- Rows can be deleted once expires_at has passed.
CREATE TABLE revoked_refresh_tokens (
    jti         UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at  TIMESTAMP   NOT NULL,
    revoked_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_revoked_refresh_tokens_expires ON revoked_refresh_tokens (expires_at);

-- ───────────────────────────────────────────────────────────────────────────
-- Email ownership (SEC-09): set when the school's admin first signs in with the
-- emailed temporary password. Unverified sign-ups can be purged so a squatted
-- email address becomes available again. Existing schools count as verified.
-- ───────────────────────────────────────────────────────────────────────────
ALTER TABLE schools ADD COLUMN email_verified_at TIMESTAMP;
UPDATE schools SET email_verified_at = NOW();
