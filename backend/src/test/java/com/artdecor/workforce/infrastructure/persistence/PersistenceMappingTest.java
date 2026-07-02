package com.artdecor.workforce.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:workforce-persistence;MODE=PostgreSQL;DATABASE_TO_UPPER=false;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PersistenceMappingTest {
    @Autowired
    private UserAccountRepository users;

    @Autowired
    private EmployeeRepository employees;

    @Test
    void persistsUsersAndEmployeesWithRepositoryQueries() {
        UserAccountEntity admin = new UserAccountEntity();
        admin.setFullName("Administrator");
        admin.setEmail("admin@artdecor.test");
        admin.setPasswordHash("hash");
        admin.setRole(UserRole.ADMINISTRATOR);
        users.save(admin);

        EmployeeEntity employee = new EmployeeEntity();
        employee.setEmployeeCode("EMP001");
        employee.setFullName("Season Worker");
        employee.setPinHash("pin-hash");
        employees.save(employee);

        assertThat(users.existsByEmailIgnoreCase("ADMIN@ARTDECOR.TEST")).isTrue();
        assertThat(users.countByRoleAndStatus(UserRole.ADMINISTRATOR, UserStatus.ACTIVE)).isEqualTo(1);
        assertThat(employees.existsByEmployeeCodeIgnoreCase("emp001")).isTrue();
        assertThat(employees.countByStatus(UserStatus.ACTIVE)).isEqualTo(1);
    }
}
