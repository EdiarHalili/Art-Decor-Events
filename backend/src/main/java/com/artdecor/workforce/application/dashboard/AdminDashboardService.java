package com.artdecor.workforce.application.dashboard;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordEntity;
import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateEntity;
import com.artdecor.workforce.infrastructure.persistence.LiveLocationUpdateRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private final EmployeeRepository employees;
    private final UserAccountRepository users;
    private final AttendanceRecordRepository attendanceRecords;
    private final LiveLocationUpdateRepository liveLocations;

    public AdminDashboardService(
            EmployeeRepository employees,
            UserAccountRepository users,
            AttendanceRecordRepository attendanceRecords,
            LiveLocationUpdateRepository liveLocations
    ) {
        this.employees = employees;
        this.users = users;
        this.attendanceRecords = attendanceRecords;
        this.liveLocations = liveLocations;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse snapshot() {
        LocalDate today = LocalDate.now();
        List<AttendanceRecordEntity> todayRecords = attendanceRecords.findReportRecords(today, today);
        Map<UUID, AttendanceRecordEntity> latestRecordByEmployee = todayRecords.stream()
                .filter(record -> record.getCheckedInAt() != null)
                .collect(Collectors.toMap(
                        record -> record.getEmployee().getId(),
                        record -> record,
                        (first, second) -> latest(first).compareTo(latest(second)) >= 0 ? first : second
                ));
        long present = latestRecordByEmployee.size();
        long late = 0;
        long currentlyWorking = latestRecordByEmployee.values().stream()
                .filter(record -> record.getCheckedOutAt() == null)
                .count();
        long absent = 0;

        return new AdminDashboardResponse(
                today,
                present,
                late,
                absent,
                currentlyWorking,
                employees.countByStatus(UserStatus.ACTIVE),
                employees.countByStatus(UserStatus.INACTIVE),
                users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE),
                users.countByRoleAndStatus(UserRole.SUPERVISOR, UserStatus.ACTIVE),
                liveAttendance(latestRecordByEmployee.values().stream().toList()),
                liveLocations(latestRecordByEmployee.values().stream()
                        .filter(record -> record.getCheckedOutAt() == null)
                        .toList()),
                List.of("Shto punëtor", "Krijo dritare", "Publiko njoftim")
        );
    }

    private List<AdminLiveAttendanceRow> liveAttendance(List<AttendanceRecordEntity> records) {
        return records.stream()
                .filter(record -> record.getCheckedInAt() != null)
                .sorted(Comparator.comparing(AttendanceRecordEntity::getCheckedInAt).reversed())
                .map(record -> new AdminLiveAttendanceRow(
                        record.getId() == null ? null : record.getId().toString(),
                        record.getEmployee().getId().toString(),
                        record.getEmployee().getEmployeeCode(),
                        record.getEmployee().getFullName(),
                        record.getStatus().name(),
                        record.getCheckedInAt(),
                        record.getCheckedOutAt(),
                        record.getWorkedMinutes(),
                        record.getOvertimeMinutes(),
                        record.isAutoCheckout(),
                        record.getCheckoutType().name(),
                        false
                ))
                .toList();
    }

    private List<AdminLiveLocationRow> liveLocations(List<AttendanceRecordEntity> activeRecords) {
        if (activeRecords.isEmpty()) {
            return List.of();
        }
        Map<UUID, AttendanceRecordEntity> activeRecordById = activeRecords.stream()
                .collect(Collectors.toMap(AttendanceRecordEntity::getId, record -> record));
        Map<UUID, AdminLiveLocationRow> latestRows = liveLocations.findLatestForAttendanceRecords(activeRecordById.keySet())
                .stream()
                .sorted(Comparator.comparing(LiveLocationUpdateEntity::getCapturedAt).reversed())
                .map(update -> new AdminLiveLocationRow(
                        update.getEmployee().getId().toString(),
                        update.getEmployee().getEmployeeCode(),
                        update.getEmployee().getFullName(),
                        update.getAttendanceRecord().getId().toString(),
                        update.getLatitude(),
                        update.getLongitude(),
                        update.getAccuracyMeters(),
                        update.getCapturedAt()
                ))
                .collect(Collectors.toMap(
                        row -> UUID.fromString(row.attendanceRecordId()),
                        row -> row,
                        (first, second) -> first
                ));

        activeRecords.stream()
                .filter(record -> !latestRows.containsKey(record.getId()))
                .filter(record -> record.getCheckInLatitude() != null && record.getCheckInLongitude() != null)
                .forEach(record -> latestRows.put(record.getId(), new AdminLiveLocationRow(
                        record.getEmployee().getId().toString(),
                        record.getEmployee().getEmployeeCode(),
                        record.getEmployee().getFullName(),
                        record.getId().toString(),
                        record.getCheckInLatitude(),
                        record.getCheckInLongitude(),
                        null,
                        record.getCheckedInAt()
                )));

        return latestRows.values().stream()
                .sorted(Comparator.comparing(AdminLiveLocationRow::capturedAt).reversed())
                .toList();
    }

    private java.time.Instant latest(AttendanceRecordEntity record) {
        return record.getCheckedOutAt() == null ? record.getCheckedInAt() : record.getCheckedOutAt();
    }
}
