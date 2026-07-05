package com.artdecor.workforce.api;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.checkinwindow.DailyCheckInWindowCommand;
import com.artdecor.workforce.application.checkinwindow.DailyCheckInWindowResponse;
import com.artdecor.workforce.application.checkinwindow.DailyCheckInWindowService;
import com.artdecor.workforce.domain.CheckoutMode;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/check-in-windows")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
public class AdminDailyCheckInWindowController {
    private final DailyCheckInWindowService service;
    private final AuditService audit;

    public AdminDailyCheckInWindowController(
            DailyCheckInWindowService service,
            AuditService audit
    ) {
        this.service = service;
        this.audit = audit;
    }

    @GetMapping
    public List<DailyCheckInWindowResponse> listWindows() {
        return service.listWindows();
    }

    @PostMapping
    public ResponseEntity<DailyCheckInWindowResponse> createWindow(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody DailyCheckInWindowRequest request
    ) {
        DailyCheckInWindowResponse response = service.createWindow(request.toCommand());
        audit.log(principal, "DAILY_WINDOW_CREATED", "WORK_SCHEDULE", UUID.fromString(response.id()));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{windowId}")
    public ResponseEntity<DailyCheckInWindowResponse> updateWindow(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID windowId,
            @Valid @RequestBody DailyCheckInWindowRequest request
    ) {
        return updateWindowInternal(principal, windowId, request);
    }

    @PostMapping("/{windowId}/update")
    public ResponseEntity<DailyCheckInWindowResponse> updateWindowWithPost(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID windowId,
            @Valid @RequestBody DailyCheckInWindowRequest request
    ) {
        return updateWindowInternal(principal, windowId, request);
    }

    private ResponseEntity<DailyCheckInWindowResponse> updateWindowInternal(
            AuthenticatedPrincipal principal,
            UUID windowId,
            DailyCheckInWindowRequest request
    ) {
        DailyCheckInWindowResponse response = service.updateWindow(windowId, request.toCommand());
        audit.log(principal, "DAILY_WINDOW_UPDATED", "WORK_SCHEDULE", windowId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{windowId}/open")
    public ResponseEntity<DailyCheckInWindowResponse> openWindow(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID windowId
    ) {
        DailyCheckInWindowResponse response = service.openWindow(windowId);
        audit.log(principal, "DAILY_WINDOW_OPENED", "WORK_SCHEDULE", windowId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{windowId}/close")
    public ResponseEntity<DailyCheckInWindowResponse> closeWindow(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID windowId
    ) {
        DailyCheckInWindowResponse response = service.closeWindow(windowId);
        audit.log(principal, "DAILY_WINDOW_CLOSED", "WORK_SCHEDULE", windowId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{windowId}/cancel")
    public ResponseEntity<DailyCheckInWindowResponse> cancelWindow(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID windowId
    ) {
        DailyCheckInWindowResponse response = service.cancelWindow(windowId);
        audit.log(principal, "DAILY_WINDOW_CANCELLED", "WORK_SCHEDULE", windowId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{windowId}")
    public ResponseEntity<Void> deleteCancelledWindow(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID windowId
    ) {
        service.deleteCancelledWindow(windowId);
        audit.log(principal, "DAILY_WINDOW_DELETED", "WORK_SCHEDULE", windowId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{windowId}/employees")
    public ResponseEntity<DailyCheckInWindowResponse> replaceEmployees(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID windowId,
            @Valid @RequestBody ReplaceEmployeesRequest request
    ) {
        DailyCheckInWindowResponse response = service.replaceEmployees(windowId, request.employeeIds());
        audit.log(principal, "DAILY_WINDOW_EMPLOYEES_UPDATED", "WORK_SCHEDULE", windowId);
        return ResponseEntity.ok(response);
    }

    public record DailyCheckInWindowRequest(
            @NotNull LocalDate workDate,
            @NotNull Instant checkInOpensAt,
            @NotNull Instant checkInClosesAt,
            CheckoutMode checkoutMode,
            Boolean autoCheckoutEnabled,
            @NotEmpty Set<UUID> employeeIds
    ) {
        DailyCheckInWindowCommand toCommand() {
            return new DailyCheckInWindowCommand(
                    workDate,
                    checkInOpensAt,
                    checkInClosesAt,
                    checkoutMode == null ? CheckoutMode.SCHEDULED_AUTO : checkoutMode,
                    autoCheckoutEnabled == null || autoCheckoutEnabled,
                    employeeIds
            );
        }
    }

    public record ReplaceEmployeesRequest(@NotEmpty Set<UUID> employeeIds) {
    }
}
