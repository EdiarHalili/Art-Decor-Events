package com.artdecor.workforce.application.location;

import java.time.Instant;
import java.util.Map;

public record LiveLocationCommand(
        Double latitude,
        Double longitude,
        Double accuracyMeters,
        Instant capturedAt,
        Map<String, Object> device
) {
}
