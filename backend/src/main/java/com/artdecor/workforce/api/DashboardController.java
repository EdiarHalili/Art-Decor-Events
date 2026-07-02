package com.artdecor.workforce.api;

import com.artdecor.workforce.application.dashboard.AdminDashboardResponse;
import com.artdecor.workforce.application.dashboard.AdminDashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    private final AdminDashboardService adminDashboardService;

    public DashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
    public AdminDashboardResponse adminDashboard() {
        return adminDashboardService.snapshot();
    }

    @GetMapping("/employee/today")
    public EmployeeTodayResponse employeeToday() {
        return new EmployeeTodayResponse(
                "No assignment published for today.",
                "Check-in is not open.",
                List.of("Welcome to Art Decor Events Workforce.")
        );
    }

    public record EmployeeTodayResponse(
            String assignment,
            String status,
            List<String> announcements
    ) {
    }
}
