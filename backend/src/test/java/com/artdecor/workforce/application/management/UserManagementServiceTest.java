package com.artdecor.workforce.application.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
        UserResponse response = service.createUser(new CreateUserCommand(
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
    void rejectsEmployeeRoleForAdminUsers() {
        assertThatThrownBy(() -> service.createUser(new CreateUserCommand(
                "Employee",
                "employee@artdecor.test",
                "ChangeMe123!",
                UserRole.EMPLOYEE
        ))).isInstanceOf(ManagementException.class);
    }
}
