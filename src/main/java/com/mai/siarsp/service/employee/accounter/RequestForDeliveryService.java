package com.mai.siarsp.service.employee.accounter;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.mapper.RequestForDeliveryMapper;
import com.mai.siarsp.models.Comment;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.repo.CommentRepository;
import com.mai.siarsp.repo.RequestForDeliveryRepository;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с заявками на поставку со стороны бухгалтера (ACCOUNTER)
 *
 * Бухгалтер может просматривать заявки и согласовывать/отклонять заявки
 * в статусе PENDING_ACCOUNTANT.
 */
@Service("accounterRequestForDeliveryService")
@Getter
@Slf4j
public class RequestForDeliveryService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final RequestForDeliveryRepository requestForDeliveryRepository;
    private final EmployeeService employeeService;
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;

    public RequestForDeliveryService(RequestForDeliveryRepository requestForDeliveryRepository,
                                      EmployeeService employeeService,
                                      NotificationService notificationService,
                                      CommentRepository commentRepository) {
        this.requestForDeliveryRepository = requestForDeliveryRepository;
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
}
