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

public interface WorkScheduleRepository extends JpaRepository<WorkScheduleEntity, UUID> {
    List<WorkScheduleEntity> findAllByOrderByWorkDateDesc();

    List<WorkScheduleEntity> findAllBySimpleOpenModeFalseOrderByWorkDateDesc();

    Optional<WorkScheduleEntity> findFirstByWorkDateAndSimpleOpenModeTrueOrderByCreatedAtAsc(LocalDate workDate);

    boolean existsByWorkDateAndSimpleOpenModeFalseAndStatusIn(LocalDate workDate, Collection<WorkScheduleStatus> statuses);

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
}
