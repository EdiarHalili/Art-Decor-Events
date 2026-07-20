package com.artdecor.workforce.infrastructure.security;

import com.artdecor.workforce.domain.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(UUID subject, UserRole role, UUID employeeId) {
        return issueToken(subject, role, employeeId, 0);
    }

    public String issueToken(UUID subject, UserRole role, UUID employeeId, int tokenVersion) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(properties.accessTokenMinutes() * 60);

        var builder = Jwts.builder()
                .issuer(properties.jwtIssuer())
                .subject(subject.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("role", role.name())
                .claim("tokenVersion", tokenVersion)
                .signWith(key);

        if (employeeId != null) {
            builder.claim("employeeId", employeeId.toString());
        }

        return builder.compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(properties.jwtIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long accessTokenSeconds() {
        return properties.accessTokenMinutes() * 60;
    }
}
