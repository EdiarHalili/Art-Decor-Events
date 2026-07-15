alter table app_settings
    add column if not exists open_mode_unlimited_checkout boolean not null default false;
