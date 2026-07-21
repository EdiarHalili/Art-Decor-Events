package com.artdecor.workforce.infrastructure.bootstrap;

import com.artdecor.workforce.domain.UserRole;
import com.artdecor.workforce.domain.UserStatus;
import com.artdecor.workforce.infrastructure.persistence.EmployeeEntity;
import com.artdecor.workforce.infrastructure.persistence.EmployeeRepository;
import com.artdecor.workforce.infrastructure.persistence.UserAccountEntity;
import com.artdecor.workforce.infrastructure.persistence.UserAccountRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }

        users.findByEmailIgnoreCase(properties.adminEmail())
                .map(this::repairAdmin)
                .orElseGet(this::createAdmin);
        employees.findByEmployeeCodeIgnoreCase(properties.employeeCode())
                .map(this::repairEmployee)
                .orElseGet(this::createEmployee);
    }

    private UserAccountEntity createAdmin() {
        UserAccountEntity admin = new UserAccountEntity();
        admin.setFullName(properties.adminName());
        admin.setEmail(properties.adminEmail());
        admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
        admin.setPasswordMustChange(false);
        admin.setRole(UserRole.SUPER_ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        return users.save(admin);
    }

    private UserAccountEntity repairAdmin(UserAccountEntity admin) {
        boolean changed = false;
        if (!passwordEncoder.matches(properties.adminPassword(), admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(properties.adminPassword()));
            admin.incrementTokenVersion();
            changed = true;
        }
        if (admin.getRole() != UserRole.SUPER_ADMIN) {
            admin.setRole(UserRole.SUPER_ADMIN);
            admin.incrementTokenVersion();
            changed = true;
        }
        if (admin.getStatus() != UserStatus.ACTIVE) {
            admin.setStatus(UserStatus.ACTIVE);
            admin.incrementTokenVersion();
            changed = true;
        }
        if (!properties.adminName().equals(admin.getFullName())) {
            admin.setFullName(properties.adminName());
            changed = true;
        }
        return changed ? users.save(admin) : admin;
    }

    private EmployeeEntity createEmployee() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setFullName(properties.employeeName());
        employee.setEmployeeCode(properties.employeeCode());
        employee.setPinHash(passwordEncoder.encode(properties.employeePin()));
        employee.setUserAccount(createEmployeeUser(properties.employeeName(), properties.employeeCode()));
        employee.setStatus(UserStatus.ACTIVE);
        return employees.save(employee);
    }

    private EmployeeEntity repairEmployee(EmployeeEntity employee) {
        boolean changed = false;
        if (!passwordEncoder.matches(properties.employeePin(), employee.getPinHash())) {
            employee.setPinHash(passwordEncoder.encode(properties.employeePin()));
            changed = true;
        }
        if (employee.getStatus() != UserStatus.ACTIVE) {
            employee.setStatus(UserStatus.ACTIVE);
            changed = true;
        }
        if (!properties.employeeName().equals(employee.getFullName())) {
            employee.setFullName(properties.employeeName());
            changed = true;
        }
        if (employee.getUserAccount() == null) {
            employee.setUserAccount(createEmployeeUser(properties.employeeName(), properties.employeeCode()));
            changed = true;
        } else {
            UserAccountEntity user = employee.getUserAccount();
            String employeePassword = defaultEmployeePassword();
            if (!passwordEncoder.matches(employeePassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(employeePassword));
                user.incrementTokenVersion();
                changed = true;
            }
            if (user.isPasswordMustChange()) {
                user.setPasswordMustChange(false);
                changed = true;
            }
            if (user.getRole() != UserRole.EMPLOYEE) {
                user.setRole(UserRole.EMPLOYEE);
                user.incrementTokenVersion();
                changed = true;
            }
            if (user.getStatus() != UserStatus.ACTIVE) {
                user.setStatus(UserStatus.ACTIVE);
                user.incrementTokenVersion();
                changed = true;
            }
        }
        return changed ? employees.save(employee) : employee;
    }

    private UserAccountEntity createEmployeeUser(String fullName, String employeeCode) {
        UserAccountEntity user = new UserAccountEntity();
        user.setFullName(fullName);
        user.setEmail(employeeCode.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(defaultEmployeePassword()));
        user.setPasswordMustChange(false);
        user.setRole(UserRole.EMPLOYEE);
        user.setStatus(UserStatus.ACTIVE);
        return users.save(user);
    }

    private String defaultEmployeePassword() {
        return properties.employeePin().length() >= 8 ? properties.employeePin() : properties.adminPassword();
    }
}
