package com.artdecor.workforce.application.dashboard;

import java.time.Instant;
import java.util.List;

public record EmployeeTodayResponse(
        String employeeName,
        String scheduleId,
        String assignment,
        String status,
        boolean checkInOpen,
        boolean checkOutAvailable,
        Instant checkInOpensAt,
        Instant checkInClosesAt,
        Instant serverNow,
        List<String> announcements
) {
}
