package com.artdecor.workforce.api;

import com.artdecor.workforce.application.management.CreateEmployeeCommand;
import com.artdecor.workforce.application.management.EmployeeManagementService;
import com.artdecor.workforce.application.management.EmployeeResponse;
import com.artdecor.workforce.application.management.UpdateEmployeeCommand;
import com.artdecor.workforce.domain.WageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public AdminEmployeeController(EmployeeManagementService employees) {
        this.employees = employees;
    }

    @GetMapping
    public List<EmployeeResponse> listEmployees() {
        return employees.listEmployees();
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(employees.createEmployee(request.toCommand()));
    }

    @PatchMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable UUID employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request
    ) {
        return ResponseEntity.ok(employees.updateEmployee(employeeId, request.toCommand()));
    }

    @PostMapping("/{employeeId}/deactivate")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(@PathVariable UUID employeeId) {
        return ResponseEntity.ok(employees.deactivateEmployee(employeeId));
    }

    public record CreateEmployeeRequest(
            @NotBlank @Size(max = 40) String employeeCode,
            @NotBlank @Size(max = 160) String fullName,
            @Pattern(regexp = "\\d{4}", message = "PIN must be 4 digits.") String pin,
            @Size(max = 80) String phone,
            String profilePhotoUrl,
            String notes,
            WageType wageType,
            @DecimalMin("0.00") BigDecimal baseWage,
            @DecimalMin("1.00") BigDecimal overtimeMultiplier
    ) {
        CreateEmployeeCommand toCommand() {
            return new CreateEmployeeCommand(employeeCode, fullName, pin, phone, profilePhotoUrl, notes,
                    wageType, baseWage, overtimeMultiplier);
        }
    }

    public record UpdateEmployeeRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Pattern(regexp = "\\d{4}|", message = "PIN must be 4 digits.") String pin,
            @Size(max = 80) String phone,
            String profilePhotoUrl,
            String notes,
            WageType wageType,
            @DecimalMin("0.00") BigDecimal baseWage,
            @DecimalMin("1.00") BigDecimal overtimeMultiplier
    ) {
        UpdateEmployeeCommand toCommand() {
            return new UpdateEmployeeCommand(fullName, pin, phone, profilePhotoUrl, notes,
                    wageType, baseWage, overtimeMultiplier);
        }
    }
}

