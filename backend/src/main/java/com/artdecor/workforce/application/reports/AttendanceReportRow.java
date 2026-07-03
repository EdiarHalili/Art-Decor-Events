package com.artdecor.workforce.application.reports;

import java.time.Instant;
import java.time.LocalDate;

public record AttendanceReportRow(
        LocalDate workDate,
        String scheduleId,
        String employeeId,
        String employeeCode,
        String employeeName,
        String status,
        Instant checkedInAt,
        Instant checkedOutAt,
        Double checkInLatitude,
        Double checkInLongitude,
        Double checkOutLatitude,
        Double checkOutLongitude,
        Integer checkInDistanceMeters,
        Integer checkOutDistanceMeters,
        int workedMinutes,
        int overtimeMinutes,
        boolean autoCheckout,
        boolean late,
        boolean absent
) {
}
