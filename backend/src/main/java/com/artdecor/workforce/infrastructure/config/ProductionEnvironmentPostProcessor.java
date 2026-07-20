package com.artdecor.workforce.infrastructure.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;

public class ProductionEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String DEVELOPMENT_JWT_SECRET = "change-this-development-secret-to-a-very-long-production-value";
    private static final String DEVELOPMENT_DB_PASSWORD = "artdecor_dev_password";
    private static final String DEVELOPMENT_ADMIN_PASSWORD = "ChangeMe123!";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!acceptsProfile(environment, "prod")) {
            return;
        }

        requireExplicit(environment, "DB_URL");
        requireExplicit(environment, "DB_USERNAME");
        String dbPassword = requireExplicit(environment, "DB_PASSWORD");
        if (DEVELOPMENT_DB_PASSWORD.equals(dbPassword)) {
            fail("DB_PASSWORD must not use the local development password in production.");
        }

        String jwtSecret = requireExplicit(environment, "JWT_SECRET");
        if (jwtSecret.length() < 48 || DEVELOPMENT_JWT_SECRET.equals(jwtSecret)) {
            fail("JWT_SECRET must be an explicit production secret with at least 48 characters.");
        }

        String allowedOrigins = requireExplicit(environment, "CORS_ALLOWED_ORIGINS");
        validateExactCorsOrigins(allowedOrigins);

        String allowedOriginPatterns = environment.getProperty("CORS_ALLOWED_ORIGIN_PATTERNS", "");
        if (!allowedOriginPatterns.isBlank()) {
            fail("CORS_ALLOWED_ORIGIN_PATTERNS must be empty in production.");
        }

        String bootstrapEnabled = requireExplicit(environment, "APP_BOOTSTRAP_ENABLED");
        if ("true".equalsIgnoreCase(bootstrapEnabled)) {
            requireExplicit(environment, "APP_BOOTSTRAP_ADMIN_EMAIL");
            String adminPassword = requireExplicit(environment, "APP_BOOTSTRAP_ADMIN_PASSWORD");
            if (adminPassword.length() < 12 || DEVELOPMENT_ADMIN_PASSWORD.equals(adminPassword)) {
                fail("APP_BOOTSTRAP_ADMIN_PASSWORD must be a strong non-development password in production.");
            }
        }

        if ("true".equalsIgnoreCase(environment.getProperty("ARTDECOR_GPS_DEBUG", "false"))) {
            fail("ARTDECOR_GPS_DEBUG must not be enabled in production.");
        }
    }

    private boolean acceptsProfile(ConfigurableEnvironment environment, String profile) {
        return Arrays.asList(environment.getActiveProfiles()).contains(profile);
    }

    private String requireExplicit(ConfigurableEnvironment environment, String name) {
        String value = environment.getProperty(name);
        if (value == null || value.isBlank()) {
            fail(name + " must be explicitly set for the production profile.");
        }
        return value.trim();
    }

    private void validateExactCorsOrigins(String origins) {
        List<String> values = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        if (values.isEmpty()) {
            fail("CORS_ALLOWED_ORIGINS must include at least one exact HTTPS frontend origin.");
        }
        for (String origin : values) {
            if (!origin.startsWith("https://")
                    || origin.contains("*")
                    || origin.contains("localhost")
                    || origin.contains("127.0.0.1")
                    || origin.contains("192.168.")
                    || origin.contains("10.")) {
                fail("CORS_ALLOWED_ORIGINS must contain only exact production HTTPS origins.");
            }
        }
    }

    private static void fail(String message) {
        throw new IllegalStateException("Production configuration error: " + message);
    }
}
