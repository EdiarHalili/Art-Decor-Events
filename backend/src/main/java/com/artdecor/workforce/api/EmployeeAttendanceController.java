package com.artdecor.workforce.api;

import com.artdecor.workforce.application.attendance.AttendanceActionCommand;
import com.artdecor.workforce.application.attendance.AttendanceResponse;
import com.artdecor.workforce.application.attendance.AttendanceService;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee/attendance")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeAttendanceController {
    private final AttendanceService attendanceService;

    public EmployeeAttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
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

    public record AttendanceActionRequest(
            @NotNull UUID scheduleId,
            @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            Map<String, Object> device
    ) {
        AttendanceActionCommand toCommand() {
            return new AttendanceActionCommand(scheduleId, latitude, longitude, device == null ? Map.of() : device);
        }
    }
}

