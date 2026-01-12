package com.mai.siarsp.component;

import com.mai.siarsp.models.Employee;
import com.mai.siarsp.repo.RoleRepository;
import com.mai.siarsp.models.Role;
import com.mai.siarsp.service.employee.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class RoleRunner implements CommandLineRunner {
    private static final String ROLE_EMPLOYEE_ADMIN = "ROLE_EMPLOYEE_ADMIN";
    private static final String ROLE_EMPLOYEE__MANAGER = "ROLE_EMPLOYEE_MANAGER";
    private static final String ROLE_VISITOR = "ROLE_VISITOR";

    private final RoleRepository roleRepository;
    private final EmployeeService employeeService;

    public RoleRunner(RoleRepository roleRepository, EmployeeService employeeService) {
        this.roleRepository = roleRepository;
        this.employeeService = employeeService;
    }

    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotFound(ROLE_EMPLOYEE_ADMIN, "Администратор");
        createRoleIfNotFound(ROLE_EMPLOYEE__MANAGER, "Руководитель");
        createRoleIfNotFound(ROLE_VISITOR, "Посетитель");
        createAdminIfNotFound();
    }

    private void createRoleIfNotFound(String roleName, String description) {
        Optional<Role> roleOptional  = roleRepository.findByName(roleName);
        if (roleOptional.isEmpty()) {
            log.info("Роли = " + roleName + " не существует в БД. Создаем роль.");
            Role role = new Role();
            role.setName(roleName);
            role.setDescription(description);
            roleRepository.save(role);
            log.info("Создана роль: {}", roleName);
        }
    }

    private void createAdminIfNotFound() {
        if(employeeService.getEmployeeRepository().findByRoleName(ROLE_EMPLOYEE_ADMIN).isEmpty()){
            log.info("Администратора не существует в БД. Создаем администратора по умолчанию.");
            Employee admin = new Employee("Техническая", "учетная", "запись", "admin", "admin");
            employeeService.saveEmployee(admin, ROLE_EMPLOYEE_ADMIN);
        }
    }
}