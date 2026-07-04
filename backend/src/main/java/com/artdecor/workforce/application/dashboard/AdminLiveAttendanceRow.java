package com.artdecor.workforce.application.dashboard;

import java.time.Instant;

public record AdminLiveAttendanceRow(
        String attendanceRecordId,
        String employeeId,
        String employeeCode,
        String employeeName,
        String status,
        Instant checkedInAt,
        Instant checkedOutAt,
        int workedMinutes,
        int overtimeMinutes,
        boolean autoCheckout,
        String checkoutType,
        boolean late
) {
}
