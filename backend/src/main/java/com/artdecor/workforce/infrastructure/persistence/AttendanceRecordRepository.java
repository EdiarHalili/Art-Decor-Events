package com.artdecor.workforce.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, UUID> {
    Optional<AttendanceRecordEntity> findByScheduleIdAndEmployeeId(UUID scheduleId, UUID employeeId);
}

