package com.artdecor.workforce.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artdecor.workforce.application.audit.AuditService;
import com.artdecor.workforce.application.management.EmployeeManagementService;
import com.artdecor.workforce.application.management.ManagementConflictException;
import com.artdecor.workforce.application.management.ManagementNotFoundException;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.infrastructure.security.AuthenticatedPrincipal;
import com.artdecor.workforce.infrastructure.security.JwtTokenService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:admin-employee-controller-security;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class AdminEmployeeControllerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenService tokens;

    @MockBean
    private EmployeeManagementService employees;

    @MockBean
    private AuditService audit;

    @Test
    void employeeDeletionRequiresAuthentication() throws Exception {
        mvc.perform(delete("/api/v1/admin/employees/{employeeId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeDeletionRejectsNonAdminUsers() throws Exception {
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.SUPERVISOR, null);

        mvc.perform(delete("/api/v1/admin/employees/{employeeId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeDeletionReturnsNoContentForAdministrator() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);

        mvc.perform(delete("/api/v1/admin/employees/{employeeId}", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void employeeDeletionReturnsNotFoundForUnknownEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);
        doThrow(new ManagementNotFoundException("Punëtori nuk u gjet."))
                .when(employees).deleteEmployee(eq(employeeId), any(AuthenticatedPrincipal.class));

        mvc.perform(delete("/api/v1/admin/employees/{employeeId}", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Punëtori nuk u gjet."));
    }

    @Test
    void employeeDeletionReturnsConflictWhenHistoryExists() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);
        String message = "Ky punëtor ka histori pune dhe nuk mund të fshihet përgjithmonë. Çaktivizojeni për të ruajtur raportet dhe të dhënat historike.";
        doThrow(new ManagementConflictException(message))
                .when(employees).deleteEmployee(eq(employeeId), any(AuthenticatedPrincipal.class));

        mvc.perform(delete("/api/v1/admin/employees/{employeeId}", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void forceEmployeeDeletionRejectsNonAdminUsers() throws Exception {
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.SUPERVISOR, null);

        mvc.perform(delete("/api/v1/admin/employees/{employeeId}/force", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void forceEmployeeDeletionReturnsNoContentForAdministrator() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);

        mvc.perform(delete("/api/v1/admin/employees/{employeeId}/force", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void forceEmployeeDeletionReturnsNotFoundForUnknownEmployee() throws Exception {
        UUID employeeId = UUID.randomUUID();
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.ADMINISTRATOR, null);
        doThrow(new ManagementNotFoundException("Punëtori nuk u gjet."))
                .when(employees).forceDeleteEmployee(eq(employeeId), any(AuthenticatedPrincipal.class));

        mvc.perform(delete("/api/v1/admin/employees/{employeeId}/force", employeeId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Punëtori nuk u gjet."));
    }

}
