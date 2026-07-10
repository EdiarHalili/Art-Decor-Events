package com.artdecor.workforce.application.checkinwindow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record DailyCheckInWindowCommand(
        LocalDate workDate,
        Instant checkInOpensAt,
        Instant checkInClosesAt,
        boolean autoCheckoutEnabled,
        Set<UUID> employeeIds
) {
}
