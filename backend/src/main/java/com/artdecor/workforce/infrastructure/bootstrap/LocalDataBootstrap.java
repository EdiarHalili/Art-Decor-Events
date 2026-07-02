package com.artdecor.workforce.infrastructure.bootstrap;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(BootstrapProperties.class)
public class LocalDataBootstrap implements ApplicationRunner {
    private final BootstrapProperties properties;
    private final UserAccountRepository users;
    private final EmployeeRepository employees;
    private final PasswordEncoder passwordEncoder;

    public LocalDataBootstrap(
            BootstrapProperties properties,
            UserAccountRepository users,
            EmployeeRepository employees,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.users = users;
        this.employees = employees;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        users.findByEmailIgnoreCase(properties.adminEmail()).orElseGet(this::createAdmin);
        employees.findByEmployeeCodeIgnoreCase(properties.employeeCode()).orElseGet(this::createEmployee);
    }

    private UserAccountEntity createAdmin() {
        UserAccountEntity admin = new UserAccountEntity();
        admin.setFullName(properties.adminName());
        admin.setEmail(properties.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
        admin.setRole(UserRole.ADMINISTRATOR);
        return users.save(admin);
    }

    private EmployeeEntity createEmployee() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setFullName(properties.employeeName());
        employee.setEmployeeCode(properties.employeeCode());
        employee.setPinHash(passwordEncoder.encode(properties.employeePin()));
        return employees.save(employee);
    }
}
