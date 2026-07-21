package com.artdecor.workforce.application.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class UserManagementServiceTest {
    private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(users, passwordEncoder);
        when(users.save(any(UserAccountEntity.class))).thenAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });
    }

    @Test
    void createsSupervisorAccount() {
        UserResponse response = service.createUser(superAdminPrincipal(), new CreateUserCommand(
                "Supervisor",
                "supervisor@artdecor.test",
                "ChangeMe123!",
                UserRole.SUPERVISOR
        ));

        assertThat(response.id()).isNotBlank();
        assertThat(response.email()).isEqualTo("supervisor@artdecor.test");
        assertThat(response.role()).isEqualTo("SUPERVISOR");

        ArgumentCaptor<UserAccountEntity> userCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        org.mockito.Mockito.verify(users).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isNotEqualTo("ChangeMe123!");
        assertThat(passwordEncoder.matches("ChangeMe123!", userCaptor.getValue().getPasswordHash())).isTrue();
        assertThat(userCaptor.getValue().isPasswordMustChange()).isTrue();
    }

    @Test
    void superAdminCanCreateAdministratorAccount() {
        UserResponse response = service.createUser(superAdminPrincipal(), new CreateUserCommand(
                "Administrator",
                "administrator@artdecor.test",
                "ChangeMe123!",
                UserRole.ADMINISTRATOR
        ));

        assertThat(response.id()).isNotBlank();
        assertThat(response.email()).isEqualTo("administrator@artdecor.test");
        assertThat(response.role()).isEqualTo(UserRole.ADMINISTRATOR.name());
    }

    @Test
    void rejectsEmployeeRoleForAdminUsers() {
        assertThatThrownBy(() -> service.createUser(superAdminPrincipal(), new CreateUserCommand(
                "Employee",
                "employee@artdecor.test",
                "ChangeMe123!",
                UserRole.EMPLOYEE
        ))).isInstanceOf(ManagementException.class);
    }

    @Test
    void rejectsAdministratorSelfDeactivation() {
        UUID adminId = UUID.randomUUID();
        UserAccountEntity admin = user(adminId, UserRole.ADMINISTRATOR);
        when(users.findById(adminId)).thenReturn(java.util.Optional.of(admin));

        assertThatThrownBy(() -> service.deactivateUser(
                new AuthenticatedPrincipal(adminId, UserRole.ADMINISTRATOR, null),
                adminId
        ))
                .isInstanceOf(ManagementException.class)
                .hasMessage("Nuk mund ta çaktivizoni llogarinë tuaj.");

        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void rejectsDeactivatingLastActiveAdministrator() {
        UUID currentAdminId = UUID.randomUUID();
        UUID targetAdminId = UUID.randomUUID();
        UserAccountEntity targetAdmin = user(targetAdminId, UserRole.ADMINISTRATOR);
        when(users.findById(targetAdminId)).thenReturn(java.util.Optional.of(targetAdmin));
        when(users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)).thenReturn(1L);
        when(users.countByRoleAndStatus(UserRole.SUPER_ADMIN, UserStatus.ACTIVE)).thenReturn(0L);

        assertThatThrownBy(() -> service.deactivateUser(
                new AuthenticatedPrincipal(currentAdminId, UserRole.SUPER_ADMIN, null),
                targetAdminId
        ))
                .isInstanceOf(ManagementException.class)
                .hasMessage("Nuk mund të çaktivizohet administratori i fundit aktiv.");

        assertThat(targetAdmin.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void allowsSuperAdminToDeactivateAnotherAdministratorWhenMultipleAdministratorsRemain() {
        UUID targetAdminId = UUID.randomUUID();
        UserAccountEntity targetAdmin = user(targetAdminId, UserRole.ADMINISTRATOR);
        when(users.findById(targetAdminId)).thenReturn(Optional.of(targetAdmin));
        when(users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)).thenReturn(2L);

        UserResponse response = service.deactivateUser(
                superAdminPrincipal(),
                targetAdminId
        );

        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE.name());
        assertThat(targetAdmin.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(targetAdmin.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void administratorCanCreateSupervisorButNotAdministrator() {
        UserResponse response = service.createUser(adminPrincipal(), new CreateUserCommand(
                "Supervisor",
                "ops@artdecor.test",
                "ChangeMe123!",
                UserRole.SUPERVISOR
        ));

        assertThat(response.role()).isEqualTo(UserRole.SUPERVISOR.name());

        assertThatThrownBy(() -> service.createUser(adminPrincipal(), new CreateUserCommand(
                "Admin",
                "admin2@artdecor.test",
                "ChangeMe123!",
                UserRole.ADMINISTRATOR
        ))).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void supervisorCannotCreateAdminUsers() {
        assertThatThrownBy(() -> service.createUser(
                new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.SUPERVISOR, null),
                new CreateUserCommand("Supervisor", "s@artdecor.test", "ChangeMe123!", UserRole.SUPERVISOR)
        )).isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void resetsPasswordAndRevokesExistingTokens() {
        UUID supervisorId = UUID.randomUUID();
        UserAccountEntity supervisor = user(supervisorId, UserRole.SUPERVISOR);
        when(users.findById(supervisorId)).thenReturn(Optional.of(supervisor));

        PasswordResetResponse response = service.resetPassword(adminPrincipal(), supervisorId, "NewPass123");

        assertThat(response.passwordMustChange()).isTrue();
        assertThat(response.temporaryPassword()).isNull();
        assertThat(passwordEncoder.matches("NewPass123", supervisor.getPasswordHash())).isTrue();
        assertThat(supervisor.isPasswordMustChange()).isTrue();
        assertThat(supervisor.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void superAdminCanChangeSupervisorRoleToAdministrator() {
        UUID userId = UUID.randomUUID();
        UserAccountEntity supervisor = user(userId, UserRole.SUPERVISOR);
        when(users.findById(userId)).thenReturn(Optional.of(supervisor));

        UserResponse response = service.changeRole(superAdminPrincipal(), userId, UserRole.ADMINISTRATOR);

        assertThat(response.role()).isEqualTo(UserRole.ADMINISTRATOR.name());
        assertThat(supervisor.getTokenVersion()).isEqualTo(1);
    }

    @Test
    void rejectsWeakAdminPassword() {
        assertThatThrownBy(() -> service.createUser(superAdminPrincipal(), new CreateUserCommand(
                "Weak",
                "weak@artdecor.test",
                "password",
                UserRole.SUPERVISOR
        ))).isInstanceOf(ManagementException.class)
                .hasMessageContaining("shkronjë të madhe");
    }

    private AuthenticatedPrincipal superAdminPrincipal() {
        return new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.SUPER_ADMIN, null);
    }

    private AuthenticatedPrincipal adminPrincipal() {
        return new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);
    }

    private UserAccountEntity user(UUID id, UserRole role) {
        UserAccountEntity user = new UserAccountEntity();
        ReflectionTestUtils.setField(user, "id", id);
        user.setFullName("Admin User");
        user.setEmail(id + "@artdecor.test");
        user.setPasswordHash(passwordEncoder.encode("ChangeMe123!"));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
