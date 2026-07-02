package com.artdecor.workforce.application.dashboard;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
        LocalDate date,
        long present,
        long late,
        long absent,
        long currentlyWorking,
        long activeEmployees,
        long inactiveEmployees,
        long administrators,
        long supervisors,
        List<String> quickActions
) {
}

