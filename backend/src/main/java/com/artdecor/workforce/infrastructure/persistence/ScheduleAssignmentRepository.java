package com.artdecor.workforce.infrastructure.persistence;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleAssignmentRepository extends JpaRepository<ScheduleAssignmentEntity, UUID> {
    Optional<ScheduleAssignmentEntity> findByScheduleIdAndEmployeeId(UUID scheduleId, UUID employeeId);

    Optional<ScheduleAssignmentEntity> findFirstByEmployeeIdAndScheduleWorkDateOrderByCreatedAtAsc(
            UUID employeeId,
            LocalDate workDate
    );

    @Query("""
            select assignment
            from ScheduleAssignmentEntity assignment
            join fetch assignment.employee employee
            join fetch assignment.schedule schedule
            where employee.id = :employeeId
              and schedule.status not in :excludedStatuses
              and (
                    schedule.workDate = :workDate
                    or (schedule.checkInOpensAt <= :now and schedule.checkInClosesAt >= :now)
              )
            order by schedule.checkInOpensAt desc, assignment.createdAt desc
            """)
    List<ScheduleAssignmentEntity> findCurrentAssignmentsForEmployee(
            @Param("employeeId") UUID employeeId,
            @Param("workDate") LocalDate workDate,
            @Param("now") Instant now,
            @Param("excludedStatuses") Collection<WorkScheduleStatus> excludedStatuses
    );

    @Query("""
            select assignment
            from ScheduleAssignmentEntity assignment
            join fetch assignment.employee employee
            join fetch assignment.schedule schedule
            where schedule.workDate between :from and :to
              and schedule.status <> :excludedStatus
            order by schedule.workDate asc, employee.fullName asc
            """)
    List<ScheduleAssignmentEntity> findReportAssignments(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("excludedStatus") WorkScheduleStatus excludedStatus
    );

    List<ScheduleAssignmentEntity> findAllByScheduleId(UUID scheduleId);

    void deleteByScheduleId(UUID scheduleId);
}
