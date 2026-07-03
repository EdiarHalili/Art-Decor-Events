ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS position_title VARCHAR(120),
    ADD COLUMN IF NOT EXISTS department_name VARCHAR(120),
    ADD COLUMN IF NOT EXISTS team_name VARCHAR(120);

CREATE TABLE IF NOT EXISTS app_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(160) NOT NULL,
    logo_url TEXT,
    primary_color VARCHAR(20) NOT NULL,
    accent_color VARCHAR(20) NOT NULL,
    timezone VARCHAR(80) NOT NULL,
    default_check_in_open_time TIME NOT NULL,
    default_check_in_close_time TIME NOT NULL,
    allowed_late_minutes INTEGER NOT NULL,
    gps_enabled BOOLEAN NOT NULL,
    notifications_enabled BOOLEAN NOT NULL,
    session_timeout_minutes INTEGER NOT NULL,
    updated_at TIMESTAMPTZ
);

INSERT INTO app_settings (
    id,
    company_name,
    logo_url,
    primary_color,
    accent_color,
    timezone,
    default_check_in_open_time,
    default_check_in_close_time,
    allowed_late_minutes,
    gps_enabled,
    notifications_enabled,
    session_timeout_minutes
)
SELECT
    gen_random_uuid(),
    'Art Decor Events',
    NULL,
    '#c9a052',
    '#496f5d',
    'Europe/Berlin',
    '06:50',
    '07:10',
    0,
    true,
    true,
    60
WHERE NOT EXISTS (SELECT 1 FROM app_settings);

CREATE TABLE IF NOT EXISTS push_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    employee_id UUID REFERENCES employees(id),
    endpoint TEXT NOT NULL UNIQUE,
    p256dh_key TEXT NOT NULL,
    auth_key TEXT NOT NULL,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_seen_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS payroll_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    period_month DATE NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS payroll_employee_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_period_id UUID NOT NULL REFERENCES payroll_periods(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id),
    wage_type VARCHAR(32) NOT NULL,
    base_wage NUMERIC(12, 2) NOT NULL,
    worked_minutes INTEGER NOT NULL DEFAULT 0,
    overtime_minutes INTEGER NOT NULL DEFAULT 0,
    regular_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    overtime_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (payroll_period_id, employee_id)
);

CREATE INDEX IF NOT EXISTS idx_announcements_visible ON announcements(visible_from, visible_until);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_employee ON push_subscriptions(employee_id);
CREATE INDEX IF NOT EXISTS idx_payroll_summaries_employee ON payroll_employee_summaries(employee_id);
