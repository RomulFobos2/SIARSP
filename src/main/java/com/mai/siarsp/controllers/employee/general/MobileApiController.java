package com.mai.siarsp.controllers.employee.general;

import com.mai.siarsp.dto.*;
import com.mai.siarsp.mapper.*;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.repo.AcceptanceActRepository;
import com.mai.siarsp.repo.TTNRepository;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.employee.manager.ClientService;
import com.mai.siarsp.service.employee.manager.ProductService;
import com.mai.siarsp.service.employee.manager.SupplierService;
import com.mai.siarsp.service.employee.manager.VehicleService;
import com.mai.siarsp.service.employee.warehouseManager.WarehouseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API контроллер для мобильного приложения
 *
 * Предоставляет GET-эндпоинты для просмотра справочников, задач и документов.
 * Доступ ограничен по ролям сотрудников. Все эндпоинты только для чтения.
 */
@RestController
@RequestMapping("/api/mobile")
@Transactional(readOnly = true)
public class MobileApiController {

    private final EmployeeService employeeService;
    private final ClientService clientService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final VehicleService vehicleService;
    private final ClientOrderService clientOrderService;
    private final DeliveryTaskService deliveryTaskService;
    private final TTNRepository ttnRepository;
    private final AcceptanceActRepository acceptanceActRepository;

    public MobileApiController(EmployeeService employeeService,
                               ClientService clientService,
                               SupplierService supplierService,
                               ProductService productService,
                               WarehouseService warehouseService,
                               VehicleService vehicleService,
                               ClientOrderService clientOrderService,
                               DeliveryTaskService deliveryTaskService,
                               TTNRepository ttnRepository,
                               AcceptanceActRepository acceptanceActRepository) {
        this.employeeService = employeeService;
        this.clientService = clientService;
        this.supplierService = supplierService;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.vehicleService = vehicleService;
        this.clientOrderService = clientOrderService;
        this.deliveryTaskService = deliveryTaskService;
        this.ttnRepository = ttnRepository;
        this.acceptanceActRepository = acceptanceActRepository;
    }

    // ========== ПРОФИЛЬ ==========

    /**
     * Текущий пользователь (ФИО, роль)
     * Доступ: все авторизованные сотрудники
     */
    @GetMapping("/profile")
    public ResponseEntity<EmployeeDTO> getProfile(@AuthenticationPrincipal Employee currentUser) {
        EmployeeDTO dto = EmployeeMapper.INSTANCE.toDTO(currentUser);
        return ResponseEntity.ok(dto);
    }

    // ========== КЛИЕНТЫ ==========

