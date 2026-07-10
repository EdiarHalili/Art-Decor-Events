package com.artdecor.workforce.application.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.artdecor.workforce.domain.WorkScheduleStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentEntity;
import com.artdecor.workforce.infrastructure.persistence.ScheduleAssignmentRepository;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleEntity;
import com.artdecor.workforce.infrastructure.persistence.WorkScheduleRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SimpleOpenModeServiceTest {
    private final WorkScheduleRepository schedules = org.mockito.Mockito.mock(WorkScheduleRepository.class);
    private final ScheduleAssignmentRepository assignments = org.mockito.Mockito.mock(ScheduleAssignmentRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-07-03T06:55:00Z"), ZoneOffset.UTC);
    private final SimpleOpenModeService service = new SimpleOpenModeService(schedules, assignments, clock);

    @Test
    void createsSimpleOpenScheduleAndAssignmentWhenNoScheduledWindowExists() {
        EmployeeEntity employee = employee();
        when(schedules.existsByWorkDateAndSimpleOpenModeFalseAndStatusIn(any(), any())).thenReturn(false);
        when(schedules.findFirstByWorkDateAndSimpleOpenModeTrueOrderByCreatedAtAsc(LocalDate.of(2026, 7, 3)))
                .thenReturn(Optional.empty());
        when(schedules.save(any(WorkScheduleEntity.class))).thenAnswer(invocation -> {
            WorkScheduleEntity schedule = invocation.getArgument(0);
            ReflectionTestUtils.setField(schedule, "id", UUID.randomUUID());
            return schedule;
        });
        when(assignments.findByScheduleIdAndEmployeeId(any(), any())).thenReturn(Optional.empty());
        when(assignments.save(any(ScheduleAssignmentEntity.class))).thenAnswer(invocation -> {
            ScheduleAssignmentEntity assignment = invocation.getArgument(0);
            ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
            return assignment;
        });

        ScheduleAssignmentEntity assignment = service.getOrCreateTodayAssignment(employee);

        assertThat(assignment.getEmployee()).isEqualTo(employee);
        assertThat(assignment.getSchedule().isSimpleOpenMode()).isTrue();
        assertThat(assignment.getSchedule().isAutoCheckoutEnabled()).isTrue();
        assertThat(assignment.getSchedule().getCheckInClosesAt()).isEqualTo(Instant.parse("2026-07-03T23:59:00Z"));
        assertThat(assignment.getSchedule().getStatus()).isEqualTo(WorkScheduleStatus.CHECK_IN_OPEN);
    }

    private EmployeeEntity employee() {
        EmployeeEntity employee = new EmployeeEntity();
        ReflectionTestUtils.setField(employee, "id", UUID.randomUUID());
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("hashed");
        return employee;
    }
}
