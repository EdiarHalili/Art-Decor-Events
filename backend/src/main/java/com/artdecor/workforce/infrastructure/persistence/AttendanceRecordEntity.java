package com.artdecor.workforce.infrastructure.persistence;

import com.artdecor.workforce.domain.AttendanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecordEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private WorkScheduleEntity schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.SCHEDULED;

    private Instant checkedInAt;
    private Instant checkedOutAt;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private Double checkOutLatitude;
    private Double checkOutLongitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> checkInDevice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> checkOutDevice;

    @Column(nullable = false)
    private int workedMinutes;

    @Column(nullable = false)
    private int overtimeMinutes;

    @Column(nullable = false)
    private boolean requiresApproval;

    private String approvalReason;
    private Instant createdAt = Instant.now();
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setSchedule(WorkScheduleEntity schedule) {
        this.schedule = schedule;
    }

    public WorkScheduleEntity getSchedule() {
        return schedule;
    }

    public void setEmployee(EmployeeEntity employee) {
        this.employee = employee;
    }

    public EmployeeEntity getEmployee() {
        return employee;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public Instant getCheckedOutAt() {
        return checkedOutAt;
    }

    public void setCheckedOutAt(Instant checkedOutAt) {
        this.checkedOutAt = checkedOutAt;
    }

    public void setCheckInLatitude(Double checkInLatitude) {
        this.checkInLatitude = checkInLatitude;
    }

    public void setCheckInLongitude(Double checkInLongitude) {
        this.checkInLongitude = checkInLongitude;
    }

    public void setCheckOutLatitude(Double checkOutLatitude) {
        this.checkOutLatitude = checkOutLatitude;
    }

    public void setCheckOutLongitude(Double checkOutLongitude) {
        this.checkOutLongitude = checkOutLongitude;
    }

    public void setCheckInDevice(Map<String, Object> checkInDevice) {
        this.checkInDevice = checkInDevice;
    }

    public void setCheckOutDevice(Map<String, Object> checkOutDevice) {
        this.checkOutDevice = checkOutDevice;
    }

    public int getWorkedMinutes() {
        return workedMinutes;
    }

    public void setWorkedMinutes(int workedMinutes) {
        this.workedMinutes = workedMinutes;
    }

    public int getOvertimeMinutes() {
        return overtimeMinutes;
    }

    public void setOvertimeMinutes(int overtimeMinutes) {
        this.overtimeMinutes = overtimeMinutes;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public String getApprovalReason() {
        return approvalReason;
    }

    public void setApprovalReason(String approvalReason) {
        this.approvalReason = approvalReason;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}

