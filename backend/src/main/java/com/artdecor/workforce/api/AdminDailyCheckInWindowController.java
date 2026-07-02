package com.artdecor.workforce.api;

import com.artdecor.workforce.application.checkinwindow.DailyCheckInWindowCommand;
import com.artdecor.workforce.application.checkinwindow.DailyCheckInWindowResponse;
import com.artdecor.workforce.application.checkinwindow.DailyCheckInWindowService;
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

    public AdminDailyCheckInWindowController(DailyCheckInWindowService service) {
        this.service = service;
    }

    @GetMapping
    public List<DailyCheckInWindowResponse> listWindows() {
        return service.listWindows();
    }

    @PostMapping
    public ResponseEntity<DailyCheckInWindowResponse> createWindow(@Valid @RequestBody DailyCheckInWindowRequest request) {
        return ResponseEntity.ok(service.createWindow(request.toCommand()));
    }

    @PatchMapping("/{windowId}")
    public ResponseEntity<DailyCheckInWindowResponse> updateWindow(
            @PathVariable UUID windowId,
            @Valid @RequestBody DailyCheckInWindowRequest request
    ) {
        return ResponseEntity.ok(service.updateWindow(windowId, request.toCommand()));
    }

    @PostMapping("/{windowId}/open")
    public ResponseEntity<DailyCheckInWindowResponse> openWindow(@PathVariable UUID windowId) {
        return ResponseEntity.ok(service.openWindow(windowId));
    }

    @PostMapping("/{windowId}/close")
    public ResponseEntity<DailyCheckInWindowResponse> closeWindow(@PathVariable UUID windowId) {
        return ResponseEntity.ok(service.closeWindow(windowId));
    }

    @PostMapping("/{windowId}/cancel")
    public ResponseEntity<DailyCheckInWindowResponse> cancelWindow(@PathVariable UUID windowId) {
        return ResponseEntity.ok(service.cancelWindow(windowId));
    }

    @PutMapping("/{windowId}/employees")
    public ResponseEntity<DailyCheckInWindowResponse> replaceEmployees(
            @PathVariable UUID windowId,
            @Valid @RequestBody ReplaceEmployeesRequest request
    ) {
        return ResponseEntity.ok(service.replaceEmployees(windowId, request.employeeIds()));
    }

    public record DailyCheckInWindowRequest(
            @NotNull LocalDate workDate,
            @NotNull Instant checkInOpensAt,
            @NotNull Instant checkInClosesAt,
            @NotEmpty Set<UUID> employeeIds
    ) {
        DailyCheckInWindowCommand toCommand() {
            return new DailyCheckInWindowCommand(workDate, checkInOpensAt, checkInClosesAt, employeeIds);
        }
    }

    public record ReplaceEmployeesRequest(@NotEmpty Set<UUID> employeeIds) {
    }
}

