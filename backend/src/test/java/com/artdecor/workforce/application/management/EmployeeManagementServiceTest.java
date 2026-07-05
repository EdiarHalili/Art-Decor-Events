package com.artdecor.workforce.application.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.WageType;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeManagementServiceTest {
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private EmployeeManagementService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeManagementService(employees, users, passwordEncoder);
        when(employees.save(any(EmployeeEntity.class))).thenAnswer(invocation -> {
            EmployeeEntity employee = invocation.getArgument(0);
            ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
            return employee;
        });
        when(users.save(any(UserAccountEntity.class))).thenAnswer(invocation -> {
            UserAccountEntity user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
            return user;
        });
    }

    @Test
    void createsEmployeeWithPasswordUserAndPayrollFields() {
        EmployeeResponse response = service.createEmployee(new CreateEmployeeCommand(
                "EMP001",
                "Season Worker",
                "secret123",
                "+383 44 000 000",
                null,
                "Decorator",
                "Operations",
                "Seasonal",
                "Reliable for early shifts",
                WageType.HOURLY,
                BigDecimal.valueOf(5.5),
                BigDecimal.valueOf(1.75)
        ));

        assertThat(response.id()).isNotBlank();
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.wageType()).isEqualTo("HOURLY");
        assertThat(response.overtimeMultiplier()).isEqualByComparingTo("1.75");

        ArgumentCaptor<EmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EmployeeEntity.class);
        org.mockito.Mockito.verify(employees).save(employeeCaptor.capture());
        assertThat(employeeCaptor.getValue().getUserAccount()).isNotNull();
        assertThat(employeeCaptor.getValue().getUserAccount().getEmail()).isEqualTo("emp001");
        assertThat(employeeCaptor.getValue().getUserAccount().isPasswordMustChange()).isTrue();
        assertThat(passwordEncoder.matches("secret123", employeeCaptor.getValue().getUserAccount().getPasswordHash())).isTrue();
    }

    @Test
    void resetsEmployeePasswordAndRequiresChange() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("legacy");
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));

        PasswordResetResponse response = service.resetEmployeePassword(employeeId);

        assertThat(response.temporaryPassword()).hasSize(12);
        assertThat(response.passwordMustChange()).isTrue();
        assertThat(employee.getUserAccount()).isNotNull();
        assertThat(employee.getUserAccount().isPasswordMustChange()).isTrue();
        assertThat(passwordEncoder.matches(response.temporaryPassword(), employee.getUserAccount().getPasswordHash())).isTrue();
    }

    @Test
    void rejectsDuplicateEmployeeCode() {
        when(employees.existsByEmployeeCodeIgnoreCase("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> service.createEmployee(new CreateEmployeeCommand(
                "EMP001",
                "Season Worker",
                "secret123",
                null,
                null,
                null,
                null,
                null,
                null,
                WageType.HOURLY,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1.5)
        ))).isInstanceOf(ManagementException.class);
    }

    @Test
    void rejectsMissingCreatePasswordBeforeEncoding() {
        assertThatThrownBy(() -> service.createEmployee(new CreateEmployeeCommand(
                "EMP002",
                "New Worker",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                WageType.HOURLY,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1.5)
        )))
                .isInstanceOf(ManagementException.class)
                .hasMessage("Password is required.");
    }

    @Test
    void rejectsShortCreatePasswordBeforeEncoding() {
        assertThatThrownBy(() -> service.createEmployee(new CreateEmployeeCommand(
                "EMP003",
                "New Worker",
                "short",
                null,
                null,
                null,
                null,
                null,
                null,
                WageType.HOURLY,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1.5)
        )))
                .isInstanceOf(ManagementException.class)
                .hasMessage("Password must contain at least 8 characters.");
    }
}
