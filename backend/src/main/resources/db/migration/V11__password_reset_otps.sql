-- ═══════════════════════════════════════════════════════════════════════════
-- V11 — Forgot password: one-time codes sent by email
-- ═══════════════════════════════════════════════════════════════════════════

CREATE TABLE password_reset_otps (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    otp_hash    VARCHAR(255) NOT NULL,          -- BCrypt hash; the code itself is never stored
    expires_at  TIMESTAMP   NOT NULL,
    attempts    INTEGER     NOT NULL DEFAULT 0, -- wrong guesses against this code
    used_at     TIMESTAMP,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_otps_user ON password_reset_otps (user_id, created_at DESC);
