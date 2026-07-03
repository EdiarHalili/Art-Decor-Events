package com.artdecor.workforce.application.settings;

import java.time.LocalTime;

public record AppSettingsCommand(
        String companyName,
        String logoUrl,
        String primaryColor,
        String accentColor,
        String timezone,
        LocalTime defaultCheckInOpenTime,
        LocalTime defaultCheckInCloseTime,
        int allowedLateMinutes,
        boolean gpsEnabled,
        Double workplaceLatitude,
        Double workplaceLongitude,
        boolean notificationsEnabled,
        int sessionTimeoutMinutes
) {
}
