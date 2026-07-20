package com.artdecor.workforce.infrastructure.security;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenService tokenService;
    private final UserAccountRepository users;
    private final EmployeeRepository employees;

    public JwtAuthenticationFilter(
            JwtTokenService tokenService,
            UserAccountRepository users,
            EmployeeRepository employees
    ) {
        this.tokenService = tokenService;
        this.users = users;
        this.employees = employees;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            authenticate(authorization.substring(7));
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            Claims claims = tokenService.parseClaims(token);
            UserRole role = UserRole.valueOf(claims.get("role", String.class));
            Integer tokenVersion = claims.get("tokenVersion", Integer.class);
            if (tokenVersion == null) {
                throw new IllegalArgumentException("Token version is missing.");
            }
            UUID employeeId = claims.get("employeeId", String.class) == null
                    ? null
                    : UUID.fromString(claims.get("employeeId", String.class));
            UUID userId = UUID.fromString(claims.getSubject());
            var user = users.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user no longer exists."));
            if (user.getStatus() != UserStatus.ACTIVE
                    || user.getRole() != role
                    || user.getTokenVersion() != tokenVersion) {
                throw new IllegalArgumentException("Token is no longer valid.");
            }
            if (role == UserRole.EMPLOYEE) {
                if (employeeId == null) {
                    throw new IllegalArgumentException("Employee token is missing employee id.");
                }
                if (!employees.existsByIdAndStatusAndUserAccountId(employeeId, UserStatus.ACTIVE, userId)) {
                    throw new IllegalArgumentException("Employee token is no longer valid.");
                }
            }
            AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                    userId,
                    role,
                    employeeId
            );
            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (RuntimeException ignored) {
            SecurityContextHolder.clearContext();
        }
    }
}
