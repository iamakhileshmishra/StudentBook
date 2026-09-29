-- ═══════════════════════════════════════════════════════════════════════════
-- V15 — Sections are optional
-- Many schools (especially small ones) have a single division per class and
-- never create sections. A student in such a class has no section.
-- ═══════════════════════════════════════════════════════════════════════════

ALTER TABLE student_enrollments ALTER COLUMN section_id DROP NOT NULL;
