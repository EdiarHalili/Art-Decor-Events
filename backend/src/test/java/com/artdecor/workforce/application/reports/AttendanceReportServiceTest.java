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
        AttendanceReportRow presentRow = response.rows().stream()
                .filter(row -> row.employeeCode().equals("EMP001"))
                .findFirst()
                .orElseThrow();
        assertThat(presentRow.checkInLatitude()).isEqualTo(42.30413);
        assertThat(presentRow.checkInLongitude()).isEqualTo(21.64894);

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
    void reportKeepsMultipleAttendanceSessionsForSameEmployeeAndSchedule() {
        LocalDate date = LocalDate.of(2026, 7, 3);
        EmployeeEntity employee = employee("EMP001", "Present Worker");
        WorkScheduleEntity schedule = schedule(date);
        schedule.setSimpleOpenMode(true);
        ScheduleAssignmentEntity assignment = assignment(schedule, employee);
        AttendanceRecordEntity first = record(schedule, employee);
        first.setCheckedInAt(Instant.parse("2026-07-03T06:00:00Z"));
        first.setCheckedOutAt(Instant.parse("2026-07-03T10:00:00Z"));
        first.setWorkedMinutes(240);
        AttendanceRecordEntity second = record(schedule, employee);
        second.setCheckedInAt(Instant.parse("2026-07-03T15:00:00Z"));
        second.setCheckedOutAt(Instant.parse("2026-07-03T21:00:00Z"));
        second.setWorkedMinutes(360);

        when(assignments.findReportAssignments(date, date, WorkScheduleStatus.CANCELLED))
                .thenReturn(List.of(assignment));
        when(attendanceRecords.findReportRecords(date, date)).thenReturn(List.of(first, second));

        AttendanceReportResponse response = service.report(date, date, "daily");

        assertThat(response.rows()).hasSize(2);
        assertThat(response.rows()).extracting(AttendanceReportRow::checkedInAt)
                .containsExactly(Instant.parse("2026-07-03T06:00:00Z"), Instant.parse("2026-07-03T15:00:00Z"));
        assertThat(response.summary().present()).isEqualTo(2);
        assertThat(response.summary().workedMinutes()).isEqualTo(600);
    }

    @Test
    void exportsEmployeeAttendance() {
        LocalDate date = LocalDate.of(2026, 7, 3);
        EmployeeEntity employee = employee("EMP001", "Present Worker");
        WorkScheduleEntity schedule = schedule(date);
        AttendanceRecordEntity record = record(schedule, employee);
        record.setCheckedOutAt(Instant.parse("2026-07-03T14:00:00Z"));
        record.setCheckInLatitude(42.30413);
        record.setCheckInLongitude(21.64894);
        record.setAutoCheckout(true);

        when(employees.findById(employee.getId())).thenReturn(Optional.of(employee));
        when(assignments.findReportAssignments(date, date, WorkScheduleStatus.CANCELLED)).thenReturn(List.of());
        when(attendanceRecords.findReportRecords(date, date)).thenReturn(List.of(record));

        ExportFile csv = service.exportEmployee(employee.getId(), date, date, "csv");
        assertThat(csv.filename()).isEqualTo("Historia_Punes_EMP001_2026-07.csv");
        assertThat(new String(csv.content())).contains("Totali i oreve").contains("Present Worker");

        ExportFile pdf = service.exportEmployee(employee.getId(), date, date, "pdf");
        assertThat(pdf.filename()).isEqualTo("Historia_Punes_EMP001_2026-07.pdf");
        assertThat(pdf.content()).startsWith("%PDF".getBytes());
        assertThat(new String(pdf.content(), StandardCharsets.ISO_8859_1))
                .contains("Përmbledhje mujore e punës")
                .contains("Punëtori: Present Worker")
                .contains("Data")
                .contains("Hyrja")
                .contains("Orët e punës")
                .contains("03.07.2026")
                .contains("06:55")
                .contains("Dalje automatike")
                .contains("GPS")
                .contains("Vendndodhja e hyrjes: Shiko në hartë")
                .contains("/URI (https://maps.google.com/?q=42.30413,21.64894)")
                .doesNotContain("42.30413, 21.64894")
                .contains("Totali i ditëve të punuara")
                .contains("(1)")
                .contains("Totali i orëve normale")
                .contains("(8h)")
                .contains("Totali i orëve shtesë")
                .contains("(1h 5min)")
                .contains("Totali i përgjithshëm i orëve")
                .contains("(9h 5min)")
                .contains("Raporti u gjenerua automatikisht nga Art Decor Events Workforce");
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
        ReflectionTestUtils.setField(record, "id", UUID.randomUUID());
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
        record.setCheckedInAt(Instant.parse("2026-07-03T04:55:00Z"));
        record.setCheckedOutAt(Instant.parse("2026-07-03T12:55:00Z"));
        record.setWorkedMinutes(480);
        record.setCheckInLatitude(42.30413);
        record.setCheckInLongitude(21.64894);
        return record;
    }
}
