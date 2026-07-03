package com.artdecor.workforce.application.audit;

import com.artdecor.workforce.infrastructure.persistence.AuditLogEntity;
import com.artdecor.workforce.infrastructure.persistence.AuditLogRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository logs;

    public AuditService(AuditLogRepository logs) {
        this.logs = logs;
    }

    public void log(AuthenticatedPrincipal principal, String action, String entityType, UUID entityId) {
        AuditLogEntity log = new AuditLogEntity();
        if (principal != null) {
            if (principal.employeeId() != null) {
                log.setActorEmployeeId(principal.employeeId());
            } else {
                log.setActorUserId(principal.userId());
            }
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setMetadata(Map.of());
        logs.save(log);
    }

    public void system(String action, String entityType, UUID entityId) {
        log(null, action, entityType, entityId);
    }

    public List<AuditLogResponse> latest() {
        return logs.findTop100ByOrderByCreatedAtDesc().stream()
                .map(log -> new AuditLogResponse(
                        log.getId().toString(),
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId() == null ? null : log.getEntityId().toString(),
                        log.getCreatedAt()
                ))
                .toList();
    }
}
