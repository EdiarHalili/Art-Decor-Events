package com.artdecor.workforce.application.auth;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import com.artdecor.workforce.infrastructure.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;
    private static final String INVALID_CREDENTIALS = "Të dhënat e identifikimit nuk janë të sakta.";

    private final UserAccountRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokens;
    private final AuditService audit;

    public AuthService(
            UserAccountRepository users,
            EmployeeRepository employees,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokens,
            AuditService audit
    ) {
        this.users = users;
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
        this.tokens = tokens;
        this.audit = audit;
    }

    @Transactional
    public AuthResponse loginAdmin(String email, String password) {
        var user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS));

        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() == UserRole.EMPLOYEE) {
            throw new AuthException(INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(INVALID_CREDENTIALS);
        }

        audit.system("ADMIN_LOGIN", "USER", user.getId());
        return new AuthResponse(
                tokens.issueToken(user.getId(), user.getRole(), null, user.getTokenVersion()),
                "Bearer",
                tokens.accessTokenSeconds(),
                user.getRole().name(),
                user.getFullName(),
                null,
                null,
                user.isPasswordMustChange()
        );
    }

    @Transactional
    public AuthResponse loginEmployee(String employeeCode, String password) {
        var employee = employees.findByEmployeeCodeIgnoreCase(employeeCode)
                .orElseThrow(() -> new AuthException(INVALID_CREDENTIALS));
        UserAccountEntity user = employee.getUserAccount();

        if (employee.getStatus() != UserStatus.ACTIVE
                || user == null
                || user.getStatus() != UserStatus.ACTIVE
                || user.getRole() != UserRole.EMPLOYEE) {
            throw new AuthException(INVALID_CREDENTIALS);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException(INVALID_CREDENTIALS);
        }

        audit.system("EMPLOYEE_LOGIN", "EMPLOYEE", employee.getId());
        return new AuthResponse(
                tokens.issueToken(user.getId(), UserRole.EMPLOYEE, employee.getId(), user.getTokenVersion()),
                "Bearer",
                tokens.accessTokenSeconds(),
                UserRole.EMPLOYEE.name(),
                employee.getFullName(),
                employee.getId().toString(),
                employee.getEmployeeCode(),
                user.isPasswordMustChange()
        );
    }

    @Transactional
    public AuthResponse changePassword(AuthenticatedPrincipal principal, String currentPassword, String newPassword) {
        validatePassword(newPassword);
        UserAccountEntity user = users.findById(principal.userId())
                .orElseThrow(() -> new AuthException("Authenticated user no longer exists."));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordMustChange(false);
        user.incrementTokenVersion();
        audit.log(principal, "PASSWORD_CHANGED", "USER", user.getId());

        String fullName = user.getFullName();
        String employeeId = null;
        String employeeCode = null;
        if (principal.employeeId() != null) {
            var employee = employees.findById(principal.employeeId())
                    .orElseThrow(() -> new AuthException("Authenticated employee no longer exists."));
            fullName = employee.getFullName();
            employeeId = employee.getId().toString();
            employeeCode = employee.getEmployeeCode();
        }

        return new AuthResponse(
                tokens.issueToken(user.getId(), user.getRole(), principal.employeeId(), user.getTokenVersion()),
                "Bearer",
                tokens.accessTokenSeconds(),
                user.getRole().name(),
                fullName,
                employeeId,
                employeeCode,
                false
        );
    }

    public CurrentUserResponse currentUser(AuthenticatedPrincipal principal) {
        if (principal.role() == UserRole.EMPLOYEE) {
            var employee = employees.findById(principal.employeeId())
                    .orElseThrow(() -> new AuthException("Authenticated employee no longer exists."));
            UserAccountEntity user = users.findById(principal.userId())
                    .orElseThrow(() -> new AuthException("Authenticated user no longer exists."));

            return new CurrentUserResponse(
                    principal.userId().toString(),
                    principal.role().name(),
                    employee.getFullName(),
                    employee.getId().toString(),
                    employee.getEmployeeCode(),
                    user.isPasswordMustChange()
            );
        }

        var user = users.findById(principal.userId())
                .orElseThrow(() -> new AuthException("Authenticated user no longer exists."));

        return new CurrentUserResponse(
                principal.userId().toString(),
                principal.role().name(),
                user.getFullName(),
                null,
                null,
                user.isPasswordMustChange()
        );
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("Password must be at least 8 characters.");
        }
        if (password.length() > MAX_PASSWORD_LENGTH) {
            throw new AuthException("Password must contain 128 characters or fewer.");
        }
    }
}
