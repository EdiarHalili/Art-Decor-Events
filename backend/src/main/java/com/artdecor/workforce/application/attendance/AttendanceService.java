package com.artdecor.workforce.application.attendance;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.location.GpsDebugLogger;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.CheckoutMode;
import com.artdecor.workforce.domain.CheckoutType;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateRepository;
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
    private final LiveLocationUpdateRepository liveLocations;
    private final AppSettingsService settings;
    private final AuditService audit;
    private final Clock clock;

    public AttendanceService(
            WorkScheduleRepository schedules,
            ScheduleAssignmentRepository assignments,
            AttendanceRecordRepository attendanceRecords,
            EmployeeRepository employees,
            LiveLocationUpdateRepository liveLocations,
            AppSettingsService settings,
            AuditService audit,
            Clock clock
    ) {
        this.schedules = schedules;
        this.assignments = assignments;
        this.attendanceRecords = attendanceRecords;
        this.employees = employees;
        this.liveLocations = liveLocations;
        this.settings = settings;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public AttendanceResponse checkIn(AuthenticatedPrincipal principal, AttendanceActionCommand command) {
        var employee = employees.findById(principal.employeeId())
                .orElseThrow(() -> new AttendanceException("EMPLOYEE_NOT_FOUND", "Employee not found."));
        WorkScheduleEntity schedule = schedules.findById(command.scheduleId())
                .orElseThrow(() -> new AttendanceException("SCHEDULE_NOT_FOUND", "Schedule not found."));

        assignments.findByScheduleIdAndEmployeeId(schedule.getId(), employee.getId())
                .orElseThrow(() -> new AttendanceException("EMPLOYEE_NOT_ASSIGNED", "Employee is not assigned to this schedule."));

        if (schedule.getStatus() == WorkScheduleStatus.CANCELLED) {
            throw new AttendanceException("SCHEDULE_CANCELLED", "This schedule has been cancelled.");
        }
        if (schedule.getStatus() == WorkScheduleStatus.CHECK_IN_CLOSED) {
            throw new AttendanceException("ATTENDANCE_WINDOW_CLOSED", "Check-in is closed for this daily window.");
        }

        Instant now = Instant.now(clock);
        boolean unlimited = schedule.getCheckoutMode() == CheckoutMode.UNLIMITED_24_7;
        if (schedule.getStatus() != WorkScheduleStatus.CHECK_IN_OPEN) {
            if (now.isBefore(schedule.getCheckInOpensAt())) {
                throw new AttendanceException("ATTENDANCE_WINDOW_NOT_OPEN", "Check-in is not open yet.");
            }
            if (!unlimited && now.isAfter(schedule.getCheckInClosesAt())) {
                throw new AttendanceException("ATTENDANCE_WINDOW_CLOSED", "Check-in is closed for this daily window.");
            }
        }

        var existing = attendanceRecords.findByScheduleIdAndEmployeeId(schedule.getId(), employee.getId());
        if (existing.isPresent() && existing.get().getCheckedInAt() != null) {
            throw new AttendanceException("DUPLICATE_CHECK_IN", "Employee is already checked in.");
        }

        AttendanceRecordEntity record = existing.orElseGet(AttendanceRecordEntity::new);
        boolean gpsEnabled = settings.current().gpsEnabled();
        GpsDebugLogger.log(
                "check-in request",
                "employeeId", employee.getId(),
                "scheduleId", schedule.getId(),
                "latitude", command.latitude(),
                "longitude", command.longitude(),
                "gpsEnabled", gpsEnabled
        );
        record.setSchedule(schedule);
        record.setEmployee(employee);
        record.setCheckedInAt(now);
        record.setCheckInLatitude(gpsEnabled ? command.latitude() : null);
        record.setCheckInLongitude(gpsEnabled ? command.longitude() : null);
        record.setCheckInDevice(command.device());
        record.setStatus(isLate(schedule, now) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);

        AttendanceRecordEntity saved = attendanceRecords.save(record);
        GpsDebugLogger.log(
                "check-in stored",
                "attendanceRecordId", saved.getId(),
                "checkInLatitude", saved.getCheckInLatitude(),
                "checkInLongitude", saved.getCheckInLongitude()
        );
        audit.log(principal, "CHECK_IN_RECORDED", "ATTENDANCE_RECORD", saved.getId());
        return toResponse(saved);
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
        boolean gpsEnabled = settings.current().gpsEnabled();
        GpsDebugLogger.log(
                "check-out request",
                "employeeId", employee.getId(),
                "scheduleId", command.scheduleId(),
                "latitude", command.latitude(),
                "longitude", command.longitude(),
                "gpsEnabled", gpsEnabled
        );
        record.setCheckedOutAt(now);
        record.setCheckOutLatitude(gpsEnabled ? command.latitude() : null);
        record.setCheckOutLongitude(gpsEnabled ? command.longitude() : null);
        record.setCheckOutDevice(command.device());
        record.setWorkedMinutes((int) Duration.between(record.getCheckedInAt(), now).toMinutes());
        record.setOvertimeMinutes(calculateOvertimeMinutes(record, now));
        record.setAutoCheckout(false);
        record.setCheckoutType(CheckoutType.MANUAL_EMPLOYEE);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
        GpsDebugLogger.log(
                "check-out stored",
                "attendanceRecordId", record.getId(),
                "checkOutLatitude", record.getCheckOutLatitude(),
                "checkOutLongitude", record.getCheckOutLongitude()
        );

        audit.log(principal, "CHECK_OUT_RECORDED", "ATTENDANCE_RECORD", record.getId());
        if (liveLocations.existsByAttendanceRecordId(record.getId())) {
            audit.log(principal, "LIVE_LOCATION_TRACKING_STOPPED", "ATTENDANCE_RECORD", record.getId());
        }
        return toResponse(record);
    }

    @Transactional
    public AttendanceResponse adminCheckOut(AuthenticatedPrincipal principal, java.util.UUID attendanceRecordId, Instant checkedOutAt) {
        AttendanceRecordEntity record = attendanceRecords.findById(attendanceRecordId)
                .orElseThrow(() -> new AttendanceException("ATTENDANCE_RECORD_NOT_FOUND", "Attendance record not found."));
        if (record.getCheckedInAt() == null) {
            throw new AttendanceException("CHECK_IN_REQUIRED", "Employee must check in before checking out.");
        }
        if (record.getCheckedOutAt() != null) {
            throw new AttendanceException("DUPLICATE_CHECK_OUT", "Employee is already checked out.");
        }
        if (checkedOutAt.isBefore(record.getCheckedInAt())) {
            throw new AttendanceException("INVALID_CHECKOUT_TIME", "Checkout time cannot be before check-in time.");
        }

        record.setCheckedOutAt(checkedOutAt);
        record.setWorkedMinutes(Math.max(0, (int) Duration.between(record.getCheckedInAt(), checkedOutAt).toMinutes()));
        record.setOvertimeMinutes(calculateOvertimeMinutes(record, checkedOutAt));
        record.setAutoCheckout(false);
        record.setCheckoutType(CheckoutType.ADMIN_CHECKED_OUT);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
        audit.log(principal, "ADMIN_CHECK_OUT_RECORDED", "ATTENDANCE_RECORD", record.getId());
        return toResponse(record);
    }

    private boolean isLate(WorkScheduleEntity schedule, Instant checkedInAt) {
        if (schedule.getPlannedStartAt() == null) {
            return false;
        }
        return checkedInAt.isAfter(schedule.getPlannedStartAt().plus(Duration.ofMinutes(settings.current().allowedLateMinutes())));
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
                record.isAutoCheckout(),
                record.getCheckoutType().name(),
                record.isRequiresApproval(),
                record.getApprovalReason()
        );
    }
}
