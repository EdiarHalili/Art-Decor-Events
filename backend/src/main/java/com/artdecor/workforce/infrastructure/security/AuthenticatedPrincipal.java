package com.artdecor.workforce.infrastructure.security;

import com.artdecor.workforce.domain.UserRole;
import java.util.UUID;

public record AuthenticatedPrincipal(
        UUID userId,
        UserRole role,
        UUID employeeId
) {
}

