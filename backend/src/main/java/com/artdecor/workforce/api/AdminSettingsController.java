package com.artdecor.workforce.api;

import com.artdecor.workforce.application.audit.AuditLogResponse;
import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.settings.AppSettingsCommand;
import com.artdecor.workforce.application.settings.AppSettingsResponse;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminSettingsController {
    private final AppSettingsService settings;
    private final AuditService audit;

    public AdminSettingsController(AppSettingsService settings, AuditService audit) {
        this.settings = settings;
        this.audit = audit;
    }

    @GetMapping
    public AppSettingsResponse current() {
        return settings.current();
    }

    @PutMapping
    public ResponseEntity<AppSettingsResponse> update(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody SettingsRequest request
    ) {
        AppSettingsResponse response = settings.update(request.toCommand());
        audit.log(principal, "SETTINGS_UPDATED", "APP_SETTINGS", null);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/audit-logs")
    public List<AuditLogResponse> auditLogs() {
        return audit.latest();
    }

    public record SettingsRequest(
            @NotBlank String companyName,
            String logoUrl,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String primaryColor,
            @Pattern(regexp = "^#[0-9a-fA-F]{6}$") String accentColor,
            @NotBlank String timezone,
            @NotNull LocalTime defaultCheckInOpenTime,
            @NotNull LocalTime defaultCheckInCloseTime,
            @Min(0) @Max(240) int allowedLateMinutes,
            boolean gpsEnabled,
            boolean notificationsEnabled,
            @Min(5) @Max(1440) int sessionTimeoutMinutes
    ) {
        AppSettingsCommand toCommand() {
            return new AppSettingsCommand(companyName, logoUrl, primaryColor, accentColor, timezone,
                    defaultCheckInOpenTime, defaultCheckInCloseTime, allowedLateMinutes, gpsEnabled,
                    notificationsEnabled, sessionTimeoutMinutes);
        }
    }
}
