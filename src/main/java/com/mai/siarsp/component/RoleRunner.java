package com.mai.siarsp.component;

import com.mai.siarsp.enumeration.AttributeType;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.ProductAttributeRepository;
import com.mai.siarsp.repo.ProductCategoryRepository;
import com.mai.siarsp.repo.RoleRepository;
import com.mai.siarsp.models.Role;
import com.mai.siarsp.service.employee.EmployeeService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class RoleRunner implements CommandLineRunner {
    private static final String ROLE_EMPLOYEE_ADMIN = "ROLE_EMPLOYEE_ADMIN";
    private static final String ROLE_EMPLOYEE_MANAGER = "ROLE_EMPLOYEE_MANAGER";
    private static final String ROLE_EMPLOYEE_WAREHOUSE_MANAGER = "ROLE_EMPLOYEE_WAREHOUSE_MANAGER";
    private static final String ROLE_EMPLOYEE_WAREHOUSE_WORKER = "ROLE_EMPLOYEE_WAREHOUSE_WORKER";
    private static final String ROLE_EMPLOYEE_COURIER = "ROLE_EMPLOYEE_COURIER";
    private static final String ROLE_EMPLOYEE_ACCOUNTER = "ROLE_EMPLOYEE_ACCOUNTER";

    private final RoleRepository roleRepository;
    private final EmployeeService employeeService;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductAttributeRepository productAttributeRepository;

    public RoleRunner(RoleRepository roleRepository, EmployeeService employeeService, ProductCategoryRepository productCategoryRepository, ProductAttributeRepository productAttributeRepository) {
        this.roleRepository = roleRepository;
        this.employeeService = employeeService;
        this.productCategoryRepository = productCategoryRepository;
        this.productAttributeRepository = productAttributeRepository;
    }

    @Transactional
    @Override
    public void run(String... args) throws Exception {
        createRoleIfNotFound(ROLE_EMPLOYEE_ADMIN, "Администратор");
        createRoleIfNotFound(ROLE_EMPLOYEE_MANAGER, "Руководитель");
        createRoleIfNotFound(ROLE_EMPLOYEE_WAREHOUSE_MANAGER, "Заведующий складом");
        createRoleIfNotFound(ROLE_EMPLOYEE_WAREHOUSE_WORKER, "Складской работник");
        createRoleIfNotFound(ROLE_EMPLOYEE_COURIER, "Водитель экспедитор");
        createRoleIfNotFound(ROLE_EMPLOYEE_ACCOUNTER, "Бухгалтер");
        createAdminIfNotFound();
        createProductAttributeGabarite();
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

    public void createProductAttributeGabarite() {
        List<ProductCategory> categories = productCategoryRepository.findAllWithAttributes();
        log.info("🔧 Загружено {} категорий из БД", categories.size());

        addAttributeIfNotExists("Длина упаковки", "см", AttributeType.NUMBER, categories);
        addAttributeIfNotExists("Ширина упаковки", "см", AttributeType.NUMBER, categories);
        addAttributeIfNotExists("Высота упаковки", "см", AttributeType.NUMBER, categories);

        productCategoryRepository.saveAll(categories);
        log.info("✅ Атрибуты добавлены и категории сохранены.");
    }

    private void addAttributeIfNotExists(String name, String unit, AttributeType dataType, List<ProductCategory> categories) {
        List<ProductAttribute> existing = productAttributeRepository.findByNameAndUnit(name, unit);

        ProductAttribute attribute;
        if (existing.isEmpty()) {
            attribute = new ProductAttribute(name, unit, dataType, new ArrayList<>());
            productAttributeRepository.save(attribute);
            log.info("➕ Создан новый атрибут: '{}' ({}) [тип: {}]", name, unit, dataType);
        } else {
            attribute = existing.get(0);
            log.info("📦 Найден существующий атрибут: '{}' ({}) [id={}, тип={}]",
                    name, unit, attribute.getId(), attribute.getDataType());
        }

        int addedCount = 0;
        for (ProductCategory category : categories) {
            if (!category.getAttributes().contains(attribute)) {
                category.getAttributes().add(attribute);
                addedCount++;
                log.debug("➡️ Добавлен атрибут '{}' к категории '{}'", name, category.getName());
            }
        }

        log.info("📊 Атрибут '{}' добавлен в {} категорий", name, addedCount);
    }
}