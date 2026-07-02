package com.artdecor.workforce.application.attendance;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {
    private final WorkScheduleRepository schedules;
    private final ScheduleAssignmentRepository assignments;
    private final AttendanceRecordRepository attendanceRecords;
    private final EmployeeRepository employees;
    private final Clock clock;

    public AttendanceService(
            WorkScheduleRepository schedules,
            ScheduleAssignmentRepository assignments,
            AttendanceRecordRepository attendanceRecords,
            EmployeeRepository employees,
            Clock clock
    ) {
        this.schedules = schedules;
        this.assignments = assignments;
        this.attendanceRecords = attendanceRecords;
        this.employees = employees;
        this.clock = clock;
    }

    @Transactional
    public AttendanceResponse checkIn(AuthenticatedPrincipal principal, AttendanceActionCommand command) {
        var employee = employees.findById(principal.employeeId())
                .orElseThrow(() -> new AttendanceException("EMPLOYEE_NOT_FOUND", "Employee not found."));
        WorkScheduleEntity schedule = schedules.findById(command.scheduleId())
                .orElseThrow(() -> new AttendanceException("SCHEDULE_NOT_FOUND", "Schedule not found."));

        assignments.findFirstByEmployeeIdAndScheduleWorkDateOrderByCreatedAtAsc(employee.getId(), schedule.getWorkDate())
                .filter(assignment -> assignment.getSchedule().getId().equals(schedule.getId()))
                .orElseThrow(() -> new AttendanceException("EMPLOYEE_NOT_ASSIGNED", "Employee is not assigned to this schedule."));

        if (schedule.getStatus() == WorkScheduleStatus.CANCELLED) {
            throw new AttendanceException("SCHEDULE_CANCELLED", "This schedule has been cancelled.");
        }

        Instant now = Instant.now(clock);
        if (now.isBefore(schedule.getCheckInOpensAt())) {
            throw new AttendanceException("ATTENDANCE_WINDOW_NOT_OPEN", "Check-in is not open yet.");
        }
        if (now.isAfter(schedule.getCheckInClosesAt())) {
            throw new AttendanceException("ATTENDANCE_WINDOW_CLOSED", "Check-in is closed for this schedule.");
        }

        var existing = attendanceRecords.findByScheduleIdAndEmployeeId(schedule.getId(), employee.getId());
        if (existing.isPresent() && existing.get().getCheckedInAt() != null) {
            throw new AttendanceException("DUPLICATE_CHECK_IN", "Employee is already checked in.");
        }

        AttendanceRecordEntity record = existing.orElseGet(AttendanceRecordEntity::new);
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setCheckedInAt(now);
        record.setCheckInLatitude(command.latitude());
        record.setCheckInLongitude(command.longitude());
        record.setCheckInDevice(command.device());
        record.setStatus(isLate(schedule, now) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);

        return toResponse(attendanceRecords.save(record));
    }

    @Transactional
    public AttendanceResponse checkOut(AuthenticatedPrincipal principal, AttendanceActionCommand command) {
        var employee = employees.findById(principal.employeeId())
                .orElseThrow(() -> new AttendanceException("EMPLOYEE_NOT_FOUND", "Employee not found."));

        AttendanceRecordEntity record = attendanceRecords.findByScheduleIdAndEmployeeId(command.scheduleId(), employee.getId())
                .orElseThrow(() -> new AttendanceException("CHECK_IN_REQUIRED", "Employee must check in before checking out."));

        if (record.getCheckedOutAt() != null) {
            throw new AttendanceException("DUPLICATE_CHECK_OUT", "Employee is already checked out.");
        }

        Instant now = Instant.now(clock);
        record.setCheckedOutAt(now);
        record.setCheckOutLatitude(command.latitude());
        record.setCheckOutLongitude(command.longitude());
        record.setCheckOutDevice(command.device());
        record.setWorkedMinutes((int) Duration.between(record.getCheckedInAt(), now).toMinutes());
        record.setOvertimeMinutes(calculateOvertimeMinutes(record, now));
        record.setStatus(AttendanceStatus.CHECKED_OUT);

        return toResponse(record);
    }

    private boolean isLate(WorkScheduleEntity schedule, Instant checkedInAt) {
        return schedule.getPlannedStartAt() != null && checkedInAt.isAfter(schedule.getPlannedStartAt());
    }

    private int calculateOvertimeMinutes(AttendanceRecordEntity record, Instant checkedOutAt) {
        Instant plannedEndAt = record.getSchedule().getPlannedEndAt();
        if (plannedEndAt == null || !checkedOutAt.isAfter(plannedEndAt)) {
            return 0;
        }

        return (int) Duration.between(plannedEndAt, checkedOutAt).toMinutes();
    }

    private AttendanceResponse toResponse(AttendanceRecordEntity record) {
        return new AttendanceResponse(
                record.getId().toString(),
                record.getSchedule().getId().toString(),
                record.getEmployee().getId().toString(),
                record.getStatus().name(),
                record.getCheckedInAt(),
                record.getCheckedOutAt(),
                record.getWorkedMinutes(),
                record.getOvertimeMinutes(),
                record.isRequiresApproval(),
                record.getApprovalReason()
        );
    }
}

