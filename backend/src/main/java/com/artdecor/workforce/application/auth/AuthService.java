package com.artdecor.workforce.application.auth;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
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

    public AuthResponse loginAdmin(String email, String password) {
        var user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AuthException("Invalid credentials."));

        if (user.getStatus() != UserStatus.ACTIVE || user.getRole() == UserRole.EMPLOYEE) {
            throw new AuthException("Invalid credentials.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException("Invalid credentials.");
        }

        audit.system("ADMIN_LOGIN", "USER", user.getId());
        return new AuthResponse(
                tokens.issueToken(user.getId(), user.getRole(), null),
                "Bearer",
                tokens.accessTokenSeconds(),
                user.getRole().name(),
                user.getFullName(),
                null
        );
    }

    public AuthResponse loginEmployee(String employeeCode, String pin) {
        var employee = employees.findByEmployeeCodeIgnoreCase(employeeCode)
                .orElseThrow(() -> new AuthException("Invalid credentials."));

        if (employee.getStatus() != UserStatus.ACTIVE || !passwordEncoder.matches(pin, employee.getPinHash())) {
            throw new AuthException("Invalid credentials.");
        }

        audit.system("EMPLOYEE_LOGIN", "EMPLOYEE", employee.getId());
        return new AuthResponse(
                tokens.issueToken(employee.getId(), UserRole.EMPLOYEE, employee.getId()),
                "Bearer",
                tokens.accessTokenSeconds(),
                UserRole.EMPLOYEE.name(),
                employee.getFullName(),
                employee.getId().toString()
        );
    }

    public CurrentUserResponse currentUser(AuthenticatedPrincipal principal) {
        if (principal.role() == UserRole.EMPLOYEE) {
            var employee = employees.findById(principal.userId())
                    .orElseThrow(() -> new AuthException("Authenticated employee no longer exists."));

            return new CurrentUserResponse(
                    principal.userId().toString(),
                    principal.role().name(),
                    employee.getFullName(),
                    employee.getId().toString()
            );
        }

        var user = users.findById(principal.userId())
                .orElseThrow(() -> new AuthException("Authenticated user no longer exists."));

        return new CurrentUserResponse(
                principal.userId().toString(),
                principal.role().name(),
                user.getFullName(),
                null
        );
    }
}