    /**
     * Список клиентов
     * Доступ: ADMIN, MANAGER, WAREHOUSE_MANAGER, ACCOUNTER
     */
    @GetMapping("/clients")
    public ResponseEntity<?> getClients(@AuthenticationPrincipal Employee currentUser) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_ADMIN", "ROLE_EMPLOYEE_MANAGER",
                "ROLE_EMPLOYEE_WAREHOUSE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        return ResponseEntity.ok(clientService.getAllClients());
    }

    /**
     * Детали клиента
     * Доступ: ADMIN, MANAGER, WAREHOUSE_MANAGER, ACCOUNTER
     */
    @GetMapping("/clients/{id}")
    public ResponseEntity<?> getClient(@AuthenticationPrincipal Employee currentUser, @PathVariable Long id) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_ADMIN", "ROLE_EMPLOYEE_MANAGER",
                "ROLE_EMPLOYEE_WAREHOUSE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        ClientDTO client = clientService.getClientById(id);
        if (client == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Клиент не найден"));
        }
        return ResponseEntity.ok(client);
    }

    // ========== ПОСТАВЩИКИ ==========

    /**
     * Список поставщиков
     * Доступ: ADMIN, MANAGER, WAREHOUSE_MANAGER, ACCOUNTER
     */
    @GetMapping("/suppliers")
    public ResponseEntity<?> getSuppliers(@AuthenticationPrincipal Employee currentUser) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_ADMIN", "ROLE_EMPLOYEE_MANAGER",
                "ROLE_EMPLOYEE_WAREHOUSE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        return ResponseEntity.ok(supplierService.getAllSuppliers());
    }

    // ========== ТОВАРЫ ==========

    /**
     * Список товаров
     * Доступ: все кроме COURIER
     */
    @GetMapping("/products")
    public ResponseEntity<?> getProducts(@AuthenticationPrincipal Employee currentUser) {
        if (hasAnyRole(currentUser, "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * Детали товара
     * Доступ: все кроме COURIER
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProduct(@AuthenticationPrincipal Employee currentUser, @PathVariable Long id) {
        if (hasAnyRole(currentUser, "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        List<ProductDTO> products = productService.getAllProducts();
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Товар не найден")));
    }

    // ========== СКЛАДЫ ==========

    /**
     * Список складов
     * Доступ: все авторизованные сотрудники
     */
    @GetMapping("/warehouses")
    public ResponseEntity<List<WarehouseDTO>> getWarehouses() {
        List<WarehouseDTO> warehouses = WarehouseMapper.INSTANCE.toDTOList(warehouseService.getAllWarehouses());
        return ResponseEntity.ok(warehouses);
    }

    // ========== ТРАНСПОРТ ==========

    /**
     * Список транспорта
     * Доступ: MANAGER, COURIER
     */
    @GetMapping("/vehicles")
    public ResponseEntity<?> getVehicles(@AuthenticationPrincipal Employee currentUser) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_MANAGER", "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        return ResponseEntity.ok(vehicleService.getAllVehicles());
    }

    // ========== ЗАКАЗЫ ==========

    /**
     * Список заказов
     * Доступ: все кроме COURIER
     */
    @GetMapping("/orders")
    public ResponseEntity<?> getOrders(@AuthenticationPrincipal Employee currentUser) {
        if (hasAnyRole(currentUser, "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        return ResponseEntity.ok(clientOrderService.getAllOrders());
    }

    /**
     * Детали заказа
     * Доступ: все кроме COURIER
     */
    @GetMapping("/orders/{id}")
    public ResponseEntity<?> getOrder(@AuthenticationPrincipal Employee currentUser, @PathVariable Long id) {
        if (hasAnyRole(currentUser, "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        var orderOpt = clientOrderService.getOrderById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Заказ не найден"));
        }
        return ResponseEntity.ok(ClientOrderMapper.INSTANCE.toDTO(orderOpt.get()));
    }

    // ========== ЗАДАЧИ НА ДОСТАВКУ ==========

    /**
     * Список задач на доставку
     * Доступ: MANAGER, WAREHOUSE_MANAGER, COURIER
     */
    @GetMapping("/deliveryTasks")
    public ResponseEntity<?> getDeliveryTasks(@AuthenticationPrincipal Employee currentUser) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_MANAGER", "ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
                "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        return ResponseEntity.ok(deliveryTaskService.getAllTasks());
    }

    // ========== ДОКУМЕНТЫ ==========

    /**
     * Список ТТН
     * Доступ: MANAGER, ACCOUNTER, WAREHOUSE_MANAGER, COURIER
     */
    @GetMapping("/documents/ttn")
    public ResponseEntity<?> getTTNList(@AuthenticationPrincipal Employee currentUser) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER",
                "ROLE_EMPLOYEE_WAREHOUSE_MANAGER", "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        List<TTNDTO> ttns = TTNMapper.INSTANCE.toDTOList(ttnRepository.findAll());
        return ResponseEntity.ok(ttns);
    }

    /**
     * Список актов приёма-передачи
     * Доступ: MANAGER, ACCOUNTER, WAREHOUSE_MANAGER, COURIER
     */
    @GetMapping("/documents/acts")
    public ResponseEntity<?> getAcceptanceActs(@AuthenticationPrincipal Employee currentUser) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER",
                "ROLE_EMPLOYEE_WAREHOUSE_MANAGER", "ROLE_EMPLOYEE_COURIER")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        List<AcceptanceActDTO> acts = AcceptanceActMapper.INSTANCE.toDTOList(acceptanceActRepository.findAll());
        return ResponseEntity.ok(acts);
    }

    // ========== СОТРУДНИКИ ==========

    /**
     * Список сотрудников
     * Доступ: только ADMIN
     */
    @GetMapping("/employees")
    public ResponseEntity<?> getEmployees(@AuthenticationPrincipal Employee currentUser) {
        if (!hasAnyRole(currentUser, "ROLE_EMPLOYEE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Нет доступа"));
        }
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private boolean hasAnyRole(Employee employee, String... roles) {
        String userRole = employee.getRole().getAuthority();
        for (String role : roles) {
            if (userRole.equals(role)) {
                return true;
            }
        }
        return false;
    }
}
