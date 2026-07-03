package com.artdecor.workforce.application.audit;

import java.time.Instant;

public record AuditLogResponse(
        String id,
        String action,
        String entityType,
        String entityId,
        Instant createdAt
) {
}
