package com.artdecor.workforce.application.management;

public record UpdateUserCommand(
        String fullName,
        String email
) {
}
