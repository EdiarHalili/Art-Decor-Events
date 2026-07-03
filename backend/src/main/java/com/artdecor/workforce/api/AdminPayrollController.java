package com.artdecor.workforce.api;

import com.artdecor.workforce.application.payroll.PayrollPreparationResponse;
import com.artdecor.workforce.application.payroll.PayrollPreparationService;
import java.time.YearMonth;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payroll")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminPayrollController {
    private final PayrollPreparationService payroll;

    public AdminPayrollController(PayrollPreparationService payroll) {
        this.payroll = payroll;
    }

    @GetMapping("/preparation")
    public PayrollPreparationResponse preparation(@RequestParam String month) {
        return payroll.prepare(YearMonth.parse(month));
    }
}
