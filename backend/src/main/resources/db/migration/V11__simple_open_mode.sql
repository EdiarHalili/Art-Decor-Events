ALTER TABLE work_schedules
    ADD COLUMN IF NOT EXISTS simple_open_mode boolean NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_work_schedules_simple_open_work_date
    ON work_schedules (work_date, simple_open_mode, status);
