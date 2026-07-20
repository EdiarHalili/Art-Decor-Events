package com.artdecor.workforce.application.attendance;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.application.settings.AppSettingsService;
import com.artdecor.workforce.application.settings.AppSettingsResponse;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimpleOpenModeService {
    private static final String SIMPLE_OPEN_TITLE = "Simple open attendance";
    private static final Set<WorkScheduleStatus> SCHEDULED_WINDOW_STATUSES = EnumSet.of(
            WorkScheduleStatus.DRAFT,
            WorkScheduleStatus.PUBLISHED,
            WorkScheduleStatus.CHECK_IN_OPEN,
            WorkScheduleStatus.CHECK_IN_CLOSED
    );

    private final WorkScheduleRepository schedules;
    private final ScheduleAssignmentRepository assignments;
    private final AppSettingsService settings;
    private final Clock clock;

    public SimpleOpenModeService(
            WorkScheduleRepository schedules,
            ScheduleAssignmentRepository assignments,
            AppSettingsService settings,
            Clock clock
    ) {
        this.schedules = schedules;
        this.assignments = assignments;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public boolean hasScheduledWindowForToday() {
        return hasScheduledWindowForDate(LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public boolean hasScheduledWindowForDate(LocalDate workDate) {
        return schedules.existsByWorkDateAndSimpleOpenModeFalseAndStatusIn(workDate, SCHEDULED_WINDOW_STATUSES);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveScheduledWindowAt(java.time.Instant now) {
        return schedules.existsActiveScheduledWindowAt(now, SCHEDULED_WINDOW_STATUSES);
    }

    @Transactional
    public ScheduleAssignmentEntity getOrCreateTodayAssignment(EmployeeEntity employee) {
        LocalDate today = LocalDate.now(clock);
        if (hasScheduledWindowForDate(today) || hasActiveScheduledWindowAt(java.time.Instant.now(clock))) {
            throw new AttendanceException("SCHEDULED_WINDOW_ACTIVE", "A scheduled daily check-in window is active today.");
        }

        WorkScheduleEntity schedule = schedules.findFirstByWorkDateAndSimpleOpenModeTrueOrderByCreatedAtAsc(today)
                .orElseGet(() -> schedules.save(simpleOpenSchedule(today)));

        return assignments.findByScheduleIdAndEmployeeId(schedule.getId(), employee.getId())
                .orElseGet(() -> {
                    ScheduleAssignmentEntity assignment = new ScheduleAssignmentEntity();
                    assignment.setSchedule(schedule);
                    assignment.setEmployee(employee);
                    return assignments.save(assignment);
                });
    }

    private WorkScheduleEntity simpleOpenSchedule(LocalDate workDate) {
        AppSettingsResponse currentSettings = settings.current();
        ZoneId zone = ZoneId.of(currentSettings.timezone());
        LocalTime cutoffTime = currentSettings.defaultCheckInCloseTime();
        Instant now = Instant.now(clock);
        LocalDate cutoffDate = workDate;
        Instant cutoff = cutoffDate.atTime(cutoffTime).atZone(zone).toInstant();
        while (!cutoff.isAfter(now)) {
            cutoffDate = cutoffDate.plusDays(1);
            cutoff = cutoffDate.atTime(cutoffTime).atZone(zone).toInstant();
        }
        WorkScheduleEntity schedule = new WorkScheduleEntity();
        schedule.setTitle(SIMPLE_OPEN_TITLE);
        schedule.setDescription("Automatically created because no daily check-in window was scheduled.");
        schedule.setWorkDate(workDate);
        schedule.setCheckInOpensAt(workDate.atStartOfDay(zone).toInstant());
        schedule.setCheckInClosesAt(cutoff);
        schedule.setPlannedStartAt(null);
        schedule.setPlannedEndAt(schedule.getCheckInClosesAt());
        schedule.setAutoCheckoutEnabled(true);
        schedule.setSimpleOpenMode(true);
        schedule.setStatus(WorkScheduleStatus.CHECK_IN_OPEN);
        return schedule;
    }
}
