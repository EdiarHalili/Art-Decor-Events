package com.artdecor.workforce.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
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
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T21:05:00Z"), ZoneOffset.UTC);
    private final AutoCheckoutService service = new AutoCheckoutService(attendanceRecords, clock);

    @Test
    void automaticallyChecksOutOpenRecordAtWindowCloseTime() {
        AttendanceRecordEntity record = checkedInRecord();
        when(attendanceRecords.findRecordsDueForAutoCheckout(Instant.parse("2026-07-03T21:05:00Z"), WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of(record));

        int updated = service.autoCheckoutDueRecords(Instant.parse("2026-07-03T21:05:00Z"));

        assertThat(updated).isEqualTo(1);
        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.CHECKED_OUT);
        assertThat(record.getCheckedOutAt()).isEqualTo(Instant.parse("2026-07-03T21:00:00Z"));
        assertThat(record.getWorkedMinutes()).isEqualTo(420);
        assertThat(record.isAutoCheckout()).isTrue();
    }

    @Test
    void doesNotDuplicateAutoCheckoutWhenRecordIsAlreadyCheckedOut() {
        AttendanceRecordEntity record = checkedInRecord();
        when(attendanceRecords.findRecordsDueForAutoCheckout(Instant.parse("2026-07-03T21:05:00Z"), WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of(record));

        assertThat(service.autoCheckoutDueRecords(Instant.parse("2026-07-03T21:05:00Z"))).isEqualTo(1);
        assertThat(service.autoCheckoutDueRecords(Instant.parse("2026-07-03T21:05:00Z"))).isZero();
        assertThat(record.getCheckedOutAt()).isEqualTo(Instant.parse("2026-07-03T21:00:00Z"));
        assertThat(record.isAutoCheckout()).isTrue();
    }

    private AttendanceRecordEntity checkedInRecord() {
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
        schedule.setTitle("Daily check-in window");
        schedule.setWorkDate(LocalDate.of(2026, 7, 3));
        schedule.setCheckInOpensAt(Instant.parse("2026-07-03T14:00:00Z"));
        schedule.setCheckInClosesAt(Instant.parse("2026-07-03T21:00:00Z"));
        schedule.setStatus(WorkScheduleStatus.CHECK_IN_OPEN);

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
}
