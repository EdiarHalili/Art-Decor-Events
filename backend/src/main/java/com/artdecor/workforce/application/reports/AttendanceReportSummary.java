package com.artdecor.workforce.application.reports;

public record AttendanceReportSummary(
        int assigned,
        int present,
        int late,
        int absent,
        int checkedOut,
        int workedMinutes,
        int overtimeMinutes
) {
}
