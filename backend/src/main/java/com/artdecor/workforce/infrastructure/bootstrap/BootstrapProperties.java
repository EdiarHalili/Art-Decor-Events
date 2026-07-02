package com.artdecor.workforce.infrastructure.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record BootstrapProperties(
        boolean enabled,
        String adminName,
        String adminEmail,
        String adminPassword,
        String employeeName,
        String employeeCode,
        String employeePin
) {
}

