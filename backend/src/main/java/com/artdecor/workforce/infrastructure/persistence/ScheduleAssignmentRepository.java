package com.artdecor.workforce.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignmentEntity, UUID> {
    Optional<ScheduleAssignmentEntity> findFirstByEmployeeIdAndScheduleWorkDateOrderByCreatedAtAsc(
            UUID employeeId,
            LocalDate workDate
    );
}

