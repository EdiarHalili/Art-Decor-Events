package com.artdecor.workforce.application.management;

import java.math.BigDecimal;
import java.time.Instant;

public record EmployeeResponse(
        String id,
        String employeeCode,
        String fullName,
        String phone,
        String profilePhotoUrl,
        String notes,
        String status,
        String wageType,
        BigDecimal baseWage,
        BigDecimal overtimeMultiplier,
        Instant createdAt,
        Instant updatedAt
) {
}

