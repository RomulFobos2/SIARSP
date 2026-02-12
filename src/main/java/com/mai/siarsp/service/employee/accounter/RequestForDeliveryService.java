package com.mai.siarsp.service.employee.accounter;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.mapper.RequestForDeliveryMapper;
import com.mai.siarsp.models.Comment;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.repo.RequestForDeliveryRepository;
import com.mai.siarsp.service.employee.requestForDelivery.RequestForDeliveryNotificationService;
import com.mai.siarsp.service.employee.requestForDelivery.RequestForDeliveryWorkflowValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service("accounterRequestForDeliveryService")
@Slf4j
public class RequestForDeliveryService {

    private final RequestForDeliveryRepository requestRepository;
    private final RequestForDeliveryWorkflowValidator workflowValidator;
    private final RequestForDeliveryNotificationService notificationService;

    public RequestForDeliveryService(RequestForDeliveryRepository requestRepository,
                                     RequestForDeliveryWorkflowValidator workflowValidator,
                                     RequestForDeliveryNotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.workflowValidator = workflowValidator;
        this.notificationService = notificationService;
    }

    public List<RequestForDeliveryDTO> getAllRequests() {
        return RequestForDeliveryMapper.INSTANCE.toDTOList(requestRepository.findAllByOrderByRequestDateDesc());
    }

    public List<RequestForDeliveryDTO> getRequestsByStatus(RequestStatus status) {
        return RequestForDeliveryMapper.INSTANCE.toDTOList(requestRepository.findByStatusOrderByRequestDateDesc(status));
    }

    public RequestForDeliveryDTO getRequestById(Long id) {
        return requestRepository.findById(id).map(RequestForDeliveryMapper.INSTANCE::toDTO).orElse(null);
    }

    @Transactional
    public boolean approveByAccountant(Long id, String commentText, Employee currentEmployee) {
        return process(id, commentText, currentEmployee, RequestStatus.APPROVED);
    }

    @Transactional
    public boolean rejectByAccountant(Long id, String commentText, Employee currentEmployee) {
        return process(id, commentText, currentEmployee, RequestStatus.REJECTED_BY_ACCOUNTANT);
    }

    private boolean process(Long id, String commentText, Employee currentEmployee, RequestStatus targetStatus) {
        Optional<RequestForDelivery> requestOptional = requestRepository.findById(id);
        if (requestOptional.isEmpty() || currentEmployee == null || commentText == null || commentText.isBlank()) {
            return false;
        }

        RequestForDelivery request = requestOptional.get();
        if (!workflowValidator.canTransition(request.getStatus(), targetStatus, "ROLE_EMPLOYEE_ACCOUNTER")) {
            log.warn("Согласование бухгалтером заявки id={} отклонено", id);
            return false;
        }

        request.setStatus(targetStatus);
        request.getComments().add(new Comment(currentEmployee, commentText.trim(), request));
        requestRepository.save(request);
        notificationService.notifyStatusChanged(request, targetStatus);
        return true;
    }
}
