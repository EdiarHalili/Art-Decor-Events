package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.WageType;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";

    private final EmployeeRepository employees;
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeManagementService(
            EmployeeRepository employees,
            UserAccountRepository users,
            PasswordEncoder passwordEncoder
    ) {
        this.employees = employees;
        this.users = users;
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
        validatePassword(command.password());
        employee.setPinHash(passwordEncoder.encode("0000"));
        employee.setUserAccount(createEmployeeUser(employee.getEmployeeCode(), employee.getFullName(), command.password(), true));
        applyEditableFields(employee, command.phone(), command.profilePhotoUrl(), command.positionTitle(),
                command.departmentName(), command.teamName(), command.notes(),
                command.wageType(), command.baseWage(), command.overtimeMultiplier());

        return toResponse(employees.save(employee));
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID employeeId, UpdateEmployeeCommand command) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementException("Employee not found."));

        employee.setFullName(command.fullName().trim());
        if (employee.getUserAccount() != null) {
            employee.getUserAccount().setFullName(command.fullName().trim());
        }
        applyEditableFields(employee, command.phone(), command.profilePhotoUrl(), command.positionTitle(),
                command.departmentName(), command.teamName(), command.notes(),
                command.wageType(), command.baseWage(), command.overtimeMultiplier());

        if (command.password() != null && !command.password().isBlank()) {
            validatePassword(command.password());
            UserAccountEntity user = ensureEmployeeUser(employee, command.password(), true);
            employee.setUserAccount(user);
        }

        return toResponse(employee);
    }

    @Transactional
    public EmployeeResponse deactivateEmployee(UUID employeeId) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementException("Employee not found."));
        employee.setStatus(UserStatus.INACTIVE);
        if (employee.getUserAccount() != null) {
            employee.getUserAccount().setStatus(UserStatus.INACTIVE);
        }
        return toResponse(employee);
    }

    @Transactional
    public PasswordResetResponse resetEmployeePassword(UUID employeeId) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementException("Employee not found."));
        String temporaryPassword = generateTemporaryPassword();
        UserAccountEntity user = ensureEmployeeUser(employee, temporaryPassword, true);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordMustChange(true);
        employee.setUserAccount(user);
        return new PasswordResetResponse(temporaryPassword, true);
    }

    private UserAccountEntity createEmployeeUser(
            String employeeCode,
            String fullName,
            String password,
            boolean passwordMustChange
    ) {
        validatePassword(password);
        String username = employeeCode.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(username)) {
            throw new ManagementException("Employee username already exists.");
        }

        UserAccountEntity user = new UserAccountEntity();
        user.setFullName(fullName.trim());
        user.setEmail(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setPasswordMustChange(passwordMustChange);
        user.setRole(UserRole.EMPLOYEE);
        return users.save(user);
    }

    private UserAccountEntity ensureEmployeeUser(EmployeeEntity employee, String password, boolean passwordMustChange) {
        validatePassword(password);
        if (employee.getUserAccount() != null) {
            employee.getUserAccount().setFullName(employee.getFullName());
            employee.getUserAccount().setEmail(employee.getEmployeeCode().trim().toLowerCase());
            employee.getUserAccount().setRole(UserRole.EMPLOYEE);
            employee.getUserAccount().setStatus(employee.getStatus());
            employee.getUserAccount().setPasswordHash(passwordEncoder.encode(password));
            employee.getUserAccount().setPasswordMustChange(passwordMustChange);
            return employee.getUserAccount();
        }
        return createEmployeeUser(employee.getEmployeeCode(), employee.getFullName(), password, passwordMustChange);
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ManagementException("Password is required.");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new ManagementException("Password must contain at least 8 characters.");
        }
    }

    private String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder();
        for (int index = 0; index < 12; index++) {
            password.append(TEMP_PASSWORD_ALPHABET.charAt(secureRandom.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private void applyEditableFields(
            EmployeeEntity employee,
            String phone,
            String profilePhotoUrl,
            String positionTitle,
            String departmentName,
            String teamName,
            String notes,
            WageType wageType,
            BigDecimal baseWage,
            BigDecimal overtimeMultiplier
    ) {
        employee.setPhone(blankToNull(phone));
        employee.setProfilePhotoUrl(blankToNull(profilePhotoUrl));
        employee.setPositionTitle(blankToNull(positionTitle));
        employee.setDepartmentName(blankToNull(departmentName));
        employee.setTeamName(blankToNull(teamName));
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
                employee.getPositionTitle(),
                employee.getDepartmentName(),
                employee.getTeamName(),
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
