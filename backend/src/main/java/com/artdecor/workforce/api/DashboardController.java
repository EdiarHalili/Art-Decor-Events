package com.artdecor.workforce.api;

import com.artdecor.workforce.application.dashboard.AdminDashboardResponse;
import com.artdecor.workforce.application.dashboard.AdminDashboardService;
import com.artdecor.workforce.application.dashboard.EmployeeTodayResponse;
import com.artdecor.workforce.application.dashboard.EmployeeTodayService;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DashboardController {
    private final AdminDashboardService adminDashboardService;
    private final EmployeeTodayService employeeTodayService;

    public DashboardController(AdminDashboardService adminDashboardService, EmployeeTodayService employeeTodayService) {
        this.adminDashboardService = adminDashboardService;
        this.employeeTodayService = employeeTodayService;
    }

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
    public AdminDashboardResponse adminDashboard() {
        return adminDashboardService.snapshot();
    }

    @GetMapping("/employee/today")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public EmployeeTodayResponse employeeToday(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return employeeTodayService.today(principal);
    }
}
