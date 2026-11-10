-- ═══════════════════════════════════════════════════════════════════════════
-- V16 — Never ship a publicly documented password
--
-- V2 seeds the sample school's admin with password "Admin@1234", which is
-- written in the README. On a fresh (e.g. hosted) database that would be an
-- open admin account. If the password was never changed, replace it with a
-- value no password can match. The owner can set a new one via
-- "Forgot password?" using the school's email address.
--
-- Databases where the password was already changed are not affected.
-- ═══════════════════════════════════════════════════════════════════════════

UPDATE users
SET password_hash = '!disabled-default-password',
    must_change_password = TRUE
WHERE id = 'b0000000-0000-0000-0000-000000000001'
  AND password_hash = '$2a$12$dBptDk9onOawvNHrQRBTZ.13VNGeHcBZm0IDam.feu1WM/6j3P376';
