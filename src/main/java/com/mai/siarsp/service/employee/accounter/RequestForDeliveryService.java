package com.mai.siarsp.service.employee.accounter;

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
import java.util.*;

/**
 * Сервис для работы с заявками на поставку со стороны бухгалтера (ACCOUNTER)
 *
 * Бухгалтер может просматривать заявки, создавать/редактировать черновики,
 * отправлять заявки напрямую директору на согласование (минуя PENDING_ACCOUNTANT),
 * а также согласовывать/отклонять заявки в статусе PENDING_ACCOUNTANT.
 */
@Service("accounterRequestForDeliveryService")
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
    public boolean approveByAccountant(Long id, String commentText) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != RequestStatus.PENDING_ACCOUNTANT) {
            log.error("Согласовать можно только заявку в статусе PENDING_ACCOUNTANT. Текущий: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        Employee currentEmployee = employeeService.getAuthenticationEmployee();
        if (currentEmployee == null) {
            log.error("Не удалось определить текущего сотрудника.");
            return false;
        }

        Comment comment = new Comment(currentEmployee, commentText, request);
        commentRepository.save(comment);

        request.setStatus(RequestStatus.PENDING_DIRECTOR);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при согласовании заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» согласована бухгалтером, ожидает согласования директора",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", notificationText);

        log.info("Заявка №{} согласована бухгалтером и отправлена директору.", id);
        return true;
    }

    @Transactional
    public boolean rejectByAccountant(Long id, String commentText) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != RequestStatus.PENDING_ACCOUNTANT) {
            log.error("Отклонить можно только заявку в статусе PENDING_ACCOUNTANT. Текущий: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        Employee currentEmployee = employeeService.getAuthenticationEmployee();
        if (currentEmployee == null) {
            log.error("Не удалось определить текущего сотрудника.");
            return false;
        }

        Comment comment = new Comment(currentEmployee, commentText, request);
        commentRepository.save(comment);

        request.setStatus(RequestStatus.REJECTED_BY_ACCOUNTANT);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при отклонении заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» отклонена бухгалтером",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER", notificationText);

        log.info("Заявка №{} отклонена бухгалтером.", id);
        return true;
    }

    // ========== СОЗДАНИЕ / РЕДАКТИРОВАНИЕ / УДАЛЕНИЕ ==========

    @Transactional
    public boolean createRequest(Long supplierId, Long warehouseId, BigDecimal deliveryCost,
                                  List<Long> productIds, List<Integer> quantities,
                                  List<BigDecimal> purchasePrices, List<String> units) {
        log.info("Бухгалтер создаёт заявку на поставку для поставщика id={}...", supplierId);

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
            String unit = (units != null && i < units.size()) ? units.get(i) : null;
            RequestedProduct rp = new RequestedProduct(productOpt.get(), qty, price);
            rp.setUnit(unit);
            request.addRequestedProduct(rp);
        }

        Warehouse warehouse = warehouseOpt.get();
        for (RequestedProduct rp : request.getRequestedProducts()) {
            if (!warehouse.canStoreProduct(rp.getProduct())) {
                log.error("Товар '{}' несовместим со складом '{}'.", rp.getProduct().getName(), warehouse.getName());
                return false;
            }
        }

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при сохранении заявки: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Заявка №{} создана бухгалтером (статус DRAFT).", request.getId());
        return true;
    }

    @Transactional
    public boolean updateRequest(Long id, Long supplierId, Long warehouseId, BigDecimal deliveryCost,
                                  List<Long> productIds, List<Integer> quantities,
                                  List<BigDecimal> purchasePrices, List<String> units) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != RequestStatus.DRAFT && request.getStatus() != RequestStatus.REJECTED_BY_DIRECTOR) {
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

        Map<Long, RequestedProduct> existing = new HashMap<>();
        for (RequestedProduct rp : request.getRequestedProducts()) {
            existing.put(rp.getProduct().getId(), rp);
        }

        Map<Long, int[]> incoming = new LinkedHashMap<>();
        Map<Long, BigDecimal> incomingPrices = new LinkedHashMap<>();
        Map<Long, String> incomingUnits = new LinkedHashMap<>();
        for (int i = 0; i < productIds.size(); i++) {
            Long pid = productIds.get(i);
            if (pid == null) continue;
            int qty = (quantities != null && i < quantities.size() && quantities.get(i) != null) ? quantities.get(i) : 1;
            BigDecimal price = (purchasePrices != null && i < purchasePrices.size() && purchasePrices.get(i) != null) ? purchasePrices.get(i) : BigDecimal.ZERO;
            String unit = (units != null && i < units.size()) ? units.get(i) : null;
            incoming.put(pid, new int[]{qty});
            incomingPrices.put(pid, price);
            incomingUnits.put(pid, unit);
        }

        for (Map.Entry<Long, int[]> e : incoming.entrySet()) {
            Long pid = e.getKey();
            RequestedProduct rp = existing.get(pid);
            if (rp != null) {
                rp.setQuantity(e.getValue()[0]);
                rp.setPurchasePrice(incomingPrices.get(pid));
                rp.setUnit(incomingUnits.get(pid));
            } else {
                Product product = productRepository.findById(pid).orElse(null);
                if (product == null) {
                    log.error("Товар с id={} не найден.", pid);
                    return false;
                }
                RequestedProduct newRp = new RequestedProduct(product, e.getValue()[0], incomingPrices.get(pid));
                newRp.setUnit(incomingUnits.get(pid));
                request.addRequestedProduct(newRp);
            }
        }
        request.getRequestedProducts().removeIf(rp -> !incoming.containsKey(rp.getProduct().getId()));

        Warehouse warehouse = warehouseOpt.get();
        for (RequestedProduct rp : request.getRequestedProducts()) {
            if (!warehouse.canStoreProduct(rp.getProduct())) {
                log.error("Товар '{}' несовместим со складом '{}'.", rp.getProduct().getName(), warehouse.getName());
                return false;
            }
        }

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при обновлении заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Заявка №{} обновлена бухгалтером.", id);
        return true;
    }

    @Transactional
    public boolean deleteRequest(Long id) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != RequestStatus.DRAFT) {
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

        log.info("Заявка №{} удалена бухгалтером.", id);
        return true;
    }

    @Transactional
    public boolean submitToDirector(Long id) {
        Optional<RequestForDelivery> requestOpt = requestForDeliveryRepository.findById(id);
        if (requestOpt.isEmpty()) {
            log.error("Заявка с id={} не найдена.", id);
            return false;
        }

        RequestForDelivery request = requestOpt.get();
        if (request.getStatus() != RequestStatus.DRAFT && request.getStatus() != RequestStatus.REJECTED_BY_DIRECTOR) {
            log.error("Отправить директору можно только черновик или отклонённую заявку. Текущий статус: '{}'.", request.getStatus().getDisplayName());
            return false;
        }

        request.setStatus(RequestStatus.PENDING_DIRECTOR);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при отправке заявки №{} директору: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» отправлена бухгалтером на согласование директору",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", notificationText);

        log.info("Заявка №{} отправлена бухгалтером директору на согласование.", id);
        return true;
    }
}
