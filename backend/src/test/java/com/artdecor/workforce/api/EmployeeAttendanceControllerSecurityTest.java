package com.artdecor.workforce.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artdecor.workforce.application.reports.AttendanceReportService;
import com.artdecor.workforce.application.reports.ExportFile;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import com.artdecor.workforce.infrastructure.security.JwtTokenService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:employee-attendance-controller-security;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
@AutoConfigureMockMvc
class EmployeeAttendanceControllerSecurityTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtTokenService tokens;

    @MockBean
    private AttendanceReportService reports;

    @MockBean
    private UserAccountRepository users;

    @MockBean
    private EmployeeRepository employees;

    @Test
    void employeeExportRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/employee/attendance/export")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31")
                        .param("format", "pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void employeeExportUsesEmployeeFromBearerToken() throws Exception {
        UUID employeeId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 7, 1);
        LocalDate to = LocalDate.of(2026, 7, 31);
        UUID userId = UUID.randomUUID();
        String token = tokens.issueToken(userId, UserRole.EMPLOYEE, employeeId);
        when(users.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(employees.existsByIdAndStatusAndUserAccountId(employeeId, UserStatus.ACTIVE, userId)).thenReturn(true);

        when(reports.exportEmployee(eq(employeeId), eq(from), eq(to), eq("pdf")))
                .thenReturn(new ExportFile("Historia_Punes_EMP001_2026-07.pdf", "application/pdf", "%PDF".getBytes()));

        mvc.perform(get("/api/v1/employee/attendance/export")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("from", from.toString())
                        .param("to", to.toString())
                        .param("format", "pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Historia_Punes_EMP001_2026-07.pdf\""));
    }

    private UserAccountEntity user(UUID userId) {
        UserAccountEntity user = new UserAccountEntity();
        ReflectionTestUtils.setField(user, "id", userId);
        user.setFullName("Employee");
        user.setEmail("emp001");
        user.setPasswordHash("hash");
        user.setRole(UserRole.EMPLOYEE);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
