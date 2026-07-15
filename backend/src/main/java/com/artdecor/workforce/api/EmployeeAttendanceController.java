package com.artdecor.workforce.api;

import com.artdecor.workforce.application.attendance.AttendanceActionCommand;
import com.artdecor.workforce.application.attendance.AttendanceResponse;
import com.artdecor.workforce.application.attendance.AttendanceService;
import com.artdecor.workforce.application.reports.AttendanceReportRow;
import com.artdecor.workforce.application.reports.AttendanceReportService;
import com.artdecor.workforce.application.reports.ExportFile;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee/attendance")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeAttendanceController {
    private final AttendanceService attendanceService;
    private final AttendanceReportService reports;

    public EmployeeAttendanceController(AttendanceService attendanceService, AttendanceReportService reports) {
        this.attendanceService = attendanceService;
        this.reports = reports;
    }

    @PostMapping("/check-in")
    public ResponseEntity<AttendanceResponse> checkIn(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody AttendanceActionRequest request
    ) {
        return ResponseEntity.ok(attendanceService.checkIn(principal, request.toCommand()));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceResponse> checkOut(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody AttendanceActionRequest request
    ) {
        return ResponseEntity.ok(attendanceService.checkOut(principal, request.toCommand()));
    }

    @GetMapping("/history")
    public List<AttendanceReportRow> history(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return reports.employeeHistory(principal.employeeId(), from, to);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "csv") String format
    ) {
        ExportFile export = reports.exportEmployee(principal.employeeId(), from, to, format);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(export.filename())
                        .build()
                        .toString())
                .header(HttpHeaders.CONTENT_TYPE, export.contentType())
                .body(export.content());
    }

    public record AttendanceActionRequest(
            @NotNull UUID scheduleId,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            Instant capturedAt,
            Map<String, Object> device
    ) {
        AttendanceActionCommand toCommand() {
            return new AttendanceActionCommand(scheduleId, latitude, longitude, device == null ? Map.of() : device, capturedAt);
        }
    }
}
