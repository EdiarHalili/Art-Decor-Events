package com.artdecor.workforce.api;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.notifications.AnnouncementCommand;
import com.artdecor.workforce.application.notifications.AnnouncementResponse;
import com.artdecor.workforce.application.notifications.NotificationService;
import com.artdecor.workforce.application.notifications.PushSubscriptionCommand;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AnnouncementController {
    private final NotificationService notifications;
    private final AuditService audit;

    public AnnouncementController(NotificationService notifications, AuditService audit) {
        this.notifications = notifications;
        this.audit = audit;
    }

    @GetMapping("/announcements")
    @PreAuthorize("isAuthenticated()")
    public List<AnnouncementResponse> visibleAnnouncements() {
        return notifications.visibleAnnouncements();
    }

    @GetMapping("/admin/announcements")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
    public List<AnnouncementResponse> listAnnouncements() {
        return notifications.listAnnouncements();
    }

    @PostMapping("/admin/announcements")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
    public ResponseEntity<AnnouncementResponse> publish(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody AnnouncementRequest request
    ) {
        AnnouncementResponse response = notifications.publish(request.toCommand());
        audit.log(principal, "ANNOUNCEMENT_PUBLISHED", "ANNOUNCEMENT", java.util.UUID.fromString(response.id()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/notifications/subscribe")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> subscribe(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody PushSubscriptionRequest request,
            HttpServletRequest httpRequest
    ) {
        notifications.subscribe(principal, request.toCommand(), httpRequest.getHeader("User-Agent"));
        audit.log(principal, "PUSH_SUBSCRIPTION_SAVED", "PUSH_SUBSCRIPTION", null);
        return ResponseEntity.ok().build();
    }

    public record AnnouncementRequest(
            @NotBlank String title,
            @NotBlank String body,
            Instant visibleFrom,
            Instant visibleUntil
    ) {
        AnnouncementCommand toCommand() {
            return new AnnouncementCommand(title, body, visibleFrom, visibleUntil);
        }
    }

    public record PushSubscriptionRequest(
            @NotBlank String endpoint,
            @NotBlank String p256dhKey,
            @NotBlank String authKey
    ) {
        PushSubscriptionCommand toCommand() {
            return new PushSubscriptionCommand(endpoint, p256dhKey, authKey);
        }
    }
}
