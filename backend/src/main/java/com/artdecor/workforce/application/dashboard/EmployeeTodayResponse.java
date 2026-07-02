package com.artdecor.workforce.application.dashboard;

import java.util.List;

public record EmployeeTodayResponse(
        String employeeName,
        String assignment,
        String status,
        boolean checkInOpen,
        boolean checkOutAvailable,
        List<String> announcements
) {
}

