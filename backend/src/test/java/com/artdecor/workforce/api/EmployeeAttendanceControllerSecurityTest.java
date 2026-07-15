package com.artdecor.workforce.api;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.artdecor.workforce.application.reports.AttendanceReportService;
import com.artdecor.workforce.application.reports.ExportFile;
import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.infrastructure.security.JwtTokenService;
import java.time.LocalDate;
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
        String token = tokens.issueToken(UUID.randomUUID(), UserRole.EMPLOYEE, employeeId);

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
}
