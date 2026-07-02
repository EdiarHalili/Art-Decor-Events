package com.artdecor.workforce.application.management;

import java.time.Instant;

public record UserResponse(
        String id,
        String fullName,
        String email,
        String role,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}

