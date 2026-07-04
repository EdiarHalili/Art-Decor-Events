package com.artdecor.workforce.api;

import com.artdecor.workforce.application.location.LiveLocationCommand;
import com.artdecor.workforce.application.location.LiveLocationResponse;
import com.artdecor.workforce.application.location.LiveLocationService;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/employee/live-location")
public class EmployeeLiveLocationController {
    private final LiveLocationService liveLocationService;

    public EmployeeLiveLocationController(LiveLocationService liveLocationService) {
        this.liveLocationService = liveLocationService;
    }

    @PostMapping
    public ResponseEntity<LiveLocationResponse> record(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody LiveLocationRequest request
    ) {
        return ResponseEntity.ok(liveLocationService.record(principal, request.toCommand()));
    }

    public record LiveLocationRequest(
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @DecimalMin("0.0") Double accuracyMeters,
            Instant capturedAt,
            Map<String, Object> device
    ) {
        LiveLocationCommand toCommand() {
            return new LiveLocationCommand(latitude, longitude, accuracyMeters, capturedAt, device);
        }
    }
}
