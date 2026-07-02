package com.artdecor.workforce.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;

class AdminDashboardServiceTest {
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
    private final AdminDashboardService service = new AdminDashboardService(employees, users);

    @Test
    void returnsCurrentWorkforceSnapshot() {
        when(employees.countByStatus(UserStatus.ACTIVE)).thenReturn(12L);
        when(employees.countByStatus(UserStatus.INACTIVE)).thenReturn(3L);
        when(users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)).thenReturn(2L);
        when(users.countByRoleAndStatus(UserRole.SUPERVISOR, UserStatus.ACTIVE)).thenReturn(4L);

        AdminDashboardResponse response = service.snapshot();

        assertThat(response.activeEmployees()).isEqualTo(12);
        assertThat(response.inactiveEmployees()).isEqualTo(3);
        assertThat(response.administrators()).isEqualTo(2);
        assertThat(response.supervisors()).isEqualTo(4);
        assertThat(response.quickActions()).isNotEmpty();
    }
}

