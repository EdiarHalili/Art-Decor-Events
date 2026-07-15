package com.artdecor.workforce.application.attendance;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AttendanceActionCommand(
        UUID scheduleId,
        Double latitude,
        Double longitude,
        Map<String, Object> device,
        Instant capturedAt
) {
}
