ALTER TABLE attendance_records
    ADD COLUMN IF NOT EXISTS auto_checkout BOOLEAN NOT NULL DEFAULT false;
