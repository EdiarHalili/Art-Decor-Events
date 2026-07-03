package com.artdecor.workforce.application.notifications;

import java.time.Instant;

public record AnnouncementCommand(
        String title,
        String body,
        Instant visibleFrom,
        Instant visibleUntil
) {
}
