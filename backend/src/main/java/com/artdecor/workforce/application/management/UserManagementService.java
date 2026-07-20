package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
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
        user.setPasswordMustChange(true);
        user.setRole(command.role());

        return toResponse(users.save(user));
    }

    @Transactional
    public UserResponse deactivateUser(AuthenticatedPrincipal principal, UUID userId) {
        UserAccountEntity user = users.findById(userId)
                .orElseThrow(() -> new ManagementException("User not found."));
        if (principal.userId().equals(user.getId())) {
            throw new ManagementException("Nuk mund ta çaktivizoni llogarinë tuaj.");
        }
        if (user.getRole() == UserRole.ADMINISTRATOR
                && users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE) <= 1) {
            throw new ManagementException("Nuk mund të çaktivizohet administratori i fundit aktiv.");
        }
        user.setStatus(UserStatus.INACTIVE);
        user.incrementTokenVersion();
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
