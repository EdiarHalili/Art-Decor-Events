package com.artdecor.workforce.application.attendance;

import com.artdecor.workforce.domain.AttendanceStatus;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutoCheckoutService {
    private final AttendanceRecordRepository attendanceRecords;
    private final Clock clock;

    public AutoCheckoutService(AttendanceRecordRepository attendanceRecords, Clock clock) {
        this.attendanceRecords = attendanceRecords;
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
            autoCheckout(record);
            updated++;
        }
        return updated;
    }

    private void autoCheckout(AttendanceRecordEntity record) {
        Instant checkoutAt = record.getSchedule().getCheckInClosesAt();
        record.setCheckedOutAt(checkoutAt);
        record.setWorkedMinutes(Math.max(0, (int) Duration.between(record.getCheckedInAt(), checkoutAt).toMinutes()));
        record.setOvertimeMinutes(calculateOvertimeMinutes(record, checkoutAt));
        record.setAutoCheckout(true);
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
