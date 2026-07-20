package com.artdecor.workforce.application.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.WageType;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.AnnouncementRepository;
import com.artdecor.workforce.infrastructure.persistence.AuditLogRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateRepository;
import com.artdecor.workforce.infrastructure.persistence.PayrollEmployeeSummaryRepository;
import com.artdecor.workforce.infrastructure.persistence.PushSubscriptionRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
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
    private final AttendanceRecordRepository attendanceRecords = org.mockito.Mockito.mock(AttendanceRecordRepository.class);
    private final LiveLocationUpdateRepository liveLocations = org.mockito.Mockito.mock(LiveLocationUpdateRepository.class);
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final AuditLogRepository auditLogs = org.mockito.Mockito.mock(AuditLogRepository.class);
    private final AnnouncementRepository announcements = org.mockito.Mockito.mock(AnnouncementRepository.class);
    private final WorkScheduleRepository schedules = org.mockito.Mockito.mock(WorkScheduleRepository.class);
    private final PushSubscriptionRepository pushSubscriptions = org.mockito.Mockito.mock(PushSubscriptionRepository.class);
    private final PayrollEmployeeSummaryRepository payrollSummaries = org.mockito.Mockito.mock(PayrollEmployeeSummaryRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private EmployeeManagementService service;

    @BeforeEach
    void setUp() {
        service = service();
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

    private EmployeeManagementService service() {
        return new EmployeeManagementService(
                employees,
                users,
                attendanceRecords,
                liveLocations,
                assignments,
                auditLogs,
                announcements,
                schedules,
                pushSubscriptions,
                payrollSummaries,
                passwordEncoder
        );
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
    void createsEmployeeWithExactlyEightCharacterPassword() {
        service.createEmployee(new CreateEmployeeCommand(
                "EMP008",
                "Eight Worker",
                "Pass1234",
                null,
                null,
                null,
                null,
                null,
                null,
                WageType.HOURLY,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1.5)
        ));

        ArgumentCaptor<EmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EmployeeEntity.class);
        org.mockito.Mockito.verify(employees).save(employeeCaptor.capture());
        String hash = employeeCaptor.getValue().getUserAccount().getPasswordHash();
        assertThat(hash).isNotEqualTo("Pass1234");
        assertThat(passwordEncoder.matches("Pass1234", hash)).isTrue();
    }

    @Test
    void createsEmployeeWithLongPasswordWithoutTruncating() {
        String longPassword = "SeasonWorkerPassword1234567890LongEnoughForRealUse";

        service.createEmployee(new CreateEmployeeCommand(
                "EMP064",
                "Long Password Worker",
                longPassword,
                null,
                null,
                null,
                null,
                null,
                null,
                WageType.HOURLY,
                BigDecimal.ZERO,
                BigDecimal.valueOf(1.5)
        ));

        ArgumentCaptor<EmployeeEntity> employeeCaptor = ArgumentCaptor.forClass(EmployeeEntity.class);
        org.mockito.Mockito.verify(employees).save(employeeCaptor.capture());
        String hash = employeeCaptor.getValue().getUserAccount().getPasswordHash();
        assertThat(hash).isNotEqualTo(longPassword);
        assertThat(hash).hasSizeGreaterThanOrEqualTo(50);
        assertThat(passwordEncoder.matches(longPassword, hash)).isTrue();
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
        assertThat(employee.getUserAccount().getPasswordHash()).isNotEqualTo(response.temporaryPassword());
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

    @Test
    void deletesEmployeeAndLoginUserWhenNoHistoryExists() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, userId);
        UserAccountEntity user = employee.getUserAccount();
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));

        service.deleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null));

        org.mockito.Mockito.verify(employees).delete(employee);
        org.mockito.Mockito.verify(employees).flush();
        org.mockito.Mockito.verify(users).delete(user);
        assertThat(employee.getUserAccount()).isNull();
    }

    @Test
    void rejectsPermanentDeletionWhenEmployeeHasAttendanceHistory() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, UUID.randomUUID());
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(attendanceRecords.existsByEmployeeId(employeeId)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(ManagementConflictException.class)
                .hasMessageContaining("histori pune");

        org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).delete(any(EmployeeEntity.class));
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).delete(any(UserAccountEntity.class));
        org.mockito.Mockito.verify(liveLocations, org.mockito.Mockito.never()).deleteAll();
    }

    @Test
    void rejectsPermanentDeletionWhenEmployeeHasLiveLocationHistory() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, UUID.randomUUID());
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(liveLocations.existsByEmployeeId(employeeId)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(ManagementConflictException.class);

        org.mockito.Mockito.verify(liveLocations, org.mockito.Mockito.never()).deleteAll();
        org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).delete(any(EmployeeEntity.class));
    }

    @Test
    void rejectsPermanentDeletionWhenEmployeeHasAssignmentsPayrollAuditOrPushReferences() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, UUID.randomUUID());
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(assignments.existsByEmployeeId(employeeId)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(ManagementConflictException.class);

        org.mockito.Mockito.verify(payrollSummaries, org.mockito.Mockito.never()).deleteAll();
        org.mockito.Mockito.verify(auditLogs, org.mockito.Mockito.never()).deleteAll();
        org.mockito.Mockito.verify(pushSubscriptions, org.mockito.Mockito.never()).deleteAll();
        org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).delete(any(EmployeeEntity.class));
    }

    @Test
    void rejectsPermanentDeletionWhenLoginUserHasHistoricalReferences() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, userId);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        when(announcements.existsByCreatedByUserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(ManagementConflictException.class)
                .hasMessageContaining("histori pune");

        org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).delete(any(EmployeeEntity.class));
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).delete(any(UserAccountEntity.class));
    }


    @Test
    void rejectsUnknownEmployeeDeletion() {
        UUID employeeId = UUID.randomUUID();
        when(employees.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(ManagementNotFoundException.class)
                .hasMessage("Punëtori nuk u gjet.");
    }

    @Test
    void rejectsDeletingOwnEmployeeLoginAccount() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, userId);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.deleteEmployee(employeeId, new AuthenticatedPrincipal(userId, UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(ManagementConflictException.class)
                .hasMessageContaining("llogarinë tuaj");
    }


    @Test
    void forceDeleteRemovesEmployeeWithAttendanceAndLocationHistory() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, userId);
        UserAccountEntity user = employee.getUserAccount();
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));

        service.forceDeleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null));

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(
                auditLogs,
                liveLocations,
                attendanceRecords,
                assignments,
                payrollSummaries,
                pushSubscriptions,
                schedules,
                announcements,
                employees,
                users
        );
        order.verify(auditLogs).save(any());
        order.verify(liveLocations).deleteByEmployeeId(employeeId);
        order.verify(attendanceRecords).deleteByEmployeeId(employeeId);
        order.verify(assignments).deleteByEmployeeId(employeeId);
        order.verify(payrollSummaries).deleteByEmployeeId(employeeId);
        order.verify(pushSubscriptions).deleteByEmployeeId(employeeId);
        order.verify(auditLogs).deleteByActorEmployeeId(employeeId);
        order.verify(attendanceRecords).clearApprovalByUserId(userId);
        order.verify(schedules).clearCreatedByUserId(userId);
        order.verify(schedules).clearSupervisorUserId(userId);
        order.verify(announcements).deleteByCreatedByUserId(userId);
        order.verify(pushSubscriptions).deleteByUserId(userId);
        order.verify(auditLogs).deleteByActorUserId(userId);
        order.verify(employees).delete(employee);
        order.verify(employees).flush();
        order.verify(users).delete(user);
        org.mockito.Mockito.verify(assignments, org.mockito.Mockito.never()).deleteByScheduleId(any());
        org.mockito.Mockito.verify(attendanceRecords, org.mockito.Mockito.never()).deleteByScheduleId(any());
    }

    @Test
    void forceDeleteRejectsAdminUserAccount() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, UUID.randomUUID(), UserRole.ADMINISTRATOR);
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> service.forceDeleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(ManagementConflictException.class)
                .hasMessageContaining("administratorit");

        org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).delete(any(EmployeeEntity.class));
    }

    @Test
    void forceDeleteDoesNotDeleteEmployeeWhenDependencyCleanupFails() {
        UUID employeeId = UUID.randomUUID();
        EmployeeEntity employee = employee(employeeId, UUID.randomUUID());
        when(employees.findById(employeeId)).thenReturn(Optional.of(employee));
        org.mockito.Mockito.doThrow(new RuntimeException("database failure"))
                .when(attendanceRecords).deleteByEmployeeId(employeeId);

        assertThatThrownBy(() -> service.forceDeleteEmployee(employeeId, new AuthenticatedPrincipal(UUID.randomUUID(), UserRole.ADMINISTRATOR, null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("database failure");

        org.mockito.Mockito.verify(liveLocations).deleteByEmployeeId(employeeId);
        org.mockito.Mockito.verify(employees, org.mockito.Mockito.never()).delete(any(EmployeeEntity.class));
        org.mockito.Mockito.verify(users, org.mockito.Mockito.never()).delete(any(UserAccountEntity.class));
    }

    private EmployeeEntity employee(UUID employeeId, UUID userId) {
        return employee(employeeId, userId, UserRole.EMPLOYEE);
    }

    private EmployeeEntity employee(UUID employeeId, UUID userId, UserRole role) {
        UserAccountEntity user = new UserAccountEntity();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setFullName("Worker");
        user.setEmail("emp001");
        user.setPasswordHash("hash");
        user.setRole(role);

        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", employeeId);
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Worker");
        employee.setPinHash("pin");
        employee.setUserAccount(user);
        return employee;
    }
}
