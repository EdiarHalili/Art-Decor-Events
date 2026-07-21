package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAll().stream()
                .filter(user -> user.getRole() != UserRole.EMPLOYEE)
                .sorted(Comparator.comparing(UserAccountEntity::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(AuthenticatedPrincipal principal, CreateUserCommand command) {
        validatePassword(command.password());
        if (command.role() == UserRole.EMPLOYEE) {
            throw new ManagementException("Llogaritë e punëtorëve krijohen nga moduli i punëtorëve.");
        }
        requireCanCreateRole(principal, command.role());

        if (users.existsByEmailIgnoreCase(command.email())) {
            throw new ManagementException("Ky email ekziston tashmë.");
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
    public UserResponse updateUser(AuthenticatedPrincipal principal, UUID userId, UpdateUserCommand command) {
        UserAccountEntity user = findManagedUser(userId);
        requireCanManageUser(principal, user);
        String email = command.email().trim().toLowerCase();
        users.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new ManagementException("Ky email ekziston tashmë.");
                });
        user.setFullName(command.fullName().trim());
        user.setEmail(email);
        return toResponse(user);
    }

    @Transactional
    public UserResponse changeRole(AuthenticatedPrincipal principal, UUID userId, UserRole role) {
        UserAccountEntity user = findManagedUser(userId);
        requireCanManageUser(principal, user);
        requireCanManageRole(principal, role);
        if (user.getRole() == UserRole.SUPER_ADMIN && role != UserRole.SUPER_ADMIN) {
            ensureAnotherActiveSuperAdminExists();
        }
        if (user.getRole() == UserRole.ADMINISTRATOR && role != UserRole.ADMINISTRATOR) {
            ensureAnotherActiveAdministratorExists();
        }
        user.setRole(role);
        user.incrementTokenVersion();
        return toResponse(user);
    }

    @Transactional
    public PasswordResetResponse resetPassword(AuthenticatedPrincipal principal, UUID userId, String temporaryPassword) {
        UserAccountEntity user = findManagedUser(userId);
        if (principal.userId().equals(user.getId())) {
            throw new ManagementException("Përdorni ndryshimin e fjalëkalimit për llogarinë tuaj.");
        }
        requireCanManageUser(principal, user);
        validatePassword(temporaryPassword);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordMustChange(true);
        user.incrementTokenVersion();
        return new PasswordResetResponse(null, true);
    }

    @Transactional
    public UserResponse activateUser(AuthenticatedPrincipal principal, UUID userId) {
        UserAccountEntity user = findManagedUser(userId);
        requireCanManageUser(principal, user);
        user.setStatus(UserStatus.ACTIVE);
        user.incrementTokenVersion();
        return toResponse(user);
    }

    @Transactional
    public UserResponse deactivateUser(AuthenticatedPrincipal principal, UUID userId) {
        UserAccountEntity user = findManagedUser(userId);
        if (principal.userId().equals(user.getId())) {
            throw new ManagementException("Nuk mund ta çaktivizoni llogarinë tuaj.");
        }
        requireCanManageUser(principal, user);
        if (user.getRole() == UserRole.SUPER_ADMIN) {
            ensureAnotherActiveSuperAdminExists();
        }
        if (user.getRole() == UserRole.ADMINISTRATOR) {
            ensureAnotherActiveAdministratorExists();
        }
        user.setStatus(UserStatus.INACTIVE);
        user.incrementTokenVersion();
        return toResponse(user);
    }

    private UserAccountEntity findManagedUser(UUID userId) {
        UserAccountEntity user = users.findById(userId)
                .orElseThrow(() -> new ManagementNotFoundException("Përdoruesi nuk u gjet."));
        if (user.getRole() == UserRole.EMPLOYEE) {
            throw new ManagementException("Llogaritë e punëtorëve menaxhohen nga moduli i punëtorëve.");
        }
        return user;
    }

    private void requireCanManageUser(AuthenticatedPrincipal principal, UserAccountEntity user) {
        requireCanManageRole(principal, user.getRole());
    }

    private void requireCanManageRole(AuthenticatedPrincipal principal, UserRole role) {
        if (principal.role() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (principal.role() == UserRole.ADMINISTRATOR && role == UserRole.SUPERVISOR) {
            return;
        }
        throw new AuthorizationDeniedException("Nuk keni leje për këtë veprim.");
    }

    private void requireCanCreateRole(AuthenticatedPrincipal principal, UserRole role) {
        if (principal.role() == UserRole.SUPER_ADMIN
                && (role == UserRole.ADMINISTRATOR || role == UserRole.SUPERVISOR)) {
            return;
        }
        if (principal.role() == UserRole.ADMINISTRATOR && role == UserRole.SUPERVISOR) {
            return;
        }
        throw new AuthorizationDeniedException("Nuk keni leje për këtë veprim.");
    }
    private void ensureAnotherActiveSuperAdminExists() {
        if (users.countByRoleAndStatus(UserRole.SUPER_ADMIN, UserStatus.ACTIVE) <= 1) {
            throw new ManagementException("Nuk mund të çaktivizohet super administratori i fundit aktiv.");
        }
    }

    private void ensureAnotherActiveAdministratorExists() {
        long activeAdministrators = users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)
                + users.countByRoleAndStatus(UserRole.SUPER_ADMIN, UserStatus.ACTIVE);
        if (activeAdministrators <= 1) {
            throw new ManagementException("Nuk mund të çaktivizohet administratori i fundit aktiv.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new ManagementException("Fjalëkalimi duhet të ketë të paktën 8 karaktere.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ManagementException("Fjalëkalimi duhet të ketë 128 karaktere ose më pak.");
        }
        if (!password.matches(".*[A-Z].*") || !password.matches(".*[a-z].*") || !password.matches(".*\\d.*")) {
            throw new ManagementException("Fjalëkalimi duhet të përmbajë shkronjë të madhe, shkronjë të vogël dhe numër.");
        }
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
