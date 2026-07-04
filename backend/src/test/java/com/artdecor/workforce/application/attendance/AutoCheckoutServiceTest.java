package com.artdecor.workforce.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AutoCheckoutServiceTest {
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final WorkScheduleRepository schedules = org.mockito.Mockito.mock(WorkScheduleRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T21:05:00Z"), ZoneOffset.UTC);
    private final AutoCheckoutService service = new AutoCheckoutService(attendanceRecords, schedules, clock);

    @Test
    void automaticallyChecksOutOpenRecordAtWindowCloseTime() {
        AttendanceRecordEntity record = checkedInRecord();
        when(attendanceRecords.findRecordsDueForAutoCheckout(Instant.parse("2026-07-03T21:05:00Z"), WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of(record));
        when(schedules.findWindowsDueForCompletion(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(record.getSchedule()));

        int updated = service.autoCheckoutDueRecords(Instant.parse("2026-07-03T21:05:00Z"));

        assertThat(updated).isEqualTo(1);
        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.CHECKED_OUT);
        assertThat(record.getCheckedOutAt()).isEqualTo(Instant.parse("2026-07-03T21:00:00Z"));
        assertThat(record.getWorkedMinutes()).isEqualTo(420);
        assertThat(record.isAutoCheckout()).isTrue();
        assertThat(record.getSchedule().getStatus()).isEqualTo(WorkScheduleStatus.COMPLETED);
    }

    @Test
    void doesNotDuplicateAutoCheckoutWhenRecordIsAlreadyCheckedOut() {
        AttendanceRecordEntity record = checkedInRecord();
        when(attendanceRecords.findRecordsDueForAutoCheckout(Instant.parse("2026-07-03T21:05:00Z"), WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of(record));
        when(schedules.findWindowsDueForCompletion(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(record.getSchedule()));

        assertThat(service.autoCheckoutDueRecords(Instant.parse("2026-07-03T21:05:00Z"))).isEqualTo(1);
        assertThat(service.autoCheckoutDueRecords(Instant.parse("2026-07-03T21:05:00Z"))).isZero();
        assertThat(record.getCheckedOutAt()).isEqualTo(Instant.parse("2026-07-03T21:00:00Z"));
        assertThat(record.isAutoCheckout()).isTrue();
    }

    @Test
    void completesDueWindowEvenWhenNoEmployeesCheckedIn() {
        WorkScheduleEntity schedule = dueSchedule();
        when(attendanceRecords.findRecordsDueForAutoCheckout(Instant.parse("2026-07-03T21:05:00Z"), WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of());
        when(schedules.findWindowsDueForCompletion(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(schedule));

        assertThat(service.autoCheckoutDueRecords(Instant.parse("2026-07-03T21:05:00Z"))).isZero();
        assertThat(schedule.getStatus()).isEqualTo(WorkScheduleStatus.COMPLETED);
    }

    private AttendanceRecordEntity checkedInRecord() {
        WorkScheduleEntity schedule = dueSchedule();

        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("hash");

        AttendanceRecordEntity record = new AttendanceRecordEntity();
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setCheckedInAt(Instant.parse("2026-07-03T14:00:00Z"));
        record.setStatus(AttendanceStatus.PRESENT);
        return record;
    }

    private WorkScheduleEntity dueSchedule() {
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
        schedule.setTitle("Daily check-in window");
        schedule.setWorkDate(LocalDate.of(2026, 7, 3));
        schedule.setCheckInOpensAt(Instant.parse("2026-07-03T14:00:00Z"));
        schedule.setCheckInClosesAt(Instant.parse("2026-07-03T21:00:00Z"));
        schedule.setStatus(WorkScheduleStatus.CHECK_IN_OPEN);
        return schedule;
    }
}
