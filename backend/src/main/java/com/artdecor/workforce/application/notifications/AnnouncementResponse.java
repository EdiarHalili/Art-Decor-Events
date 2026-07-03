package com.artdecor.workforce.application.notifications;

import java.time.Instant;

public record AnnouncementResponse(
        String id,
        String title,
        String body,
        Instant visibleFrom,
        Instant visibleUntil,
        Instant createdAt
) {
}
