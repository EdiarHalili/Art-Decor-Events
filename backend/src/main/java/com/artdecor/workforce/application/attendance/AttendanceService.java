package com.artdecor.workforce.application.attendance;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.location.GpsDebugLogger;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.domain.AttendanceStatus;
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
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.dao.DataIntegrityViolationException;
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

        attendanceRecords.findActiveRecordsByEmployeeId(employee.getId())
                .stream()
                .findFirst()
                .ifPresent(record -> {
                    throw new AttendanceException("DUPLICATE_ACTIVE_CHECK_IN", "Ju tashmë keni filluar orarin e punës.");
                });

        if (schedule.getStatus() == WorkScheduleStatus.CANCELLED) {
            throw new AttendanceException("SCHEDULE_CANCELLED", "This schedule has been cancelled.");
        }
        Instant actionTime = actionTime(command);
        if (schedule.getStatus() == WorkScheduleStatus.CHECK_IN_CLOSED && actionTime.isAfter(schedule.getCheckInClosesAt())) {
            throw new AttendanceException("ATTENDANCE_WINDOW_CLOSED", "Check-in is closed for this daily window.");
        }
        if (!schedule.isSimpleOpenMode() && schedule.getStatus() != WorkScheduleStatus.CHECK_IN_OPEN) {
            if (actionTime.isBefore(schedule.getCheckInOpensAt())) {
                throw new AttendanceException("ATTENDANCE_WINDOW_NOT_OPEN", "Check-in is not open yet.");
            }
            if (actionTime.isAfter(schedule.getCheckInClosesAt())) {
                throw new AttendanceException("ATTENDANCE_WINDOW_CLOSED", "Check-in is closed for this daily window.");
            }
        }

        if (!schedule.isSimpleOpenMode()
                && attendanceRecords.existsByScheduleIdAndEmployeeIdAndCheckedInAtIsNotNull(schedule.getId(), employee.getId())) {
            throw new AttendanceException("DUPLICATE_CHECK_IN", "Ju tashmë keni bërë hyrje për këtë orar. Hyrja e dytë nuk lejohet.");
        }

        ensureSimpleOpenCutoffAfterAction(schedule, actionTime);

        AttendanceRecordEntity record = new AttendanceRecordEntity();
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
        record.setCheckedInAt(actionTime);
        record.setCheckInLatitude(gpsEnabled ? command.latitude() : null);
        record.setCheckInLongitude(gpsEnabled ? command.longitude() : null);
        record.setCheckInDevice(command.device());
        record.setStatus(AttendanceStatus.PRESENT);

        AttendanceRecordEntity saved;
        try {
            saved = attendanceRecords.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            throw new AttendanceException("DUPLICATE_ACTIVE_CHECK_IN", "Ju tashmë keni filluar orarin e punës.");
        }
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

        AttendanceRecordEntity record = attendanceRecords.findActiveRecordByScheduleIdAndEmployeeId(command.scheduleId(), employee.getId())
                .orElseThrow(() -> new AttendanceException("CHECK_IN_REQUIRED", "Nuk ka një orar aktiv për ta përfunduar."));

        if (record.getCheckedOutAt() != null) {
            throw new AttendanceException("DUPLICATE_CHECK_OUT", "Nuk ka një orar aktiv për ta përfunduar.");
        }

        Instant actionTime = actionTime(command);
        if (actionTime.isBefore(record.getCheckedInAt())) {
            throw new AttendanceException("INVALID_CHECKOUT_TIME", "Koha e daljes nuk mund të jetë para kohës së hyrjes.");
        }

        boolean gpsEnabled = settings.current().gpsEnabled();
        GpsDebugLogger.log(
                "check-out request",
                "employeeId", employee.getId(),
                "scheduleId", command.scheduleId(),
                "latitude", command.latitude(),
                "longitude", command.longitude(),
                "gpsEnabled", gpsEnabled
        );
        record.setCheckedOutAt(actionTime);
        record.setCheckOutLatitude(gpsEnabled ? command.latitude() : null);
        record.setCheckOutLongitude(gpsEnabled ? command.longitude() : null);
        record.setCheckOutDevice(command.device());
        record.setWorkedMinutes((int) Duration.between(record.getCheckedInAt(), actionTime).toMinutes());
        record.setOvertimeMinutes(calculateOvertimeMinutes(record, actionTime));
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
            throw new AttendanceException("CHECK_IN_REQUIRED", "Nuk ka një orar aktiv për ta përfunduar.");
        }
        if (record.getCheckedOutAt() != null) {
            throw new AttendanceException("DUPLICATE_CHECK_OUT", "Nuk ka një orar aktiv për ta përfunduar.");
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

    @Transactional
    public AttendanceResponse extendCheckout(AuthenticatedPrincipal principal, java.util.UUID attendanceRecordId, Instant extendedUntil) {
        AttendanceRecordEntity record = attendanceRecords.findById(attendanceRecordId)
                .orElseThrow(() -> new AttendanceException("ATTENDANCE_RECORD_NOT_FOUND", "Regjistrimi nuk u gjet."));
        if (record.getCheckedInAt() == null || record.getCheckedOutAt() != null) {
            throw new AttendanceException("NO_ACTIVE_ATTENDANCE", "Nuk ka një orar aktiv për ta vazhduar.");
        }
        Instant currentAutoCheckout = record.getExtendedCheckoutUntil() == null
                ? record.getSchedule().getCheckInClosesAt()
                : record.getExtendedCheckoutUntil();
        if (!extendedUntil.isAfter(currentAutoCheckout)) {
            throw new AttendanceException("INVALID_EXTENSION_TIME", "Koha e vazhdimit duhet të jetë pas kohës aktuale të mbylljes.");
        }
        record.setExtendedCheckoutUntil(extendedUntil);
        audit.log(principal, "ATTENDANCE_CHECKOUT_EXTENDED", "ATTENDANCE_RECORD", record.getId());
        return toResponse(record);
    }

    private int calculateOvertimeMinutes(AttendanceRecordEntity record, Instant checkedOutAt) {
        Instant plannedEndAt = record.getSchedule().getPlannedEndAt();
        if (plannedEndAt == null || !checkedOutAt.isAfter(plannedEndAt)) {
            return 0;
        }

        return (int) Duration.between(plannedEndAt, checkedOutAt).toMinutes();
    }

    private Instant actionTime(AttendanceActionCommand command) {
        return command.capturedAt() == null ? Instant.now(clock) : command.capturedAt();
    }

    private void ensureSimpleOpenCutoffAfterAction(WorkScheduleEntity schedule, Instant actionTime) {
        if (!schedule.isSimpleOpenMode()
                || !schedule.isAutoCheckoutEnabled()
                || schedule.getCheckInClosesAt().isAfter(actionTime)) {
            return;
        }
        var currentSettings = settings.current();
        if (currentSettings.openModeUnlimitedCheckout()) {
            return;
        }

        ZoneId zone = ZoneId.of(currentSettings.timezone());
        LocalDate cutoffDate = actionTime.atZone(zone).toLocalDate();
        Instant cutoff = cutoffDate.atTime(currentSettings.defaultCheckInCloseTime()).atZone(zone).toInstant();
        while (!cutoff.isAfter(actionTime)) {
            cutoffDate = cutoffDate.plusDays(1);
            cutoff = cutoffDate.atTime(currentSettings.defaultCheckInCloseTime()).atZone(zone).toInstant();
        }
        schedule.setCheckInClosesAt(cutoff);
        schedule.setPlannedEndAt(cutoff);
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
