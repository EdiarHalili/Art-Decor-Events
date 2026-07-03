ALTER TABLE attendance_records
    ALTER COLUMN check_in_latitude TYPE DOUBLE PRECISION USING check_in_latitude::DOUBLE PRECISION,
    ALTER COLUMN check_in_longitude TYPE DOUBLE PRECISION USING check_in_longitude::DOUBLE PRECISION,
    ALTER COLUMN check_out_latitude TYPE DOUBLE PRECISION USING check_out_latitude::DOUBLE PRECISION,
    ALTER COLUMN check_out_longitude TYPE DOUBLE PRECISION USING check_out_longitude::DOUBLE PRECISION;
