package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.UserRole;

public record CreateUserCommand(
        String fullName,
        String email,
        String password,
        UserRole role
) {
}

