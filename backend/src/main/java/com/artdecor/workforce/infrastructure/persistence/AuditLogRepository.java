package com.artdecor.workforce.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findTop100ByOrderByCreatedAtDesc();

    boolean existsByActorEmployeeId(UUID employeeId);

    boolean existsByActorUserId(UUID userId);

    void deleteByActorEmployeeId(UUID employeeId);

    void deleteByActorUserId(UUID userId);
}
