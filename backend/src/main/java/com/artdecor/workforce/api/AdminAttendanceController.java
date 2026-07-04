package com.artdecor.workforce.api;

import com.artdecor.workforce.application.attendance.AttendanceResponse;
import com.artdecor.workforce.application.attendance.AttendanceService;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/attendance")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
public class AdminAttendanceController {
    private final AttendanceService attendance;

    public AdminAttendanceController(AttendanceService attendance) {
        this.attendance = attendance;
    }

    @PostMapping("/{attendanceRecordId}/checkout")
    public ResponseEntity<AttendanceResponse> adminCheckout(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID attendanceRecordId,
            @Valid @RequestBody AdminCheckoutRequest request
    ) {
        return ResponseEntity.ok(attendance.adminCheckOut(principal, attendanceRecordId, request.checkedOutAt()));
    }

    public record AdminCheckoutRequest(@NotNull Instant checkedOutAt) {
    }
}
