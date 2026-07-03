package com.artdecor.workforce.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payroll_employee_summaries")
public class PayrollEmployeeSummaryEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_period_id")
    private PayrollPeriodEntity payrollPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private EmployeeEntity employee;

    private String wageType;
    private BigDecimal baseWage = BigDecimal.ZERO;
    private int workedMinutes;
    private int overtimeMinutes;
    private BigDecimal regularAmount = BigDecimal.ZERO;
    private BigDecimal overtimeAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
}
