-- ═══════════════════════════════════════════════════════════════════════════
-- V17 — Remove the demo school from databases where it was never used
--
-- V2/V3 seed a demo school ("New Shanti Niketan Vidyapeeth", admin User ID
-- "admin"). Hosted databases should start empty, so delete it when it is
-- still untouched: its admin password is the disabled default from V16, and
-- it has no students and no payments.
--
-- A database where someone took over the demo school (changed the password or
-- added data) keeps it. Older migrations are left as they are because
-- editing applied migrations breaks Flyway validation on existing databases.
-- ═══════════════════════════════════════════════════════════════════════════

DO $$
DECLARE
    demo_school CONSTANT UUID := 'a0000000-0000-0000-0000-000000000001';
BEGIN
    IF EXISTS (SELECT 1 FROM users
               WHERE id = 'b0000000-0000-0000-0000-000000000001'
                 AND password_hash = '!disabled-default-password')
       AND NOT EXISTS (SELECT 1 FROM students WHERE school_id = demo_school)
       AND NOT EXISTS (SELECT 1 FROM payment_transactions WHERE school_id = demo_school)
    THEN
        DELETE FROM audit_logs              WHERE school_id = demo_school;
        DELETE FROM monthly_fee_structures  WHERE school_id = demo_school;
        DELETE FROM fee_categories          WHERE school_id = demo_school;
        DELETE FROM sections                WHERE school_id = demo_school;
        DELETE FROM classes                 WHERE school_id = demo_school;
        DELETE FROM academic_years          WHERE school_id = demo_school;
        DELETE FROM users                   WHERE school_id = demo_school;  -- OTPs/revoked tokens cascade
        DELETE FROM schools                 WHERE id = demo_school;
        RAISE NOTICE 'V17: removed unused demo school';
    END IF;
END $$;
