-- Drop the hard unique constraints on classes and sections
ALTER TABLE classes DROP CONSTRAINT uq_classes_school_order;
ALTER TABLE classes DROP CONSTRAINT uq_classes_school_name;
ALTER TABLE sections DROP CONSTRAINT uq_sections_class_name;

-- Recreate them as partial unique indexes that only apply to active records.
-- This allows recreating a class/section with the same name or order if the previous one was soft-deleted.
CREATE UNIQUE INDEX uq_classes_school_order_active ON classes (school_id, numeric_order) WHERE is_active = TRUE;
CREATE UNIQUE INDEX uq_classes_school_name_active ON classes (school_id, name) WHERE is_active = TRUE;
CREATE UNIQUE INDEX uq_sections_class_name_active ON sections (class_id, name) WHERE is_active = TRUE;
