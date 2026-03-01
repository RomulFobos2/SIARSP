package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.DeliveryTaskDTO;
import com.mai.siarsp.enumeration.*;
import com.mai.siarsp.mapper.DeliveryTaskMapper;
import com.mai.siarsp.models.*;
import com.mai.siarsp.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Сервис управления задачами на доставку
 *
 * Бизнес-процесс доставки:
 * 1. Заведующий складом создаёт задачу → PENDING, назначает водителя и автомобиль
 * 2. Складской работник выполняет погрузку → LOADING, завершает → LOADED, ClientOrder SHIPPED, stock↓, ZoneProduct↓
 * 3. Бухгалтер оформляет ТТН
 * 4. Водитель-экспедитор начинает доставку → IN_TRANSIT, завершает → DELIVERED, ClientOrder DELIVERED
 */
@Service
@Slf4j
public class DeliveryTaskService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    private final DeliveryTaskRepository deliveryTaskRepository;
    private final ClientOrderRepository clientOrderRepository;
    private final VehicleRepository vehicleRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final TTNRepository ttnRepository;
    private final AcceptanceActRepository acceptanceActRepository;
    private final ZoneProductRepository zoneProductRepository;
    private final NotificationService notificationService;

    public DeliveryTaskService(DeliveryTaskRepository deliveryTaskRepository,
                               ClientOrderRepository clientOrderRepository,
                               VehicleRepository vehicleRepository,
                               EmployeeRepository employeeRepository,
                               ProductRepository productRepository,
                               TTNRepository ttnRepository,
                               AcceptanceActRepository acceptanceActRepository,
                               ZoneProductRepository zoneProductRepository,
                               NotificationService notificationService) {
        this.deliveryTaskRepository = deliveryTaskRepository;
        this.clientOrderRepository = clientOrderRepository;
        this.vehicleRepository = vehicleRepository;
        this.employeeRepository = employeeRepository;
        this.productRepository = productRepository;
        this.ttnRepository = ttnRepository;
        this.acceptanceActRepository = acceptanceActRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.notificationService = notificationService;
    }

    // ========== ЗАПРОСЫ ==========

    /**
     * Запрос на маршрутную точку при создании задачи
     */
    public record RoutePointRequest(RoutePointType pointType, String address,
                                    Double latitude, Double longitude, String comment) {}

    @Transactional(readOnly = true)
    public List<DeliveryTaskDTO> getAllTasks() {
        List<DeliveryTask> tasks = deliveryTaskRepository.findAllWithDetails();
        return DeliveryTaskMapper.INSTANCE.toDTOList(tasks);
    }

    @Transactional(readOnly = true)
    public List<DeliveryTaskDTO> getTasksByStatuses(List<DeliveryTaskStatus> statuses) {
        List<DeliveryTask> tasks = deliveryTaskRepository.findByStatusInWithDetails(statuses);
        return DeliveryTaskMapper.INSTANCE.toDTOList(tasks);
    }

    @Transactional(readOnly = true)
    public List<DeliveryTaskDTO> getTasksByDriver(Long driverId) {
        List<DeliveryTask> tasks = deliveryTaskRepository.findByDriverIdWithDetails(driverId);
        return DeliveryTaskMapper.INSTANCE.toDTOList(tasks);
    }

    @Transactional(readOnly = true)
    public List<DeliveryTaskDTO> getTasksByDriverAndStatuses(Long driverId, List<DeliveryTaskStatus> statuses) {
        List<DeliveryTask> tasks = deliveryTaskRepository.findByDriverIdAndStatusInWithDetails(driverId, statuses);
        return DeliveryTaskMapper.INSTANCE.toDTOList(tasks);
    }

    @Transactional(readOnly = true)
    public Optional<DeliveryTask> getTaskById(Long id) {
        return deliveryTaskRepository.findByIdWithDetails(id);
    }

    @Transactional(readOnly = true)
    public List<Employee> getAvailableDrivers() {
        return employeeRepository.findAllByRoleName("ROLE_EMPLOYEE_COURIER").stream()
                .filter(Employee::isActive)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getAvailableVehicles() {
        return vehicleRepository.findByStatus(VehicleStatus.AVAILABLE);
    }

    // ========== СОЗДАНИЕ ЗАДАЧИ (WAREHOUSE_MANAGER) ==========

    /**
     * Создаёт задачу на доставку для заказа
     */
    @Transactional
    public boolean createTask(Long orderId, Long driverId, Long vehicleId,
                              LocalDateTime plannedStartTime, LocalDateTime plannedEndTime,
                              List<RoutePointRequest> routePoints) {
        try {
            Optional<ClientOrder> optOrder = clientOrderRepository.findById(orderId);
            if (optOrder.isEmpty()) {
                log.error("Заказ с id={} не найден", orderId);
                return false;
            }
            ClientOrder order = optOrder.get();

            if (order.getStatus() != ClientOrderStatus.READY) {
                log.error("Заказ №{} не в статусе READY (текущий: {})", order.getOrderNumber(), order.getStatus());
                return false;
            }

            if (deliveryTaskRepository.existsByClientOrder(order)) {
                log.error("Для заказа №{} уже существует задача на доставку", order.getOrderNumber());
                return false;
            }

            Optional<Employee> optDriver = employeeRepository.findById(driverId);
            if (optDriver.isEmpty()) {
                log.error("Водитель с id={} не найден", driverId);
                return false;
            }
            Employee driver = optDriver.get();

            if (!driver.getRole().getName().equals("ROLE_EMPLOYEE_COURIER")) {
                log.error("Сотрудник {} не является водителем-экспедитором", driver.getFullName());
                return false;
            }

            Optional<Vehicle> optVehicle = vehicleRepository.findById(vehicleId);
            if (optVehicle.isEmpty()) {
                log.error("Автомобиль с id={} не найден", vehicleId);
                return false;
            }
            Vehicle vehicle = optVehicle.get();

            if (!vehicle.isAvailable()) {
                log.error("Автомобиль {} не доступен (статус: {})", vehicle.getFullName(), vehicle.getStatus());
                return false;
            }

            DeliveryTask task = new DeliveryTask(order, driver, vehicle);
            task.setPlannedStartTime(plannedStartTime);
            task.setPlannedEndTime(plannedEndTime);

            // Добавить маршрутные точки
            if (routePoints != null) {
                for (int i = 0; i < routePoints.size(); i++) {
                    RoutePointRequest rpr = routePoints.get(i);
                    RoutePoint rp = new RoutePoint(i + 1, rpr.pointType(),
                            rpr.latitude(), rpr.longitude(), rpr.address());
                    rp.setComment(rpr.comment());
                    task.addRoutePoint(rp);
                }
            }

            // Автомобиль → IN_USE
            vehicle.setStatus(VehicleStatus.IN_USE);
            vehicleRepository.save(vehicle);

            deliveryTaskRepository.save(task);

            // Уведомления
            notificationService.createNotification(driver,
                    "Вам назначена задача на доставку заказа №" + order.getOrderNumber()
                            + ". Клиент: " + order.getClient().getOrganizationName());

            notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER",
                    "Создана задача на доставку заказа №" + order.getOrderNumber()
                            + " — необходимо оформить ТТН.");

            notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_WORKER",
                    "Создана задача на доставку заказа №" + order.getOrderNumber()
                            + " — ожидает погрузки.");

            log.info("Создана задача на доставку заказа №{}: водитель {}, авто {}",
                    order.getOrderNumber(), driver.getFullName(), vehicle.getFullName());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при создании задачи на доставку: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== ПОГРУЗКА (WAREHOUSE_WORKER) ==========

    /**
     * Начинает погрузку (PENDING → LOADING)
     */
    @Transactional
    public boolean startLoading(Long taskId) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getStatus() != DeliveryTaskStatus.PENDING) {
                log.error("Задача id={} не в статусе PENDING (текущий: {})", taskId, task.getStatus());
                return false;
            }

            task.setStatus(DeliveryTaskStatus.LOADING);
            deliveryTaskRepository.save(task);

            log.info("Задача id={}: погрузка начата (заказ №{})",
                    taskId, task.getClientOrder().getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при начале погрузки: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Завершает погрузку: ClientOrder READY → SHIPPED, stockQuantity↓, reservedQuantity↓
     */
    @Transactional
    public boolean completeLoading(Long taskId) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getStatus() != DeliveryTaskStatus.LOADING) {
                log.error("Задача id={} не в статусе LOADING (текущий: {})", taskId, task.getStatus());
                return false;
            }

            ClientOrder order = task.getClientOrder();
            if (order.getStatus() != ClientOrderStatus.READY) {
                log.error("Заказ №{} не в статусе READY (текущий: {})", order.getOrderNumber(), order.getStatus());
                return false;
            }

            // Списание со склада: уменьшить stockQuantity, reservedQuantity и ZoneProduct
            for (OrderedProduct op : order.getOrderedProducts()) {
                Product product = op.getProduct();
                product.setStockQuantity(Math.max(0, product.getStockQuantity() - op.getQuantity()));
                product.setReservedQuantity(Math.max(0, product.getReservedQuantity() - op.getQuantity()));
                productRepository.save(product);

                // Списание из зон хранения
                List<ZoneProduct> zoneProducts = zoneProductRepository.findByProduct(product);
                int remaining = op.getQuantity();
                for (ZoneProduct zp : zoneProducts) {
                    if (remaining <= 0) break;
                    if (zp.getQuantity() <= remaining) {
                        remaining -= zp.getQuantity();
                        zoneProductRepository.delete(zp);
                    } else {
                        zp.setQuantity(zp.getQuantity() - remaining);
                        zoneProductRepository.save(zp);
                        remaining = 0;
                    }
                }
            }

            // DeliveryTask → LOADED (ожидает отправки)
            task.setStatus(DeliveryTaskStatus.LOADED);

            // ClientOrder → SHIPPED
            order.setStatus(ClientOrderStatus.SHIPPED);
            clientOrderRepository.save(order);

            deliveryTaskRepository.save(task);

            // Уведомление водителю
            notificationService.createNotification(task.getDriver(),
                    "Погрузка заказа №" + order.getOrderNumber() + " завершена. Можно начинать доставку.");

            log.info("Задача id={}: погрузка завершена (статус LOADED), заказ №{} отгружен",
                    taskId, order.getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при завершении погрузки: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== ОФОРМЛЕНИЕ ТТН (ACCOUNTER) ==========

    /**
     * Создаёт ТТН для задачи на доставку
     */
    @Transactional
    public boolean createTTN(Long taskId, String cargoDescription,
                             Double totalWeight, Double totalVolume, String comment) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getTtn() != null) {
                log.error("Для задачи id={} ТТН уже оформлена", taskId);
                return false;
            }

            String ttnNumber = generateTTNNumber();
            TTN ttn = new TTN(ttnNumber, task, task.getVehicle(), task.getDriver());
            ttn.setCargoDescription(cargoDescription);
            ttn.setTotalWeight(totalWeight);
            ttn.setTotalVolume(totalVolume);
            ttn.setComment(comment);

            ttnRepository.save(ttn);

            // Обновить номер ТТН в задаче
            task.setTtnNumber(ttnNumber);
            deliveryTaskRepository.save(task);

            log.info("Оформлена ТТН {} для задачи id={} (заказ №{})",
                    ttnNumber, taskId, task.getClientOrder().getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при оформлении ТТН: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== ДОСТАВКА (COURIER) ==========

    /**
     * Начинает доставку (LOADED → IN_TRANSIT)
     */
    @Transactional
    public boolean startDelivery(Long taskId, Integer startMileage) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getStatus() != DeliveryTaskStatus.LOADED) {
                log.error("Задача id={} не в статусе LOADED (текущий: {})", taskId, task.getStatus());
                return false;
            }

            task.setStatus(DeliveryTaskStatus.IN_TRANSIT);
            task.setActualStartTime(LocalDateTime.now());
            task.setStartMileage(startMileage);
            deliveryTaskRepository.save(task);

            log.info("Задача id={}: доставка начата, начальный пробег {} км",
                    taskId, startMileage);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при начале доставки: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Обновляет GPS-координаты водителя
     */
    @Transactional
    public boolean updateLocation(Long taskId, Double latitude, Double longitude) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getStatus() != DeliveryTaskStatus.IN_TRANSIT) {
                log.error("Задача id={} не в статусе IN_TRANSIT", taskId);
                return false;
            }

            task.setCurrentLatitude(latitude);
            task.setCurrentLongitude(longitude);
            deliveryTaskRepository.save(task);

            return true;

        } catch (Exception e) {
            log.error("Ошибка при обновлении GPS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Отмечает маршрутную точку как пройденную
     */
    @Transactional
    public boolean markRoutePointReached(Long taskId, Long routePointId) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getStatus() != DeliveryTaskStatus.IN_TRANSIT) {
                log.error("Задача id={} не в статусе IN_TRANSIT", taskId);
                return false;
            }

            RoutePoint targetPoint = null;
            for (RoutePoint rp : task.getRoutePoints()) {
                if (rp.getId().equals(routePointId)) {
                    targetPoint = rp;
                    break;
                }
            }

            if (targetPoint == null) {
                log.error("Маршрутная точка id={} не найдена в задаче id={}", routePointId, taskId);
                return false;
            }

            targetPoint.setReached(true);
            targetPoint.setActualArrivalTime(LocalDateTime.now());
            deliveryTaskRepository.save(task);

            log.info("Задача id={}: маршрутная точка '{}' пройдена",
                    taskId, targetPoint.getAddress());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отметке маршрутной точки: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Завершает доставку: IN_TRANSIT → DELIVERED, ClientOrder → DELIVERED, AcceptanceAct
     */
    @Transactional
    public boolean completeDelivery(Long taskId, Integer endMileage,
                                    String clientRepresentative, String actComment) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getStatus() != DeliveryTaskStatus.IN_TRANSIT) {
                log.error("Задача id={} не в статусе IN_TRANSIT (текущий: {})", taskId, task.getStatus());
                return false;
            }

            // Задача → DELIVERED
            task.setStatus(DeliveryTaskStatus.DELIVERED);
            task.setActualEndTime(LocalDateTime.now());
            task.setEndMileage(endMileage);

            // ClientOrder → DELIVERED
            ClientOrder order = task.getClientOrder();
            order.setStatus(ClientOrderStatus.DELIVERED);
            order.setActualDeliveryDate(LocalDate.now());
            clientOrderRepository.save(order);

            // Vehicle → AVAILABLE
            Vehicle vehicle = task.getVehicle();
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);

            // AcceptanceAct
            String actNumber = generateActNumber();
            AcceptanceAct act = new AcceptanceAct(actNumber, order, order.getClient(), task.getDriver());
            act.setComment(actComment);
            if (clientRepresentative != null && !clientRepresentative.isBlank()) {
                act.markAsSigned(clientRepresentative);
            }
            acceptanceActRepository.save(act);

            deliveryTaskRepository.save(task);

            // Уведомления
            notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER",
                    "Заказ №" + order.getOrderNumber() + " доставлен клиенту "
                            + order.getClient().getOrganizationName() + ".");

            notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
                    "Заказ №" + order.getOrderNumber() + " доставлен.");

            log.info("Задача id={}: доставка завершена, заказ №{} доставлен, пробег {} км",
                    taskId, order.getOrderNumber(), task.getTotalMileage());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при завершении доставки: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== ОТМЕНА ==========

    /**
     * Отменяет задачу (только PENDING или LOADING)
     */
    @Transactional
    public boolean cancelTask(Long taskId) {
        try {
            Optional<DeliveryTask> optTask = deliveryTaskRepository.findById(taskId);
            if (optTask.isEmpty()) {
                log.error("Задача с id={} не найдена", taskId);
                return false;
            }
            DeliveryTask task = optTask.get();

            if (task.getStatus() != DeliveryTaskStatus.PENDING
                    && task.getStatus() != DeliveryTaskStatus.LOADING) {
                log.error("Задачу id={} нельзя отменить (статус: {})", taskId, task.getStatus());
                return false;
            }

            task.setStatus(DeliveryTaskStatus.CANCELLED);

            // Vehicle → AVAILABLE
            Vehicle vehicle = task.getVehicle();
            vehicle.setStatus(VehicleStatus.AVAILABLE);
            vehicleRepository.save(vehicle);

            deliveryTaskRepository.save(task);

            // Уведомление водителю
            notificationService.createNotification(task.getDriver(),
                    "Задача на доставку заказа №" + task.getClientOrder().getOrderNumber() + " отменена.");

            log.info("Задача id={} отменена (заказ №{})", taskId, task.getClientOrder().getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отмене задачи: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private String generateTTNNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String number;
        do {
            int num = 1000 + RANDOM.nextInt(9000);
            number = "ТТН-" + datePart + "-" + num;
        } while (ttnRepository.existsByTtnNumber(number));
        return number;
    }

    private String generateActNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String number;
        do {
            int num = 1000 + RANDOM.nextInt(9000);
            number = "АПП-" + datePart + "-" + num;
        } while (acceptanceActRepository.existsByActNumber(number));
        return number;
    }
}
