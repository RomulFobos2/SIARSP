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
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
            DRAFT, Set.of(PENDING_DIRECTOR, CANCELLED),
            PENDING_DIRECTOR, Set.of(PENDING_ACCOUNTANT, REJECTED_BY_DIRECTOR),
            REJECTED_BY_DIRECTOR, Set.of(PENDING_DIRECTOR, CANCELLED),
            PENDING_ACCOUNTANT, Set.of(APPROVED, REJECTED_BY_ACCOUNTANT),
            REJECTED_BY_ACCOUNTANT, Set.of(PENDING_DIRECTOR, CANCELLED),
            APPROVED, Set.of(PARTIALLY_RECEIVED, RECEIVED),
            PARTIALLY_RECEIVED, Set.of(RECEIVED)
    );

    private final RequestForDeliveryRepository requestForDeliveryRepository;
    private final RequestedProductRepository requestedProductRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;

    public RequestForDeliveryService(RequestForDeliveryRepository requestForDeliveryRepository,
                                      RequestedProductRepository requestedProductRepository,
                                      SupplierRepository supplierRepository,
                                      ProductRepository productRepository,
                                      EmployeeService employeeService,
                                      NotificationService notificationService,
                                      CommentRepository commentRepository) {
        this.requestForDeliveryRepository = requestForDeliveryRepository;
        this.requestedProductRepository = requestedProductRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
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
    public boolean createRequest(Long supplierId, List<Long> productIds, List<Integer> quantities) {
        log.info("Создание заявки на поставку для поставщика id={}...", supplierId);

        Optional<Supplier> supplierOpt = supplierRepository.findById(supplierId);
        if (supplierOpt.isEmpty()) {
            log.error("Поставщик с id={} не найден.", supplierId);
            return false;
        }

        if (productIds == null || productIds.isEmpty()) {
            log.error("Список товаров пуст.");
            return false;
        }

        RequestForDelivery request = new RequestForDelivery(supplierOpt.get());

        for (int i = 0; i < productIds.size(); i++) {
            Optional<Product> productOpt = productRepository.findById(productIds.get(i));
            if (productOpt.isEmpty()) {
                log.error("Товар с id={} не найден.", productIds.get(i));
                return false;
            }
            int qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
            request.addRequestedProduct(new RequestedProduct(productOpt.get(), qty));
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
    public boolean updateRequest(Long id, Long supplierId, List<Long> productIds, List<Integer> quantities) {
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

        request.setSupplier(supplierOpt.get());

        // Удаляем старые позиции через репозиторий и flush, чтобы DELETE выполнился в БД
        // до INSERT новых записей (иначе UniqueConstraint на request_id+product_id нарушается)
        requestedProductRepository.deleteAll(request.getRequestedProducts());
        requestedProductRepository.flush();
        request.getRequestedProducts().clear();

        for (int i = 0; i < productIds.size(); i++) {
            Optional<Product> productOpt = productRepository.findById(productIds.get(i));
            if (productOpt.isEmpty()) {
                log.error("Товар с id={} не найден.", productIds.get(i));
                return false;
            }
            int qty = (quantities != null && i < quantities.size()) ? quantities.get(i) : 1;
            request.addRequestedProduct(new RequestedProduct(productOpt.get(), qty));
        }

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

        request.setStatus(PENDING_DIRECTOR);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при отправке заявки №{} на согласование: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» отправлена на согласование",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", notificationText);

        log.info("Заявка №{} отправлена на согласование директору.", id);
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

        request.setStatus(PENDING_DIRECTOR);

        try {
            requestForDeliveryRepository.save(request);
        } catch (Exception e) {
            log.error("Ошибка при повторной отправке заявки №{}: {}", id, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        String notificationText = String.format(
                "Заявка на поставку №%d от %s для поставщика «%s» отправлена на согласование",
                request.getId(), request.getRequestDate().format(DATE_FMT), request.getSupplier().getName());
        notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", notificationText);

        log.info("Заявка №{} повторно отправлена на согласование.", id);
        return true;
    }
}
