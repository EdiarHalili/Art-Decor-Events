package com.artdecor.workforce.application.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.application.attendance.AttendanceException;
import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.settings.AppSettingsResponse;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateEntity;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LiveLocationServiceTest {
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final LiveLocationUpdateRepository liveLocations = org.mockito.Mockito.mock(LiveLocationUpdateRepository.class);
    private final AppSettingsService settings = org.mockito.Mockito.mock(AppSettingsService.class);
    private final AuditService audit = org.mockito.Mockito.mock(AuditService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T07:00:00Z"), ZoneOffset.UTC);
    private final LiveLocationService service = new LiveLocationService(attendanceRecords, liveLocations, settings, audit, clock);
    private final UUID employeeId = UUID.randomUUID();
    private final AuthenticatedPrincipal principal = new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.EMPLOYEE, employeeId);

    @BeforeEach
    void setUp() {
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
                true,
                10,
                60,
                false,
                Instant.parse("2026-07-03T00:00:00Z")
        ));
    }

    @Test
    void rejectsLocationWhenEmployeeIsNotCheckedIn() {
        when(attendanceRecords.findActiveRecordsByEmployeeId(employeeId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.record(principal, command()))
                .isInstanceOf(AttendanceException.class)
                .hasMessageContaining("after check-in and before check-out");
    }

    @Test
    void storesLocationForActiveShiftAndLogsTrackingStart() {
        AttendanceRecordEntity record = activeRecord();
        when(attendanceRecords.findActiveRecordsByEmployeeId(employeeId)).thenReturn(List.of(record));
        when(liveLocations.existsByAttendanceRecordId(record.getId())).thenReturn(false);
        when(liveLocations.save(any(LiveLocationUpdateEntity.class))).thenAnswer(invocation -> {
            LiveLocationUpdateEntity update = invocation.getArgument(0);
            ReflectionTestUtils.setField(update, "id", UUID.randomUUID());
            return update;
        });

        LiveLocationResponse response = service.record(principal, command());

        assertThat(response.latitude()).isEqualTo(42.30413);
        assertThat(response.longitude()).isEqualTo(21.64894);
        verify(audit).log(principal, "LIVE_LOCATION_TRACKING_STARTED", "ATTENDANCE_RECORD", record.getId());
    }

    private LiveLocationCommand command() {
        return new LiveLocationCommand(
                42.30413,
                21.64894,
                15.0,
                Instant.parse("2026-07-03T06:59:00Z"),
                Map.of("platform", "test")
        );
    }

    private AttendanceRecordEntity activeRecord() {
        UUID scheduleId = UUID.randomUUID();
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", scheduleId);

        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Demo Employee");

        AttendanceRecordEntity record = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setCheckedInAt(Instant.parse("2026-07-03T06:50:00Z"));
        return record;
    }
}
