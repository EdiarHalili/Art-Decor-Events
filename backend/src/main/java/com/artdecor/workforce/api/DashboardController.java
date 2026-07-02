package com.artdecor.workforce.api;

import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    @GetMapping("/admin/dashboard")
    public AdminDashboardResponse adminDashboard() {
        return new AdminDashboardResponse(
                LocalDate.now(),
                0,
                0,
                0,
                0,
                List.of("Create first schedule", "Add employee profiles", "Review attendance policy")
        );
    }

    @GetMapping("/employee/today")
    public EmployeeTodayResponse employeeToday() {
        return new EmployeeTodayResponse(
                "No assignment published for today.",
                "Check-in is not open.",
                List.of("Welcome to Art Decor Events Workforce.")
        );
    }

    public record AdminDashboardResponse(
            LocalDate date,
            int present,
            int late,
            int absent,
            int currentlyWorking,
            List<String> quickActions
    ) {
    }

    public record EmployeeTodayResponse(
            String assignment,
            String status,
            List<String> announcements
    ) {
    }
}

