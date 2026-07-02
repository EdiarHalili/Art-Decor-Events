package com.artdecor.workforce.application.reports;

public record AttendanceReportBucket(
        String label,
        int assigned,
        int present,
        int late,
        int absent,
        int workedMinutes,
        int overtimeMinutes
) {
}
