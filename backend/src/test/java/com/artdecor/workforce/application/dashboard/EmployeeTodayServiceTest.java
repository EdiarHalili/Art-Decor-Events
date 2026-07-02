package com.artdecor.workforce.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import com.artdecor.workforce.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeTodayServiceTest {
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T06:55:00Z"), ZoneOffset.UTC);
    private final EmployeeTodayService service = new EmployeeTodayService(employees, assignments, clock);

    @Test
    void returnsAuthenticatedEmployeeDashboard() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("hashed");
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));

        EmployeeTodayResponse response = service.today(new AuthenticatedPrincipal(employeeId, UserRole.EMPLOYEE, employeeId));

        assertThat(response.employeeName()).isEqualTo("Season Worker");
        assertThat(response.assignment()).contains("No assignment");
        assertThat(response.scheduleId()).isNull();
        assertThat(response.checkInOpen()).isFalse();
        assertThat(response.announcements()).isNotEmpty();
    }
}
