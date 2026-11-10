-- ═══════════════════════════════════════════════════════════════════════════
-- V17 — Tell parents when a fee is paid
--
-- 1. payment_transactions.share_token: a random, unguessable token behind the
--    public receipt link (/r/<token>) sent to parents. Receipt numbers are
--    sequential and guessable, so they are never used in the public link.
-- 2. notification_log: one row per attempt to notify a parent (email now,
--    WhatsApp later), so staff can see what was sent and resend failures.
-- 3. schools.notify_parent_email: per-school switch, on by default. A message
--    is only sent when the student has a parent email on file.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE payment_transactions ADD COLUMN share_token VARCHAR(64);

-- Existing receipts get a token too, so they can be resent later
UPDATE payment_transactions
SET share_token = translate(encode(gen_random_bytes(32), 'base64'), '+/=', '-_')
WHERE share_token IS NULL;

ALTER TABLE payment_transactions ALTER COLUMN share_token SET NOT NULL;
CREATE UNIQUE INDEX uq_payment_transactions_share_token ON payment_transactions (share_token);

CREATE TABLE notification_log (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id      UUID         NOT NULL REFERENCES schools(id),
    transaction_id UUID         NOT NULL REFERENCES payment_transactions(id),
    channel        VARCHAR(20)  NOT NULL,              -- EMAIL | WHATSAPP
    recipient      VARCHAR(100),
    status         VARCHAR(20)  NOT NULL,              -- SENT | FAILED | SKIPPED
    detail         TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_log_transaction ON notification_log (transaction_id, created_at DESC);

ALTER TABLE schools ADD COLUMN notify_parent_email BOOLEAN NOT NULL DEFAULT TRUE;
