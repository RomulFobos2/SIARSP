package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.EmployeeDTO;
import com.mai.siarsp.mapper.EmployeeMapper;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Role;
import com.mai.siarsp.repo.EmployeeRepository;
import com.mai.siarsp.repo.RoleRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Optional;

@Service
@Getter
@Slf4j
public class EmployeeService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public EmployeeService(EmployeeRepository employeeRepository, RoleRepository roleRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Не найден сотрудник с username = " + username + "."));
    }

    @Transactional
    public boolean saveEmployee(Employee newEmployee, String roleName) {
        log.info("Начинаем сохранять сотрудника с username = {}...", newEmployee.getUsername());

        if (checkUserName(newEmployee.getUsername())) {
            log.error("Сотрудник с username = {} уже существует. Используйте другой username.", newEmployee.getUsername());
            return false;
        }

        Optional<Role> roleOptional = roleRepository.findByName(roleName);

        if (roleOptional.isEmpty()) {
            log.error("Роль {} не найдена в базе данных. Невозможно создать сотрудника.", roleName);
            return false;
        }

        Role role = roleOptional.get();
        newEmployee.setRole(role);
        newEmployee.setPassword(bCryptPasswordEncoder.encode(newEmployee.getPassword()));

        try {
            employeeRepository.save(newEmployee);
        } catch (Exception e) {
            log.error("Ошибка при сохранении сотрудника {}: {}", newEmployee.getUsername(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Сотрудник с username = {} ({}) успешно сохранён.", newEmployee.getUsername(), role.getDescription());

        return true;
    }

    @Transactional
    public boolean editEmployee(Long id,
                                String inputLastName, String inputFirstName,
                                String inputPatronymicName, String inputUsername,
                                String roleName) {
        Optional<Employee> employeeOptional = employeeRepository.findById(id);

        if (employeeOptional.isEmpty()) {
            log.error("Не найден сотрудник с id = {}...", id);
            return false;
        }

        if (employeeRepository.existsByUsernameAndIdNot(inputUsername, id)) {
            log.error("Сотрудник с username = {} уже существует. Используйте другой username.", inputUsername);
            return false;
        }

        Optional<Role> roleOptional = roleRepository.findByName(roleName);

        if (roleOptional.isEmpty()) {
            log.error("Роль {} не найдена в базе данных. Невозможно отредактировать сотрудника.", roleName);
            return false;
        }

        Role role = roleOptional.get();
        Employee employee = employeeOptional.get();

        log.info("Начинаем сохранять изменения для сотрудника с username = {}...", employee.getUsername());

        employee.setRole(role);
        employee.setFirstName(inputFirstName);
        employee.setLastName(inputLastName);
        employee.setPatronymicName(inputPatronymicName.isEmpty() ? inputPatronymicName : "");
        employee.setUsername(inputUsername);

        try {
            employeeRepository.save(employee);
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения для сотрудника успешно сохранены.");

        return true;
    }

    @Transactional
    public boolean resetEmployeePassword(long id, String newPassword) {
        Optional<Employee> employeeOptional = employeeRepository.findById(id);

        if (employeeOptional.isEmpty()) {
            log.error("Не найден сотрудник с id = {}...", id);
            return false;
        }

        Employee employee = employeeOptional.get();

        log.info("Начинаем сохранять новый пароль для сотрудника с username = {}...", employee.getUsername());

        employee.setPassword(bCryptPasswordEncoder.encode(newPassword));
        employee.setNeedChangePass(true);

        try {
            employeeRepository.save(employee);
        } catch (Exception e) {
            log.error("Ошибка при сохранении нового пароля сотрудника: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Новый пароль для сотрудника успешно сохранен.");

        return true;
    }

    @Transactional
    public boolean deleteEmployee(long id) {
        Optional<Employee> employeeOptional = employeeRepository.findById(id);

        if (employeeOptional.isEmpty()) {
            log.error("Не найден сотрудник с id = {}...", id);
            return false;
        }

        Employee employee = employeeOptional.get();

        log.info("Начинаем удаление сотрудника с username = {}...", employee.getUsername());

        try {
            employeeRepository.delete(employee);
        } catch (Exception e) {
            log.error("Ошибка при удалении сотрудника: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Сотрудник успешно удалён.");

        return true;
    }

    public boolean checkUserName(String username) {
        return employeeRepository.existsByUsername(username);
    }

    public boolean accountEmployeeLocked(Long id) {
        Optional<Employee> employeeOptional = employeeRepository.findById(id);

        if (employeeOptional.isEmpty()) {
            log.error("Не найден сотрудник с id = {}...", id);
            return false;
        }

        Employee employee = employeeOptional.get();
        log.info("Начинаем блокировку аккаунта сотрудника с username = {}...", employee.getUsername());
        employee.setActive(false);

        try {
            employeeRepository.save(employee);
        } catch (Exception e) {
            log.error("Ошибка при блокировке аккаунта сотрудника: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
        log.info("Аккаунт сотрудника успешно заблокирован.");
        return true;
    }

    public boolean accountEmployeeUnlocked(Long id) {
        Optional<Employee> employeeOptional = employeeRepository.findById(id);

        if (employeeOptional.isEmpty()) {
            log.error("Не найден сотрудник с id = {}...", id);
            return false;
        }

        Employee employee = employeeOptional.get();
        log.info("Начинаем разблокировку аккаунта сотрудника с username = {}...", employee.getUsername());
        employee.setActive(true);

        try {
            employeeRepository.save(employee);
        } catch (Exception e) {
            log.error("Ошибка при разблокировке аккаунта сотрудника: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
        log.info("Аккаунт сотрудника успешно разблокирован.");
        return true;
    }

    public List<EmployeeDTO> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll()
                .stream().filter(e -> !e.getId().equals(getAuthenticationEmployeeDTO().getId()))
                .toList();
        return EmployeeMapper.INSTANCE.toDTOList(employees);
    }

    public EmployeeDTO getAuthenticationEmployeeDTO() {
        Optional<Employee> employeeOptional = employeeRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        return employeeOptional.map(EmployeeMapper.INSTANCE::toDTO).orElse(null);
    }

    public Employee getAuthenticationEmployee() {
        return employeeRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName()).orElse(null);
    }

    public boolean changePassword(String newPassword) {
        Optional<Employee> employeeOptional = employeeRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();
            employee.setPassword(newPassword);
            employee.setNeedChangePass(false);
            employeeRepository.save(employee);
            return true;
        }
        return false;
    }
}
