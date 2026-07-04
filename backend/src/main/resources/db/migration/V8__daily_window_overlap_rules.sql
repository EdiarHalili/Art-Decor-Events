DROP INDEX IF EXISTS uq_daily_check_in_window_active_date;

CREATE INDEX IF NOT EXISTS idx_work_schedules_active_window_times
ON work_schedules(check_in_opens_at, check_in_closes_at)
WHERE status IN ('PUBLISHED', 'CHECK_IN_OPEN');
