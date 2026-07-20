package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.WageType;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.AuditLogEntity;
import com.artdecor.workforce.infrastructure.persistence.AuditLogRepository;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateRepository;
import com.artdecor.workforce.infrastructure.persistence.PayrollEmployeeSummaryRepository;
import com.artdecor.workforce.infrastructure.persistence.PushSubscriptionRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeManagementService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#";

    private final EmployeeRepository employees;
    private final UserAccountRepository users;
    private final AttendanceRecordRepository attendanceRecords;
    private final LiveLocationUpdateRepository liveLocations;
    private final ScheduleAssignmentRepository assignments;
    private final AuditLogRepository auditLogs;
    private final PushSubscriptionRepository pushSubscriptions;
    private final PayrollEmployeeSummaryRepository payrollSummaries;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public EmployeeManagementService(
            EmployeeRepository employees,
            UserAccountRepository users,
            AttendanceRecordRepository attendanceRecords,
            LiveLocationUpdateRepository liveLocations,
            ScheduleAssignmentRepository assignments,
            AuditLogRepository auditLogs,
            PushSubscriptionRepository pushSubscriptions,
            PayrollEmployeeSummaryRepository payrollSummaries,
            PasswordEncoder passwordEncoder
    ) {
        this.employees = employees;
        this.users = users;
        this.attendanceRecords = attendanceRecords;
        this.liveLocations = liveLocations;
        this.assignments = assignments;
        this.auditLogs = auditLogs;
        this.pushSubscriptions = pushSubscriptions;
        this.payrollSummaries = payrollSummaries;
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
            user.incrementTokenVersion();
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
            employee.getUserAccount().incrementTokenVersion();
        }
        return toResponse(employee);
    }

    @Transactional
    public PasswordResetResponse resetEmployeePassword(UUID employeeId) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementNotFoundException("Punëtori nuk u gjet."));
        String temporaryPassword = generateTemporaryPassword();
        UserAccountEntity user = ensureEmployeeUser(employee, temporaryPassword, true);
        user.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        user.setPasswordMustChange(true);
        user.incrementTokenVersion();
        employee.setUserAccount(user);
        return new PasswordResetResponse(temporaryPassword, true);
    }

    @Transactional
    public void deleteEmployee(UUID employeeId, AuthenticatedPrincipal principal) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementNotFoundException("Punëtori nuk u gjet."));
        UserAccountEntity user = employee.getUserAccount();
        if (user != null && principal != null && user.getId().equals(principal.userId())) {
            throw new ManagementConflictException("Nuk mund ta fshini llogarinë tuaj.");
        }
        if (hasHistoricalDependencies(employee, user)) {
            throw new ManagementConflictException("Ky punëtor ka histori pune dhe nuk mund të fshihet përgjithmonë. Çaktivizojeni për të ruajtur raportet dhe të dhënat historike.");
        }

        employee.setUserAccount(null);
        employees.delete(employee);
        if (user != null) {
            employees.flush();
            users.delete(user);
        }
    }

    @Transactional
    public void forceDeleteEmployee(UUID employeeId, AuthenticatedPrincipal principal) {
        EmployeeEntity employee = employees.findById(employeeId)
                .orElseThrow(() -> new ManagementNotFoundException("Punëtori nuk u gjet."));
        UserAccountEntity user = employee.getUserAccount();
        if (user != null && principal != null && user.getId().equals(principal.userId())) {
            throw new ManagementConflictException("Nuk mund ta fshini llogarinë tuaj.");
        }
        if (user != null && user.getRole() == UserRole.ADMINISTRATOR) {
            throw new ManagementConflictException("Llogaria e administratorit nuk mund të fshihet me këtë veprim.");
        }

        logForceDelete(principal, employee);

        UUID employeeIdValue = employee.getId();
        liveLocations.deleteByEmployeeId(employeeIdValue);
        attendanceRecords.deleteByEmployeeId(employeeIdValue);
        assignments.deleteByEmployeeId(employeeIdValue);
        payrollSummaries.deleteByEmployeeId(employeeIdValue);
        pushSubscriptions.deleteByEmployeeId(employeeIdValue);
        auditLogs.deleteByActorEmployeeId(employeeIdValue);
        if (user != null) {
            pushSubscriptions.deleteByUserId(user.getId());
            auditLogs.deleteByActorUserId(user.getId());
        }

        employee.setUserAccount(null);
        employees.delete(employee);
        employees.flush();
        if (user != null) {
            users.delete(user);
        }
    }

    private boolean hasHistoricalDependencies(EmployeeEntity employee, UserAccountEntity user) {
        UUID employeeId = employee.getId();
        if (attendanceRecords.existsByEmployeeId(employeeId)
                || liveLocations.existsByEmployeeId(employeeId)
                || assignments.existsByEmployeeId(employeeId)
                || auditLogs.existsByActorEmployeeId(employeeId)
                || pushSubscriptions.existsByEmployeeId(employeeId)
                || payrollSummaries.existsByEmployeeId(employeeId)) {
            return true;
        }
        return user != null && (auditLogs.existsByActorUserId(user.getId()) || pushSubscriptions.existsByUserId(user.getId()));
    }

    private void logForceDelete(AuthenticatedPrincipal principal, EmployeeEntity employee) {
        AuditLogEntity log = new AuditLogEntity();
        if (principal != null) {
            if (principal.employeeId() != null) {
                log.setActorEmployeeId(principal.employeeId());
            } else {
                log.setActorUserId(principal.userId());
            }
        }
        log.setAction("EMPLOYEE_FORCE_DELETED");
        log.setEntityType("EMPLOYEE");
        log.setEntityId(employee.getId());
        log.setMetadata(Map.of("employeeCode", employee.getEmployeeCode()));
        auditLogs.save(log);
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
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new ManagementException("Password must contain 128 characters or fewer.");
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
