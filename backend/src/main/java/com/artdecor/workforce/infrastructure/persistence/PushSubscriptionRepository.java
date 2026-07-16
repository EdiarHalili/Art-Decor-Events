package com.artdecor.workforce.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscriptionEntity, UUID> {
    Optional<PushSubscriptionEntity> findByEndpoint(String endpoint);

    boolean existsByEmployeeId(UUID employeeId);

    boolean existsByUserId(UUID userId);

    void deleteByEmployeeId(UUID employeeId);

    void deleteByUserId(UUID userId);
}
