package com.artdecor.workforce.application.checkinwindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class DailyCheckInWindowServiceTest {
    private final WorkScheduleRepository windows = org.mockito.Mockito.mock(WorkScheduleRepository.class);
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private DailyCheckInWindowService service;
    private UUID employeeId;
    private EmployeeEntity employee;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-03T12:00:00Z"), ZoneOffset.UTC);
        service = new DailyCheckInWindowService(windows, assignments, attendanceRecords, employees, clock);
        employeeId = UUID.randomUUID();
        employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("hash");

        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(windows.findOverlappingWindows(any(), any(), any(), any())).thenReturn(List.of());
        when(windows.findOverlappingWindowsExcluding(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(windows.save(any(WorkScheduleEntity.class))).thenAnswer(invocation -> {
            WorkScheduleEntity window = invocation.getArgument(0);
            ReflectionTestUtils.setField(window, "id", UUID.randomUUID());
            return window;
        });
    }

    @Test
    void createsDailyWindowWithAllowedEmployees() {
        DailyCheckInWindowResponse response = service.createWindow(command(Set.of(employeeId)));

        assertThat(response.workDate()).isEqualTo(LocalDate.of(2026, 7, 4));
        assertThat(response.status()).isEqualTo("PUBLISHED");

        ArgumentCaptor<ScheduleAssignmentEntity> assignmentCaptor = ArgumentCaptor.forClass(ScheduleAssignmentEntity.class);
        org.mockito.Mockito.verify(assignments).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().getEmployee().getId()).isEqualTo(employeeId);
    }

    @Test
    void rejectsOverlappingActiveWindow() {
        when(windows.findOverlappingWindows(any(), any(), any(), any()))
                .thenReturn(List.of(window(UUID.randomUUID())));

        assertThatThrownBy(() -> service.createWindow(command(Set.of(employeeId))))
                .isInstanceOf(DailyCheckInWindowException.class)
                .hasMessageContaining("This time range overlaps an active window.");
    }

    @Test
    void closedCompletedOrCancelledWindowsDoNotBlockNewWindowForSameDate() {
        when(windows.findOverlappingWindows(any(), any(), any(), any())).thenReturn(List.of());

        DailyCheckInWindowResponse response = service.createWindow(command(Set.of(employeeId)));

        assertThat(response.workDate()).isEqualTo(LocalDate.of(2026, 7, 4));
        assertThat(response.status()).isEqualTo("PUBLISHED");
    }

    @Test
    void createsOvernightWindowWhenCloseIsNextDay() {
        DailyCheckInWindowResponse response = service.createWindow(overnightCommand(Set.of(employeeId)));

        assertThat(response.checkInOpensAt()).isEqualTo(Instant.parse("2026-07-04T16:00:00Z"));
        assertThat(response.checkInClosesAt()).isEqualTo(Instant.parse("2026-07-05T05:10:00Z"));
    }

    @Test
    void rejectsEmptyEmployeeSelectionWithClearMessage() {
        assertThatThrownBy(() -> service.createWindow(command(Set.of())))
                .isInstanceOf(DailyCheckInWindowException.class)
                .hasMessageContaining("Please select at least one employee.");
    }

    @Test
    void opensAndClosesWindowManually() {
        UUID windowId = UUID.randomUUID();
        WorkScheduleEntity window = window(windowId);
        when(windows.findById(windowId)).thenReturn(Optional.of(window));
        when(assignments.findAllByScheduleId(windowId)).thenReturn(List.of());

        assertThat(service.openWindow(windowId).status()).isEqualTo("CHECK_IN_OPEN");
        assertThat(service.closeWindow(windowId).status()).isEqualTo("CHECK_IN_CLOSED");
    }

    @Test
    void deletesOnlyCancelledWindows() {
        UUID windowId = UUID.randomUUID();
        WorkScheduleEntity window = window(windowId);
        window.setStatus(WorkScheduleStatus.CANCELLED);
        when(windows.findById(windowId)).thenReturn(Optional.of(window));

        service.deleteCancelledWindow(windowId);

        org.mockito.Mockito.verify(attendanceRecords).deleteByScheduleId(windowId);
        org.mockito.Mockito.verify(assignments).deleteByScheduleId(windowId);
        org.mockito.Mockito.verify(windows).delete(window);
    }

    @Test
    void rejectsDeleteForActiveWindows() {
        UUID windowId = UUID.randomUUID();
        WorkScheduleEntity window = window(windowId);
        when(windows.findById(windowId)).thenReturn(Optional.of(window));

        assertThatThrownBy(() -> service.deleteCancelledWindow(windowId))
                .isInstanceOf(DailyCheckInWindowException.class)
                .hasMessageContaining("Only cancelled");
    }

    private DailyCheckInWindowCommand command(Set<UUID> employeeIds) {
        return new DailyCheckInWindowCommand(
                LocalDate.of(2026, 7, 4),
                Instant.parse("2026-07-04T04:50:00Z"),
                Instant.parse("2026-07-04T05:10:00Z"),
                employeeIds
        );
    }

    private DailyCheckInWindowCommand overnightCommand(Set<UUID> employeeIds) {
        return new DailyCheckInWindowCommand(
                LocalDate.of(2026, 7, 4),
                Instant.parse("2026-07-04T16:00:00Z"),
                Instant.parse("2026-07-05T05:10:00Z"),
                employeeIds
        );
    }

    private WorkScheduleEntity window(UUID id) {
        WorkScheduleEntity window = new WorkScheduleEntity();
        ReflectionTestUtils.setField(window, "id", id);
        window.setTitle("Daily check-in window");
        window.setWorkDate(LocalDate.of(2026, 7, 4));
        window.setCheckInOpensAt(Instant.parse("2026-07-04T04:50:00Z"));
        window.setCheckInClosesAt(Instant.parse("2026-07-04T05:10:00Z"));
        window.setStatus(WorkScheduleStatus.PUBLISHED);
        return window;
    }
}
