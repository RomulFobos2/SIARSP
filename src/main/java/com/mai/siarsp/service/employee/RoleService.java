package com.mai.siarsp.service.employee;

import com.mai.siarsp.models.Role;
import com.mai.siarsp.repo.EmployeeRepository;
import com.mai.siarsp.repo.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сервис управления ролями сотрудников
 *
 * Позволяет создавать, редактировать и удалять роли.
 * 6 фундаментальных ролей (созданных при инициализации системы)
 * не могут быть удалены или переименованы — допускается только
 * изменение их описания (description).
 */
@Service
@Slf4j
public class RoleService {

    private static final Set<String> FUNDAMENTAL_ROLES = Set.of(
            "ROLE_EMPLOYEE_ADMIN",
            "ROLE_EMPLOYEE_MANAGER",
            "ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
            "ROLE_EMPLOYEE_WAREHOUSE_WORKER",
            "ROLE_EMPLOYEE_COURIER",
            "ROLE_EMPLOYEE_ACCOUNTER"
    );

    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;

    public RoleService(RoleRepository roleRepository, EmployeeRepository employeeRepository) {
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Optional<Role> getRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public boolean isFundamental(Role role) {
        return FUNDAMENTAL_ROLES.contains(role.getName());
    }

    @Transactional
    public boolean createRole(String name, String description) {
        log.info("Создание новой роли: name={}, description={}", name, description);

        if (roleRepository.findByName(name).isPresent()) {
            log.error("Роль с именем {} уже существует.", name);
            return false;
        }

        try {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            roleRepository.save(role);
        } catch (Exception e) {
            log.error("Ошибка при создании роли: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Роль {} успешно создана.", name);
        return true;
    }

    @Transactional
    public boolean updateRoleDescription(Long id, String description) {
        Optional<Role> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            log.error("Роль с id={} не найдена.", id);
            return false;
        }

        Role role = roleOptional.get();
        log.info("Обновление описания роли {}: {} -> {}", role.getName(), role.getDescription(), description);
        role.setDescription(description);

        try {
            roleRepository.save(role);
        } catch (Exception e) {
            log.error("Ошибка при обновлении роли: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Описание роли {} успешно обновлено.", role.getName());
        return true;
    }

    @Transactional
    public boolean updateRole(Long id, String name, String description) {
        Optional<Role> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            log.error("Роль с id={} не найдена.", id);
            return false;
        }

        Role role = roleOptional.get();

        if (isFundamental(role)) {
            log.error("Невозможно изменить техническое имя фундаментальной роли {}.", role.getName());
            return false;
        }

        Optional<Role> existingRole = roleRepository.findByName(name);
        if (existingRole.isPresent() && !existingRole.get().getId().equals(id)) {
            log.error("Роль с именем {} уже существует.", name);
            return false;
        }

        log.info("Обновление роли id={}: name={}, description={}", id, name, description);
        role.setName(name);
        role.setDescription(description);

        try {
            roleRepository.save(role);
        } catch (Exception e) {
            log.error("Ошибка при обновлении роли: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Роль успешно обновлена.");
        return true;
    }

    @Transactional
    public boolean deleteRole(Long id) {
        Optional<Role> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            log.error("Роль с id={} не найдена.", id);
            return false;
        }

        Role role = roleOptional.get();

        if (isFundamental(role)) {
            log.error("Невозможно удалить фундаментальную роль {}.", role.getName());
            return false;
        }

        if (!employeeRepository.findByRoleName(role.getName()).isEmpty()) {
            log.error("Невозможно удалить роль {} — есть сотрудники с этой ролью.", role.getName());
            return false;
        }

        try {
            roleRepository.delete(role);
        } catch (Exception e) {
            log.error("Ошибка при удалении роли: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Роль {} успешно удалена.", role.getName());
        return true;
    }
}
