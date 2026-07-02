package com.artdecor.workforce.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record JwtProperties(
        String jwtSecret,
        String jwtIssuer,
        long accessTokenMinutes
) {
}

