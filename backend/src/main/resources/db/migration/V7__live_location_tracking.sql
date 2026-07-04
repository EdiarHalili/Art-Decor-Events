ALTER TABLE app_settings
    ADD COLUMN live_location_tracking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN live_location_interval_minutes INTEGER NOT NULL DEFAULT 10;

CREATE TABLE live_location_updates (
    id UUID PRIMARY KEY,
    attendance_record_id UUID NOT NULL REFERENCES attendance_records(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id),
    schedule_id UUID NOT NULL REFERENCES work_schedules(id),
    latitude FLOAT(53) NOT NULL,
    longitude FLOAT(53) NOT NULL,
    accuracy_meters FLOAT(53),
    captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
    received_at TIMESTAMP WITH TIME ZONE NOT NULL,
    device JSONB
);

CREATE INDEX idx_live_location_updates_record_time
    ON live_location_updates(attendance_record_id, captured_at DESC);

CREATE INDEX idx_live_location_updates_employee_time
    ON live_location_updates(employee_id, captured_at DESC);
