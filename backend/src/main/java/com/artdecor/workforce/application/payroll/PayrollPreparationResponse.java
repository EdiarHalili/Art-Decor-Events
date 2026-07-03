package com.artdecor.workforce.application.payroll;

import java.time.YearMonth;
import java.util.List;

public record PayrollPreparationResponse(
        YearMonth month,
        String status,
        List<PayrollPreparationEmployeeResponse> employees
) {
}
