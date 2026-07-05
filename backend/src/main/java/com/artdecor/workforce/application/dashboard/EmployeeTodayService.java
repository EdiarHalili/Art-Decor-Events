package com.artdecor.workforce.application.dashboard;

import com.artdecor.workforce.application.auth.AuthException;
import com.artdecor.workforce.application.attendance.SimpleOpenModeService;
import com.artdecor.workforce.application.notifications.NotificationService;
import com.artdecor.workforce.domain.CheckoutMode;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeTodayService {
    private static final Set<WorkScheduleStatus> INACTIVE_ASSIGNMENT_STATUSES = EnumSet.of(
            WorkScheduleStatus.CANCELLED,
            WorkScheduleStatus.COMPLETED
    );

    private final EmployeeRepository employees;
    private final ScheduleAssignmentRepository assignments;
    private final AttendanceRecordRepository attendanceRecords;
    private final NotificationService notifications;
    private final SimpleOpenModeService simpleOpenMode;
    private final Clock clock;

    public EmployeeTodayService(
            EmployeeRepository employees,
            ScheduleAssignmentRepository assignments,
            AttendanceRecordRepository attendanceRecords,
            NotificationService notifications,
            SimpleOpenModeService simpleOpenMode,
            Clock clock
    ) {
        this.employees = employees;
        this.assignments = assignments;
        this.attendanceRecords = attendanceRecords;
        this.notifications = notifications;
        this.simpleOpenMode = simpleOpenMode;
        this.clock = clock;
    }

    @Transactional
    public EmployeeTodayResponse today(AuthenticatedPrincipal principal) {
        var employee = employees.findById(principal.employeeId())
                .orElseThrow(() -> new AuthException("Authenticated employee no longer exists."));

        Instant now = Instant.now(clock);

        var scheduledAssignment = assignments.findCurrentScheduledAssignmentsForEmployee(
                        employee.getId(),
                        LocalDate.now(clock),
                        now,
                        INACTIVE_ASSIGNMENT_STATUSES
                )
                .stream()
                .findFirst();
        if (scheduledAssignment.isPresent()) {
            return responseForAssignment(employee.getFullName(), scheduledAssignment.get(), "Daily check-in window", false, now);
        }

        if (simpleOpenMode.hasScheduledWindowForDate(LocalDate.now(clock))) {
            return new EmployeeTodayResponse(
                        employee.getFullName(),
                        null,
                        "Daily check-in window",
                        "Employee is not assigned to today's scheduled window.",
                        false,
                        false,
                        null,
                        null,
                        false,
                        now,
                        announcements()
                );
        }

        return responseForAssignment(
                employee.getFullName(),
                simpleOpenMode.getOrCreateTodayAssignment(employee),
                "Simple Open Mode",
                true,
                now
        );
    }

    private EmployeeTodayResponse responseForAssignment(
            String employeeName,
            com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity assignment,
            String assignmentLabel,
            boolean simpleMode,
            Instant now
    ) {
        var schedule = assignment.getSchedule();
        var attendance = attendanceRecords.findByScheduleIdAndEmployeeId(schedule.getId(), assignment.getEmployee().getId());
        boolean checkedIn = attendance.map(record -> record.getCheckedInAt() != null).orElse(false);
        boolean checkedOut = attendance.map(record -> record.getCheckedOutAt() != null).orElse(false);
        boolean checkInOpen = !checkedIn && (simpleMode || isCheckInOpen(
                schedule.getStatus(),
                schedule.getCheckoutMode(),
                schedule.getCheckInOpensAt(),
                schedule.getCheckInClosesAt(),
                now
        ));
        boolean checkOutAvailable = checkedIn && !checkedOut;
        String status = simpleMode
                ? simpleStatusText(checkInOpen, checkOutAvailable, checkedOut)
                : statusText(schedule.getStatus(), checkInOpen, checkOutAvailable, checkedOut);
        return new EmployeeTodayResponse(
                employeeName,
                schedule.getId().toString(),
                assignmentLabel,
                status,
                checkInOpen,
                checkOutAvailable,
                simpleMode ? null : schedule.getCheckInOpensAt(),
                simpleMode ? null : schedule.getCheckInClosesAt(),
                simpleMode,
                now,
                announcements()
        );
    }

    private List<String> announcements() {
        List<String> visible = notifications.visibleAnnouncements().stream()
                .map(announcement -> announcement.title() + ": " + announcement.body())
                .toList();
        return visible.isEmpty() ? List.of("Welcome to Art Decor Events Workforce.") : visible;
    }

    private boolean isCheckInOpen(WorkScheduleStatus status, CheckoutMode checkoutMode, Instant opensAt, Instant closesAt, Instant now) {
        if (status == WorkScheduleStatus.CANCELLED || status == WorkScheduleStatus.CHECK_IN_CLOSED) {
            return false;
        }
        if (status == WorkScheduleStatus.CHECK_IN_OPEN) {
            return true;
        }
        if (checkoutMode == CheckoutMode.UNLIMITED_24_7) {
            return !now.isBefore(opensAt);
        }
        return !now.isBefore(opensAt) && !now.isAfter(closesAt);
    }

    private String statusText(
            WorkScheduleStatus status,
            boolean checkInOpen,
            boolean checkOutAvailable,
            boolean checkedOut
    ) {
        if (status == WorkScheduleStatus.CANCELLED) {
            return "Today's check-in window was cancelled.";
        }
        if (checkedOut) {
            return "Checked out.";
        }
        if (checkOutAvailable) {
            return "Checked in. Check-out is available.";
        }
        return checkInOpen ? "Check-in is open." : "Check-in is closed.";
    }

    private String simpleStatusText(boolean checkInOpen, boolean checkOutAvailable, boolean checkedOut) {
        if (checkedOut) {
            return "Checked out.";
        }
        if (checkOutAvailable) {
            return "Checked in. Check-out is available.";
        }
        return checkInOpen ? "Check In is available." : "Check-in is not available.";
    }
}
