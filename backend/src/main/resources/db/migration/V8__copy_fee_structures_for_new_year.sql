DO $$
DECLARE
    school_record RECORD;
    old_year_id UUID;
    new_year_id UUID;
BEGIN
    FOR school_record IN SELECT id FROM schools LOOP
        SELECT id INTO new_year_id FROM academic_years WHERE school_id = school_record.id AND is_current = true LIMIT 1;
        SELECT id INTO old_year_id FROM academic_years WHERE school_id = school_record.id AND is_current = false ORDER BY end_date DESC LIMIT 1;

        IF new_year_id IS NOT NULL AND old_year_id IS NOT NULL THEN
            IF NOT EXISTS (SELECT 1 FROM monthly_fee_structures WHERE academic_year_id = new_year_id) THEN
                INSERT INTO monthly_fee_structures (id, school_id, academic_year_id, class_id, fee_category_id, default_amount, created_at, updated_at)
                SELECT gen_random_uuid(), school_id, new_year_id, class_id, fee_category_id, default_amount, NOW(), NOW()
                FROM monthly_fee_structures
                WHERE academic_year_id = old_year_id;
            END IF;
        END IF;
    END LOOP;
END $$;
