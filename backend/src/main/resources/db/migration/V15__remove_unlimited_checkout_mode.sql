UPDATE work_schedules
SET checkout_mode = 'SCHEDULED_AUTO'
WHERE checkout_mode IN ('MANUAL_ADMIN', 'UNLIMITED_24_7');
