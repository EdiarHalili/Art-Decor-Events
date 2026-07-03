package com.artdecor.workforce.application.payroll;

import java.math.BigDecimal;

public record PayrollPreparationEmployeeResponse(
        String employeeId,
        String employeeCode,
        String employeeName,
        String wageType,
        BigDecimal baseWage,
        BigDecimal overtimeMultiplier,
        int workedMinutes,
        int overtimeMinutes
) {
}
