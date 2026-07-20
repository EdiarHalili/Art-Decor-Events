package com.artdecor.workforce.infrastructure.persistence;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkScheduleRepository extends JpaRepository<WorkScheduleEntity, UUID> {
    List<WorkScheduleEntity> findAllByOrderByWorkDateDesc();

    List<WorkScheduleEntity> findAllBySimpleOpenModeFalseOrderByWorkDateDesc();

    Optional<WorkScheduleEntity> findFirstByWorkDateAndSimpleOpenModeTrueOrderByCreatedAtAsc(LocalDate workDate);

    boolean existsByWorkDateAndSimpleOpenModeFalseAndStatusIn(LocalDate workDate, Collection<WorkScheduleStatus> statuses);

    @Query("""
            select count(schedule) > 0
            from WorkScheduleEntity schedule
            where schedule.simpleOpenMode = false
              and schedule.status in :statuses
              and schedule.checkInOpensAt <= :now
              and schedule.checkInClosesAt >= :now
            """)
    boolean existsActiveScheduledWindowAt(
            @Param("now") Instant now,
            @Param("statuses") Collection<WorkScheduleStatus> statuses
    );

    @Query("""
            select schedule
            from WorkScheduleEntity schedule
            where schedule.status in :statuses
              and schedule.simpleOpenMode = false
              and schedule.checkInOpensAt < :endsAt
              and schedule.checkInClosesAt > :startsAt
              and schedule.checkInClosesAt > :now
            """)
    List<WorkScheduleEntity> findOverlappingWindows(
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt,
            @Param("now") Instant now,
            @Param("statuses") Collection<WorkScheduleStatus> statuses
    );

    @Query("""
            select schedule
            from WorkScheduleEntity schedule
            where schedule.id <> :excludedId
              and schedule.status in :statuses
              and schedule.simpleOpenMode = false
              and schedule.checkInOpensAt < :endsAt
              and schedule.checkInClosesAt > :startsAt
              and schedule.checkInClosesAt > :now
            """)
    List<WorkScheduleEntity> findOverlappingWindowsExcluding(
            @Param("excludedId") UUID excludedId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt,
            @Param("now") Instant now,
            @Param("statuses") Collection<WorkScheduleStatus> statuses
    );

    @Query("""
            select schedule
            from WorkScheduleEntity schedule
            where schedule.status in :statuses
              and schedule.simpleOpenMode = false
              and schedule.checkInClosesAt <= :now
            """)
    List<WorkScheduleEntity> findWindowsDueForCompletion(
            @Param("now") Instant now,
            @Param("statuses") Collection<WorkScheduleStatus> statuses
    );

    @Query(value = "select exists(select 1 from work_schedules where created_by = :userId or supervisor_id = :userId)", nativeQuery = true)
    boolean existsByCreatedByOrSupervisorUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "update work_schedules set created_by = null where created_by = :userId", nativeQuery = true)
    void clearCreatedByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query(value = "update work_schedules set supervisor_id = null where supervisor_id = :userId", nativeQuery = true)
    void clearSupervisorUserId(@Param("userId") UUID userId);
}
