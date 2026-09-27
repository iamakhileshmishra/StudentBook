-- ═══════════════════════════════════════════════════════════════════════════
-- V14 — Fee data integrity (test&bug.md BUG-04, BUG-05)
-- ═══════════════════════════════════════════════════════════════════════════

-- BUG-04: at most one confirmed payment per student per month. Makes the
-- "already paid" check race-free (double-click, two cashiers at once).
CREATE UNIQUE INDEX uq_payment_student_month_confirmed
    ON payment_transactions (student_id, collection_year, collection_month)
    WHERE status = 'CONFIRMED';

-- BUG-05: SHORT(10)-YYYY-MMDD-ADMNO(20)-XXXX can reach 44 characters;
-- the old VARCHAR(30) made fee collection fail for longer admission numbers.
ALTER TABLE payment_transactions ALTER COLUMN receipt_number TYPE VARCHAR(64);
