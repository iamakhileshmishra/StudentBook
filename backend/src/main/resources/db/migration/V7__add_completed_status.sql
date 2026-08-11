ALTER TABLE student_enrollments DROP CONSTRAINT chk_enrollment_status;
ALTER TABLE student_enrollments ADD CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'LEFT', 'TRANSFERRED', 'GRADUATED', 'COMPLETED'));
