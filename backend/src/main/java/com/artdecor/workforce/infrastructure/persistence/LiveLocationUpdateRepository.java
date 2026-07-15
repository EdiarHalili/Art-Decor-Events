package com.artdecor.workforce.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiveLocationUpdateRepository extends JpaRepository<LiveLocationUpdateEntity, UUID> {
    boolean existsByAttendanceRecordId(UUID attendanceRecordId);

    boolean existsByEmployeeId(UUID employeeId);

    Optional<LiveLocationUpdateEntity> findTopByAttendanceRecordIdOrderByCapturedAtDesc(UUID attendanceRecordId);

    @Query("""
            select update
            from LiveLocationUpdateEntity update
            join fetch update.attendanceRecord record
            join fetch update.employee employee
            join fetch update.schedule schedule
            where update.attendanceRecord.id in :attendanceRecordIds
              and update.capturedAt = (
                  select max(innerUpdate.capturedAt)
                  from LiveLocationUpdateEntity innerUpdate
                  where innerUpdate.attendanceRecord.id = update.attendanceRecord.id
              )
            """)
    List<LiveLocationUpdateEntity> findLatestForAttendanceRecords(@Param("attendanceRecordIds") Collection<UUID> attendanceRecordIds);
}
