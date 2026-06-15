package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.WriteOffActDTO;
import com.mai.siarsp.enumeration.WriteOffActStatus;
import com.mai.siarsp.enumeration.WriteOffReason;
import com.mai.siarsp.mapper.WriteOffActMapper;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.WriteOffAct;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.StorageZoneRepository;
import com.mai.siarsp.repo.SupplyRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.repo.WriteOffActRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Сервис списания: проверяет основания, формирует акт и корректно отражает изменения по остаткам.
 */

@Service
@Slf4j
public class WriteOffActService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    private final WriteOffActRepository writeOffActRepository;
    private final ProductRepository productRepository;
    private final ZoneProductRepository zoneProductRepository;
    private final WarehouseRepository warehouseRepository;
    private final NotificationService notificationService;
    private final SupplyRepository supplyRepository;
    private final StorageZoneRepository storageZoneRepository;

    public WriteOffActService(WriteOffActRepository writeOffActRepository,
                              ProductRepository productRepository,
                              ZoneProductRepository zoneProductRepository,
                              WarehouseRepository warehouseRepository,
                              NotificationService notificationService,
                              SupplyRepository supplyRepository,
                              StorageZoneRepository storageZoneRepository) {
        this.writeOffActRepository = writeOffActRepository;
        this.productRepository = productRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.warehouseRepository = warehouseRepository;
        this.notificationService = notificationService;
        this.supplyRepository = supplyRepository;
        this.storageZoneRepository = storageZoneRepository;
    }

    /**
     * Стоимость списанного товара = количество × последняя закупочная цена из Supply.
     * Возвращает null, если у товара нет ни одной закупки или цена не задана.
     * Snapshot фиксируется при создании акта и не пересчитывается потом.
     */
    public BigDecimal calculateWriteOffCost(Long productId, int quantity) {
        if (quantity <= 0) return null;
        List<Supply> supplies = supplyRepository.findByProductIdOrderByDelivery_DeliveryDateDesc(productId);
        if (supplies.isEmpty()) return null;
        BigDecimal lastPrice = supplies.get(0).getPurchasePrice();
        if (lastPrice == null) return null;
        return lastPrice.multiply(BigDecimal.valueOf(quantity));
    }

    @Transactional(readOnly = true)
    public List<WriteOffActDTO> getAllActs() {
        List<WriteOffAct> acts = writeOffActRepository.findAllByOrderByActDateDesc();
        return WriteOffActMapper.INSTANCE.toDTOList(acts);
    }

    @Transactional(readOnly = true)
    public Optional<WriteOffAct> getActById(Long id) {
        return writeOffActRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<WriteOffActDTO> getActsByStatus(WriteOffActStatus status) {
        List<WriteOffAct> acts = writeOffActRepository.findByStatusOrderByActDateDesc(status);
        return WriteOffActMapper.INSTANCE.toDTOList(acts);
    }

    @Transactional
    public boolean createAct(Long productId, Long supplyId, Long zoneId, int quantity,
                             WriteOffReason reason, String comment, Employee responsible,
                             Long warehouseId) {
        try {
            Optional<Product> optProduct = productRepository.findById(productId);
            if (optProduct.isEmpty()) {
                log.error("Товар с id={} не найден", productId);
                return false;
            }

            Product product = optProduct.get();

            if (quantity <= 0) {
                log.error("Некорректное количество для списания: {}", quantity);
                return false;
            }

            Optional<Warehouse> optWarehouse = warehouseRepository.findById(warehouseId);
            if (optWarehouse.isEmpty()) {
                log.error("Склад с id={} не найден", warehouseId);
                return false;
            }

            Warehouse warehouse = optWarehouse.get();

            Optional<Supply> optSupply = supplyRepository.findById(supplyId);
            if (optSupply.isEmpty()) {
                log.error("Партия с id={} не найдена", supplyId);
                return false;
            }
            Supply supply = optSupply.get();
            if (!supply.getProduct().getId().equals(productId)) {
                log.error("Партия #{} принадлежит товару '{}', а не выбранному '{}'",
                        supplyId, supply.getProduct().getName(), product.getName());
                return false;
            }

            Optional<StorageZone> optZone = storageZoneRepository.findById(zoneId);
            if (optZone.isEmpty()) {
                log.error("Зона хранения с id={} не найдена", zoneId);
                return false;
            }
            StorageZone zone = optZone.get();
            if (!zone.getShelf().getWarehouse().getId().equals(warehouseId)) {
                log.error("Зона '{}' не принадлежит складу '{}'",
                        zone.getLabel(), warehouse.getName());
                return false;
            }

            Optional<ZoneProduct> optZp = zoneProductRepository.findByZoneAndSupply(zone, supply);
            if (optZp.isEmpty()) {
                log.error("В зоне '{}' нет партии #{} товара '{}'",
                        zone.getLabel(), supplyId, product.getName());
                return false;
            }
            int availableInZone = optZp.get().getQuantity();
            if (quantity > availableInZone) {
                log.error("Количество списания ({}) превышает остаток в зоне '{}' (доступно: {}) " +
                                "для партии #{} товара '{}'",
                        quantity, zone.getLabel(), availableInZone, supplyId, product.getName());
                return false;
            }

            // Защита от списания товара, который уже обещан клиентам (резерв).
            // Доступно для списания = размещено в зонах − зарезервировано под заказы.
            int writableTotal = Math.max(0, product.getPlacedQuantity() - product.getReservedQuantity());
            if (quantity > writableTotal) {
                log.error("Количество списания ({}) превышает доступное ({}) для товара '{}' "
                                + "с учётом резерва ({}). Сначала отмените или отгрузите соответствующие заказы.",
                        quantity, writableTotal, product.getName(), product.getReservedQuantity());
                return false;
            }

            String actNumber = generateActNumber();

            WriteOffAct act = new WriteOffAct(actNumber, product, quantity, reason, responsible);
            act.setStatus(WriteOffActStatus.PENDING_DIRECTOR);
            act.setComment(comment);
            act.setWarehouse(warehouse);
            act.setSupply(supply);
            act.setZone(zone);
            act.setTotalCost(calculateWriteOffCost(productId, quantity));

            writeOffActRepository.save(act);

            notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER",
                    "Акт списания №" + actNumber + " на подпись. Товар: " + product.getName()
                            + ", кол-во: " + quantity + " шт., склад: " + warehouse.getName()
                            + ", зона: " + zone.getLabel() + ", партия #" + supplyId);

            log.info("Создан акт списания №{}: товар '{}', партия #{}, зона '{}', кол-во {}, причина {}, склад '{}'",
                    actNumber, product.getName(), supplyId, zone.getLabel(), quantity, reason, warehouse.getName());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при создании акта списания: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Transactional
    public boolean approveAct(Long actId) {
        try {
            Optional<WriteOffAct> optAct = writeOffActRepository.findById(actId);
            if (optAct.isEmpty()) {
                log.error("Акт списания id={} не найден", actId);
                return false;
            }

            WriteOffAct act = optAct.get();

            if (act.getStatus() != WriteOffActStatus.PENDING_DIRECTOR) {
                log.error("Акт №{} не в статусе ожидания подписи (текущий: {})",
                        act.getActNumber(), act.getStatus());
                return false;
            }

            Product product = act.getProduct();

            // Повторная валидация — между созданием и утверждением могло пройти время
            if (act.getQuantity() > product.getStockQuantity()) {
                log.error("Недостаточно товара '{}' для списания: нужно {}, на складе {}",
                        product.getName(), act.getQuantity(), product.getStockQuantity());
                return false;
            }

            // Списание глобального остатка
            product.setStockQuantity(product.getStockQuantity() - act.getQuantity());
            productRepository.save(product);

            // Списываем физически из зон склада (если склад указан)
            if (act.getWarehouse() != null) {
                int remaining = act.getQuantity();
                List<ZoneProduct> zoneProducts;
                if (act.getZone() != null && act.getSupply() != null) {
                    // Адресное списание: ровно одна запись ZoneProduct(zone, supply)
                    zoneProducts = zoneProductRepository
                            .findByZoneAndSupply(act.getZone(), act.getSupply())
                            .map(List::of)
                            .orElse(List.of());
                    log.info("Адресное списание акта №{}: зона '{}', партия #{}",
                            act.getActNumber(), act.getZone().getLabel(), act.getSupply().getId());
                } else if (act.getSupply() != null) {
                    // Без зоны: все ZoneProduct'ы партии на складе (автосписание просрочки)
                    zoneProducts = zoneProductRepository
                            .findBySupplyAndWarehouseId(act.getSupply(), act.getWarehouse().getId());
                    log.info("Списание акта №{} по партии #{}: затронуто зон — {}",
                            act.getActNumber(), act.getSupply().getId(), zoneProducts.size());
                } else {
                    // Legacy: акты без партии — все ZoneProduct'ы товара на складе
                    zoneProducts = zoneProductRepository
                            .findByProductAndWarehouseId(product, act.getWarehouse().getId());
                }
                for (ZoneProduct zp : zoneProducts) {
                    if (remaining <= 0) break;
                    int zpQty = zp.getQuantity();
                    if (zpQty <= remaining) {
                        zoneProductRepository.delete(zp);
                        remaining -= zpQty;
                    } else {
                        zp.setQuantity(zpQty - remaining);
                        zoneProductRepository.save(zp);
                        remaining = 0;
                    }
                }
            } else {
                // Обратная совместимость — акты без склада: уменьшаем неразмещённый товар
                if (product.getQuantityForStock() > 0) {
                    int decrease = Math.min(product.getQuantityForStock(), act.getQuantity());
                    product.setQuantityForStock(product.getQuantityForStock() - decrease);
                    productRepository.save(product);
                }
            }

            act.setStatus(WriteOffActStatus.APPROVED);
            writeOffActRepository.save(act);

            // Уведомления
            notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
                    "Акт списания №" + act.getActNumber() + " утверждён. Товар '"
                            + product.getName() + "' списан в количестве " + act.getQuantity() + " шт.");

            notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER",
                    "Акт списания №" + act.getActNumber() + " утверждён для учёта. Товар: "
                            + product.getName() + ", кол-во: " + act.getQuantity() + " шт.");

            log.info("Акт списания №{} утверждён. Товар '{}' списан: {} шт.",
                    act.getActNumber(), product.getName(), act.getQuantity());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при утверждении акта списания: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Transactional
    public boolean rejectAct(Long actId, String directorComment) {
        try {
            Optional<WriteOffAct> optAct = writeOffActRepository.findById(actId);
            if (optAct.isEmpty()) {
                log.error("Акт списания id={} не найден", actId);
                return false;
            }

            WriteOffAct act = optAct.get();

            if (act.getStatus() != WriteOffActStatus.PENDING_DIRECTOR) {
                log.error("Акт №{} не в статусе ожидания подписи (текущий: {})",
                        act.getActNumber(), act.getStatus());
                return false;
            }

            act.setStatus(WriteOffActStatus.REJECTED);
            act.setDirectorComment(directorComment);
            writeOffActRepository.save(act);

            notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
                    "Акт списания №" + act.getActNumber() + " отклонён директором. Причина: "
                            + (directorComment != null ? directorComment : "не указана"));

            log.info("Акт списания №{} отклонён. Причина: {}", act.getActNumber(), directorComment);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отклонении акта списания: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    private String generateActNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String actNumber;
        do {
            int number = 1000 + RANDOM.nextInt(9000);
            actNumber = "АС-" + datePart + "-" + number;
        } while (writeOffActRepository.existsByActNumber(actNumber));
        return actNumber;
    }
}
