package com.artdecor.workforce.api;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.management.CreateEmployeeCommand;
import com.artdecor.workforce.application.management.EmployeeManagementService;
import com.artdecor.workforce.application.management.EmployeeResponse;
import com.artdecor.workforce.application.management.PasswordResetResponse;
import com.artdecor.workforce.application.management.UpdateEmployeeCommand;
import com.artdecor.workforce.domain.WageType;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/employees")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'SUPERVISOR')")
public class AdminEmployeeController {
    private final EmployeeManagementService employees;
    private final AuditService audit;

    public AdminEmployeeController(EmployeeManagementService employees, AuditService audit) {
        this.employees = employees;
        this.audit = audit;
    }

    @GetMapping
    public List<EmployeeResponse> listEmployees() {
        return employees.listEmployees();
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreateEmployeeRequest request
    ) {
        EmployeeResponse response = employees.createEmployee(request.toCommand());
        audit.log(principal, "EMPLOYEE_CREATED", "EMPLOYEE", UUID.fromString(response.id()));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        EmployeeResponse response = employees.updateEmployee(employeeId, request.toCommand());
        audit.log(principal, "EMPLOYEE_UPDATED", "EMPLOYEE", employeeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{employeeId}/deactivate")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID employeeId
    ) {
        EmployeeResponse response = employees.deactivateEmployee(employeeId);
        audit.log(principal, "EMPLOYEE_DEACTIVATED", "EMPLOYEE", employeeId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{employeeId}/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID employeeId
    ) {
        PasswordResetResponse response = employees.resetEmployeePassword(employeeId);
        audit.log(principal, "EMPLOYEE_PASSWORD_RESET", "EMPLOYEE", employeeId);
        return ResponseEntity.ok(response);
    }

    public record CreateEmployeeRequest(
            @NotBlank @Size(max = 40) String employeeCode,
            @NotBlank @Size(max = 160) String fullName,
            @NotBlank @Size(min = 8, max = 128) String password,
            @Size(max = 80) String phone,
            String profilePhotoUrl,
            @Size(max = 120) String positionTitle,
            @Size(max = 120) String departmentName,
            @Size(max = 120) String teamName,
            String notes,
            WageType wageType,
            @DecimalMin("0.00") BigDecimal baseWage,
            @DecimalMin("1.00") BigDecimal overtimeMultiplier
    ) {
        CreateEmployeeCommand toCommand() {
            return new CreateEmployeeCommand(employeeCode, fullName, password, phone, profilePhotoUrl,
                    positionTitle, departmentName, teamName, notes, wageType, baseWage, overtimeMultiplier);
        }
    }

    public record UpdateEmployeeRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Size(min = 8, max = 128) String password,
            @Size(max = 80) String phone,
            String profilePhotoUrl,
            @Size(max = 120) String positionTitle,
            @Size(max = 120) String departmentName,
            @Size(max = 120) String teamName,
            String notes,
            WageType wageType,
            @DecimalMin("0.00") BigDecimal baseWage,
            @DecimalMin("1.00") BigDecimal overtimeMultiplier
    ) {
        UpdateEmployeeCommand toCommand() {
            return new UpdateEmployeeCommand(fullName, password, phone, profilePhotoUrl,
                    positionTitle, departmentName, teamName, notes, wageType, baseWage, overtimeMultiplier);
        }
    }
}
