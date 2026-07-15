ALTER TABLE attendance_records
    DROP CONSTRAINT IF EXISTS attendance_records_schedule_id_employee_id_key;

CREATE INDEX IF NOT EXISTS idx_attendance_schedule_employee_checked_in
    ON attendance_records (schedule_id, employee_id, checked_in_at);
