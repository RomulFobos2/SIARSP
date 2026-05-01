package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.NotificationDTO;
import com.mai.siarsp.enumeration.NotificationStatus;
import com.mai.siarsp.mapper.NotificationMapper;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Notification;
import com.mai.siarsp.repo.EmployeeRepository;
import com.mai.siarsp.repo.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Сервис оповещений. Рассылает сотрудникам события, чтобы процессы не «зависали» между этапами.
 */

@Service
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

    public NotificationService(NotificationRepository notificationRepository,
                                EmployeeRepository employeeRepository) {
        this.notificationRepository = notificationRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public void createNotification(Employee recipient, String text) {
        Notification notification = new Notification(recipient, text);
        notificationRepository.save(notification);
        log.info("Создано уведомление для сотрудника '{}': {}", recipient.getFullName(), text);
    }

    /** Управленческие роли — события, направленные им, дублируются админу-владельцу ИП. */
    private static final String ROLE_ADMIN = "ROLE_EMPLOYEE_ADMIN";
    private static final Set<String> MANAGEMENT_ROLES = Set.of(
            "ROLE_EMPLOYEE_MANAGER",
            "ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
            "ROLE_EMPLOYEE_ACCOUNTER"
    );

    @Transactional
    public void notifyByRole(String roleName, String text) {
        Set<Long> notified = new HashSet<>();
        sendToRole(roleName, text, notified);
        if (MANAGEMENT_ROLES.contains(roleName)) {
            sendToRole(ROLE_ADMIN, text, notified);
        }
        log.info("Отправлено уведомление {} сотрудникам по запросу роли '{}' (с учётом дублирования администратору).",
                notified.size(), roleName);
    }

    @Transactional
    public void notifyByRoles(List<String> roleNames, String text) {
        Set<Long> notified = new HashSet<>();
        boolean anyManagement = false;
        for (String roleName : roleNames) {
            sendToRole(roleName, text, notified);
            if (MANAGEMENT_ROLES.contains(roleName)) {
                anyManagement = true;
            }
        }
        if (anyManagement) {
            sendToRole(ROLE_ADMIN, text, notified);
        }
        log.info("Отправлено уведомление {} сотрудникам по ролям {} (с учётом дублирования администратору).",
                notified.size(), roleNames);
    }

    private void sendToRole(String roleName, String text, Set<Long> notified) {
        List<Employee> employees = employeeRepository.findAllByRoleName(roleName);
        for (Employee employee : employees) {
            if (notified.add(employee.getId())) {
                createNotification(employee, text);
            }
        }
    }

    public List<NotificationDTO> getNotificationsForEmployee(Long employeeId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdAndVisibleTrueOrderByCreatedAtDesc(employeeId);
        return NotificationMapper.INSTANCE.toDTOList(notifications);
    }

    public Page<NotificationDTO> getNotificationsForEmployee(Long employeeId, NotificationStatus status, String search, Pageable pageable) {
        Page<Notification> page;
        boolean hasSearch = search != null && !search.isBlank();

        if (status != null && hasSearch) {
            page = notificationRepository.findByRecipientIdAndVisibleTrueAndStatusAndTextContainingIgnoreCaseOrderByCreatedAtDesc(employeeId, status, search, pageable);
        } else if (status != null) {
            page = notificationRepository.findByRecipientIdAndVisibleTrueAndStatusOrderByCreatedAtDesc(employeeId, status, pageable);
        } else if (hasSearch) {
            page = notificationRepository.findByRecipientIdAndVisibleTrueAndTextContainingIgnoreCaseOrderByCreatedAtDesc(employeeId, search, pageable);
        } else {
            page = notificationRepository.findByRecipientIdAndVisibleTrueOrderByCreatedAtDesc(employeeId, pageable);
        }

        return page.map(NotificationMapper.INSTANCE::toDTO);
    }

    public long getUnreadCount(Long employeeId) {
        return notificationRepository.countByRecipientIdAndStatusAndVisibleTrue(employeeId, NotificationStatus.NEW);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Optional<Notification> optional = notificationRepository.findById(notificationId);
        if (optional.isPresent()) {
            Notification notification = optional.get();
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(Long employeeId) {
        List<Notification> unread = notificationRepository
                .findByRecipientIdAndVisibleTrueAndStatusOrderByCreatedAtDesc(employeeId, NotificationStatus.NEW);
        for (Notification notification : unread) {
            notification.setStatus(NotificationStatus.READ);
        }
        notificationRepository.saveAll(unread);
        log.info("Все уведомления сотрудника id={} отмечены как прочитанные ({} шт.).", employeeId, unread.size());
    }

    @Transactional
    public boolean hideNotification(Long notificationId) {
        Optional<Notification> optional = notificationRepository.findById(notificationId);
        if (optional.isPresent()) {
            Notification notification = optional.get();
            notification.setVisible(false);
            notificationRepository.save(notification);
            log.info("Уведомление id={} скрыто.", notificationId);
            return true;
        }
        return false;
    }
}
