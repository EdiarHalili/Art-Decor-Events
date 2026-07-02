CREATE UNIQUE INDEX IF NOT EXISTS uq_daily_check_in_window_active_date
ON work_schedules(work_date)
WHERE status <> 'CANCELLED';

