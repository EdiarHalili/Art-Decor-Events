package com.artdecor.workforce.application.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String role,
        String fullName,
        String employeeId,
        boolean passwordMustChange
) {
}
