package com.artdecor.workforce.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_subscriptions")
public class PushSubscriptionEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID userId;
    private UUID employeeId;
    private String endpoint;
    private String p256dhKey;
    private String authKey;
    private String userAgent;
    private Instant createdAt = Instant.now();
    private Instant lastSeenAt;

    public UUID getId() { return id; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public void setP256dhKey(String p256dhKey) { this.p256dhKey = p256dhKey; }
    public void setAuthKey(String authKey) { this.authKey = authKey; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
}
