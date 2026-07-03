package com.artdecor.workforce.application.settings;

import java.time.Instant;
import java.time.LocalTime;

public record AppSettingsResponse(
        String companyName,
        String logoUrl,
        String primaryColor,
        String accentColor,
        String timezone,
        LocalTime defaultCheckInOpenTime,
        LocalTime defaultCheckInCloseTime,
        int allowedLateMinutes,
        boolean gpsEnabled,
        boolean notificationsEnabled,
        int sessionTimeoutMinutes,
        Instant updatedAt
) {
}
