package com.artdecor.workforce.application.dashboard;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final EmployeeRepository employees;
    private final UserAccountRepository users;

    public AdminDashboardService(EmployeeRepository employees, UserAccountRepository users) {
        this.employees = employees;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse snapshot() {
        return new AdminDashboardResponse(
                LocalDate.now(),
                0,
                0,
                0,
                0,
                employees.countByStatus(UserStatus.ACTIVE),
                employees.countByStatus(UserStatus.INACTIVE),
                users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE),
                users.countByRoleAndStatus(UserRole.SUPERVISOR, UserStatus.ACTIVE),
                List.of("Create first schedule", "Add employee profiles", "Post announcement")
        );
    }
}

