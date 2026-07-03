package com.artdecor.workforce.application.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.WageType;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class EmployeeManagementServiceTest {
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private EmployeeManagementService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeManagementService(employees, passwordEncoder);
        when(employees.save(any(EmployeeEntity.class))).thenAnswer(invocation -> {
            EmployeeEntity employee = invocation.getArgument(0);
            ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
            return employee;
        });
    }

    @Test
    void createsEmployeeWithHashedPinAndPayrollFields() {
        EmployeeResponse response = service.createEmployee(new CreateEmployeeCommand(
                "EMP001",
                "Season Worker",
                "1234",
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
        assertThat(employeeCaptor.getValue().getPinHash()).isNotEqualTo("1234");
        assertThat(passwordEncoder.matches("1234", employeeCaptor.getValue().getPinHash())).isTrue();
    }

    @Test
    void rejectsDuplicateEmployeeCode() {
        when(employees.existsByEmployeeCodeIgnoreCase("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> service.createEmployee(new CreateEmployeeCommand(
                "EMP001",
                "Season Worker",
                "1234",
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
}
