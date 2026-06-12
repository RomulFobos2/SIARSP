package com.mai.siarsp.component;

import com.mai.siarsp.enumeration.EquipmentStatus;
import com.mai.siarsp.enumeration.WriteOffActStatus;
import com.mai.siarsp.enumeration.WriteOffReason;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.WarehouseEquipment;
import com.mai.siarsp.models.WriteOffAct;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.EmployeeRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.WarehouseEquipmentRepository;
import com.mai.siarsp.repo.WriteOffActRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import com.mai.siarsp.service.employee.NotificationService;
import com.mai.siarsp.service.employee.WriteOffActService;
import com.mai.siarsp.service.general.ProductExpirationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class ScheduleTask {

    private static final String MANAGER_ROLE = "ROLE_EMPLOYEE_MANAGER";
    private static final String ROLE_EMPLOYEE_WAREHOUSE_MANAGER = "ROLE_EMPLOYEE_WAREHOUSE_MANAGER";

    private final WarehouseEquipmentRepository warehouseEquipmentRepository;
    private final ProductRepository productRepository;
    private final ZoneProductRepository zoneProductRepository;
    private final WriteOffActRepository writeOffActRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final ProductExpirationService productExpirationService;
    private final WriteOffActService writeOffActService;

    public ScheduleTask(WarehouseEquipmentRepository warehouseEquipmentRepository,
                        ProductRepository productRepository,
                        ZoneProductRepository zoneProductRepository,
                        WriteOffActRepository writeOffActRepository,
                        EmployeeRepository employeeRepository,
                        NotificationService notificationService,
                        ProductExpirationService productExpirationService,
                        WriteOffActService writeOffActService) {
        this.warehouseEquipmentRepository = warehouseEquipmentRepository;
        this.productRepository = productRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.writeOffActRepository = writeOffActRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.productExpirationService = productExpirationService;
        this.writeOffActService = writeOffActService;
    }

    @Bean
    public ApplicationRunner applicationRunner() {
        return args -> {
            checkEquipmentExpiration();
            checkProductExpiration();
        };
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void cleanUpDirectories() {
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void checkEquipmentExpiration() {

        log.info("Запуск проверки сроков службы оборудования склада");

        List<WarehouseEquipment> activeEquipment =
                warehouseEquipmentRepository.findByStatusNot(EquipmentStatus.WRITTEN_OFF);

        LocalDate today = LocalDate.now();
        int notifiedCount = 0;

        for (WarehouseEquipment eq : activeEquipment) {
            LocalDate expDate = eq.getExpirationDate();
            if (expDate == null) {
                continue;
            }

            long daysLeft = ChronoUnit.DAYS.between(today, expDate);
            String warehouseName = eq.getWarehouse() != null ? eq.getWarehouse().getName() : "—";
            String text = null;

            if (daysLeft < 0) {
                text = "⚠️ Срок службы истёк: «" + eq.getName() + "» (склад: " + warehouseName
                        + "). Дата окончания: " + expDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ". Статус: "
                        + eq.getStatus().getDisplayName() + ".";
            } else if (daysLeft <= 7) {
                text = "⚠️ Срок службы заканчивается через " + daysLeft + " дн.: «"
                        + eq.getName() + "» (склад: " + warehouseName + ", до " + expDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ").";
            } else if (daysLeft <= 30) {
                text = "ℹ️ Срок службы заканчивается через месяц (" + daysLeft + " дн.): «"
                        + eq.getName() + "» (склад: " + warehouseName + ", до " + expDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ").";
            }

            if (text != null) {
                notificationService.notifyByRole(MANAGER_ROLE, text);
                notificationService.notifyByRole(ROLE_EMPLOYEE_WAREHOUSE_MANAGER, text);
                notifiedCount++;
            }
        }

        log.info("Проверка сроков завершена. Отправлено уведомлений по {} единицам оборудования", notifiedCount);
    }

    /**
     * Проверка сроков годности товаров.
     * Выполняется при старте приложения и ежедневно в 08:00.
     *
     * Группы оповещений:
     * - истёк срок годности
     * - срок истекает через 1 день
     * - срок истекает в ближайшие 2 недели
     * - срок истекает в ближайший месяц
     */
    @Transactional
    @Scheduled(cron = "0 0 8 * * ?")
    public void checkProductExpiration() {
        log.info("Запуск проверки сроков годности партий");

        LocalDate today = LocalDate.now();
        // Партии, у которых есть остатки в зонах и срок уже истёк
        List<com.mai.siarsp.models.Supply> expired = productExpirationService.findExpiredSuppliesOnStock(today);
        int actsCreated = 0;
        for (com.mai.siarsp.models.Supply supply : expired) {
            Product product = supply.getProduct();
            LocalDate expirationDate = supply.getExpirationDate();
            String text = "🚨 Просрочена партия товара «" + product.getName() + "» (артикул "
                    + product.getArticle() + ", срок до " + expirationDate + "). Будет автоматически списана.";
            notificationService.notifyByRole(MANAGER_ROLE, text);
            notificationService.notifyByRole(ROLE_EMPLOYEE_WAREHOUSE_MANAGER, text);
            if (createAutomaticWriteOffAct(product, expirationDate, supply)) {
                actsCreated++;
            }
        }
        log.info("Проверка сроков годности завершена. Просроченных партий: {}, автосозданных актов: {}",
                expired.size(), actsCreated);
    }

    private String buildExpirationNotification(Product product, LocalDate expirationDate, long daysLeft) {
        String dateText = expirationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

        if (daysLeft < 0) {
            return "🚨 Товар просрочен: «" + product.getName() + "» (артикул " + product.getArticle() + ")" +
                    ". Срок годности истёк " + dateText + ".";
        }
        if (daysLeft == 1) {
            return "⚠️ До истечения срока годности 1 день: «" + product.getName() + "» (артикул " + product.getArticle() + ")" +
                    ", дата: " + dateText + ".";
        }
        if (daysLeft <= 14) {
            return "⚠️ Товар истекает в ближайшие 2 недели: «" + product.getName() + "» (артикул " + product.getArticle() + ")" +
                    ", осталось " + daysLeft + " дн., дата: " + dateText + ".";
        }
        if (daysLeft <= 30) {
            return "ℹ️ Товар истекает в ближайший месяц: «" + product.getName() + "» (артикул " + product.getArticle() + ")" +
                    ", осталось " + daysLeft + " дн., дата: " + dateText + ".";
        }
        return null;
    }

    private boolean createAutomaticWriteOffAct(Product product, LocalDate expirationDate,
                                                com.mai.siarsp.models.Supply supply) {
        // Дубль для той же партии не создаём
        boolean pendingExists = writeOffActRepository.existsByProductIdAndReasonAndStatus(
                product.getId(), WriteOffReason.EXPIRED, WriteOffActStatus.PENDING_DIRECTOR
        );
        if (pendingExists) {
            return false;
        }

        Optional<Employee> responsibleOpt = findResponsibleForAutoWriteOff();
        if (responsibleOpt.isEmpty()) {
            log.warn("Не найден сотрудник для автосоздания акта списания просрочки по партии id={}", supply.getId());
            return false;
        }

        Employee responsible = responsibleOpt.get();
        // Берём ZoneProduct'ы конкретной партии — сумма количеств = подлежит списанию,
        // склад берём от первой найденной зоны.
        List<ZoneProduct> partyZones = zoneProductRepository.findByProductId(product.getId()).stream()
                .filter(zp -> zp.getSupply() != null && zp.getSupply().equals(supply))
                .toList();
        int qtyToWriteOff = partyZones.stream().mapToInt(ZoneProduct::getQuantity).sum();
        if (qtyToWriteOff <= 0) {
            return false;
        }
        Optional<Warehouse> warehouseOpt = partyZones.stream()
                .filter(zp -> zp.getZone() != null && zp.getZone().getShelf() != null
                        && zp.getZone().getShelf().getWarehouse() != null)
                .map(zp -> zp.getZone().getShelf().getWarehouse())
                .findFirst();

        String actNumber = "AUTO-WR-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + product.getId() + "-S" + supply.getId();

        WriteOffAct act = new WriteOffAct(
                actNumber,
                product,
                qtyToWriteOff,
                WriteOffReason.EXPIRED,
                responsible
        );

        act.setStatus(WriteOffActStatus.PENDING_DIRECTOR);
        act.setComment("Автоматическое списание просроченной партии. Срок годности истёк "
                + expirationDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".");
        warehouseOpt.ifPresent(act::setWarehouse);
        act.setSupply(supply);
        // Стоимость по фактической закупочной цене этой партии
        java.math.BigDecimal price = supply.getPurchasePrice();
        if (price != null) {
            act.setTotalCost(price.multiply(java.math.BigDecimal.valueOf(qtyToWriteOff)));
        }

        writeOffActRepository.save(act);

        String text = "📄 Автоматически создан акт списания " + actNumber
                + " для товара «" + product.getName() + "» (просрочка). Требуется подпись руководителя.";
        notificationService.notifyByRole(MANAGER_ROLE, text);
        notificationService.notifyByRole(ROLE_EMPLOYEE_WAREHOUSE_MANAGER, text);
        return true;
    }

    private Optional<Employee> findResponsibleForAutoWriteOff() {
        List<Employee> warehouseManagers = employeeRepository.findAllByRoleName(ROLE_EMPLOYEE_WAREHOUSE_MANAGER);
        if (!warehouseManagers.isEmpty()) {
            return Optional.of(warehouseManagers.get(0));
        }

        List<Employee> managers = employeeRepository.findAllByRoleName(MANAGER_ROLE);
        if (!managers.isEmpty()) {
            return Optional.of(managers.get(0));
        }

        return Optional.empty();
    }
}
