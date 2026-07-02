package com.artdecor.workforce.application.dashboard;

import com.artdecor.workforce.application.auth.AuthException;
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
    private final Clock clock;

    public EmployeeTodayService(EmployeeRepository employees, ScheduleAssignmentRepository assignments, Clock clock) {
        this.employees = employees;
        this.assignments = assignments;
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
                    boolean checkInOpen = !now.isBefore(schedule.getCheckInOpensAt())
                            && !now.isAfter(schedule.getCheckInClosesAt());
                    return new EmployeeTodayResponse(
                            employee.getFullName(),
                            schedule.getId().toString(),
                            schedule.getTitle(),
                            checkInOpen ? "Check-in is open." : "Check-in is not open.",
                            checkInOpen,
                            true,
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
}
