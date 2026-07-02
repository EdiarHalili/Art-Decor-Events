package com.artdecor.workforce.infrastructure.persistence;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkScheduleRepository extends JpaRepository<WorkScheduleEntity, UUID> {
    List<WorkScheduleEntity> findAllByOrderByWorkDateDesc();

    Optional<WorkScheduleEntity> findByWorkDateAndStatusNot(LocalDate workDate, WorkScheduleStatus status);

    boolean existsByWorkDateAndStatusNot(LocalDate workDate, WorkScheduleStatus status);

    boolean existsByWorkDateAndStatusNotAndIdNot(LocalDate workDate, WorkScheduleStatus status, UUID id);
}
