package com.artdecor.workforce.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "payroll_periods")
public class PayrollPeriodEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private LocalDate periodMonth;
    private String status = "DRAFT";
    private Instant createdAt = Instant.now();
    private Instant updatedAt;

    public UUID getId() { return id; }
    public LocalDate getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(LocalDate periodMonth) { this.periodMonth = periodMonth; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
