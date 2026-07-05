ALTER TABLE attendance_records
ADD COLUMN IF NOT EXISTS extended_checkout_until TIMESTAMPTZ;
