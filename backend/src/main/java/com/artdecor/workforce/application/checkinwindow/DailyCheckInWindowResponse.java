package com.artdecor.workforce.application.checkinwindow;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record DailyCheckInWindowResponse(
        String id,
        LocalDate workDate,
        Instant checkInOpensAt,
        Instant checkInClosesAt,
        String status,
        List<String> employeeIds,
        int allowedEmployeeCount,
        Instant createdAt,
        Instant updatedAt
) {
}

