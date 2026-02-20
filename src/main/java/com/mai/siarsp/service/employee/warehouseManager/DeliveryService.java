package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.DeliveryDTO;
import com.mai.siarsp.dto.SupplyInputDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.mapper.DeliveryMapper;
import com.mai.siarsp.models.*;
import com.mai.siarsp.repo.DeliveryRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.RequestForDeliveryRepository;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис приёмки поставок товара от поставщиков.
 *
 * Реализует бизнес-процесс оформления поставки:
 * 1. Заведующий складом выбирает согласованную заявку (APPROVED)
 * 2. Указывает фактически полученное количество по каждой позиции
 * 3. При расхождениях (недопоставка, брак) заполняет причину дефицита
 * 4. Система создаёт поставку (Delivery) с позициями (Supply)
 * 5. Оприходует товар на склад (Product.stockQuantity, quantityForStock)
 * 6. Устанавливает статус заявки (RECEIVED / PARTIALLY_RECEIVED)
 * 7. Отправляет уведомления директору и бухгалтеру
 *
 * Доступ: ROLE_EMPLOYEE_WAREHOUSE_MANAGER
 */
@Service("warehouseManagerDeliveryService")
@Getter
@Slf4j
public class DeliveryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DeliveryRepository deliveryRepository;
    private final RequestForDeliveryRepository requestForDeliveryRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public DeliveryService(DeliveryRepository deliveryRepository,
                           RequestForDeliveryRepository requestForDeliveryRepository,
                           ProductRepository productRepository,
                           NotificationService notificationService) {
        this.deliveryRepository = deliveryRepository;
        this.requestForDeliveryRepository = requestForDeliveryRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    /**
     * Получает список всех поставок, отсортированных по дате (новые сначала).
     *
     * @return список DTO поставок
     */
    @Transactional(readOnly = true)
    public List<DeliveryDTO> getAllDeliveries() {
        List<Delivery> deliveries = deliveryRepository.findAllByOrderByDeliveryDateDesc();
        return DeliveryMapper.INSTANCE.toDTOList(deliveries);
    }

    /**
     * Получает поставку по идентификатору.
     *
     * @param id идентификатор поставки
     * @return Optional с поставкой или empty
     */
    @Transactional(readOnly = true)
    public Optional<Delivery> getDeliveryById(Long id) {
        return deliveryRepository.findById(id);
    }

    /**
     * Получает список согласованных заявок, доступных для оформления поставки.
     *
     * @return список заявок со статусом APPROVED
     */
    @Transactional(readOnly = true)
    public List<RequestForDelivery> getApprovedRequests() {
        return requestForDeliveryRepository.findByStatusOrderByRequestDateDesc(RequestStatus.APPROVED);
    }

    /**
     * Получает заявку по идентификатору.
     *
     * @param id идентификатор заявки
     * @return Optional с заявкой или empty
     */
    @Transactional(readOnly = true)
    public Optional<RequestForDelivery> getRequestById(Long id) {
        return requestForDeliveryRepository.findById(id);
    }

    /**
     * Создаёт поставку на основе согласованной заявки.
     *
     * Бизнес-процесс:
     * 1. Валидация: заявка должна быть в статусе APPROVED
     * 2. Валидация: количество каждой позиции не превышает заказанное
     * 3. Валидация: при наличии дефицита обязательна причина
     * 4. Создание Delivery и Supply для каждой позиции
     * 5. Оприходование товара: Product.stockQuantity += quantity, Product.quantityForStock += quantity
     * 6. Установка статуса заявки: RECEIVED (все полностью) или PARTIALLY_RECEIVED (есть дефицит)
     * 7. Отправка уведомлений директору и бухгалтеру
     *
     * @param requestId    идентификатор заявки (должна быть APPROVED)
     * @param supplyInputs список позиций поставки с фактическими данными
     * @param deliveryDate дата приёмки
     * @return true если поставка создана успешно, false при ошибке
     */
    @Transactional
    public boolean createDeliveryFromRequest(Long requestId, List<SupplyInputDTO> supplyInputs,
                                              LocalDate deliveryDate) {
        try {
            // 1. Найти заявку
            Optional<RequestForDelivery> optRequest = requestForDeliveryRepository.findById(requestId);
            if (optRequest.isEmpty()) {
                log.error("Заявка с ID {} не найдена", requestId);
                return false;
            }
            RequestForDelivery request = optRequest.get();

            // 2. Проверить статус
            if (request.getStatus() != RequestStatus.APPROVED) {
                log.error("Заявка ID {} не в статусе APPROVED (текущий: {})", requestId, request.getStatus());
                return false;
            }

            // 3. Проверить наличие позиций
            if (supplyInputs == null || supplyInputs.isEmpty()) {
                log.error("Список позиций поставки пуст для заявки ID {}", requestId);
                return false;
            }

            // 4. Построить карту RequestedProduct по productId
            Map<Long, RequestedProduct> requestedMap = request.getRequestedProducts().stream()
                    .collect(Collectors.toMap(rp -> rp.getProduct().getId(), Function.identity()));

            // 5. Создать Delivery
            Delivery delivery = new Delivery(request.getSupplier(), deliveryDate);
            boolean hasDeficit = false;

            // 6. Обработать каждую позицию
            for (SupplyInputDTO input : supplyInputs) {
                RequestedProduct requestedProduct = requestedMap.get(input.getProductId());
                if (requestedProduct == null) {
                    log.error("Товар ID {} не найден в заявке ID {}", input.getProductId(), requestId);
                    return false;
                }

                // Валидация количества
                if (input.getQuantity() < 0 || input.getQuantity() > requestedProduct.getQuantity()) {
                    log.error("Некорректное количество для товара ID {}: принято={}, заказано={}",
                            input.getProductId(), input.getQuantity(), requestedProduct.getQuantity());
                    return false;
                }

                // Расчёт дефицита
                int deficit = requestedProduct.getQuantity() - input.getQuantity();

                // Валидация причины дефицита
                if (deficit > 0 && (input.getDeficitReason() == null || input.getDeficitReason().isBlank())) {
                    log.error("Не указана причина дефицита для товара ID {} (дефицит: {} шт.)",
                            input.getProductId(), deficit);
                    return false;
                }

                if (deficit > 0) {
                    hasDeficit = true;
                }

                // Найти товар
                Optional<Product> optProduct = productRepository.findById(input.getProductId());
                if (optProduct.isEmpty()) {
                    log.error("Товар с ID {} не найден в справочнике", input.getProductId());
                    return false;
                }
                Product product = optProduct.get();

                // Создать Supply
                Supply supply = new Supply(product, input.getPurchasePrice(), input.getQuantity());
                supply.setDeficitQuantity(deficit);
                supply.setDeficitReason(deficit > 0 ? input.getDeficitReason() : null);
                delivery.addSupply(supply);

                // Оприходование на склад
                product.setStockQuantity(product.getStockQuantity() + input.getQuantity());
                product.setQuantityForStock(product.getQuantityForStock() + input.getQuantity());
                productRepository.save(product);

                log.info("Позиция поставки: товар '{}' (ID {}), принято: {}, дефицит: {}",
                        product.getName(), product.getId(), input.getQuantity(), deficit);
            }

            // 7. Связать поставку с заявкой
            request.setDelivery(delivery);

            // 8. Определить статус заявки
            if (hasDeficit) {
                request.setStatus(RequestStatus.PARTIALLY_RECEIVED);
                log.info("Заявка ID {} → статус PARTIALLY_RECEIVED", requestId);
            } else {
                request.setStatus(RequestStatus.RECEIVED);
                request.setReceivedDate(LocalDate.now());
                log.info("Заявка ID {} → статус RECEIVED, дата получения: {}", requestId, LocalDate.now());
            }

            // 9. Сохранить (каскад сохранит Delivery и Supply)
            requestForDeliveryRepository.save(request);

            // 10. Уведомления
            String supplierName = request.getSupplier().getName();
            String dateStr = deliveryDate.format(DATE_FMT);
            if (hasDeficit) {
                String notificationText = String.format(
                        "Поставка №%d от поставщика «%s» принята частично (%s). Имеются расхождения.",
                        delivery.getId(), supplierName, dateStr);
                notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", notificationText);
                notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER", notificationText);
            } else {
                String notificationText = String.format(
                        "Поставка №%d от поставщика «%s» полностью принята (%s).",
                        delivery.getId(), supplierName, dateStr);
                notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", notificationText);
                notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER", notificationText);
            }

            log.info("Поставка ID {} создана для заявки ID {} от поставщика '{}', позиций: {}",
                    delivery.getId(), requestId, supplierName, supplyInputs.size());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при создании поставки для заявки ID {}: {}", requestId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }
}
