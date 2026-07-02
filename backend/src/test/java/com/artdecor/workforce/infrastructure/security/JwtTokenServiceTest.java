package com.artdecor.workforce.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.artdecor.workforce.domain.UserRole;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtTokenServiceTest {
    @Test
    void issuesSignedTokenWithRoleAndEmployeeClaims() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "test-secret-key-that-is-long-enough-for-hmac-signing",
                "art-decor-test",
                30
        ));
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        String token = service.issueToken(userId, UserRole.EMPLOYEE, employeeId);
        Claims claims = service.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("EMPLOYEE");
        assertThat(claims.get("employeeId", String.class)).isEqualTo(employeeId.toString());
        assertThat(service.accessTokenSeconds()).isEqualTo(1800);
    }
}

