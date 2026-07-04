package com.artdecor.workforce.application.reports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.AppSettingsRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AttendanceReportServiceTest {
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final AppSettingsRepository settings = org.mockito.Mockito.mock(AppSettingsRepository.class);
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final AttendanceReportService service = new AttendanceReportService(assignments, attendanceRecords, settings, employees);

    @Test
    void buildsSummaryRowsAndExports() {
        LocalDate date = LocalDate.of(2026, 7, 3);
        WorkScheduleEntity schedule = schedule(date);
        EmployeeEntity presentEmployee = employee("EMP001", "Present Worker");
        EmployeeEntity absentEmployee = employee("EMP002", "Absent Worker");

        ScheduleAssignmentEntity presentAssignment = assignment(schedule, presentEmployee);
        ScheduleAssignmentEntity absentAssignment = assignment(schedule, absentEmployee);
        AttendanceRecordEntity record = record(schedule, presentEmployee);

        when(assignments.findReportAssignments(date, date, WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of(absentAssignment, presentAssignment));
        when(attendanceRecords.findReportRecords(date, date)).thenReturn(List.of(record));

        AttendanceReportResponse response = service.report(date, date, "daily");

        assertThat(response.summary().assigned()).isEqualTo(2);
        assertThat(response.summary().present()).isEqualTo(1);
        assertThat(response.summary().absent()).isEqualTo(1);
        assertThat(response.summary().workedMinutes()).isEqualTo(480);
        assertThat(response.rows()).extracting(AttendanceReportRow::status)
                .containsExactly(AttendanceStatus.ABSENT.name(), AttendanceStatus.CHECKED_OUT.name());

        ExportFile csv = service.export(date, date, "csv");
        assertThat(csv.filename()).endsWith(".csv");
        assertThat(new String(csv.content())).contains("Present Worker");

        ExportFile pdf = service.export(date, date, "pdf");
        assertThat(pdf.content()).startsWith("%PDF".getBytes());
    }

    @Test
    void employeeHistoryPrefersActualAttendanceOverSameDayAbsentAssignment() {
        LocalDate date = LocalDate.of(2026, 7, 3);
        EmployeeEntity employee = employee("EMP001", "Present Worker");
        WorkScheduleEntity checkedInSchedule = schedule(date);
        WorkScheduleEntity replacementSchedule = schedule(date);

        ScheduleAssignmentEntity replacementAssignment = assignment(replacementSchedule, employee);
        AttendanceRecordEntity record = record(checkedInSchedule, employee);

        when(assignments.findReportAssignments(date, date, WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of(replacementAssignment));
        when(attendanceRecords.findReportRecords(date, date)).thenReturn(List.of(record));

        List<AttendanceReportRow> history = service.employeeHistory(employee.getId(), date, date);

        assertThat(history).hasSize(1);
        assertThat(history.getFirst().status()).isEqualTo(AttendanceStatus.CHECKED_OUT.name());
        assertThat(history.getFirst().checkedInAt()).isEqualTo(Instant.parse("2026-07-03T04:55:00Z"));
    }

    @Test
    void exportsEmployeeAttendance() {
        LocalDate date = LocalDate.of(2026, 7, 3);
        EmployeeEntity employee = employee("EMP001", "Present Worker");
        WorkScheduleEntity schedule = schedule(date);
        AttendanceRecordEntity record = record(schedule, employee);
        record.setWorkedMinutes(545);
        record.setCheckInLatitude(42.30413);
        record.setCheckInLongitude(21.64894);
        record.setAutoCheckout(true);

        when(employees.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(assignments.findReportAssignments(date, date, WorkScheduleStatus.CANCELLED)).thenReturn(List.of());
        when(attendanceRecords.findReportRecords(date, date)).thenReturn(List.of(record));

        ExportFile csv = service.exportEmployee(employee.getId(), date, date, "csv");
        assertThat(csv.filename()).contains("EMP001").endsWith(".csv");
        assertThat(new String(csv.content())).contains("Total worked hours").contains("Present Worker");

        ExportFile pdf = service.exportEmployee(employee.getId(), date, date, "pdf");
        assertThat(pdf.content()).startsWith("%PDF".getBytes());
        assertThat(new String(pdf.content(), StandardCharsets.ISO_8859_1))
                .contains("Permbledhje mujore e punes")
                .contains("Punetori : Present Worker")
                .contains("Data       Check In")
                .contains("03.07.2026 06:55")
                .contains("Auto Check Out")
                .contains("GPS")
                .contains("View on Map")
                .contains("Totali i diteve te punuara : 1")
                .contains("Totali i oreve normale    : 8h")
                .contains("Totali i oreve shtese     : 1h 05min")
                .contains("Totali i oreve            : 9h 05min");
    }

    private WorkScheduleEntity schedule(LocalDate date) {
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
        schedule.setTitle("Daily check-in window");
        schedule.setWorkDate(date);
        schedule.setCheckInOpensAt(Instant.parse("2026-07-03T04:50:00Z"));
        schedule.setCheckInClosesAt(Instant.parse("2026-07-03T05:10:00Z"));
        schedule.setStatus(WorkScheduleStatus.CHECK_IN_OPEN);
        return schedule;
    }

    private EmployeeEntity employee(String code, String name) {
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
        employee.setEmployeeCode(code);
        employee.setFullName(name);
        employee.setPinHash("hash");
        return employee;
    }

    private ScheduleAssignmentEntity assignment(WorkScheduleEntity schedule, EmployeeEntity employee) {
        ScheduleAssignmentEntity assignment = new ScheduleAssignmentEntity();
        assignment.setSchedule(schedule);
        assignment.setEmployee(employee);
        return assignment;
    }

    private AttendanceRecordEntity record(WorkScheduleEntity schedule, EmployeeEntity employee) {
        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
        record.setCheckedInAt(Instant.parse("2026-07-03T04:55:00Z"));
        record.setCheckedOutAt(Instant.parse("2026-07-03T12:55:00Z"));
        record.setWorkedMinutes(480);
        return record;
    }
}
