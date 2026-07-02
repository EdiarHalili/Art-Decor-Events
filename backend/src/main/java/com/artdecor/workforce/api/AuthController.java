package com.artdecor.workforce.api;

import com.artdecor.workforce.application.auth.AuthResponse;
import com.artdecor.workforce.application.auth.AuthService;
import com.artdecor.workforce.application.auth.CurrentUserResponse;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(authService.loginAdmin(request.email(), request.password()));
    }

    @PostMapping("/employee/login")
    public ResponseEntity<AuthResponse> employeeLogin(@Valid @RequestBody EmployeeLoginRequest request) {
        return ResponseEntity.ok(authService.loginEmployee(request.employeeCode(), request.pin()));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(authService.currentUser(principal));
    }

    public record AdminLoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record EmployeeLoginRequest(
            @NotBlank String employeeCode,
            @Pattern(regexp = "\\d{4}", message = "PIN must be 4 digits.") String pin
    ) {
    }
}
