package com.artdecor.workforce.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "announcements")
public class AnnouncementEntity {
    @Id
    @GeneratedValue
    private UUID id;
    private String title;
    private String body;
    private Instant visibleFrom = Instant.now();
    private Instant visibleUntil;
    private Instant createdAt = Instant.now();

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Instant getVisibleFrom() { return visibleFrom; }
    public void setVisibleFrom(Instant visibleFrom) { this.visibleFrom = visibleFrom; }
    public Instant getVisibleUntil() { return visibleUntil; }
    public void setVisibleUntil(Instant visibleUntil) { this.visibleUntil = visibleUntil; }
    public Instant getCreatedAt() { return createdAt; }
}
