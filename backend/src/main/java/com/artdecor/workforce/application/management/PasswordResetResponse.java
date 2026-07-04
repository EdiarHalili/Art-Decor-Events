package com.artdecor.workforce.application.management;

public record PasswordResetResponse(
        String temporaryPassword,
        boolean passwordMustChange
) {
}
