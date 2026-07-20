package com.artdecor.workforce.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.mock.env.MockEnvironment;

class ProductionEnvironmentPostProcessorTest {
    private final ProductionEnvironmentPostProcessor processor = new ProductionEnvironmentPostProcessor();

    @Test
    void ignoresNonProductionProfiles() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsProductionStartupWithMissingRequiredSecrets() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DB_URL");
    }

    @Test
    void rejectsDevelopmentValuesInProduction() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("DB_PASSWORD", "artdecor_dev_password");

        assertThatThrownBy(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("local development password");
    }

    @Test
    void acceptsExplicitProductionConfiguration() {
        MockEnvironment environment = productionEnvironment();

        assertThatCode(() -> processor.postProcessEnvironment(environment, new SpringApplication()))
                .doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironment() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");
        return environment
                .withProperty("DB_URL", "jdbc:postgresql://db.example.com:5432/artdecor")
                .withProperty("DB_USERNAME", "artdecor_app")
                .withProperty("DB_PASSWORD", "very-strong-production-db-password")
                .withProperty("JWT_SECRET", "a-production-jwt-secret-with-more-than-forty-eight-characters")
                .withProperty("CORS_ALLOWED_ORIGINS", "https://workforce.example.com")
                .withProperty("CORS_ALLOWED_ORIGIN_PATTERNS", "")
                .withProperty("APP_BOOTSTRAP_ENABLED", "false");
    }
}
