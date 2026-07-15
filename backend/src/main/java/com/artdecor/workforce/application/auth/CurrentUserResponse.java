package com.artdecor.workforce.application.auth;

public record CurrentUserResponse(
        String userId,
        String role,
        String fullName,
        String employeeId,
        String employeeCode,
        boolean passwordMustChange
) {
}
