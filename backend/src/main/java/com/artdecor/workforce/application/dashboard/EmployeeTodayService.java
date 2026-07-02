package com.artdecor.workforce.application.dashboard;

import com.artdecor.workforce.application.auth.AuthException;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeTodayService {
    private final EmployeeRepository employees;

    public EmployeeTodayService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public EmployeeTodayResponse today(AuthenticatedPrincipal principal) {
        var employee = employees.findById(principal.employeeId())
                .orElseThrow(() -> new AuthException("Authenticated employee no longer exists."));

        return new EmployeeTodayResponse(
                employee.getFullName(),
                "No assignment published for today.",
                "Check-in is not open.",
                false,
                false,
                List.of("Welcome to Art Decor Events Workforce.")
        );
    }
}

