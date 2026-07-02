package com.artdecor.workforce.application.reports;

import java.time.LocalDate;
import java.util.List;

public record AttendanceReportResponse(
        LocalDate from,
        LocalDate to,
        String period,
        AttendanceReportSummary summary,
        List<AttendanceReportBucket> buckets,
        List<EmployeeAttendanceSummary> employees,
        List<AttendanceReportRow> rows
) {
}
