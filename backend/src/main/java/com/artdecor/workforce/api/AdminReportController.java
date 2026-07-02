package com.artdecor.workforce.api;

import com.artdecor.workforce.application.reports.AttendanceReportResponse;
import com.artdecor.workforce.application.reports.AttendanceReportRow;
import com.artdecor.workforce.application.reports.AttendanceReportService;
import com.artdecor.workforce.application.reports.ExportFile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reports")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
public class AdminReportController {
    private final AttendanceReportService reports;

    public AdminReportController(AttendanceReportService reports) {
        this.reports = reports;
    }

    @GetMapping("/attendance")
    public AttendanceReportResponse attendanceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "daily") String period
    ) {
        return reports.report(from, to, period);
    }

    @GetMapping("/employees/{employeeId}/history")
    public List<AttendanceReportRow> employeeHistory(
            @PathVariable UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reports.employeeHistory(employeeId, from, to);
    }

    @GetMapping("/attendance/export")
    public ResponseEntity<byte[]> exportAttendance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ExportFile export = reports.export(from, to, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename())
                        .build()
                        .toString())
                .header(HttpHeaders.CONTENT_TYPE, export.contentType())
                .body(export.content());
    }
}
