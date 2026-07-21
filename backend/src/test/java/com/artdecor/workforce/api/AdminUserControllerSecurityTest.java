package com.artdecor.workforce.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.management.PasswordResetResponse;
import com.artdecor.workforce.application.management.UserManagementService;
import com.artdecor.workforce.application.management.UserResponse;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.JwtTokenService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:admin-user-controller-security;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class AdminUserControllerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenService tokens;

    @MockBean
    private UserManagementService userManagement;

    @MockBean
    private AuditService audit;

    @MockBean
    private UserAccountRepository users;

    @Test
    void userCreationRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(UserRole.SUPERVISOR)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userCreationRejectsSupervisor() throws Exception {
        mvc.perform(post("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(UserRole.SUPERVISOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(UserRole.SUPERVISOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminCanCreateAdministrativeUser() throws Exception {
        when(userManagement.createUser(any(), any())).thenReturn(response(UserRole.SUPERVISOR));

        mvc.perform(post("/api/v1/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(UserRole.SUPER_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(UserRole.SUPERVISOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPERVISOR"));
    }

    @Test
    void superAdminCanChangeRoleAndResetPassword() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userManagement.changeRole(any(), any(), any())).thenReturn(response(UserRole.ADMINISTRATOR));
        when(userManagement.resetPassword(any(), any(), any())).thenReturn(new PasswordResetResponse(null, true));
        String token = tokenFor(UserRole.SUPER_ADMIN);

        mvc.perform(patch("/api/v1/admin/users/{userId}/role", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMINISTRATOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMINISTRATOR"));

        mvc.perform(post("/api/v1/admin/users/{userId}/reset-password", userId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temporaryPassword\":\"NewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordMustChange").value(true));
    }

    private String createBody(UserRole role) {
        return """
                {
                  "fullName": "Supervisor",
                  "email": "supervisor@artdecor.test",
                  "password": "ChangeMe123",
                  "role": "%s"
                }
                """.formatted(role.name());
    }

    private UserResponse response(UserRole role) {
        return new UserResponse(
                UUID.randomUUID().toString(),
                "Supervisor",
                "supervisor@artdecor.test",
                role.name(),
                UserStatus.ACTIVE.name(),
                Instant.parse("2026-07-03T00:00:00Z"),
                null
        );
    }

    private String tokenFor(UserRole role) {
        UUID userId = UUID.randomUUID();
        when(users.findById(userId)).thenReturn(Optional.of(user(userId, role)));
        return tokens.issueToken(userId, role, null);
    }

    private UserAccountEntity user(UUID userId, UserRole role) {
        UserAccountEntity user = new UserAccountEntity();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setFullName("User");
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
