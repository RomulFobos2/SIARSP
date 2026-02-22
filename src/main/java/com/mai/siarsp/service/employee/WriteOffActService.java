package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.WriteOffActDTO;
import com.mai.siarsp.enumeration.WriteOffActStatus;
import com.mai.siarsp.enumeration.WriteOffReason;
import com.mai.siarsp.mapper.WriteOffActMapper;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.WriteOffAct;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.repo.WriteOffActRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Сервис управления актами списания товаров
 *
 * Бизнес-процесс:
 * 1. Заведующий складом создаёт акт → статус PENDING_DIRECTOR, уведомление директору
 * 2. Директор утверждает → статус APPROVED, товар списывается, уведомления
 * 3. Директор отклоняет → статус REJECTED, уведомление заведующему
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

    public WriteOffActService(WriteOffActRepository writeOffActRepository,
                              ProductRepository productRepository,
                              ZoneProductRepository zoneProductRepository,
                              WarehouseRepository warehouseRepository,
                              NotificationService notificationService) {
        this.writeOffActRepository = writeOffActRepository;
        this.productRepository = productRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.warehouseRepository = warehouseRepository;
        this.notificationService = notificationService;
    }

    /**
     * Получает все акты списания (новейшие первыми)
     */
    @Transactional(readOnly = true)
    public List<WriteOffActDTO> getAllActs() {
        List<WriteOffAct> acts = writeOffActRepository.findAllByOrderByActDateDesc();
        return WriteOffActMapper.INSTANCE.toDTOList(acts);
    }

    /**
     * Получает акт по идентификатору
     */
    @Transactional(readOnly = true)
    public Optional<WriteOffAct> getActById(Long id) {
        return writeOffActRepository.findById(id);
    }

    /**
     * Получает акты по статусу
     */
    @Transactional(readOnly = true)
    public List<WriteOffActDTO> getActsByStatus(WriteOffActStatus status) {
        List<WriteOffAct> acts = writeOffActRepository.findByStatusOrderByActDateDesc(status);
        return WriteOffActMapper.INSTANCE.toDTOList(acts);
    }

    /**
     * Создаёт акт списания и отправляет на подпись директору
     *
     * @param productId    идентификатор товара
     * @param quantity     количество для списания
     * @param reason       причина списания
     * @param comment      комментарий
     * @param responsible  ответственный сотрудник (заведующий складом)
     * @param warehouseId  идентификатор склада, с которого производится списание
     * @return true при успешном создании
     */
    @Transactional
    public boolean createAct(Long productId, int quantity, WriteOffReason reason,
                             String comment, Employee responsible, Long warehouseId) {
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

            int availableInWarehouse = zoneProductRepository
                    .sumQuantityByProductIdAndWarehouseId(productId, warehouseId);
            if (availableInWarehouse <= 0) {
                log.error("Товар '{}' не размещён на складе '{}'", product.getName(), warehouse.getName());
                return false;
            }

            if (quantity > availableInWarehouse) {
                log.error("Количество списания ({}) превышает остаток ({}) в складе '{}' для товара '{}'",
                        quantity, availableInWarehouse, warehouse.getName(), product.getName());
                return false;
            }

            String actNumber = generateActNumber();

            WriteOffAct act = new WriteOffAct(actNumber, product, quantity, reason, responsible);
            act.setStatus(WriteOffActStatus.PENDING_DIRECTOR);
            act.setComment(comment);
            act.setWarehouse(warehouse);

            writeOffActRepository.save(act);

            notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER",
                    "Акт списания №" + actNumber + " на подпись. Товар: " + product.getName()
                            + ", кол-во: " + quantity + " шт., склад: " + warehouse.getName());

            log.info("Создан акт списания №{}: товар '{}', кол-во {}, причина {}, склад '{}'",
                    actNumber, product.getName(), quantity, reason, warehouse.getName());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при создании акта списания: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Утверждает акт списания — списывает товар со склада
     *
     * @param actId идентификатор акта
     * @return true при успешном утверждении
     */
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
                List<ZoneProduct> zoneProducts = zoneProductRepository
                        .findByProductAndWarehouseId(product, act.getWarehouse().getId());
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

    /**
     * Отклоняет акт списания
     *
     * @param actId           идентификатор акта
     * @param directorComment комментарий директора (причина отклонения)
     * @return true при успешном отклонении
     */
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

    /**
     * Генерирует уникальный номер акта в формате АС-YYYYMMDD-NNNN
     */
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
