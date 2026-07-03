package com.artdecor.workforce.application.notifications;

public record PushSubscriptionCommand(
        String endpoint,
        String p256dhKey,
        String authKey
) {
}
