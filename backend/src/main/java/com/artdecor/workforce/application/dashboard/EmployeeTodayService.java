package com.artdecor.workforce.application.dashboard;

import com.artdecor.workforce.application.auth.AuthException;
import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeTodayService {
    private final EmployeeRepository employees;
    private final ScheduleAssignmentRepository assignments;
    private final AttendanceRecordRepository attendanceRecords;
    private final Clock clock;

    public EmployeeTodayService(
            EmployeeRepository employees,
            ScheduleAssignmentRepository assignments,
            AttendanceRecordRepository attendanceRecords,
            Clock clock
    ) {
        this.employees = employees;
        this.assignments = assignments;
        this.attendanceRecords = attendanceRecords;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EmployeeTodayResponse today(AuthenticatedPrincipal principal) {
        var employee = employees.findById(principal.employeeId())
                .orElseThrow(() -> new AuthException("Authenticated employee no longer exists."));

        return assignments.findFirstByEmployeeIdAndScheduleWorkDateOrderByCreatedAtAsc(
                        employee.getId(),
                        LocalDate.now(clock)
                )
                .map(assignment -> {
                    var schedule = assignment.getSchedule();
                    Instant now = Instant.now(clock);
                    var attendance = attendanceRecords.findByScheduleIdAndEmployeeId(schedule.getId(), employee.getId());
                    boolean checkedIn = attendance.map(record -> record.getCheckedInAt() != null).orElse(false);
                    boolean checkedOut = attendance.map(record -> record.getCheckedOutAt() != null).orElse(false);
                    boolean checkInOpen = !checkedIn && isCheckInOpen(schedule.getStatus(), schedule.getCheckInOpensAt(), schedule.getCheckInClosesAt(), now);
                    boolean checkOutAvailable = checkedIn && !checkedOut;
                    String status = statusText(schedule.getStatus(), checkInOpen, checkOutAvailable, checkedOut);
                    return new EmployeeTodayResponse(
                            employee.getFullName(),
                            schedule.getId().toString(),
                            "Daily check-in window",
                            status,
                            checkInOpen,
                            checkOutAvailable,
                            List.of("Welcome to Art Decor Events Workforce.")
                    );
                })
                .orElseGet(() -> new EmployeeTodayResponse(
                        employee.getFullName(),
                        null,
                        "No assignment published for today.",
                        "Check-in is not open.",
                        false,
                        false,
                        List.of("Welcome to Art Decor Events Workforce.")
                ));
    }

    private boolean isCheckInOpen(WorkScheduleStatus status, Instant opensAt, Instant closesAt, Instant now) {
        if (status == WorkScheduleStatus.CANCELLED || status == WorkScheduleStatus.CHECK_IN_CLOSED) {
            return false;
        }
        if (status == WorkScheduleStatus.CHECK_IN_OPEN) {
            return true;
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
}
