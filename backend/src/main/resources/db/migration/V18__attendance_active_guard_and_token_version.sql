ALTER TABLE users
    ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uq_attendance_one_active_per_employee
    ON attendance_records (employee_id)
    WHERE checked_in_at IS NOT NULL
      AND checked_out_at IS NULL;
