ALTER TABLE schools 
ADD COLUMN fee_due_date INTEGER DEFAULT 10,
ADD COLUMN late_fee_rules JSONB DEFAULT '[]'::jsonb;
