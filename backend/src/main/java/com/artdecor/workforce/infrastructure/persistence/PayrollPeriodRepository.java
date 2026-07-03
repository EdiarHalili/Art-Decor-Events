package com.artdecor.workforce.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriodEntity, UUID> {
    Optional<PayrollPeriodEntity> findByPeriodMonth(LocalDate periodMonth);
}
