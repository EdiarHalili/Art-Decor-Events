package com.artdecor.workforce.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.application.notifications.NotificationService;
import com.artdecor.workforce.application.attendance.SimpleOpenModeService;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import com.artdecor.workforce.domain.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeTodayServiceTest {
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final NotificationService notifications = org.mockito.Mockito.mock(NotificationService.class);
    private final SimpleOpenModeService simpleOpenMode = org.mockito.Mockito.mock(SimpleOpenModeService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T06:55:00Z"), ZoneOffset.UTC);
    private final EmployeeTodayService service = new EmployeeTodayService(employees, assignments, attendanceRecords, notifications, simpleOpenMode, clock);

    @Test
    void returnsAuthenticatedEmployeeDashboard() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("hashed");
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(notifications.visibleAnnouncements()).thenReturn(List.of());
        when(simpleOpenMode.hasScheduledWindowForDate(java.time.LocalDate.of(2026, 7, 3))).thenReturn(false);
        when(simpleOpenMode.getOrCreateTodayAssignment(employee)).thenReturn(simpleAssignment(employee));

        EmployeeTodayResponse response = service.today(new AuthenticatedPrincipal(employeeId, UserRole.EMPLOYEE, employeeId));

        assertThat(response.employeeName()).isEqualTo("Season Worker");
        assertThat(response.assignment()).contains("Mënyra e hapur");
        assertThat(response.scheduleId()).isNotNull();
        assertThat(response.checkInOpen()).isTrue();
        assertThat(response.simpleOpenMode()).isTrue();
        assertThat(response.serverNow()).isEqualTo(Instant.parse("2026-07-03T06:55:00Z"));
        assertThat(response.announcements()).isEmpty();
    }

    @Test
    void doesNotFallbackWhenScheduledWindowExistsAndEmployeeIsNotAssigned() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("hashed");
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(notifications.visibleAnnouncements()).thenReturn(List.of());
        when(simpleOpenMode.hasScheduledWindowForDate(java.time.LocalDate.of(2026, 7, 3))).thenReturn(true);

        EmployeeTodayResponse response = service.today(new AuthenticatedPrincipal(employeeId, UserRole.EMPLOYEE, employeeId));

        assertThat(response.assignment()).isEqualTo("Daily check-in window");
        assertThat(response.status()).contains("nuk është i caktuar");
        assertThat(response.scheduleId()).isNull();
        assertThat(response.checkInOpen()).isFalse();
    }

    private ScheduleAssignmentEntity simpleAssignment(EmployeeEntity employee) {
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
        schedule.setTitle("Simple open attendance");
        schedule.setWorkDate(java.time.LocalDate.of(2026, 7, 3));
        schedule.setCheckInOpensAt(Instant.parse("2026-07-03T00:00:00Z"));
        schedule.setCheckInClosesAt(Instant.parse("2026-07-03T23:59:59Z"));
        schedule.setAutoCheckoutEnabled(false);
        schedule.setSimpleOpenMode(true);
        schedule.setStatus(WorkScheduleStatus.CHECK_IN_OPEN);
        ScheduleAssignmentEntity assignment = new ScheduleAssignmentEntity();
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
        assignment.setSchedule(schedule);
        assignment.setEmployee(employee);
        return assignment;
    }
}
