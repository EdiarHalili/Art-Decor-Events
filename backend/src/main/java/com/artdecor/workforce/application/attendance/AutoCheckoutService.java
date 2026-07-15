package com.artdecor.workforce.application.attendance;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.CheckoutType;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutoCheckoutService {
    private static final java.util.Set<WorkScheduleStatus> COMPLETION_CANDIDATES = EnumSet.of(
            WorkScheduleStatus.PUBLISHED,
            WorkScheduleStatus.CHECK_IN_OPEN,
            WorkScheduleStatus.CHECK_IN_CLOSED
    );

    private final AttendanceRecordRepository attendanceRecords;
    private final WorkScheduleRepository schedules;
    private final AppSettingsService settings;
    private final Clock clock;

    public AutoCheckoutService(
            AttendanceRecordRepository attendanceRecords,
            WorkScheduleRepository schedules,
            AppSettingsService settings,
            Clock clock
    ) {
        this.attendanceRecords = attendanceRecords;
        this.schedules = schedules;
        this.settings = settings;
        this.clock = clock;
    }

    @Scheduled(
            initialDelayString = "${app.attendance.auto-checkout-initial-delay-ms:30000}",
            fixedDelayString = "${app.attendance.auto-checkout-delay-ms:60000}"
    )
    @Transactional
    public void autoCheckoutDueRecords() {
        autoCheckoutDueRecords(Instant.now(clock));
    }

    @Transactional
    public int autoCheckoutDueRecords(Instant now) {
        int updated = 0;
        for (AttendanceRecordEntity record : attendanceRecords.findRecordsDueForAutoCheckout(now, WorkScheduleStatus.CANCELLED)) {
            if (record.getCheckedOutAt() != null || record.getCheckedInAt() == null) {
                continue;
            }
            if (!record.getSchedule().isAutoCheckoutEnabled()) {
                continue;
            }
            if (record.getSchedule().isSimpleOpenMode() && settings.current().openModeUnlimitedCheckout()) {
                continue;
            }
            autoCheckout(record);
            updated++;
        }
        completeDueWindows(now);
        return updated;
    }

    private void completeDueWindows(Instant now) {
        for (var schedule : schedules.findWindowsDueForCompletion(now, COMPLETION_CANDIDATES)) {
            if (schedule.isAutoCheckoutEnabled()) {
                schedule.setStatus(WorkScheduleStatus.COMPLETED);
            }
        }
    }

    private void autoCheckout(AttendanceRecordEntity record) {
        Instant checkoutAt = record.getExtendedCheckoutUntil() == null
                ? record.getSchedule().getCheckInClosesAt()
                : record.getExtendedCheckoutUntil();
        record.setCheckedOutAt(checkoutAt);
        record.setWorkedMinutes(Math.max(0, (int) Duration.between(record.getCheckedInAt(), checkoutAt).toMinutes()));
        record.setOvertimeMinutes(calculateOvertimeMinutes(record, checkoutAt));
        record.setAutoCheckout(true);
        record.setCheckoutType(CheckoutType.AUTO_CHECKED_OUT);
        record.setStatus(AttendanceStatus.CHECKED_OUT);
    }

    private int calculateOvertimeMinutes(AttendanceRecordEntity record, Instant checkedOutAt) {
        Instant plannedEndAt = record.getSchedule().getPlannedEndAt();
        if (plannedEndAt == null || !checkedOutAt.isAfter(plannedEndAt)) {
            return 0;
        }
        return (int) Duration.between(plannedEndAt, checkedOutAt).toMinutes();
    }
}
