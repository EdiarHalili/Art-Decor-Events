package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.WageType;
import java.math.BigDecimal;

public record UpdateEmployeeCommand(
        String fullName,
        String pin,
        String phone,
        String profilePhotoUrl,
        String notes,
        WageType wageType,
        BigDecimal baseWage,
        BigDecimal overtimeMultiplier
) {
}

