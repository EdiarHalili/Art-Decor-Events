package com.artdecor.workforce.application.location;

import com.artdecor.workforce.application.attendance.AttendanceException;
import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateEntity;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LiveLocationService {
    private final AttendanceRecordRepository attendanceRecords;
    private final LiveLocationUpdateRepository liveLocations;
    private final AppSettingsService settings;
    private final AuditService audit;
    private final Clock clock;

    public LiveLocationService(
            AttendanceRecordRepository attendanceRecords,
            LiveLocationUpdateRepository liveLocations,
            AppSettingsService settings,
            AuditService audit,
            Clock clock
    ) {
        this.attendanceRecords = attendanceRecords;
        this.liveLocations = liveLocations;
        this.settings = settings;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public LiveLocationResponse record(AuthenticatedPrincipal principal, LiveLocationCommand command) {
        if (!settings.current().liveLocationTrackingEnabled()) {
            throw new AttendanceException("LIVE_LOCATION_DISABLED", "Live location tracking is disabled.");
        }
        validate(command);
        GpsDebugLogger.log(
                "live-location request",
                "employeeId", principal.employeeId(),
                "latitude", command.latitude(),
                "longitude", command.longitude(),
                "accuracyMeters", command.accuracyMeters()
        );

        AttendanceRecordEntity attendanceRecord = attendanceRecords.findActiveRecordsByEmployeeId(principal.employeeId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new AttendanceException(
                        "ACTIVE_CHECK_IN_REQUIRED",
                        "Live location can only be recorded after check-in and before check-out."
                ));

        boolean firstUpdate = !liveLocations.existsByAttendanceRecordId(attendanceRecord.getId());

        LiveLocationUpdateEntity update = new LiveLocationUpdateEntity();
        update.setAttendanceRecord(attendanceRecord);
        update.setEmployee(attendanceRecord.getEmployee());
        update.setSchedule(attendanceRecord.getSchedule());
        update.setLatitude(command.latitude());
        update.setLongitude(command.longitude());
        update.setAccuracyMeters(command.accuracyMeters());
        update.setCapturedAt(command.capturedAt() == null ? Instant.now(clock) : command.capturedAt());
        update.setReceivedAt(Instant.now(clock));
        update.setDevice(command.device());

        LiveLocationUpdateEntity saved = liveLocations.save(update);
        GpsDebugLogger.log(
                "live-location stored",
                "liveLocationId", saved.getId(),
                "attendanceRecordId", saved.getAttendanceRecord().getId(),
                "latitude", saved.getLatitude(),
                "longitude", saved.getLongitude()
        );
        if (firstUpdate) {
            audit.log(principal, "LIVE_LOCATION_TRACKING_STARTED", "ATTENDANCE_RECORD", attendanceRecord.getId());
        }
        return toResponse(saved);
    }

    private void validate(LiveLocationCommand command) {
        if (command.latitude() == null || command.longitude() == null) {
            throw new AttendanceException("LOCATION_REQUIRED", "Live location latitude and longitude are required.");
        }
        if (command.latitude() < -90 || command.latitude() > 90) {
            throw new AttendanceException("INVALID_LATITUDE", "Latitude must be between -90 and 90.");
        }
        if (command.longitude() < -180 || command.longitude() > 180) {
            throw new AttendanceException("INVALID_LONGITUDE", "Longitude must be between -180 and 180.");
        }
        if (command.accuracyMeters() != null && command.accuracyMeters() < 0) {
            throw new AttendanceException("INVALID_ACCURACY", "Location accuracy cannot be negative.");
        }
    }

    private LiveLocationResponse toResponse(LiveLocationUpdateEntity update) {
        return new LiveLocationResponse(
                update.getId().toString(),
                update.getAttendanceRecord().getId().toString(),
                update.getEmployee().getId().toString(),
                update.getLatitude(),
                update.getLongitude(),
                update.getAccuracyMeters(),
                update.getCapturedAt()
        );
    }
}
