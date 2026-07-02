package com.artdecor.workforce.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignmentEntity, UUID> {
    Optional<ScheduleAssignmentEntity> findFirstByEmployeeIdAndScheduleWorkDateOrderByCreatedAtAsc(
            UUID employeeId,
            LocalDate workDate
    );

    List<ScheduleAssignmentEntity> findAllByScheduleId(UUID scheduleId);

    void deleteByScheduleId(UUID scheduleId);
}
