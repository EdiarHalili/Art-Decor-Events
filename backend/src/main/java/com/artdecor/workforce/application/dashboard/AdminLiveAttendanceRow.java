package com.artdecor.workforce.application.dashboard;

import java.time.Instant;

public record AdminLiveAttendanceRow(
        String employeeId,
        String employeeCode,
        String employeeName,
        String status,
        Instant checkedInAt,
        Instant checkedOutAt,
        int workedMinutes,
        int overtimeMinutes,
        boolean late
) {
}
