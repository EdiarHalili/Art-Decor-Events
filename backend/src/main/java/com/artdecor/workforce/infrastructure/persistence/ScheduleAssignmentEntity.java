package com.artdecor.workforce.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_assignments")
public class ScheduleAssignmentEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkScheduleEntity schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    private String assignmentNotes;
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public WorkScheduleEntity getSchedule() {
        return schedule;
    }

    public void setSchedule(WorkScheduleEntity schedule) {
        this.schedule = schedule;
    }

    public EmployeeEntity getEmployee() {
        return employee;
    }

    public void setEmployee(EmployeeEntity employee) {
        this.employee = employee;
    }

    public String getAssignmentNotes() {
        return assignmentNotes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
