package com.artdecor.workforce.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, UUID> {
    boolean existsByScheduleIdAndEmployeeIdAndCheckedInAtIsNotNull(UUID scheduleId, UUID employeeId);

    @Query("""
            select record
            from AttendanceRecordEntity record
            join fetch record.employee employee
            join fetch record.schedule schedule
            where schedule.id = :scheduleId
              and employee.id = :employeeId
              and record.checkedInAt is not null
              and record.checkedOutAt is null
            order by record.checkedInAt desc
            """)
    java.util.Optional<AttendanceRecordEntity> findActiveRecordByScheduleIdAndEmployeeId(
            @Param("scheduleId") UUID scheduleId,
            @Param("employeeId") UUID employeeId
    );

    @Query("""
            select record
            from AttendanceRecordEntity record
            join fetch record.employee employee
            join fetch record.schedule schedule
            where employee.id = :employeeId
              and record.checkedInAt is not null
              and record.checkedOutAt is null
            order by record.checkedInAt desc
            """)
    java.util.List<AttendanceRecordEntity> findActiveRecordsByEmployeeId(@Param("employeeId") UUID employeeId);

    void deleteByScheduleId(UUID scheduleId);

    @Query("""
            select record
            from AttendanceRecordEntity record
            join fetch record.employee employee
            join fetch record.schedule schedule
            where record.checkedInAt is not null
              and record.checkedOutAt is null
              and schedule.status <> :excludedStatus
              and schedule.autoCheckoutEnabled = true
              and (
                    (record.extendedCheckoutUntil is not null and record.extendedCheckoutUntil <= :now)
                    or (record.extendedCheckoutUntil is null and schedule.checkInClosesAt <= :now)
              )
            """)
    java.util.List<AttendanceRecordEntity> findRecordsDueForAutoCheckout(
            @Param("now") java.time.Instant now,
            @Param("excludedStatus") com.artdecor.workforce.domain.WorkScheduleStatus excludedStatus
    );

    @Query("""
            select record
            from AttendanceRecordEntity record
            join fetch record.employee employee
            join fetch record.schedule schedule
            where schedule.workDate between :from and :to
            order by schedule.workDate asc, employee.fullName asc
            """)
    java.util.List<AttendanceRecordEntity> findReportRecords(
            @Param("from") java.time.LocalDate from,
            @Param("to") java.time.LocalDate to
    );
}
