package com.artdecor.workforce.application.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminDashboardServiceTest {
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final AdminDashboardService service = new AdminDashboardService(employees, users, assignments, attendanceRecords);

    @Test
    void returnsCurrentWorkforceSnapshot() {
        when(employees.countByStatus(UserStatus.ACTIVE)).thenReturn(12L);
        when(employees.countByStatus(UserStatus.INACTIVE)).thenReturn(3L);
        when(users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)).thenReturn(2L);
        when(users.countByRoleAndStatus(UserRole.SUPERVISOR, UserStatus.ACTIVE)).thenReturn(4L);
        when(attendanceRecords.findReportRecords(LocalDate.now(), LocalDate.now())).thenReturn(List.of());
        when(assignments.findReportAssignments(LocalDate.now(), LocalDate.now(), WorkScheduleStatus.CANCELLED)).thenReturn(List.of());

        AdminDashboardResponse response = service.snapshot();

        assertThat(response.activeEmployees()).isEqualTo(12);
        assertThat(response.inactiveEmployees()).isEqualTo(3);
        assertThat(response.administrators()).isEqualTo(2);
        assertThat(response.supervisors()).isEqualTo(4);
        assertThat(response.quickActions()).isNotEmpty();
    }

    @Test
    void includesCheckedInEmployeesInDashboardCountersAndLiveAttendance() {
        LocalDate today = LocalDate.now();
        UUID scheduleId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", scheduleId);
        schedule.setWorkDate(today);

        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Demo Employee");

        ScheduleAssignmentEntity assignment = new ScheduleAssignmentEntity();
        assignment.setSchedule(schedule);
        assignment.setEmployee(employee);

        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setStatus(AttendanceStatus.PRESENT);
        record.setCheckedInAt(Instant.parse("2026-07-03T04:58:00Z"));

        when(employees.countByStatus(UserStatus.ACTIVE)).thenReturn(1L);
        when(employees.countByStatus(UserStatus.INACTIVE)).thenReturn(0L);
        when(users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)).thenReturn(1L);
        when(users.countByRoleAndStatus(UserRole.SUPERVISOR, UserStatus.ACTIVE)).thenReturn(0L);
        when(attendanceRecords.findReportRecords(today, today)).thenReturn(List.of(record));
        when(assignments.findReportAssignments(today, today, WorkScheduleStatus.CANCELLED)).thenReturn(List.of(assignment));

        AdminDashboardResponse response = service.snapshot();

        assertThat(response.present()).isEqualTo(1);
        assertThat(response.currentlyWorking()).isEqualTo(1);
        assertThat(response.absent()).isZero();
        assertThat(response.liveAttendance()).hasSize(1);
        assertThat(response.liveAttendance().getFirst().employeeName()).isEqualTo("Demo Employee");
    }

    @Test
    void exposesAutoCheckoutRecordsWithoutCountingThemAsCurrentlyWorking() {
        LocalDate today = LocalDate.now();
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
        schedule.setWorkDate(today);

        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
        employee.setEmployeeCode("EMP002");
        employee.setFullName("Auto Checkout Employee");

        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
        record.setCheckedInAt(Instant.parse("2026-07-03T14:00:00Z"));
        record.setCheckedOutAt(Instant.parse("2026-07-03T21:00:00Z"));
        record.setWorkedMinutes(420);
        record.setAutoCheckout(true);

        when(employees.countByStatus(UserStatus.ACTIVE)).thenReturn(1L);
        when(employees.countByStatus(UserStatus.INACTIVE)).thenReturn(0L);
        when(users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)).thenReturn(1L);
        when(users.countByRoleAndStatus(UserRole.SUPERVISOR, UserStatus.ACTIVE)).thenReturn(0L);
        when(attendanceRecords.findReportRecords(today, today)).thenReturn(List.of(record));
        when(assignments.findReportAssignments(today, today, WorkScheduleStatus.CANCELLED)).thenReturn(List.of());

        AdminDashboardResponse response = service.snapshot();

        assertThat(response.present()).isEqualTo(1);
        assertThat(response.currentlyWorking()).isZero();
        assertThat(response.liveAttendance()).hasSize(1);
        assertThat(response.liveAttendance().getFirst().autoCheckout()).isTrue();
        assertThat(response.liveAttendance().getFirst().workedMinutes()).isEqualTo(420);
    }
}
