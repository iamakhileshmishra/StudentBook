-- ═══════════════════════════════════════════════════════════════════════════
-- V2 — Seed Data
-- New Shanti Niketan Vidyapeeth Public School
-- ═══════════════════════════════════════════════════════════════════════════

-- ───────────────────────────────────────────────────────────────────────────
-- School
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO schools (id, name, short_name, city, state, is_active, subscription_plan)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'New Shanti Niketan Vidyapeeth Public School',
    'NSNVPS',
    'Your City',
    'Your State',
    TRUE,
    'BASIC'
);

-- ───────────────────────────────────────────────────────────────────────────
-- Initial Admin User
-- Username: admin
-- Password: Admin@1234  (BCrypt hash — MUST be changed on first login)
-- Generated with BCrypt strength 12
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO users (id, school_id, username, full_name, password_hash, role, is_active)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    'admin',
    'School Administrator',
    '$2a$12$dBptDk9onOawvNHrQRBTZ.13VNGeHcBZm0IDam.feu1WM/6j3P376',
    'SCHOOL_ADMIN',
    TRUE
);
-- NOTE: The hash above is for 'Admin@1234'. Change password immediately after first login.

-- ───────────────────────────────────────────────────────────────────────────
-- Academic Year 2025-26 (Current)
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO academic_years (id, school_id, name, start_date, end_date, is_current)
VALUES (
    'c0000000-0000-0000-0000-000000000001',
    'a0000000-0000-0000-0000-000000000001',
    '2025-26',
    '2025-04-01',
    '2026-03-31',
    TRUE
);

-- ───────────────────────────────────────────────────────────────────────────
-- Classes: Nursery, LKG, UKG, Std I–V
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO classes (id, school_id, name, numeric_order, is_active) VALUES
    ('d0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'Nursery', 1, TRUE),
    ('d0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'LKG',     2, TRUE),
    ('d0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000001', 'UKG',     3, TRUE),
    ('d0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000001', 'Std I',   4, TRUE),
    ('d0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000001', 'Std II',  5, TRUE),
    ('d0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000001', 'Std III', 6, TRUE),
    ('d0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000001', 'Std IV',  7, TRUE),
    ('d0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000001', 'Std V',   8, TRUE);

-- ───────────────────────────────────────────────────────────────────────────
-- Sections: One "A" section per class
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO sections (school_id, class_id, name, is_active)
SELECT
    'a0000000-0000-0000-0000-000000000001',
    id,
    'A',
    TRUE
FROM classes
WHERE school_id = 'a0000000-0000-0000-0000-000000000001';

-- ───────────────────────────────────────────────────────────────────────────
-- Fee Categories (8 default categories)
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO fee_categories (school_id, name, display_order, is_active) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Tuition Fee',     1, TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'Examination Fee', 2, TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'Development Fee', 3, TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'Computer Fee',    4, TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'Transport Fee',   5, TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'Sports Fee',      6, TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'Laboratory Fee',  7, TRUE),
    ('a0000000-0000-0000-0000-000000000001', 'Miscellaneous',   8, TRUE);

-- ───────────────────────────────────────────────────────────────────────────
-- Monthly Fee Structures: Initialize all to ₹0 for all classes
-- Admin will fill in actual amounts via the Fee Setup screen after login.
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO monthly_fee_structures (school_id, academic_year_id, class_id, fee_category_id, default_amount)
SELECT
    'a0000000-0000-0000-0000-000000000001' AS school_id,
    'c0000000-0000-0000-0000-000000000001' AS academic_year_id,
    c.id AS class_id,
    fc.id AS fee_category_id,
    0.00 AS default_amount
FROM classes c
CROSS JOIN fee_categories fc
WHERE c.school_id = 'a0000000-0000-0000-0000-000000000001'
  AND fc.school_id = 'a0000000-0000-0000-0000-000000000001';

-- ───────────────────────────────────────────────────────────────────────────
-- Seed audit log entry for initial setup
-- ───────────────────────────────────────────────────────────────────────────
INSERT INTO audit_logs (school_id, user_id, action, entity_type, entity_id, description)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'b0000000-0000-0000-0000-000000000001',
    'SYSTEM_INITIALIZED',
    'School',
    'a0000000-0000-0000-0000-000000000001',
    'School ERP initialized. New Shanti Niketan Vidyapeeth Public School created with 8 classes and default fee categories.'
);
