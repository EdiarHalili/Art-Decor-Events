package com.artdecor.workforce.infrastructure.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class LocalDataBootstrapTest {
    private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final BootstrapProperties properties = new BootstrapProperties(
            true,
            "Art Decor Administrator",
            "admin@artdecor.local",
            "ChangeMe123!",
            "Demo Employee",
            "EMP001",
            "1234",
            "Employee123!"
    );

    @Test
    void resetsExistingDevCredentialsWhenBootstrapIsEnabled() {
        UserAccountEntity admin = new UserAccountEntity();
        admin.setFullName("Old Admin");
        admin.setEmail("admin@artdecor.local");
        admin.setPasswordHash(passwordEncoder.encode("OldPassword123!"));
        admin.setRole(UserRole.SUPERVISOR);
        admin.setStatus(UserStatus.INACTIVE);

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Old Employee");
        employee.setPinHash(passwordEncoder.encode("9999"));
        employee.setStatus(UserStatus.INACTIVE);

        when(users.findByEmailIgnoreCase("admin@artdecor.local")).thenReturn(Optional.of(admin));
        when(employees.findByEmployeeCodeIgnoreCase("EMP001")).thenReturn(Optional.of(employee));
        when(users.save(any(UserAccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(employees.save(any(EmployeeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new LocalDataBootstrap(properties, users, employees, passwordEncoder).run(null);

        ArgumentCaptor<UserAccountEntity> userCaptor = ArgumentCaptor.forClass(UserAccountEntity.class);
        org.mockito.Mockito.verify(users, org.mockito.Mockito.atLeastOnce()).save(userCaptor.capture());
        assertThat(userCaptor.getAllValues())
                .anySatisfy(saved -> {
                    assertThat(saved.getEmail()).isEqualTo("admin@artdecor.local");
                    assertThat(passwordEncoder.matches("ChangeMe123!", saved.getPasswordHash())).isTrue();
                    assertThat(saved.getRole()).isEqualTo(UserRole.SUPER_ADMIN);
                    assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
                })
                .anySatisfy(saved -> {
                    assertThat(saved.getEmail()).isEqualTo("emp001");
                    assertThat(passwordEncoder.matches("Employee123!", saved.getPasswordHash())).isTrue();
                    assertThat(saved.getRole()).isEqualTo(UserRole.EMPLOYEE);
                    assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
                });

        ArgumentCaptor<EmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EmployeeEntity.class);
        org.mockito.Mockito.verify(employees).save(employeeCaptor.capture());
        assertThat(passwordEncoder.matches("1234", employeeCaptor.getValue().getPinHash())).isTrue();
        assertThat(employeeCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
