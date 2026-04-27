package com.mai.siarsp.service.employee.manager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.mapper.RequestForDeliveryMapper;
import com.mai.siarsp.models.*;
import com.mai.siarsp.repo.*;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис обработки заявок на поставку в контуре роли. Отвечает за статусы, позиции и движение заявки по процессу.
 */

@Service("managerRequestForDeliveryService")
@Getter
@Slf4j
public class RequestForDeliveryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

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

    public List<RequestForDeliveryDTO> getRequestsByStatus(RequestStatus status) {
        return RequestForDeliveryMapper.INSTANCE.toDTOList(
                requestForDeliveryRepository.findByStatusOrderByRequestDateDesc(status));
    }

    public RequestForDeliveryDTO getRequestById(Long id) {
        Optional<RequestForDelivery> optional = requestForDeliveryRepository.findById(id);
        return optional.map(RequestForDeliveryMapper.INSTANCE::toDTO).orElse(null);
    }

    public RequestForDelivery getRequestEntity(Long id) {
        return requestForDeliveryRepository.findById(id).orElse(null);
    }

    @Transactional
    public boolean createApprovedRequest(Long supplierId, Long warehouseId, BigDecimal deliveryCost,
                                          List<Long> productIds, List<Integer> quantities,
                                          List<BigDecimal> purchasePrices, List<String> units) {
        log.info("Создание заявки на заказ товара со статусом APPROVED (директор/админ), поставщик id={}...", supplierId);

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
        request.setStatus(RequestStatus.APPROVED);

        for (int i = 0; i < productIds.size(); i++) {
            Optional<Product> productOpt = productRepository.findById(productIds.get(i));
            if (productOpt.isEmpty()) {
                log.error("Товар с id={} не найден.", productIds.get(i));
                return false;
            }
            int qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
            BigDecimal price = (purchasePrices != null && i < purchasePrices.size()) ? purchasePrices.get(i) : BigDecimal.ZERO;
            String unit = (units != null && i < units.size()) ? units.get(i) : null;
            RequestedProduct rp = new RequestedProduct(productOpt.get(), qty, price);
            rp.setUnit(unit);
            request.addRequestedProduct(rp);
        }

        Warehouse warehouse = warehouseOpt.get();
        for (RequestedProduct rp : request.getRequestedProducts()) {
            if (!warehouse.canStoreProduct(rp.getProduct())) {
                log.error("Товар '{}' несовместим со складом '{}'",
                        rp.getProduct().getName(), warehouse.getName());
                return false;
            }
        }

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при сохранении заявки директора: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Директор создал заявку на заказ товара №%d для поставщика «%s». Заявка согласована и готова к приёмке.",
                request.getId(), request.getSupplier().getName());
        notificationService.notifyByRoles(List.of("ROLE_EMPLOYEE_WAREHOUSE_MANAGER"), notificationText);

        log.info("Заявка №{} создана директором со статусом APPROVED.", request.getId());
        return true;
    }

    @Transactional
    public boolean approveByDirector(Long id, String commentText) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != RequestStatus.PENDING_DIRECTOR) {
            log.error("Согласовать можно только заявку в статусе PENDING_DIRECTOR. Текущий: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        Employee currentEmployee = employeeService.getAuthenticationEmployee();
        if (currentEmployee == null) {
            log.error("Не удалось определить текущего сотрудника.");
            return false;
        }

        Comment comment = new Comment(currentEmployee, commentText, request);
        commentRepository.save(comment);

        request.setStatus(RequestStatus.APPROVED);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при согласовании заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» полностью согласована директором",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRoles(
                List.of("ROLE_EMPLOYEE_WAREHOUSE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER"), notificationText);

        log.info("Заявка №{} полностью согласована директором.", id);
        return true;
    }

    @Transactional
    public boolean rejectByDirector(Long id, String commentText) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != RequestStatus.PENDING_DIRECTOR) {
            log.error("Отклонить можно только заявку в статусе PENDING_DIRECTOR. Текущий: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        Employee currentEmployee = employeeService.getAuthenticationEmployee();
        if (currentEmployee == null) {
            log.error("Не удалось определить текущего сотрудника.");
            return false;
        }

        Comment comment = new Comment(currentEmployee, commentText, request);
        commentRepository.save(comment);

        request.setStatus(RequestStatus.REJECTED_BY_DIRECTOR);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при отклонении заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» отклонена директором",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRoles(
                List.of("ROLE_EMPLOYEE_WAREHOUSE_MANAGER", "ROLE_EMPLOYEE_ACCOUNTER"), notificationText);

        log.info("Заявка №{} отклонена директором.", id);
        return true;
    }

    // ========== АДМИНИСТРАТОР: РЕДАКТИРОВАНИЕ И УДАЛЕНИЕ ==========

    /**
     * Редактирование заявки администратором.
     * Разрешено в любом статусе, кроме PARTIALLY_RECEIVED и RECEIVED
     * (там уже есть фактическая приёмка — менять заявку нельзя).
     */
    @Transactional
    public boolean updateRequestByAdmin(Long id, Long supplierId, Long warehouseId, BigDecimal deliveryCost,
                                        List<Long> productIds, List<Integer> quantities,
                                        List<BigDecimal> purchasePrices, List<String> units) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();

        if (request.getStatus() == RequestStatus.PARTIALLY_RECEIVED
                || request.getStatus() == RequestStatus.RECEIVED) {
            log.error("Заявку №{} нельзя редактировать — уже принята (статус '{}').",
                    id, request.getStatus().getDisplayName());
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

        // Нормализуем входные позиции
        Map<Long, AdminProductData> incoming = new LinkedHashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            if (pid == null) continue;
            int qty = (quantities != null && i < quantities.size() && quantities.get(i) != null)
                    ? quantities.get(i) : 1;
            if (qty < 1) qty = 1;
            BigDecimal price = (purchasePrices != null && i < purchasePrices.size() && purchasePrices.get(i) != null)
                    ? purchasePrices.get(i) : BigDecimal.ZERO;
            String unit = (units != null && i < units.size()) ? units.get(i) : null;
            incoming.put(pid, new AdminProductData(qty, price, unit));
        }

        Map<Long, RequestedProduct> existing = new HashMap<>();
        for (RequestedProduct rp : request.getRequestedProducts()) {
            existing.put(rp.getProduct().getId(), rp);
        }

        for (Map.Entry<Long, AdminProductData> e : incoming.entrySet()) {
            RequestedProduct rp = existing.get(e.getKey());
            AdminProductData data = e.getValue();
            if (rp != null) {
                rp.setQuantity(data.quantity);
                rp.setPurchasePrice(data.price);
                rp.setUnit(data.unit);
            } else {
                Product product = productRepository.findById(e.getKey()).orElse(null);
                if (product == null) {
                    log.error("Товар с id={} не найден.", e.getKey());
                    return false;
                }
                RequestedProduct newRp = new RequestedProduct(product, data.quantity, data.price);
                newRp.setUnit(data.unit);
                request.addRequestedProduct(newRp);
            }
        }

        request.getRequestedProducts().removeIf(rp -> !incoming.containsKey(rp.getProduct().getId()));

        Warehouse warehouse = warehouseOpt.get();
        for (RequestedProduct rpCheck : request.getRequestedProducts()) {
            if (!warehouse.canStoreProduct(rpCheck.getProduct())) {
                log.error("Товар '{}' (тип {}) несовместим со складом '{}' (тип {})",
                        rpCheck.getProduct().getName(), rpCheck.getProduct().getWarehouseType(),
                        warehouse.getName(), warehouse.getType());
                return false;
            }
        }

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка обновления заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Заявка №{} обновлена администратором.", id);
        return true;
    }

    private record AdminProductData(int quantity, BigDecimal price, String unit) {}

    /**
     * Удаление заявки администратором.
     * Запрещено только если уже была приёмка (PARTIALLY_RECEIVED / RECEIVED).
     */
    @Transactional
    public boolean deleteRequestByAdmin(Long id) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() == RequestStatus.PARTIALLY_RECEIVED
                || request.getStatus() == RequestStatus.RECEIVED) {
            log.error("Заявку №{} нельзя удалить — уже принята (статус '{}').",
                    id, request.getStatus().getDisplayName());
            return false;
        }

        try {
            requestForDeliveryRepository.delete(request);
        } catch (Exception e) {
            log.error("Ошибка удаления заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Заявка №{} удалена администратором.", id);
        return true;
    }
}
