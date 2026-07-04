package com.artdecor.workforce.application.management;

import com.artdecor.workforce.domain.WageType;
import java.math.BigDecimal;

public record UpdateEmployeeCommand(
        String fullName,
        String password,
        String phone,
        String profilePhotoUrl,
        String positionTitle,
        String departmentName,
        String teamName,
        String notes,
        WageType wageType,
        BigDecimal baseWage,
        BigDecimal overtimeMultiplier
) {
}
