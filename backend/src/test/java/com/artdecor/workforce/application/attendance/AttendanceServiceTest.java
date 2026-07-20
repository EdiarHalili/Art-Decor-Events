package com.artdecor.workforce.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.settings.AppSettingsResponse;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.CheckoutType;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

class AttendanceServiceTest {
    private final WorkScheduleRepository schedules = org.mockito.Mockito.mock(WorkScheduleRepository.class);
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final LiveLocationUpdateRepository liveLocations = org.mockito.Mockito.mock(LiveLocationUpdateRepository.class);
    private final AppSettingsService settings = org.mockito.Mockito.mock(AppSettingsService.class);
    private final AuditService audit = org.mockito.Mockito.mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T06:55:00Z"), ZoneOffset.UTC);
    private AttendanceService service;
    private UUID employeeId;
    private UUID scheduleId;
    private EmployeeEntity employee;
    private WorkScheduleEntity schedule;
    private AuthenticatedPrincipal principal;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(schedules, assignments, attendanceRecords, employees, liveLocations, settings, audit, clock);
        employeeId = UUID.randomUUID();
        scheduleId = UUID.randomUUID();
        employee = employee(employeeId);
        schedule = schedule(scheduleId);
        principal = new AuthenticatedPrincipal(employeeId, UserRole.EMPLOYEE, employeeId);

        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(schedules.findById(scheduleId)).thenReturn(Optional.of(schedule));
        when(assignments.findByScheduleIdAndEmployeeId(scheduleId, employeeId))
                .thenReturn(Optional.of(assignment(employee, schedule)));
        when(attendanceRecords.findActiveRecordsByEmployeeId(employeeId)).thenReturn(List.of());
        when(settings.current()).thenReturn(new AppSettingsResponse(
                "Art Decor Events",
                null,
                "#c9a052",
                "#4f7f63",
                "Europe/Berlin",
                LocalTime.of(6, 50),
                LocalTime.of(7, 10),
                0,
                true,
                null,
                null,
                true,
                false,
                10,
                60,
                false,
                Instant.parse("2026-07-03T00:00:00Z")
        ));
        when(attendanceRecords.saveAndFlush(any(AttendanceRecordEntity.class))).thenAnswer(invocation -> {
            AttendanceRecordEntity record = invocation.getArgument(0);
            ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
            return record;
        });
    }

    @Test
    void recordsCheckInWithinOpenWindow() {
        AttendanceResponse response = service.checkIn(principal, command());

        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT.name());
        assertThat(response.checkedInAt()).isEqualTo(Instant.parse("2026-07-03T06:55:00Z"));
    }

    @Test
    void storesBrowserGpsCoordinatesWithoutSwappingLatitudeAndLongitude() {
        AttendanceActionCommand gpsCommand = new AttendanceActionCommand(
                scheduleId,
                42.30413,
                21.64894,
                Map.of("platform", "test"),
                null
        );

        service.checkIn(principal, gpsCommand);

        ArgumentCaptor<AttendanceRecordEntity> captor = ArgumentCaptor.forClass(AttendanceRecordEntity.class);
        verify(attendanceRecords).saveAndFlush(captor.capture());
        AttendanceRecordEntity saved = captor.getValue();
        assertThat(saved.getCheckInLatitude()).isEqualTo(42.30413);
        assertThat(saved.getCheckInLongitude()).isEqualTo(21.64894);
    }

    @Test
    void recordsCheckInWhenEmployeeHasExactAssignmentForSubmittedWindow() {
        UUID oldScheduleId = UUID.randomUUID();
        WorkScheduleEntity oldSchedule = schedule(oldScheduleId);
        oldSchedule.setStatus(WorkScheduleStatus.CANCELLED);
        when(assignments.findFirstByEmployeeIdAndScheduleWorkDateOrderByCreatedAtAsc(employeeId, schedule.getWorkDate()))
                .thenReturn(Optional.of(assignment(employee, oldSchedule)));
        when(assignments.findByScheduleIdAndEmployeeId(scheduleId, employeeId))
                .thenReturn(Optional.of(assignment(employee, schedule)));

        AttendanceResponse response = service.checkIn(principal, command());

        assertThat(response.scheduleId()).isEqualTo(scheduleId.toString());
        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT.name());
    }

    @Test
    void preventsDuplicateCheckIn() {
        AttendanceRecordEntity existing = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setSchedule(schedule);
        existing.setEmployee(employee);
        existing.setCheckedInAt(Instant.parse("2026-07-03T06:54:00Z"));
        when(attendanceRecords.existsByScheduleIdAndEmployeeIdAndCheckedInAtIsNotNull(scheduleId, employeeId)).thenReturn(true);

        assertThatThrownBy(() -> service.checkIn(principal, command()))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("Hyrja e dytë nuk lejohet.");
    }

    @Test
    void allowsMultipleCompletedOpenModeSessionsOnSameDay() {
        schedule.setSimpleOpenMode(true);
        AttendanceRecordEntity completed = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(completed, "id", UUID.randomUUID());
        completed.setSchedule(schedule);
        completed.setEmployee(employee);
        completed.setCheckedInAt(Instant.parse("2026-07-03T04:00:00Z"));
        completed.setCheckedOutAt(Instant.parse("2026-07-03T08:00:00Z"));

        AttendanceResponse response = service.checkIn(principal, new AttendanceActionCommand(
                scheduleId,
                42.0,
                21.0,
                Map.of("platform", "test"),
                Instant.parse("2026-07-03T09:00:00Z")
        ));

        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT.name());
        ArgumentCaptor<AttendanceRecordEntity> captor = ArgumentCaptor.forClass(AttendanceRecordEntity.class);
        verify(attendanceRecords).saveAndFlush(captor.capture());
        assertThat(captor.getValue()).isNotSameAs(completed);
        assertThat(captor.getValue().getCheckedInAt()).isEqualTo(Instant.parse("2026-07-03T09:00:00Z"));
    }

    @Test
    void allowsOpenModeCheckInAfterOvernightCheckoutOnSameCalendarDay() {
        schedule.setSimpleOpenMode(true);
        AttendanceRecordEntity overnight = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(overnight, "id", UUID.randomUUID());
        overnight.setSchedule(schedule);
        overnight.setEmployee(employee);
        overnight.setCheckedInAt(Instant.parse("2026-07-15T18:00:00Z"));
        overnight.setCheckedOutAt(Instant.parse("2026-07-16T01:30:00Z"));

        AttendanceResponse response = service.checkIn(principal, new AttendanceActionCommand(
                scheduleId,
                42.0,
                21.0,
                Map.of("platform", "test"),
                Instant.parse("2026-07-16T09:00:00Z")
        ));

        assertThat(response.checkedInAt()).isEqualTo(Instant.parse("2026-07-16T09:00:00Z"));
    }

    @Test
    void preventsCheckInWhenEmployeeAlreadyHasAnotherActiveRecord() {
        UUID activeScheduleId = UUID.randomUUID();
        WorkScheduleEntity activeSchedule = schedule(activeScheduleId);
        AttendanceRecordEntity active = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(active, "id", UUID.randomUUID());
        active.setSchedule(activeSchedule);
        active.setEmployee(employee);
        active.setCheckedInAt(Instant.parse("2026-07-03T06:30:00Z"));
        when(attendanceRecords.findActiveRecordsByEmployeeId(employeeId)).thenReturn(List.of(active));

        assertThatThrownBy(() -> service.checkIn(principal, command()))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("Ju tashmë keni filluar orarin e punës.");
    }

    @Test
    void recordsCheckOutAndWorkedMinutes() {
        AttendanceRecordEntity existing = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setSchedule(schedule);
        existing.setEmployee(employee);
        existing.setCheckedInAt(Instant.parse("2026-07-03T06:00:00Z"));
        when(attendanceRecords.findActiveRecordByScheduleIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.of(existing));

        AttendanceResponse response = service.checkOut(principal, command());

        assertThat(response.status()).isEqualTo(AttendanceStatus.CHECKED_OUT.name());
        assertThat(response.workedMinutes()).isEqualTo(55);
    }

    @Test
    void queuedCheckInUsesOriginalCapturedTimeAndGps() {
        AttendanceActionCommand offlineCommand = new AttendanceActionCommand(
                scheduleId,
                42.30413,
                21.64894,
                Map.of("platform", "phone", "offlineActionId", "offline-1"),
                Instant.parse("2026-07-03T06:52:00Z")
        );

        AttendanceResponse response = service.checkIn(principal, offlineCommand);

        assertThat(response.checkedInAt()).isEqualTo(Instant.parse("2026-07-03T06:52:00Z"));
        ArgumentCaptor<AttendanceRecordEntity> captor = ArgumentCaptor.forClass(AttendanceRecordEntity.class);
        verify(attendanceRecords).saveAndFlush(captor.capture());
        AttendanceRecordEntity saved = captor.getValue();
        assertThat(saved.getCheckInLatitude()).isEqualTo(42.30413);
        assertThat(saved.getCheckInLongitude()).isEqualTo(21.64894);
    }

    @Test
    void queuedCheckInIsAcceptedAfterWindowClosedWhenCapturedDuringWindow() {
        schedule.setStatus(WorkScheduleStatus.CHECK_IN_CLOSED);

        AttendanceResponse response = service.checkIn(principal, new AttendanceActionCommand(
                scheduleId,
                42.30413,
                21.64894,
                Map.of("platform", "phone", "offlineActionId", "offline-closed-window"),
                Instant.parse("2026-07-03T06:52:00Z")
        ));

        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT.name());
        assertThat(response.checkedInAt()).isEqualTo(Instant.parse("2026-07-03T06:52:00Z"));
    }

    @Test
    void queuedCheckOutUsesOriginalCapturedTimeForWorkedMinutes() {
        AttendanceRecordEntity existing = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
        existing.setSchedule(schedule);
        existing.setEmployee(employee);
        existing.setCheckedInAt(Instant.parse("2026-07-03T06:52:00Z"));
        when(attendanceRecords.findActiveRecordByScheduleIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.of(existing));

        AttendanceResponse response = service.checkOut(principal, new AttendanceActionCommand(
                scheduleId,
                42.30413,
                21.64894,
                Map.of("platform", "phone", "offlineActionId", "offline-2"),
                Instant.parse("2026-07-03T15:10:00Z")
        ));

        assertThat(response.checkedOutAt()).isEqualTo(Instant.parse("2026-07-03T15:10:00Z"));
        assertThat(response.workedMinutes()).isEqualTo(498);
        assertThat(response.overtimeMinutes()).isEqualTo(10);
    }

    @Test
    void convertsDatabaseActiveSessionRaceConflictToFriendlyMessage() {
        when(attendanceRecords.saveAndFlush(any(AttendanceRecordEntity.class)))
                .thenThrow(new DataIntegrityViolationException("uq_attendance_one_active_per_employee"));

        assertThatThrownBy(() -> service.checkIn(principal, command()))
                .isInstanceOf(AttendanceException.class)
                .hasMessage("Ju tashmë keni filluar orarin e punës.");
    }

    @Test
    void extendsStaleSimpleOpenCutoffPastActualCheckInTime() {
        schedule.setSimpleOpenMode(true);
        schedule.setAutoCheckoutEnabled(true);
        schedule.setCheckInClosesAt(Instant.parse("2026-07-03T07:10:00Z"));
        schedule.setPlannedEndAt(Instant.parse("2026-07-03T07:10:00Z"));

        AttendanceResponse response = service.checkIn(principal, new AttendanceActionCommand(
                scheduleId,
                42.0,
                21.0,
                Map.of("platform", "test"),
                Instant.parse("2026-07-03T14:00:00Z")
        ));

        assertThat(response.checkedInAt()).isEqualTo(Instant.parse("2026-07-03T14:00:00Z"));
        assertThat(schedule.getCheckInClosesAt()).isAfter(response.checkedInAt());
        assertThat(schedule.getCheckInClosesAt()).isEqualTo(Instant.parse("2026-07-04T05:10:00Z"));
        assertThat(schedule.getPlannedEndAt()).isEqualTo(schedule.getCheckInClosesAt());
    }

    @Test
    void logsLiveTrackingStopWhenCheckedOutAfterLocationUpdates() {
        UUID recordId = UUID.randomUUID();
        AttendanceRecordEntity existing = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(existing, "id", recordId);
        existing.setSchedule(schedule);
        existing.setEmployee(employee);
        existing.setCheckedInAt(Instant.parse("2026-07-03T06:00:00Z"));
        when(attendanceRecords.findActiveRecordByScheduleIdAndEmployeeId(scheduleId, employeeId)).thenReturn(Optional.of(existing));
        when(liveLocations.existsByAttendanceRecordId(recordId)).thenReturn(true);

        service.checkOut(principal, command());

        verify(audit).log(principal, "LIVE_LOCATION_TRACKING_STOPPED", "ATTENDANCE_RECORD", recordId);
    }

    @Test
    void adminCheckoutSetsTypeAndRejectsDuplicateCheckout() {
        UUID recordId = UUID.randomUUID();
        AuthenticatedPrincipal admin = new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);
        AttendanceRecordEntity existing = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(existing, "id", recordId);
        existing.setSchedule(schedule);
        existing.setEmployee(employee);
        existing.setCheckedInAt(Instant.parse("2026-07-03T06:00:00Z"));
        when(attendanceRecords.findById(recordId)).thenReturn(Optional.of(existing));

        AttendanceResponse response = service.adminCheckOut(admin, recordId, Instant.parse("2026-07-03T14:00:00Z"));

        assertThat(response.status()).isEqualTo(AttendanceStatus.CHECKED_OUT.name());
        assertThat(response.workedMinutes()).isEqualTo(480);
        assertThat(response.checkoutType()).isEqualTo(CheckoutType.ADMIN_CHECKED_OUT.name());
        verify(audit).log(admin, "ADMIN_CHECK_OUT_RECORDED", "ATTENDANCE_RECORD", recordId);

        assertThatThrownBy(() -> service.adminCheckOut(admin, recordId, Instant.parse("2026-07-03T15:00:00Z")))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("Nuk ka një orar aktiv për ta përfunduar.");
    }

    private AttendanceActionCommand command() {
        return new AttendanceActionCommand(scheduleId, 42.0, 21.0, Map.of("platform", "test"), null);
    }

    private EmployeeEntity employee(UUID id) {
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", id);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("hashed");
        return employee;
    }

    private WorkScheduleEntity schedule(UUID id) {
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", id);
        ReflectionTestUtils.setField(schedule, "title", "Breta Palace setup");
        ReflectionTestUtils.setField(schedule, "workDate", LocalDate.of(2026, 7, 3));
        ReflectionTestUtils.setField(schedule, "checkInOpensAt", Instant.parse("2026-07-03T06:50:00Z"));
        ReflectionTestUtils.setField(schedule, "checkInClosesAt", Instant.parse("2026-07-03T07:10:00Z"));
        ReflectionTestUtils.setField(schedule, "plannedStartAt", Instant.parse("2026-07-03T07:00:00Z"));
        ReflectionTestUtils.setField(schedule, "plannedEndAt", Instant.parse("2026-07-03T15:00:00Z"));
        ReflectionTestUtils.setField(schedule, "status", WorkScheduleStatus.PUBLISHED);
        return schedule;
    }

    private ScheduleAssignmentEntity assignment(EmployeeEntity employee, WorkScheduleEntity schedule) {
        ScheduleAssignmentEntity assignment = new ScheduleAssignmentEntity();
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(assignment, "employee", employee);
        ReflectionTestUtils.setField(assignment, "schedule", schedule);
        return assignment;
    }
}
