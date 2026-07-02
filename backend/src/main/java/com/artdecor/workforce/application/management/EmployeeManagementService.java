package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.domain.WageType;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementService {
    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;

    public EmployeeManagementService(EmployeeRepository employees, PasswordEncoder passwordEncoder) {
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> listEmployees() {
        return employees.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public EmployeeResponse createEmployee(CreateEmployeeCommand command) {
        if (employees.existsByEmployeeCodeIgnoreCase(command.employeeCode())) {
            throw new ManagementException("Employee ID already exists.");
        }

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeCode(command.employeeCode().trim());
        employee.setFullName(command.fullName().trim());
        employee.setPinHash(passwordEncoder.encode(command.pin()));
        applyEditableFields(employee, command.phone(), command.profilePhotoUrl(), command.notes(),
                command.wageType(), command.baseWage(), command.overtimeMultiplier());

        return toResponse(employees.save(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID employeeId, UpdateEmployeeCommand command) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementException("Employee not found."));

        employee.setFullName(command.fullName().trim());
        applyEditableFields(employee, command.phone(), command.profilePhotoUrl(), command.notes(),
                command.wageType(), command.baseWage(), command.overtimeMultiplier());

        if (command.pin() != null && !command.pin().isBlank()) {
            employee.setPinHash(passwordEncoder.encode(command.pin()));
        }

        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse deactivateEmployee(UUID employeeId) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementException("Employee not found."));
        employee.setStatus(UserStatus.INACTIVE);
        return toResponse(employee);
    }

    private void applyEditableFields(
            EmployeeEntity employee,
            String phone,
            String profilePhotoUrl,
            String notes,
            WageType wageType,
            BigDecimal baseWage,
            BigDecimal overtimeMultiplier
    ) {
        employee.setPhone(blankToNull(phone));
        employee.setProfilePhotoUrl(blankToNull(profilePhotoUrl));
        employee.setNotes(blankToNull(notes));
        employee.setWageType(wageType == null ? WageType.HOURLY : wageType);
        employee.setBaseWage(baseWage == null ? BigDecimal.ZERO : baseWage);
        employee.setOvertimeMultiplier(overtimeMultiplier == null ? BigDecimal.valueOf(1.5) : overtimeMultiplier);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private EmployeeResponse toResponse(EmployeeEntity employee) {
        return new EmployeeResponse(
                employee.getId().toString(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getPhone(),
                employee.getProfilePhotoUrl(),
                employee.getNotes(),
                employee.getStatus().name(),
                employee.getWageType().name(),
                employee.getBaseWage(),
                employee.getOvertimeMultiplier(),
                employee.getCreatedAt(),
                employee.getUpdatedAt()
        );
    }
}

