ALTER TABLE attendance_records
ADD COLUMN IF NOT EXISTS checkout_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL_EMPLOYEE';

UPDATE attendance_records
SET checkout_type = 'AUTO_CHECKED_OUT'
WHERE auto_checkout = true;

ALTER TABLE work_schedules
ADD COLUMN IF NOT EXISTS checkout_mode VARCHAR(32) NOT NULL DEFAULT 'SCHEDULED_AUTO',
ADD COLUMN IF NOT EXISTS auto_checkout_enabled BOOLEAN NOT NULL DEFAULT true;
