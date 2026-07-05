package com.artdecor.workforce.application.notifications;

import com.artdecor.workforce.infrastructure.persistence.AnnouncementEntity;
import com.artdecor.workforce.infrastructure.persistence.AnnouncementRepository;
import com.artdecor.workforce.infrastructure.persistence.PushSubscriptionEntity;
import com.artdecor.workforce.infrastructure.persistence.PushSubscriptionRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {
    private final AnnouncementRepository announcements;
    private final PushSubscriptionRepository subscriptions;
    private final Clock clock;

    public NotificationService(
            AnnouncementRepository announcements,
            PushSubscriptionRepository subscriptions,
            Clock clock
    ) {
        this.announcements = announcements;
        this.subscriptions = subscriptions;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> visibleAnnouncements() {
        return announcements.findVisible(Instant.now(clock)).stream()
                .filter(this::isAdminAnnouncement)
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listAnnouncements() {
        return announcements.findAll().stream()
                .filter(this::isAdminAnnouncement)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AnnouncementResponse publish(AnnouncementCommand command) {
        if (command.title() == null || command.title().isBlank() || command.body() == null || command.body().isBlank()) {
            throw new IllegalArgumentException("Announcement title and message are required.");
        }
        AnnouncementEntity announcement = new AnnouncementEntity();
        announcement.setTitle(command.title().trim());
        announcement.setBody(command.body().trim());
        announcement.setVisibleFrom(command.visibleFrom() == null ? Instant.now(clock) : command.visibleFrom());
        announcement.setVisibleUntil(command.visibleUntil());
        return toResponse(announcements.save(announcement));
    }

    @Transactional
    public void subscribe(AuthenticatedPrincipal principal, PushSubscriptionCommand command, String userAgent) {
        PushSubscriptionEntity subscription = subscriptions.findByEndpoint(command.endpoint())
                .orElseGet(PushSubscriptionEntity::new);
        subscription.setEndpoint(command.endpoint());
        subscription.setP256dhKey(command.p256dhKey());
        subscription.setAuthKey(command.authKey());
        subscription.setUserAgent(userAgent);
        subscription.setLastSeenAt(Instant.now(clock));
        if (principal.employeeId() != null) {
            subscription.setEmployeeId(principal.employeeId());
        } else {
            subscription.setUserId(principal.userId());
        }
        subscriptions.save(subscription);
    }

    public AnnouncementResponse publishSystem(String title, String body) {
        return publish(new AnnouncementCommand(title, body, Instant.now(clock), null));
    }

    @Transactional
    public AnnouncementResponse update(java.util.UUID announcementId, AnnouncementCommand command) {
        if (command.title() == null || command.title().isBlank() || command.body() == null || command.body().isBlank()) {
            throw new IllegalArgumentException("Titulli dhe mesazhi i njoftimit janë të detyrueshëm.");
        }
        AnnouncementEntity announcement = announcements.findById(announcementId)
                .orElseThrow(() -> new IllegalArgumentException("Njoftimi nuk u gjet."));
        announcement.setTitle(command.title().trim());
        announcement.setBody(command.body().trim());
        announcement.setVisibleFrom(command.visibleFrom() == null ? announcement.getVisibleFrom() : command.visibleFrom());
        announcement.setVisibleUntil(command.visibleUntil());
        return toResponse(announcements.save(announcement));
    }

    @Transactional
    public void delete(java.util.UUID announcementId) {
        if (!announcements.existsById(announcementId)) {
            throw new IllegalArgumentException("Njoftimi nuk u gjet.");
        }
        announcements.deleteById(announcementId);
    }

    private boolean isAdminAnnouncement(AnnouncementEntity announcement) {
        return !java.util.Set.of("Daily check-in window published", "Check-in is open")
                .contains(announcement.getTitle());
    }

    private AnnouncementResponse toResponse(AnnouncementEntity announcement) {
        return new AnnouncementResponse(
                announcement.getId().toString(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getVisibleFrom(),
                announcement.getVisibleUntil(),
                announcement.getCreatedAt()
        );
    }
}
