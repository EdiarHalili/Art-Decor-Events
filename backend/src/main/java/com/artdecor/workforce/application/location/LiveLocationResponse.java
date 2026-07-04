package com.artdecor.workforce.application.location;

import java.time.Instant;

public record LiveLocationResponse(
        String id,
        String attendanceRecordId,
        String employeeId,
        Double latitude,
        Double longitude,
        Double accuracyMeters,
        Instant capturedAt
) {
}
