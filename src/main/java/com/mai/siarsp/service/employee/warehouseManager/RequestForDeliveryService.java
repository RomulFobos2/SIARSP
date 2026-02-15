package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.mapper.RequestForDeliveryMapper;
import com.mai.siarsp.models.*;
import com.mai.siarsp.repo.CommentRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.RequestForDeliveryRepository;
import com.mai.siarsp.repo.RequestedProductRepository;
import com.mai.siarsp.repo.SupplierRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.mai.siarsp.enumeration.RequestStatus.*;

/**
 * Сервис для управления заявками на поставку со стороны заведующего складом
 *
 * Предоставляет операции:
 * - Получение списка всех заявок
 * - Создание, редактирование, удаление заявки (только DRAFT / REJECTED_BY_*)
 * - Отправка на согласование директору (DRAFT → PENDING_DIRECTOR)
 * - Повторная отправка после отклонения (REJECTED_BY_* → PENDING_DIRECTOR + комментарий)
 */
@Service("warehouseManagerRequestForDeliveryService")
@Getter
@Slf4j
public class RequestForDeliveryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Map<RequestStatus, Set<RequestStatus>> VALID_TRANSITIONS = Map.of(
            DRAFT, Set.of(PENDING_ACCOUNTANT, CANCELLED),
            PENDING_ACCOUNTANT, Set.of(PENDING_DIRECTOR, REJECTED_BY_ACCOUNTANT),
            REJECTED_BY_ACCOUNTANT, Set.of(PENDING_ACCOUNTANT, CANCELLED),
            PENDING_DIRECTOR, Set.of(APPROVED, REJECTED_BY_DIRECTOR),
            REJECTED_BY_DIRECTOR, Set.of(PENDING_ACCOUNTANT, CANCELLED),
            APPROVED, Set.of(PARTIALLY_RECEIVED, RECEIVED, CANCELLED),
            PARTIALLY_RECEIVED, Set.of(RECEIVED)
    );

    private final RequestForDeliveryRepository requestForDeliveryRepository;
    private final RequestedProductRepository requestedProductRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;

    public RequestForDeliveryService(RequestForDeliveryRepository requestForDeliveryRepository,
                                      RequestedProductRepository requestedProductRepository,
                                      SupplierRepository supplierRepository,
                                      ProductRepository productRepository,
                                      WarehouseRepository warehouseRepository,
                                      EmployeeService employeeService,
                                      NotificationService notificationService,
                                      CommentRepository commentRepository) {
        this.requestForDeliveryRepository = requestForDeliveryRepository;
        this.requestedProductRepository = requestedProductRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.employeeService = employeeService;
        this.notificationService = notificationService;
        this.commentRepository = commentRepository;
    }

    public List<RequestForDeliveryDTO> getAllRequests() {
        return RequestForDeliveryMapper.INSTANCE.toDTOList(
                requestForDeliveryRepository.findAllByOrderByRequestDateDesc());
    }

    public RequestForDeliveryDTO getRequestById(Long id) {
        Optional<RequestForDelivery> optional = requestForDeliveryRepository.findById(id);
        return optional.map(RequestForDeliveryMapper.INSTANCE::toDTO).orElse(null);
    }

    public RequestForDelivery getRequestEntity(Long id) {
        return requestForDeliveryRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean createRequest(Long supplierId, Long warehouseId, BigDecimal deliveryCost,
                                  List<Long> productIds, List<Integer> quantities, List<BigDecimal> purchasePrices) {
        log.info("Создание заявки на поставку для поставщика id={}...", supplierId);

        Optional<Supplier> supplierOpt = supplierRepository.findById(supplierId);
        if (supplierOpt.isEmpty()) {
            log.error("Поставщик с id={} не найден.", supplierId);
            return false;
        }

        Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
        if (warehouseOpt.isEmpty()) {
            log.error("Склад с id={} не найден.", warehouseId);
            return false;
        }

        if (productIds == null || productIds.isEmpty()) {
            log.error("Список товаров пуст.");
            return false;
        }

        RequestForDelivery request = new RequestForDelivery(supplierOpt.get());
        request.setWarehouse(warehouseOpt.get());
        request.setDeliveryCost(deliveryCost != null ? deliveryCost : BigDecimal.ZERO);

        for (int i = 0; i < productIds.size(); i++) {
            Optional<Product> productOpt = productRepository.findById(productIds.get(i));
            if (productOpt.isEmpty()) {
                log.error("Товар с id={} не найден.", productIds.get(i));
                return false;
            }
            int qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
            BigDecimal price = (purchasePrices != null && i < purchasePrices.size()) ? purchasePrices.get(i) : BigDecimal.ZERO;
            request.addRequestedProduct(new RequestedProduct(productOpt.get(), qty, price));
        }

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при сохранении заявки: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Заявка №{} успешно создана.", request.getId());
        return true;
    }

    @Transactional
    public boolean updateRequest(Long id, Long supplierId, Long warehouseId, BigDecimal deliveryCost,
                                  List<Long> productIds, List<Integer> quantities, List<BigDecimal> purchasePrices) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();

        if (request.getStatus() != DRAFT
                && request.getStatus() != REJECTED_BY_DIRECTOR
                && request.getStatus() != REJECTED_BY_ACCOUNTANT) {
            log.error("Заявку №{} нельзя редактировать в статусе '{}'.", id, request.getStatus().getDisplayName());
            return false;
        }

        Optional<Supplier> supplierOpt = supplierRepository.findById(supplierId);
        if (supplierOpt.isEmpty()) {
            log.error("Поставщик с id={} не найден.", supplierId);
            return false;
        }

        Optional<Warehouse> warehouseOpt = warehouseRepository.findById(warehouseId);
        if (warehouseOpt.isEmpty()) {
            log.error("Склад с id={} не найден.", warehouseId);
            return false;
        }

        request.setSupplier(supplierOpt.get());
        request.setWarehouse(warehouseOpt.get());
        request.setDeliveryCost(deliveryCost != null ? deliveryCost : BigDecimal.ZERO);

        // 1) Нормализуем вход: productId -> {qty, price}
        Map<Long, ProductData> incoming = new LinkedHashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            if (pid == null) continue;

            int qty = (quantities != null && i < quantities.size() && quantities.get(i) != null)
                    ? quantities.get(i) : 1;
            if (qty < 1) qty = 1;

            BigDecimal price = (purchasePrices != null && i < purchasePrices.size() && purchasePrices.get(i) != null)
                    ? purchasePrices.get(i) : BigDecimal.ZERO;

            incoming.put(pid, new ProductData(qty, price));
        }

        // 2) Индексируем текущие позиции заявки по productId
        Map<Long, RequestedProduct> existing = new HashMap<>();
        for (RequestedProduct rp : request.getRequestedProducts()) {
            existing.put(rp.getProduct().getId(), rp);
        }

        // 3) UPDATE существующих + ADD новых
        for (Map.Entry<Long, ProductData> e : incoming.entrySet()) {
            Long pid = e.getKey();
            ProductData data = e.getValue();

            RequestedProduct rp = existing.get(pid);
            if (rp != null) {
                rp.setQuantity(data.quantity);
                rp.setPurchasePrice(data.price); // UPDATE
            } else {
                Product product = productRepository.findById(pid).orElse(null);
                if (product == null) {
                    log.error("Товар с id={} не найден.", pid);
                    return false;
                }
                request.addRequestedProduct(new RequestedProduct(product, data.quantity, data.price)); // INSERT
            }
        }

        // 4) REMOVE позиций, которых больше нет во входе
        request.getRequestedProducts().removeIf(rp -> !incoming.containsKey(rp.getProduct().getId()));

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при обновлении заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Заявка №{} успешно обновлена.", id);
        return true;
    }

    // Вспомогательный класс для хранения данных о товаре
    private record ProductData(int quantity, BigDecimal price) {}


    @Transactional
    public boolean deleteRequest(Long id) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != DRAFT) {
            log.error("Удалить можно только черновик. Текущий статус: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        try {
            requestForDeliveryRepository.delete(request);
        } catch (Exception e) {
            log.error("Ошибка при удалении заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Заявка №{} удалена.", id);
        return true;
    }

    @Transactional
    public boolean submitForApproval(Long id) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != DRAFT) {
            log.error("Отправить на согласование можно только черновик. Текущий статус: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        request.setStatus(PENDING_ACCOUNTANT);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при отправке заявки №{} на согласование: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» отправлена на согласование бухгалтеру",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER", notificationText);

        log.info("Заявка №{} отправлена на согласование бухгалтеру.", id);
        return true;
    }

    @Transactional
    public boolean resubmitForApproval(Long id, String commentText) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != REJECTED_BY_DIRECTOR && request.getStatus() != REJECTED_BY_ACCOUNTANT) {
            log.error("Повторная отправка возможна только для отклонённых заявок. Текущий статус: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        Employee currentEmployee = employeeService.getAuthenticationEmployee();
        if (currentEmployee == null) {
            log.error("Не удалось определить текущего сотрудника.");
            return false;
        }

        Comment comment = new Comment(currentEmployee, commentText, request);
        commentRepository.save(comment);

        request.setStatus(PENDING_ACCOUNTANT);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при повторной отправке заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» повторно отправлена на согласование бухгалтеру",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER", notificationText);

        log.info("Заявка №{} повторно отправлена на согласование бухгалтеру.", id);
        return true;
    }

    @Transactional
    public boolean cancelRequest(Long id) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != APPROVED) {
            log.error("Отменить можно только согласованную заявку. Текущий статус: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        request.setStatus(CANCELLED);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при отмене заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» отменена заведующим складом",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", notificationText);
        notificationService.notifyByRole("ROLE_EMPLOYEE_ACCOUNTER", notificationText);

        log.info("Заявка №{} отменена.", id);
        return true;
    }
}
