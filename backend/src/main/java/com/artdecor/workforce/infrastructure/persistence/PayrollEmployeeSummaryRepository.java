package com.artdecor.workforce.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollEmployeeSummaryRepository extends JpaRepository<PayrollEmployeeSummaryEntity, UUID> {
    boolean existsByEmployeeId(UUID employeeId);

    void deleteByEmployeeId(UUID employeeId);
}
