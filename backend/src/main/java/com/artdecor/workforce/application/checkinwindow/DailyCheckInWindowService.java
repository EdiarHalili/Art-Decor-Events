package com.artdecor.workforce.application.checkinwindow;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyCheckInWindowService {
    private static final String WINDOW_TITLE = "Daily check-in window";

    private final WorkScheduleRepository windows;
    private final ScheduleAssignmentRepository assignments;
    private final AttendanceRecordRepository attendanceRecords;
    private final EmployeeRepository employees;

    public DailyCheckInWindowService(
            WorkScheduleRepository windows,
            ScheduleAssignmentRepository assignments,
            AttendanceRecordRepository attendanceRecords,
            EmployeeRepository employees
    ) {
        this.windows = windows;
        this.assignments = assignments;
        this.attendanceRecords = attendanceRecords;
        this.employees = employees;
    }

    @Transactional(readOnly = true)
    public List<DailyCheckInWindowResponse> listWindows() {
        return windows.findAllByOrderByWorkDateDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DailyCheckInWindowResponse createWindow(DailyCheckInWindowCommand command) {
        validate(command);
        if (windows.existsByWorkDateAndStatusNot(command.workDate(), WorkScheduleStatus.CANCELLED)) {
            throw new DailyCheckInWindowException("A daily check-in window already exists for this date.");
        }

        WorkScheduleEntity window = new WorkScheduleEntity();
        apply(window, command);
        window.setStatus(WorkScheduleStatus.PUBLISHED);

        WorkScheduleEntity saved = windows.save(window);
        replaceAssignments(saved, command.employeeIds());
        return toResponse(saved);
    }

    @Transactional
    public DailyCheckInWindowResponse updateWindow(UUID windowId, DailyCheckInWindowCommand command) {
        validate(command);
        WorkScheduleEntity window = findWindow(windowId);
        if (window.getStatus() == WorkScheduleStatus.CANCELLED) {
            throw new DailyCheckInWindowException("Cancelled daily check-in windows cannot be edited.");
        }
        if (windows.existsByWorkDateAndStatusNotAndIdNot(command.workDate(), WorkScheduleStatus.CANCELLED, windowId)) {
            throw new DailyCheckInWindowException("A daily check-in window already exists for this date.");
        }

        apply(window, command);
        replaceAssignments(window, command.employeeIds());
        return toResponse(window);
    }

    @Transactional
    public DailyCheckInWindowResponse openWindow(UUID windowId) {
        WorkScheduleEntity window = findWindow(windowId);
        ensureNotCancelled(window);
        window.setStatus(WorkScheduleStatus.CHECK_IN_OPEN);
        return toResponse(window);
    }

    @Transactional
    public DailyCheckInWindowResponse closeWindow(UUID windowId) {
        WorkScheduleEntity window = findWindow(windowId);
        ensureNotCancelled(window);
        window.setStatus(WorkScheduleStatus.CHECK_IN_CLOSED);
        return toResponse(window);
    }

    @Transactional
    public DailyCheckInWindowResponse cancelWindow(UUID windowId) {
        WorkScheduleEntity window = findWindow(windowId);
        window.setStatus(WorkScheduleStatus.CANCELLED);
        return toResponse(window);
    }

    @Transactional
    public void deleteCancelledWindow(UUID windowId) {
        WorkScheduleEntity window = findWindow(windowId);
        if (window.getStatus() != WorkScheduleStatus.CANCELLED) {
            throw new DailyCheckInWindowException("Only cancelled daily check-in windows can be deleted.");
        }
        attendanceRecords.deleteByScheduleId(windowId);
        assignments.deleteByScheduleId(windowId);
        windows.delete(window);
    }

    @Transactional
    public DailyCheckInWindowResponse replaceEmployees(UUID windowId, Set<UUID> employeeIds) {
        WorkScheduleEntity window = findWindow(windowId);
        ensureNotCancelled(window);
        replaceAssignments(window, employeeIds);
        return toResponse(window);
    }

    private WorkScheduleEntity findWindow(UUID windowId) {
        return windows.findById(windowId)
                .orElseThrow(() -> new DailyCheckInWindowException("Daily check-in window not found."));
    }

    private void apply(WorkScheduleEntity window, DailyCheckInWindowCommand command) {
        window.setTitle(WINDOW_TITLE);
        window.setDescription(null);
        window.setWorkDate(command.workDate());
        window.setCheckInOpensAt(command.checkInOpensAt());
        window.setCheckInClosesAt(command.checkInClosesAt());
        window.setPlannedStartAt(command.checkInClosesAt());
        window.setPlannedEndAt(null);
    }

    private void replaceAssignments(WorkScheduleEntity window, Set<UUID> employeeIds) {
        assignments.deleteByScheduleId(window.getId());
        for (UUID employeeId : employeeIds) {
            var employee = employees.findById(employeeId)
                    .orElseThrow(() -> new DailyCheckInWindowException("Selected employee was not found."));
            ScheduleAssignmentEntity assignment = new ScheduleAssignmentEntity();
            assignment.setSchedule(window);
            assignment.setEmployee(employee);
            assignments.save(assignment);
        }
    }

    private void validate(DailyCheckInWindowCommand command) {
        if (command.checkInClosesAt().isBefore(command.checkInOpensAt())
                || command.checkInClosesAt().equals(command.checkInOpensAt())) {
            throw new DailyCheckInWindowException("Check-in close time must be after open time.");
        }
        if (command.employeeIds() == null || command.employeeIds().isEmpty()) {
            throw new DailyCheckInWindowException("Select at least one allowed employee.");
        }
    }

    private void ensureNotCancelled(WorkScheduleEntity window) {
        if (window.getStatus() == WorkScheduleStatus.CANCELLED) {
            throw new DailyCheckInWindowException("Cancelled daily check-in windows cannot be changed.");
        }
    }

    private DailyCheckInWindowResponse toResponse(WorkScheduleEntity window) {
        List<String> employeeIds = assignments.findAllByScheduleId(window.getId()).stream()
                .map(assignment -> assignment.getEmployee().getId().toString())
                .toList();

        return new DailyCheckInWindowResponse(
                window.getId().toString(),
                window.getWorkDate(),
                window.getCheckInOpensAt(),
                window.getCheckInClosesAt(),
                window.getStatus().name(),
                employeeIds,
                employeeIds.size(),
                window.getCreatedAt(),
                window.getUpdatedAt()
        );
    }
}
