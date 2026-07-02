package com.artdecor.workforce.infrastructure.persistence;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "work_schedules")
public class WorkScheduleEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(nullable = false)
    private LocalDate workDate;

    @Column(nullable = false)
    private Instant checkInOpensAt;

    @Column(nullable = false)
    private Instant checkInClosesAt;

    private Instant plannedStartAt;
    private Instant plannedEndAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkScheduleStatus status = WorkScheduleStatus.DRAFT;

    private Instant createdAt = Instant.now();
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getWorkDate() {
        return workDate;
    }

    public void setWorkDate(LocalDate workDate) {
        this.workDate = workDate;
    }

    public Instant getCheckInOpensAt() {
        return checkInOpensAt;
    }

    public void setCheckInOpensAt(Instant checkInOpensAt) {
        this.checkInOpensAt = checkInOpensAt;
    }

    public Instant getCheckInClosesAt() {
        return checkInClosesAt;
    }

    public void setCheckInClosesAt(Instant checkInClosesAt) {
        this.checkInClosesAt = checkInClosesAt;
    }

    public Instant getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(Instant plannedStartAt) {
        this.plannedStartAt = plannedStartAt;
    }

    public Instant getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(Instant plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
    }

    public WorkScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(WorkScheduleStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
