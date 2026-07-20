package com.artdecor.workforce.api;

import com.artdecor.workforce.application.management.CreateUserCommand;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminUserController {
    private final UserManagementService users;

    public AdminUserController(UserManagementService users) {
        this.users = users;
    }

    @GetMapping
    public List<UserResponse> listUsers() {
        return users.listUsers();
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(users.createUser(request.toCommand()));
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(users.deactivateUser(principal, userId));
    }

    public record CreateUserRequest(
            @NotBlank @Size(max = 160) String fullName,
            @Email @NotBlank @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 128) String password,
            @NotNull UserRole role
    ) {
        CreateUserCommand toCommand() {
            return new CreateUserCommand(fullName, email, password, role);
        }
    }
}
