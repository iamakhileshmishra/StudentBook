ALTER TABLE students DROP CONSTRAINT chk_students_status;
ALTER TABLE students ADD CONSTRAINT chk_students_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'TRANSFERRED', 'LEFT', 'GRADUATED'));

ALTER TABLE student_enrollments DROP CONSTRAINT chk_enrollment_status;
ALTER TABLE student_enrollments ADD CONSTRAINT chk_enrollment_status CHECK (status IN ('ACTIVE', 'LEFT', 'TRANSFERRED', 'GRADUATED'));
