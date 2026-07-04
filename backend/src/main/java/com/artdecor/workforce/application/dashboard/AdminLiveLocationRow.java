package com.artdecor.workforce.application.dashboard;

import java.time.Instant;

public record AdminLiveLocationRow(
        String employeeId,
        String employeeCode,
        String employeeName,
        String attendanceRecordId,
        Double latitude,
        Double longitude,
        Double accuracyMeters,
        Instant capturedAt
) {
}
