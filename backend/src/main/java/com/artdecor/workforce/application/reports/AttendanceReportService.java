package com.artdecor.workforce.application.reports;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AppSettingsRepository;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private static final DateTimeFormatter PDF_DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter PDF_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ScheduleAssignmentRepository assignments;
    private final AttendanceRecordRepository attendanceRecords;
    private final AppSettingsRepository settings;
    private final EmployeeRepository employees;

    public AttendanceReportService(
            ScheduleAssignmentRepository assignments,
            AttendanceRecordRepository attendanceRecords,
            AppSettingsRepository settings,
            EmployeeRepository employees
    ) {
        this.assignments = assignments;
        this.attendanceRecords = attendanceRecords;
        this.settings = settings;
        this.employees = employees;
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
        WorkplaceCoordinates workplace = workplaceCoordinates();
        List<AttendanceReportRow> assignmentRows = rows(from, to).stream()
                .filter(row -> row.employeeId().equals(employeeId.toString()))
                .toList();
        List<AttendanceReportRow> actualAttendanceRows = attendanceRecords.findReportRecords(from, to).stream()
                .filter(record -> record.getEmployee().getId().equals(employeeId))
                .map(record -> attendanceRow(record, workplace))
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

    @Transactional(readOnly = true)
    public ExportFile exportEmployee(UUID employeeId, LocalDate from, LocalDate to, String format) {
        validateRange(from, to);
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee was not found."));
        List<AttendanceReportRow> rows = employeeHistory(employeeId, from, to);
        String normalized = format == null ? "csv" : format.toLowerCase(Locale.ROOT);
        String baseName = "art-decor-" + employee.getEmployeeCode() + "-" + from + "-to-" + to;

        return switch (normalized) {
            case "pdf" -> new ExportFile(
                    baseName + ".pdf",
                    "application/pdf",
                    SimplePdf.render("Permbledhje mujore e punes", employeeReportLines(employee, from, to, rows))
            );
            case "xlsx", "xls", "excel" -> new ExportFile(
                    baseName + ".xls",
                    "application/vnd.ms-excel",
                    employeeExcelXml(employee, from, to, rows).getBytes(StandardCharsets.UTF_8)
            );
            default -> new ExportFile(
                    baseName + ".csv",
                    "text/csv; charset=UTF-8",
                    employeeCsv(employee, from, to, rows).getBytes(StandardCharsets.UTF_8)
            );
        };
    }

    private List<AttendanceReportRow> rows(LocalDate from, LocalDate to) {
        WorkplaceCoordinates workplace = workplaceCoordinates();
        Map<String, AttendanceRecordEntity> records = new LinkedHashMap<>();
        for (AttendanceRecordEntity record : attendanceRecords.findReportRecords(from, to)) {
            records.put(key(record.getSchedule().getId(), record.getEmployee().getId()), record);
        }

        List<AttendanceReportRow> rows = new ArrayList<>();
        for (ScheduleAssignmentEntity assignment : assignments.findReportAssignments(from, to, WorkScheduleStatus.CANCELLED)) {
            AttendanceRecordEntity record = records.get(key(assignment.getSchedule().getId(), assignment.getEmployee().getId()));
            rows.add(record == null ? absentRow(assignment) : attendanceRow(record, workplace));
        }

        rows.sort(Comparator
                .comparing(AttendanceReportRow::workDate)
                .thenComparing(AttendanceReportRow::employeeName));
        return rows;
    }

    private AttendanceReportRow absentRow(ScheduleAssignmentEntity assignment) {
        return new AttendanceReportRow(
                assignment.getSchedule().getWorkDate(),
                null,
                assignment.getSchedule().getId().toString(),
                assignment.getEmployee().getId().toString(),
                assignment.getEmployee().getEmployeeCode(),
                assignment.getEmployee().getFullName(),
                AttendanceStatus.ABSENT.name(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                false,
                null,
                false,
                true
        );
    }

    private AttendanceReportRow attendanceRow(AttendanceRecordEntity record, WorkplaceCoordinates workplace) {
        return new AttendanceReportRow(
                record.getSchedule().getWorkDate(),
                record.getId() == null ? null : record.getId().toString(),
                record.getSchedule().getId().toString(),
                record.getEmployee().getId().toString(),
                record.getEmployee().getEmployeeCode(),
                record.getEmployee().getFullName(),
                record.getStatus().name(),
                record.getCheckedInAt(),
                record.getCheckedOutAt(),
                record.getCheckInLatitude(),
                record.getCheckInLongitude(),
                record.getCheckOutLatitude(),
                record.getCheckOutLongitude(),
                distanceMeters(record.getCheckInLatitude(), record.getCheckInLongitude(), workplace),
                distanceMeters(record.getCheckOutLatitude(), record.getCheckOutLongitude(), workplace),
                record.getWorkedMinutes(),
                record.getOvertimeMinutes(),
                record.isAutoCheckout(),
                record.getCheckoutType().name(),
                record.getStatus() == AttendanceStatus.LATE,
                false
        );
    }

    private WorkplaceCoordinates workplaceCoordinates() {
        return settings.findAll().stream()
                .findFirst()
                .filter(setting -> setting.getWorkplaceLatitude() != null && setting.getWorkplaceLongitude() != null)
                .map(setting -> new WorkplaceCoordinates(setting.getWorkplaceLatitude(), setting.getWorkplaceLongitude()))
                .orElse(null);
    }

    private Integer distanceMeters(Double latitude, Double longitude, WorkplaceCoordinates workplace) {
        if (latitude == null || longitude == null) {
            return null;
        }
        if (workplace == null) {
            return null;
        }
        return (int) Math.round(distanceMeters(
                workplace.latitude(),
                workplace.longitude(),
                latitude,
                longitude
        ));
    }

    private double distanceMeters(double fromLatitude, double fromLongitude, double toLatitude, double toLongitude) {
        double earthRadiusMeters = 6_371_000;
        double fromLat = Math.toRadians(fromLatitude);
        double toLat = Math.toRadians(toLatitude);
        double deltaLat = Math.toRadians(toLatitude - fromLatitude);
        double deltaLon = Math.toRadians(toLongitude - fromLongitude);
        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(fromLat) * Math.cos(toLat)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusMeters * c;
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
        StringBuilder builder = new StringBuilder("Date,Employee ID,Employee,Status,Check In,Check In GPS,Check In Distance M,Check Out,Check Out GPS,Check Out Distance M,Checkout Type,Worked Hours,Overtime Hours\n");
        for (AttendanceReportRow row : rows) {
            builder.append(csvValue(row.workDate().toString())).append(',')
                    .append(csvValue(row.employeeCode())).append(',')
                    .append(csvValue(row.employeeName())).append(',')
                    .append(csvValue(row.status())).append(',')
                    .append(csvValue(row.checkedInAt() == null ? "" : row.checkedInAt().toString())).append(',')
                    .append(csvValue(gps(row.checkInLatitude(), row.checkInLongitude()))).append(',')
                    .append(csvValue(row.checkInDistanceMeters() == null ? "" : row.checkInDistanceMeters().toString())).append(',')
                    .append(csvValue(row.checkedOutAt() == null ? "" : row.checkedOutAt().toString())).append(',')
                    .append(csvValue(gps(row.checkOutLatitude(), row.checkOutLongitude()))).append(',')
                    .append(csvValue(row.checkOutDistanceMeters() == null ? "" : row.checkOutDistanceMeters().toString())).append(',')
                    .append(csvValue(checkoutTypeLabel(row))).append(',')
                    .append(minutesToHours(row.workedMinutes())).append(',')
                    .append(minutesToHours(row.overtimeMinutes())).append('\n');
        }
        return builder.toString();
    }

    private String employeeCsv(EmployeeEntity employee, LocalDate from, LocalDate to, List<AttendanceReportRow> rows) {
        AttendanceReportSummary summary = summarize(rows);
        StringBuilder builder = new StringBuilder();
        builder.append(csvValue("Employee name")).append(',').append(csvValue(employee.getFullName())).append('\n');
        builder.append(csvValue("Employee code")).append(',').append(csvValue(employee.getEmployeeCode())).append('\n');
        builder.append(csvValue("Date range")).append(',').append(csvValue(from + " to " + to)).append('\n');
        builder.append(csvValue("Total worked days")).append(',').append(workedDays(rows)).append('\n');
        builder.append(csvValue("Total worked hours")).append(',').append(minutesToHours(summary.workedMinutes())).append('\n');
        builder.append(csvValue("Total overtime")).append(',').append(minutesToHours(summary.overtimeMinutes())).append('\n');
        builder.append(csvValue("Late days")).append(',').append(summary.late()).append('\n');
        builder.append(csvValue("Absent days")).append(',').append(summary.absent()).append("\n\n");
        builder.append("Date,Check In,Check Out,Worked Hours,Status\n");
        for (AttendanceReportRow row : rows) {
            builder.append(csvValue(row.workDate().toString())).append(',')
                    .append(csvValue(time(row.checkedInAt()))).append(',')
                    .append(csvValue(time(row.checkedOutAt()))).append(',')
                    .append(String.format(Locale.ROOT, "%.2f", minutesToHours(row.workedMinutes()))).append(',')
                    .append(csvValue(statusLabel(row)))
                    .append('\n');
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
        builder.append(excelRow(List.of("Date", "Employee ID", "Employee", "Status", "Check In", "Check In GPS", "Check In Distance M", "Check Out", "Check Out GPS", "Check Out Distance M", "Checkout Type", "Worked Hours", "Overtime Hours")));
        for (AttendanceReportRow row : rows) {
            builder.append(excelRow(List.of(
                    row.workDate().toString(),
                    row.employeeCode(),
                    row.employeeName(),
                    row.status(),
                    row.checkedInAt() == null ? "" : row.checkedInAt().toString(),
                    gps(row.checkInLatitude(), row.checkInLongitude()),
                    row.checkInDistanceMeters() == null ? "" : row.checkInDistanceMeters().toString(),
                    row.checkedOutAt() == null ? "" : row.checkedOutAt().toString(),
                    gps(row.checkOutLatitude(), row.checkOutLongitude()),
                    row.checkOutDistanceMeters() == null ? "" : row.checkOutDistanceMeters().toString(),
                    checkoutTypeLabel(row),
                    String.format(Locale.ROOT, "%.2f", minutesToHours(row.workedMinutes())),
                    String.format(Locale.ROOT, "%.2f", minutesToHours(row.overtimeMinutes()))
            )));
        }
        builder.append("</Table></Worksheet></Workbook>");
        return builder.toString();
    }

    private String employeeExcelXml(EmployeeEntity employee, LocalDate from, LocalDate to, List<AttendanceReportRow> rows) {
        AttendanceReportSummary summary = summarize(rows);
        StringBuilder builder = new StringBuilder("""
                <?xml version="1.0"?>
                <Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
                 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">
                <Worksheet ss:Name="Employee Attendance"><Table>
                """);
        builder.append(excelRow(List.of("Employee name", employee.getFullName())));
        builder.append(excelRow(List.of("Employee code", employee.getEmployeeCode())));
        builder.append(excelRow(List.of("Date range", from + " to " + to)));
        builder.append(excelRow(List.of("Total worked days", String.valueOf(workedDays(rows)))));
        builder.append(excelRow(List.of("Total worked hours", String.format(Locale.ROOT, "%.2f", minutesToHours(summary.workedMinutes())))));
        builder.append(excelRow(List.of("Total overtime", String.format(Locale.ROOT, "%.2f", minutesToHours(summary.overtimeMinutes())))));
        builder.append(excelRow(List.of("Late days", String.valueOf(summary.late()))));
        builder.append(excelRow(List.of("Absent days", String.valueOf(summary.absent()))));
        builder.append(excelRow(List.of()));
        builder.append(excelRow(List.of("Date", "Check In", "Check Out", "Worked Hours", "Status")));
        for (AttendanceReportRow row : rows) {
            builder.append(excelRow(List.of(
                    row.workDate().toString(),
                    time(row.checkedInAt()),
                    time(row.checkedOutAt()),
                    String.format(Locale.ROOT, "%.2f", minutesToHours(row.workedMinutes())),
                    statusLabel(row)
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
        lines.add("Date | Employee | Status | Check In GPS | Check Out GPS | Checkout | Worked");
        for (AttendanceReportRow row : rows) {
            lines.add(row.workDate() + " | " + row.employeeName() + " | " + row.status() + " | "
                    + gps(row.checkInLatitude(), row.checkInLongitude()) + " | "
                    + gps(row.checkOutLatitude(), row.checkOutLongitude()) + " | "
                    + checkoutTypeLabel(row) + " | "
                    + String.format(Locale.ROOT, "%.2f", minutesToHours(row.workedMinutes())));
        }
        return lines;
    }

    private List<String> employeeReportLines(EmployeeEntity employee, LocalDate from, LocalDate to, List<AttendanceReportRow> rows) {
        ZoneId zone = businessZone();
        EmployeePdfTotals totals = employeePdfTotals(rows);
        List<String> lines = new ArrayList<>();
        lines.add("Kompania : " + companyName());
        lines.add("Punetori : " + employee.getFullName());
        lines.add("Kodi     : " + employee.getEmployeeCode());
        lines.add("Periudha : " + PDF_DATE.format(from) + " - " + PDF_DATE.format(to));
        lines.add("");
        lines.add(String.format("%-10s %-17s %-22s %-13s %s",
                "Data", "Check In", "Check Out", "Oret e punes", "Statusi"));
        lines.add("---------- ----------------- ---------------------- ------------- ----------------");
        for (AttendanceReportRow row : rows) {
            lines.add(String.format(
                    "%-10s %-17s %-22s %-13s %s",
                    PDF_DATE.format(row.workDate()),
                    dateTime(row.checkedInAt(), zone),
                    checkOutLabel(row, zone),
                    minutesLabel(row.workedMinutes()),
                    statusLabel(row)
            ));
        }

        List<String> gpsLines = rows.stream()
                .filter(row -> hasGps(row.checkInLatitude(), row.checkInLongitude())
                        || hasGps(row.checkOutLatitude(), row.checkOutLongitude()))
                .flatMap(row -> gpsLines(row).stream())
                .toList();

        boolean hasSystemCheckout = rows.stream().anyMatch(row ->
                row.autoCheckout() || (row.checkoutType() != null && !row.checkoutType().equals("MANUAL_EMPLOYEE")));
        if (!gpsLines.isEmpty()) {
            lines.add("");
            if (hasSystemCheckout) {
                lines.add("Shenim: Auto Check Out = dalje automatike nga sistemi. Admin Check Out = dalje e regjistruar nga administratori.");
                lines.add("");
            }
            lines.add("GPS");
            lines.addAll(gpsLines);
        } else if (hasSystemCheckout) {
            lines.add("");
            lines.add("Shenim: Auto Check Out = dalje automatike nga sistemi. Admin Check Out = dalje e regjistruar nga administratori.");
        }
        lines.add("");
        lines.add("Totali i diteve te punuara : " + totals.workedDays());
        lines.add("Totali i oreve normale    : " + minutesLabel(totals.normalMinutes()));
        lines.add("Totali i oreve shtese     : " + minutesLabel(totals.overtimeMinutes()));
        lines.add("Totali i oreve            : " + minutesLabel(totals.totalMinutes()));
        return lines;
    }

    private double minutesToHours(int minutes) {
        return minutes / 60.0;
    }

    private int workedDays(List<AttendanceReportRow> rows) {
        return (int) rows.stream()
                .filter(row -> !row.absent())
                .filter(row -> row.workedMinutes() > 0 || row.checkedInAt() != null)
                .count();
    }

    private String time(Instant value) {
        return value == null ? "" : value.toString();
    }

    private String time(Instant value, ZoneId zone) {
        return value == null ? "-" : PDF_TIME.withZone(zone).format(value);
    }

    private String date(Instant value, ZoneId zone) {
        return value == null ? "-" : PDF_DATE.withZone(zone).format(value);
    }

    private String dateTime(Instant value, ZoneId zone) {
        return value == null ? "-" : date(value, zone) + " " + time(value, zone);
    }

    private String checkOutLabel(AttendanceReportRow row, ZoneId zone) {
        if (row.checkedOutAt() == null) {
            return "-";
        }
        if ("AUTO_CHECKED_OUT".equals(row.checkoutType()) || row.autoCheckout()) {
            return "Auto Check Out " + time(row.checkedOutAt(), zone);
        }
        if ("ADMIN_CHECKED_OUT".equals(row.checkoutType())) {
            return "Admin Check Out " + time(row.checkedOutAt(), zone);
        }
        return dateTime(row.checkedOutAt(), zone);
    }

    private String minutesLabel(int minutes) {
        int safeMinutes = Math.max(minutes, 0);
        int hours = safeMinutes / 60;
        int remainingMinutes = safeMinutes % 60;
        if (remainingMinutes == 0) {
            return hours + "h";
        }
        return hours + "h " + String.format(Locale.ROOT, "%02d", remainingMinutes) + "min";
    }

    private String statusLabel(AttendanceReportRow row) {
        if ("AUTO_CHECKED_OUT".equals(row.checkoutType()) || row.autoCheckout()) {
            return "Auto dalje";
        }
        if ("ADMIN_CHECKED_OUT".equals(row.checkoutType())) {
            return "Dalje nga admin";
        }
        if (row.absent()) {
            return "Mungese";
        }
        if (row.late()) {
            return "Me vonese";
        }
        if (AttendanceStatus.CHECKED_OUT.name().equals(row.status())) {
            return "Perfunduar";
        }
        if (AttendanceStatus.PRESENT.name().equals(row.status())) {
            return "Prezent";
        }
        if (AttendanceStatus.PENDING_APPROVAL.name().equals(row.status())) {
            return "Ne pritje";
        }
        return row.status();
    }

    private String checkoutTypeLabel(AttendanceReportRow row) {
        if ("AUTO_CHECKED_OUT".equals(row.checkoutType()) || row.autoCheckout()) {
            return "Auto checkout";
        }
        if ("ADMIN_CHECKED_OUT".equals(row.checkoutType())) {
            return "Admin checkout";
        }
        if ("MANUAL_EMPLOYEE".equals(row.checkoutType())) {
            return "Manual employee checkout";
        }
        return "";
    }

    private ZoneId businessZone() {
        return settings.findAll().stream()
                .findFirst()
                .map(setting -> setting.getTimezone() == null ? "Europe/Berlin" : setting.getTimezone())
                .map(this::zone)
                .orElse(ZoneId.of("Europe/Berlin"));
    }

    private ZoneId zone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (RuntimeException ignored) {
            return ZoneId.of("Europe/Berlin");
        }
    }

    private String companyName() {
        return settings.findAll().stream()
                .findFirst()
                .map(setting -> setting.getCompanyName() == null ? "Art Decor Events" : setting.getCompanyName())
                .orElse("Art Decor Events");
    }

    private String gps(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return "";
        }
        return latitude + "," + longitude;
    }

    private boolean hasGps(Double latitude, Double longitude) {
        return latitude != null && longitude != null;
    }

    private String gpsWithDistance(Double latitude, Double longitude, Integer distanceMeters) {
        if (!hasGps(latitude, longitude)) {
            return "-";
        }
        String value = String.format(Locale.ROOT, "%.5f, %.5f", latitude, longitude);
        if (distanceMeters != null) {
            value += " (" + distanceMeters + " m)";
        }
        return value;
    }

    private List<String> gpsLines(AttendanceReportRow row) {
        List<String> lines = new ArrayList<>();
        lines.add(PDF_DATE.format(row.workDate()));
        lines.add("  Check In GPS : " + gpsWithDistance(row.checkInLatitude(), row.checkInLongitude(), row.checkInDistanceMeters()));
        lines.add("  Check Out GPS: " + gpsWithDistance(row.checkOutLatitude(), row.checkOutLongitude(), row.checkOutDistanceMeters()));
        if (hasGps(row.checkInLatitude(), row.checkInLongitude())) {
            lines.add("  View on Map  : " + mapUrl(row.checkInLatitude(), row.checkInLongitude()));
        } else if (hasGps(row.checkOutLatitude(), row.checkOutLongitude())) {
            lines.add("  View on Map  : " + mapUrl(row.checkOutLatitude(), row.checkOutLongitude()));
        }
        return lines;
    }

    private String mapUrl(Double latitude, Double longitude) {
        return String.format(Locale.ROOT, "https://maps.google.com/?q=%.5f,%.5f", latitude, longitude);
    }

    private EmployeePdfTotals employeePdfTotals(List<AttendanceReportRow> rows) {
        int normalMinutes = 0;
        int overtimeMinutes = 0;
        int workedDays = 0;
        for (AttendanceReportRow row : rows) {
            if (row.absent() || row.workedMinutes() <= 0) {
                continue;
            }
            workedDays++;
            int dayNormalMinutes = Math.min(row.workedMinutes(), 8 * 60);
            normalMinutes += dayNormalMinutes;
            overtimeMinutes += Math.max(row.workedMinutes() - dayNormalMinutes, 0);
        }
        return new EmployeePdfTotals(workedDays, normalMinutes, overtimeMinutes, normalMinutes + overtimeMinutes);
    }

    private record EmployeePdfTotals(int workedDays, int normalMinutes, int overtimeMinutes, int totalMinutes) {
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

    private record WorkplaceCoordinates(double latitude, double longitude) {
    }
}
