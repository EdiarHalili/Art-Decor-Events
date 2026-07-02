package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(CreateUserCommand command) {
        if (command.role() == UserRole.EMPLOYEE) {
            throw new ManagementException("Employee portal accounts must be created from Employee Management.");
        }

        if (users.existsByEmailIgnoreCase(command.email())) {
            throw new ManagementException("Email already exists.");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setFullName(command.fullName().trim());
        user.setEmail(command.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setRole(command.role());

        return toResponse(users.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(UUID userId) {
        UserAccountEntity user = users.findById(userId)
                .orElseThrow(() -> new ManagementException("User not found."));
        user.setStatus(UserStatus.INACTIVE);
        return toResponse(user);
    }

    private UserResponse toResponse(UserAccountEntity user) {
        return new UserResponse(
                user.getId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}

