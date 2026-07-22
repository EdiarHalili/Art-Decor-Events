package com.artdecor.workforce.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.artdecor.workforce.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class JwtTokenServiceTest {
    private static final long THIRTY_DAYS_IN_SECONDS = 2_592_000;
    private static final long THIRTY_DAYS_IN_MINUTES = 43_200;

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
        assertThat(claims.get("tokenVersion", Integer.class)).isZero();
        assertThat(service.accessTokenSeconds()).isEqualTo(1800);
    }

    @Test
    void applicationConfigurationSetsAccessTokenLifetimeToThirtyDays() throws IOException {
        var loader = new YamlPropertySourceLoader();
        var propertySources = loader.load("application", new ClassPathResource("application.yml"));

        Object configuredMinutes = propertySources.stream()
                .map(source -> source.getProperty("app.security.access-token-minutes"))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow();

        assertThat(configuredMinutes).isEqualTo((int) THIRTY_DAYS_IN_MINUTES);
    }

    @Test
    void issuesTokenThatExpiresExactlyThirtyDaysAfterIssueTime() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "test-secret-key-that-is-long-enough-for-hmac-signing",
                "art-decor-test",
                THIRTY_DAYS_IN_MINUTES
        ));

        String token = service.issueToken(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);
        Claims claims = service.parseClaims(token);

        long lifetimeSeconds = Duration.between(
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        ).toSeconds();
        assertThat(service.accessTokenSeconds()).isEqualTo(THIRTY_DAYS_IN_SECONDS);
        assertThat(lifetimeSeconds).isEqualTo(THIRTY_DAYS_IN_SECONDS);
    }

    @Test
    void rejectsExpiredTokens() {
        JwtTokenService service = new JwtTokenService(new JwtProperties(
                "test-secret-key-that-is-long-enough-for-hmac-signing",
                "art-decor-test",
                -1
        ));

        String token = service.issueToken(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);

        assertThatThrownBy(() -> service.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
