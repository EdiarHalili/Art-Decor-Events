package com.artdecor.workforce.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.JwtProperties;
import com.artdecor.workforce.infrastructure.security.JwtTokenService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;

class AuthServiceTest {
    private final UserAccountRepository users = org.mockito.Mockito.mock(UserAccountRepository.class);
    private final EmployeeRepository employees = org.mockito.Mockito.mock(EmployeeRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    private final AuditService audit = org.mockito.Mockito.mock(AuditService.class);
    private final JwtTokenService tokenService = new JwtTokenService(new JwtProperties(
            "test-secret-key-that-is-long-enough-for-hmac-signing",
            "art-decor-test",
            60
    ));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, employees, passwordEncoder, tokenService, audit);
    }

    @Test
    void logsInAdministratorWithValidPassword() {
        UserAccountEntity admin = user("Owner", "owner@artdecor.test", "secret123", UserRole.ADMINISTRATOR);
        when(users.findByEmailIgnoreCase("owner@artdecor.test")).thenReturn(Optional.of(admin));

        AuthResponse response = authService.loginAdmin("owner@artdecor.test", "secret123");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo("ADMINISTRATOR");
        assertThat(response.fullName()).isEqualTo("Owner");
        assertThat(response.employeeId()).isNull();
    }

    @Test
    void rejectsEmployeeOnAdminLogin() {
        UserAccountEntity employeeUser = user("Employee", "employee@artdecor.test", "secret123", UserRole.EMPLOYEE);
        when(users.findByEmailIgnoreCase("employee@artdecor.test")).thenReturn(Optional.of(employeeUser));

        assertThatThrownBy(() -> authService.loginAdmin("employee@artdecor.test", "secret123"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void logsInEmployeeWithValidPin() {
        UserAccountEntity employeeUser = user("Season Worker", "emp001", "secret123", UserRole.EMPLOYEE);
        EmployeeEntity employee = employee("EMP001", "Season Worker", "1234");
        employee.setUserAccount(employeeUser);
        when(employees.findByEmployeeCodeIgnoreCase("EMP001")).thenReturn(Optional.of(employee));

        AuthResponse response = authService.loginEmployee("EMP001", "1234");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("EMPLOYEE");
        assertThat(response.fullName()).isEqualTo("Season Worker");
    }

    @Test
    void rejectsInvalidEmployeePin() {
        UserAccountEntity employeeUser = user("Season Worker", "emp001", "secret123", UserRole.EMPLOYEE);
        EmployeeEntity employee = employee("EMP001", "Season Worker", "1234");
        employee.setUserAccount(employeeUser);
        when(employees.findByEmployeeCodeIgnoreCase("EMP001")).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> authService.loginEmployee("EMP001", "wrongpin"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void logsInEmployeeWithTemporaryPasswordForFirstLoginCompatibility() {
        UserAccountEntity employeeUser = user("Season Worker", "emp001", "TempPass123", UserRole.EMPLOYEE);
        EmployeeEntity employee = employee("EMP001", "Season Worker", "1234");
        employee.setUserAccount(employeeUser);
        when(employees.findByEmployeeCodeIgnoreCase("EMP001")).thenReturn(Optional.of(employee));

        AuthResponse response = authService.loginEmployee("EMP001", "TempPass123");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.role()).isEqualTo("EMPLOYEE");
    }

    @Test
    void logsInEmployeeWithLongPasswordAndKeepsPinCompatibility() {
        String longPassword = "LongPasswordForSeasonalWorker1234567890";
        UserAccountEntity employeeUser = user("Season Worker", "emp001", longPassword, UserRole.EMPLOYEE);
        EmployeeEntity employee = employee("EMP001", "Season Worker", "123456789");
        employee.setUserAccount(employeeUser);
        when(employees.findByEmployeeCodeIgnoreCase("EMP001")).thenReturn(Optional.of(employee));

        AuthResponse passwordResponse = authService.loginEmployee("EMP001", longPassword);
        AuthResponse pinResponse = authService.loginEmployee("EMP001", "123456789");

        assertThat(passwordResponse.accessToken()).isNotBlank();
        assertThat(passwordResponse.employeeCode()).isEqualTo("EMP001");
        assertThat(pinResponse.accessToken()).isNotBlank();
    }

    @Test
    void changePasswordClearsFirstLoginFlag() {
        UserAccountEntity admin = user("Owner", "owner@artdecor.test", "secret123", UserRole.ADMINISTRATOR);
        admin.setPasswordMustChange(true);
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));

        AuthResponse response = authService.changePassword(
                new com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal(admin.getId(), UserRole.ADMINISTRATOR, null),
                "secret123",
                "newSecret123"
        );

        assertThat(response.passwordMustChange()).isFalse();
        assertThat(admin.isPasswordMustChange()).isFalse();
        assertThat(passwordEncoder.matches("newSecret123", admin.getPasswordHash())).isTrue();
    }

    @Test
    void changePasswordAcceptsExactlyEightAndLongPasswords() {
        UserAccountEntity employeeUser = user("Season Worker", "emp001", "TempPass123", UserRole.EMPLOYEE);
        employeeUser.setPasswordMustChange(true);
        EmployeeEntity employee = employee("EMP001", "Season Worker", "1234");
        employee.setUserAccount(employeeUser);
        when(users.findById(employeeUser.getId())).thenReturn(Optional.of(employeeUser));
        when(employees.findById(employee.getId())).thenReturn(Optional.of(employee));

        AuthResponse first = authService.changePassword(
                new com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal(employeeUser.getId(), UserRole.EMPLOYEE, employee.getId()),
                "TempPass123",
                "Pass1234"
        );
        assertThat(first.passwordMustChange()).isFalse();
        assertThat(passwordEncoder.matches("Pass1234", employeeUser.getPasswordHash())).isTrue();

        AuthResponse second = authService.changePassword(
                new com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal(employeeUser.getId(), UserRole.EMPLOYEE, employee.getId()),
                "Pass1234",
                "AnotherLongPasswordForEmployee1234567890"
        );
        assertThat(second.passwordMustChange()).isFalse();
        assertThat(passwordEncoder.matches("AnotherLongPasswordForEmployee1234567890", employeeUser.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsPasswordLongerThanMaximum() {
        UserAccountEntity admin = user("Owner", "owner@artdecor.test", "secret123", UserRole.ADMINISTRATOR);
        when(users.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.changePassword(
                new com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal(admin.getId(), UserRole.ADMINISTRATOR, null),
                "secret123",
                "x".repeat(129)
        )).isInstanceOf(AuthException.class)
                .hasMessage("Password must contain 128 characters or fewer.");
    }

    private UserAccountEntity user(String fullName, String email, String password, UserRole role) {
        UserAccountEntity user = new UserAccountEntity();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        return user;
    }

    private EmployeeEntity employee(String employeeCode, String fullName, String pin) {
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
        employee.setEmployeeCode(employeeCode);
        employee.setFullName(fullName);
        employee.setPinHash(passwordEncoder.encode(pin));
        return employee;
    }
}
