package com.artdecor.workforce.application.payroll;

import com.artdecor.workforce.infrastructure.persistence.AttendanceRecordRepository;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import java.time.YearMonth;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayrollPreparationService {
    private final EmployeeRepository employees;
    private final AttendanceRecordRepository attendanceRecords;

    public PayrollPreparationService(EmployeeRepository employees, AttendanceRecordRepository attendanceRecords) {
        this.employees = employees;
        this.attendanceRecords = attendanceRecords;
    }

    @Transactional(readOnly = true)
    public PayrollPreparationResponse prepare(YearMonth month) {
        var records = attendanceRecords.findReportRecords(month.atDay(1), month.atEndOfMonth());
        Map<UUID, Totals> totalsByEmployee = records.stream().collect(Collectors.toMap(
                record -> record.getEmployee().getId(),
                record -> new Totals(record.getWorkedMinutes(), record.getOvertimeMinutes()),
                Totals::plus
        ));

        var rows = employees.findAll().stream()
                .map(employee -> {
                    Totals totals = totalsByEmployee.getOrDefault(employee.getId(), new Totals(0, 0));
                    return new PayrollPreparationEmployeeResponse(
                            employee.getId().toString(),
                            employee.getEmployeeCode(),
                            employee.getFullName(),
                            employee.getWageType().name(),
                            employee.getBaseWage(),
                            employee.getOvertimeMultiplier(),
                            totals.workedMinutes,
                            totals.overtimeMinutes
                    );
                })
                .toList();

        return new PayrollPreparationResponse(month, "PREPARATION_ONLY", rows);
    }

    private record Totals(int workedMinutes, int overtimeMinutes) {
        private Totals plus(Totals other) {
            return new Totals(workedMinutes + other.workedMinutes, overtimeMinutes + other.overtimeMinutes);
        }
    }
}
