package com.artdecor.workforce.api;

import com.artdecor.workforce.application.auth.AuthResponse;
import com.artdecor.workforce.application.auth.AuthService;
import com.artdecor.workforce.application.auth.CurrentUserResponse;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(authService.changePassword(principal, request.currentPassword(), request.newPassword()));
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ResponseEntity.ok(authService.currentUser(principal));
    }

    public record AdminLoginRequest(
            @Email(message = "Email-i i administratorit duhet të jetë valid.") @NotBlank(message = "Email-i i administratorit është i detyrueshëm.") String email,
            @NotBlank(message = "Fjalëkalimi i administratorit është i detyrueshëm.") String password
    ) {
    }

    public record EmployeeLoginRequest(
            @JsonAlias("username")
            @NotBlank(message = "ID e punëtorit është e detyrueshme.")
            String employeeCode,
            @JsonAlias("password")
            @NotBlank(message = "PIN është i detyrueshëm.")
            String pin
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank(message = "Fjalëkalimi aktual është i detyrueshëm.") String currentPassword,
            @NotBlank(message = "Fjalëkalimi i ri është i detyrueshëm.") @Size(min = 8, max = 128, message = "Fjalëkalimi i ri duhet të ketë të paktën 8 karaktere.") String newPassword
    ) {
    }
}
