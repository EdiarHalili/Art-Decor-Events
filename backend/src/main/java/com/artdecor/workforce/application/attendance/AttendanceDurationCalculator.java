package com.artdecor.workforce.application.attendance;

import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import java.time.Duration;
import java.time.Instant;

public final class AttendanceDurationCalculator {
    private static final int ROUND_UP_SECONDS = 30;

    private AttendanceDurationCalculator() {
    }

    public static int roundedMinutesBetween(Instant start, Instant end) {
        if (start == null || end == null || end.isBefore(start)) {
            return 0;
        }
        long seconds = Duration.between(start, end).getSeconds();
        long minutes = seconds / 60;
        if (seconds % 60 >= ROUND_UP_SECONDS) {
            minutes++;
        }
        return Math.toIntExact(minutes);
    }

    public static int workedMinutes(AttendanceRecordEntity record) {
        if (record.getCheckedInAt() == null || record.getCheckedOutAt() == null) {
            return Math.max(record.getWorkedMinutes(), 0);
        }
        return roundedMinutesBetween(record.getCheckedInAt(), record.getCheckedOutAt());
    }

    public static int overtimeMinutes(AttendanceRecordEntity record, Instant checkedOutAt) {
        Instant plannedEndAt = record.getSchedule().getPlannedEndAt();
        if (plannedEndAt == null || checkedOutAt == null || !checkedOutAt.isAfter(plannedEndAt)) {
            return 0;
        }
        return roundedMinutesBetween(plannedEndAt, checkedOutAt);
    }

    public static int overtimeMinutes(AttendanceRecordEntity record) {
        if (record.getCheckedOutAt() == null) {
            return Math.max(record.getOvertimeMinutes(), 0);
        }
        return overtimeMinutes(record, record.getCheckedOutAt());
    }
}
