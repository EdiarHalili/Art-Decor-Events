package com.artdecor.workforce.api;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.management.CreateUserCommand;
import com.artdecor.workforce.application.management.PasswordResetResponse;
import com.artdecor.workforce.application.management.UpdateUserCommand;
import com.artdecor.workforce.application.management.UserManagementService;
import com.artdecor.workforce.application.management.UserResponse;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRATOR')")
public class AdminUserController {
    private final UserManagementService users;
    private final AuditService audit;

    public AdminUserController(UserManagementService users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return users.listUsers();
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = users.createUser(principal, request.toCommand());
        audit.log(principal, "ADMIN_USER_CREATED", "USER", UUID.fromString(response.id()));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        UserResponse response = users.updateUser(principal, userId, request.toCommand());
        audit.log(principal, "ADMIN_USER_UPDATED", "USER", userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserResponse> changeRole(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request
    ) {
        UserResponse response = users.changeRole(principal, userId, request.role());
        audit.log(principal, "ADMIN_USER_ROLE_CHANGED", "USER", userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<UserResponse> activateUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID userId
    ) {
        UserResponse response = users.activateUser(principal, userId);
        audit.log(principal, "ADMIN_USER_ACTIVATED", "USER", userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID userId
    ) {
        UserResponse response = users.deactivateUser(principal, userId);
        audit.log(principal, "ADMIN_USER_DEACTIVATED", "USER", userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        PasswordResetResponse response = users.resetPassword(principal, userId, request.temporaryPassword());
        audit.log(principal, "ADMIN_USER_PASSWORD_RESET", "USER", userId);
        return ResponseEntity.ok(response);
    }

    public record CreateUserRequest(
            @NotBlank(message = "Emri i plotë është i detyrueshëm.") @Size(max = 160, message = "Emri i plotë duhet të ketë 160 karaktere ose më pak.") String fullName,
            @Email(message = "Email-i duhet të jetë valid.") @NotBlank(message = "Email-i është i detyrueshëm.") @Size(max = 190, message = "Email-i duhet të ketë 190 karaktere ose më pak.") String email,
            @NotBlank(message = "Fjalëkalimi i përkohshëm është i detyrueshëm.") @Size(min = 8, max = 128, message = "Fjalëkalimi duhet të ketë të paktën 8 karaktere.") String password,
            @NotNull UserRole role
    ) {
        CreateUserCommand toCommand() {
            return new CreateUserCommand(fullName, email, password, role);
        }
    }

    public record UpdateUserRequest(
            @NotBlank(message = "Emri i plotë është i detyrueshëm.") @Size(max = 160, message = "Emri i plotë duhet të ketë 160 karaktere ose më pak.") String fullName,
            @Email(message = "Email-i duhet të jetë valid.") @NotBlank(message = "Email-i është i detyrueshëm.") @Size(max = 190, message = "Email-i duhet të ketë 190 karaktere ose më pak.") String email
    ) {
        UpdateUserCommand toCommand() {
            return new UpdateUserCommand(fullName, email);
        }
    }

    public record ChangeRoleRequest(
            @NotNull(message = "Roli është i detyrueshëm.") UserRole role
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank(message = "Fjalëkalimi i përkohshëm është i detyrueshëm.") @Size(min = 8, max = 128, message = "Fjalëkalimi duhet të ketë të paktën 8 karaktere.") String temporaryPassword
    ) {
    }
}
