package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.dto.RequestedProductDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.mapper.RequestForDeliveryMapper;
import com.mai.siarsp.models.Comment;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.models.RequestedProduct;
import com.mai.siarsp.models.Supplier;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.RequestForDeliveryRepository;
import com.mai.siarsp.repo.SupplierRepository;
import com.mai.siarsp.service.employee.requestForDelivery.RequestForDeliveryNotificationService;
import com.mai.siarsp.service.employee.requestForDelivery.RequestForDeliveryWorkflowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service("warehouseManagerRequestForDeliveryService")
@Slf4j
public class RequestForDeliveryService {

    private final RequestForDeliveryRepository requestRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final RequestForDeliveryWorkflowValidator workflowValidator;
    private final RequestForDeliveryNotificationService notificationService;

    public RequestForDeliveryService(RequestForDeliveryRepository requestRepository,
                                     SupplierRepository supplierRepository,
                                     ProductRepository productRepository,
                                     RequestForDeliveryWorkflowValidator workflowValidator,
                                     RequestForDeliveryNotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.workflowValidator = workflowValidator;
        this.notificationService = notificationService;
    }

    public List<RequestForDeliveryDTO> getAllRequests() {
        return RequestForDeliveryMapper.INSTANCE.toDTOList(requestRepository.findAllByOrderByRequestDateDesc());
    }

    public RequestForDeliveryDTO getRequestById(Long id) {
        Optional<RequestForDelivery> requestOptional = requestRepository.findById(id);
        return requestOptional.map(RequestForDeliveryMapper.INSTANCE::toDTO).orElse(null);
    }

    @Transactional
    public RequestForDeliveryDTO createRequest(RequestForDeliveryDTO dto) {
        Supplier supplier = supplierRepository.findById(dto.getSupplierId()).orElse(null);
        if (supplier == null) {
            log.error("Не найден поставщик id={}", dto.getSupplierId());
            return null;
        }

        RequestForDelivery request = new RequestForDelivery(supplier);
        request.setStatus(RequestStatus.DRAFT);
        applyRequestedProducts(request, dto.getRequestedProducts());

        if (request.getRequestedProducts().isEmpty()) {
            log.error("Создание заявки невозможно: отсутствуют валидные позиции");
            return null;
        }

        RequestForDelivery saved = requestRepository.save(request);
        return RequestForDeliveryMapper.INSTANCE.toDTO(saved);
    }

    @Transactional
    public boolean updateRequest(Long id, RequestForDeliveryDTO dto) {
        Optional<RequestForDelivery> requestOptional = requestRepository.findById(id);
        if (requestOptional.isEmpty()) {
            return false;
        }

        RequestForDelivery request = requestOptional.get();
        if (!List.of(RequestStatus.DRAFT, RequestStatus.REJECTED_BY_DIRECTOR, RequestStatus.REJECTED_BY_ACCOUNTANT)
                .contains(request.getStatus())) {
            log.warn("Редактирование заявки id={} запрещено в статусе {}", id, request.getStatus());
            return false;
        }

        Supplier supplier = supplierRepository.findById(dto.getSupplierId()).orElse(null);
        if (supplier == null) {
            log.error("Не найден поставщик id={} для редактирования заявки {}", dto.getSupplierId(), id);
            return false;
        }

        request.setSupplier(supplier);
        applyRequestedProducts(request, dto.getRequestedProducts());
        if (request.getRequestedProducts().isEmpty()) {
            log.error("Редактирование заявки id={} невозможно: отсутствуют валидные позиции", id);
            return false;
        }

        requestRepository.save(request);
        return true;
    }

    @Transactional
    public boolean deleteRequest(Long id) {
        Optional<RequestForDelivery> requestOptional = requestRepository.findById(id);
        if (requestOptional.isEmpty()) {
            return false;
        }

        RequestForDelivery request = requestOptional.get();
        if (request.getStatus() != RequestStatus.DRAFT) {
            log.warn("Удаление заявки id={} запрещено в статусе {}", id, request.getStatus());
            return false;
        }

        requestRepository.delete(request);
        return true;
    }

    @Transactional
    public boolean submitForApproval(Long id) {
        Optional<RequestForDelivery> requestOptional = requestRepository.findById(id);
        if (requestOptional.isEmpty()) {
            return false;
        }

        RequestForDelivery request = requestOptional.get();
        if (!workflowValidator.canTransition(request.getStatus(), RequestStatus.PENDING_DIRECTOR,
                "ROLE_EMPLOYEE_WAREHOUSE_MANAGER")) {
            return false;
        }

        request.setStatus(RequestStatus.PENDING_DIRECTOR);
        requestRepository.save(request);
        notificationService.notifyStatusChanged(request, RequestStatus.PENDING_DIRECTOR);
        return true;
    }

    @Transactional
    public boolean resubmitForApproval(Long id, String commentText, Employee currentEmployee) {
        Optional<RequestForDelivery> requestOptional = requestRepository.findById(id);
        if (requestOptional.isEmpty()) {
            return false;
        }

        if (commentText == null || commentText.isBlank() || currentEmployee == null) {
            return false;
        }

        RequestForDelivery request = requestOptional.get();
        if (!workflowValidator.canTransition(request.getStatus(), RequestStatus.PENDING_DIRECTOR,
                "ROLE_EMPLOYEE_WAREHOUSE_MANAGER")) {
            return false;
        }

        request.setStatus(RequestStatus.PENDING_DIRECTOR);
        request.getComments().add(new Comment(currentEmployee, commentText.trim(), request));
        requestRepository.save(request);
        notificationService.notifyStatusChanged(request, RequestStatus.PENDING_DIRECTOR);
        return true;
    }

    private void applyRequestedProducts(RequestForDelivery request, List<RequestedProductDTO> items) {
        request.getRequestedProducts().clear();
        if (items == null) {
            return;
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (RequestedProductDTO item : items) {
            if (item == null || item.getProductId() == null || item.getQuantity() <= 0) {
                continue;
            }
            quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = productRepository.findById(entry.getKey()).orElse(null);
            if (product == null) {
                continue;
            }
            request.addRequestedProduct(new RequestedProduct(product, entry.getValue()));
        }
    }
}
