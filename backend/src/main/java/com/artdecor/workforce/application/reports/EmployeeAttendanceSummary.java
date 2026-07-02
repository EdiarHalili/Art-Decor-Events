package com.artdecor.workforce.application.reports;

public record EmployeeAttendanceSummary(
        String employeeId,
        String employeeCode,
        String employeeName,
        int assigned,
        int present,
        int late,
        int absent,
        int workedMinutes,
        int overtimeMinutes
) {
}
