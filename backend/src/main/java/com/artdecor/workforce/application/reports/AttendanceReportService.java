package com.artdecor.workforce.application.reports;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ScheduleAssignmentRepository assignments;
    private final AttendanceRecordRepository attendanceRecords;

    public AttendanceReportService(
            ScheduleAssignmentRepository assignments,
            AttendanceRecordRepository attendanceRecords
    ) {
        this.assignments = assignments;
        this.attendanceRecords = attendanceRecords;
    }

    @Transactional(readOnly = true)
    public AttendanceReportResponse report(LocalDate from, LocalDate to, String period) {
        validateRange(from, to);
        String normalizedPeriod = normalizePeriod(period);
        List<AttendanceReportRow> rows = rows(from, to);

        return new AttendanceReportResponse(
                from,
                to,
                normalizedPeriod,
                summarize(rows),
                bucketRows(rows, normalizedPeriod),
                summarizeEmployees(rows),
                rows
        );
    }

    @Transactional(readOnly = true)
    public List<AttendanceReportRow> employeeHistory(UUID employeeId, LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<AttendanceReportRow> assignmentRows = rows(from, to).stream()
                .filter(row -> row.employeeId().equals(employeeId.toString()))
                .toList();
        List<AttendanceReportRow> actualAttendanceRows = attendanceRecords.findReportRecords(from, to).stream()
                .filter(record -> record.getEmployee().getId().equals(employeeId))
                .map(this::attendanceRow)
                .toList();

        return java.util.stream.Stream.concat(assignmentRows.stream(), actualAttendanceRows.stream())
                .collect(Collectors.toMap(
                        AttendanceReportRow::workDate,
                        row -> row,
                        this::preferredHistoryRow,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .toList();
    }

    @Transactional(readOnly = true)
    public ExportFile export(LocalDate from, LocalDate to, String format) {
        validateRange(from, to);
        List<AttendanceReportRow> rows = rows(from, to);
        return switch (format == null ? "csv" : format.toLowerCase(Locale.ROOT)) {
            case "xlsx", "xls", "excel" -> new ExportFile(
                    "art-decor-attendance-" + from + "-to-" + to + ".xls",
                    "application/vnd.ms-excel",
                    excelXml(rows).getBytes(StandardCharsets.UTF_8)
            );
            case "pdf" -> new ExportFile(
                    "art-decor-attendance-" + from + "-to-" + to + ".pdf",
                    "application/pdf",
                    SimplePdf.render("Art Decor Attendance Report", reportLines(rows))
            );
            default -> new ExportFile(
                    "art-decor-attendance-" + from + "-to-" + to + ".csv",
                    "text/csv; charset=UTF-8",
                    csv(rows).getBytes(StandardCharsets.UTF_8)
            );
        };
    }

    private List<AttendanceReportRow> rows(LocalDate from, LocalDate to) {
        Map<String, AttendanceRecordEntity> records = new LinkedHashMap<>();
        for (AttendanceRecordEntity record : attendanceRecords.findReportRecords(from, to)) {
            records.put(key(record.getSchedule().getId(), record.getEmployee().getId()), record);
        }

        List<AttendanceReportRow> rows = new ArrayList<>();
        for (ScheduleAssignmentEntity assignment : assignments.findReportAssignments(from, to, WorkScheduleStatus.CANCELLED)) {
            AttendanceRecordEntity record = records.get(key(assignment.getSchedule().getId(), assignment.getEmployee().getId()));
            rows.add(record == null ? absentRow(assignment) : attendanceRow(record));
        }

        rows.sort(Comparator
                .comparing(AttendanceReportRow::workDate)
                .thenComparing(AttendanceReportRow::employeeName));
        return rows;
    }

    private AttendanceReportRow absentRow(ScheduleAssignmentEntity assignment) {
        return new AttendanceReportRow(
                assignment.getSchedule().getWorkDate(),
                assignment.getSchedule().getId().toString(),
                assignment.getEmployee().getId().toString(),
                assignment.getEmployee().getEmployeeCode(),
                assignment.getEmployee().getFullName(),
                AttendanceStatus.ABSENT.name(),
                null,
                null,
                0,
                0,
                false,
                true
        );
    }

    private AttendanceReportRow attendanceRow(AttendanceRecordEntity record) {
        return new AttendanceReportRow(
                record.getSchedule().getWorkDate(),
                record.getSchedule().getId().toString(),
                record.getEmployee().getId().toString(),
                record.getEmployee().getEmployeeCode(),
                record.getEmployee().getFullName(),
                record.getStatus().name(),
                record.getCheckedInAt(),
                record.getCheckedOutAt(),
                record.getWorkedMinutes(),
                record.getOvertimeMinutes(),
                record.getStatus() == AttendanceStatus.LATE,
                false
        );
    }

    private AttendanceReportRow preferredHistoryRow(AttendanceReportRow first, AttendanceReportRow second) {
        if (first.absent() != second.absent()) {
            return first.absent() ? second : first;
        }
        if (first.checkedInAt() == null) {
            return second;
        }
        if (second.checkedInAt() == null) {
            return first;
        }
        return first.checkedInAt().isAfter(second.checkedInAt()) ? first : second;
    }

    private AttendanceReportSummary summarize(List<AttendanceReportRow> rows) {
        return new AttendanceReportSummary(
                rows.size(),
                (int) rows.stream().filter(row -> !row.absent()).count(),
                (int) rows.stream().filter(AttendanceReportRow::late).count(),
                (int) rows.stream().filter(AttendanceReportRow::absent).count(),
                (int) rows.stream().filter(row -> row.status().equals(AttendanceStatus.CHECKED_OUT.name())).count(),
                rows.stream().mapToInt(AttendanceReportRow::workedMinutes).sum(),
                rows.stream().mapToInt(AttendanceReportRow::overtimeMinutes).sum()
        );
    }

    private List<AttendanceReportBucket> bucketRows(List<AttendanceReportRow> rows, String period) {
        Map<String, List<AttendanceReportRow>> grouped = new LinkedHashMap<>();
        for (AttendanceReportRow row : rows) {
            grouped.computeIfAbsent(bucketLabel(row.workDate(), period), ignored -> new ArrayList<>()).add(row);
        }

        return grouped.entrySet().stream()
                .map(entry -> {
                    AttendanceReportSummary summary = summarize(entry.getValue());
                    return new AttendanceReportBucket(
                            entry.getKey(),
                            summary.assigned(),
                            summary.present(),
                            summary.late(),
                            summary.absent(),
                            summary.workedMinutes(),
                            summary.overtimeMinutes()
                    );
                })
                .toList();
    }

    private List<EmployeeAttendanceSummary> summarizeEmployees(List<AttendanceReportRow> rows) {
        Map<String, List<AttendanceReportRow>> grouped = new LinkedHashMap<>();
        for (AttendanceReportRow row : rows) {
            grouped.computeIfAbsent(row.employeeId(), ignored -> new ArrayList<>()).add(row);
        }

        return grouped.values().stream()
                .map(employeeRows -> {
                    AttendanceReportRow first = employeeRows.getFirst();
                    AttendanceReportSummary summary = summarize(employeeRows);
                    return new EmployeeAttendanceSummary(
                            first.employeeId(),
                            first.employeeCode(),
                            first.employeeName(),
                            summary.assigned(),
                            summary.present(),
                            summary.late(),
                            summary.absent(),
                            summary.workedMinutes(),
                            summary.overtimeMinutes()
                    );
                })
                .sorted(Comparator.comparing(EmployeeAttendanceSummary::employeeName))
                .toList();
    }

    private String bucketLabel(LocalDate date, String period) {
        if ("monthly".equals(period)) {
            return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        }
        if ("weekly".equals(period)) {
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            int year = date.get(WeekFields.ISO.weekBasedYear());
            return year + "-W" + String.format("%02d", week);
        }
        return DATE.format(date);
    }

    private String csv(List<AttendanceReportRow> rows) {
        StringBuilder builder = new StringBuilder("Date,Employee ID,Employee,Status,Check In,Check Out,Worked Hours,Overtime Hours\n");
        for (AttendanceReportRow row : rows) {
            builder.append(csvValue(row.workDate().toString())).append(',')
                    .append(csvValue(row.employeeCode())).append(',')
                    .append(csvValue(row.employeeName())).append(',')
                    .append(csvValue(row.status())).append(',')
                    .append(csvValue(row.checkedInAt() == null ? "" : row.checkedInAt().toString())).append(',')
                    .append(csvValue(row.checkedOutAt() == null ? "" : row.checkedOutAt().toString())).append(',')
                    .append(minutesToHours(row.workedMinutes())).append(',')
                    .append(minutesToHours(row.overtimeMinutes())).append('\n');
        }
        return builder.toString();
    }

    private String excelXml(List<AttendanceReportRow> rows) {
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0"?>
                <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
                <Worksheet ss:Name="Attendance"><Table>
                """);
        builder.append(excelRow(List.of("Date", "Employee ID", "Employee", "Status", "Check In", "Check Out", "Worked Hours", "Overtime Hours")));
        for (AttendanceReportRow row : rows) {
            builder.append(excelRow(List.of(
                    row.workDate().toString(),
                    row.employeeCode(),
                    row.employeeName(),
                    row.status(),
                    row.checkedInAt() == null ? "" : row.checkedInAt().toString(),
                    row.checkedOutAt() == null ? "" : row.checkedOutAt().toString(),
                    String.format(Locale.ROOT, "%.2f", minutesToHours(row.workedMinutes())),
                    String.format(Locale.ROOT, "%.2f", minutesToHours(row.overtimeMinutes()))
            )));
        }
        builder.append("</Table></Worksheet></Workbook>");
        return builder.toString();
    }

    private String excelRow(List<String> cells) {
        StringBuilder builder = new StringBuilder("<Row>");
        for (String cell : cells) {
            builder.append("<Cell><Data ss:Type=\"String\">")
                    .append(xml(cell))
                    .append("</Data></Cell>");
        }
        return builder.append("</Row>").toString();
    }

    private List<String> reportLines(List<AttendanceReportRow> rows) {
        List<String> lines = new ArrayList<>();
        AttendanceReportSummary summary = summarize(rows);
        lines.add("Assigned: " + summary.assigned() + "  Present: " + summary.present() + "  Late: " + summary.late() + "  Absent: " + summary.absent());
        lines.add("Worked hours: " + String.format(Locale.ROOT, "%.2f", minutesToHours(summary.workedMinutes())));
        lines.add("");
        lines.add("Date | Employee | Status | Worked");
        for (AttendanceReportRow row : rows) {
            lines.add(row.workDate() + " | " + row.employeeName() + " | " + row.status() + " | "
                    + String.format(Locale.ROOT, "%.2f", minutesToHours(row.workedMinutes())));
        }
        return lines;
    }

    private double minutesToHours(int minutes) {
        return minutes / 60.0;
    }

    private String csvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private String xml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String key(UUID scheduleId, UUID employeeId) {
        return scheduleId + ":" + employeeId;
    }

    private String normalizePeriod(String period) {
        if ("weekly".equalsIgnoreCase(period) || "monthly".equalsIgnoreCase(period)) {
            return period.toLowerCase(Locale.ROOT);
        }
        return "daily";
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Invalid report date range.");
        }
        if (from.plusYears(2).isBefore(to)) {
            throw new IllegalArgumentException("Report range cannot exceed two years.");
        }
    }
}
