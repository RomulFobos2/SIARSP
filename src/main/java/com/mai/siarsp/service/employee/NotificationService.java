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

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления уведомлениями сотрудников
 *
 * Предоставляет функционал:
 * - Создание уведомления для конкретного сотрудника
 * - Массовая рассылка уведомлений по роли
 * - Получение списка уведомлений сотрудника
 * - Подсчёт непрочитанных уведомлений (для badge в header)
 * - Отметка уведомлений как прочитанных
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

    /**
     * Создаёт уведомление для конкретного сотрудника
     *
     * @param recipient сотрудник-получатель
     * @param text текст уведомления
     */
    @Transactional
    public void createNotification(Employee recipient, String text) {
        Notification notification = new Notification(recipient, text);
        notificationRepository.save(notification);
        log.info("Создано уведомление для сотрудника '{}': {}", recipient.getFullName(), text);
    }

    /**
     * Рассылает уведомление всем сотрудникам с указанной ролью
     *
     * @param roleName имя роли (например, "ROLE_EMPLOYEE_MANAGER")
     * @param text текст уведомления
     */
    @Transactional
    public void notifyByRole(String roleName, String text) {
        List<Employee> employees = employeeRepository.findAllByRoleName(roleName);
        for (Employee employee : employees) {
            createNotification(employee, text);
        }
        log.info("Отправлено уведомление {} сотрудникам с ролью '{}'.", employees.size(), roleName);
    }

    /**
     * Рассылает уведомление всем сотрудникам с указанными ролями
     *
     * @param roleNames список имён ролей
     * @param text текст уведомления
     */
    @Transactional
    public void notifyByRoles(List<String> roleNames, String text) {
        for (String roleName : roleNames) {
            notifyByRole(roleName, text);
        }
    }

    /**
     * Возвращает все уведомления сотрудника (новейшие первыми)
     *
     * @param employeeId ID сотрудника
     * @return список NotificationDTO
     */
    public List<NotificationDTO> getNotificationsForEmployee(Long employeeId) {
        List<Notification> notifications = notificationRepository.findByRecipientIdAndVisibleTrueOrderByCreatedAtDesc(employeeId);
        return NotificationMapper.INSTANCE.toDTOList(notifications);
    }

    /**
     * Возвращает уведомления сотрудника с пагинацией и фильтрацией
     *
     * @param employeeId ID сотрудника
     * @param status     фильтр по статусу (может быть null)
     * @param search     поиск по тексту (может быть null или пустым)
     * @param pageable   параметры пагинации
     * @return страница NotificationDTO
     */
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

    /**
     * Возвращает количество непрочитанных уведомлений сотрудника
     *
     * @param employeeId ID сотрудника
     * @return количество непрочитанных
     */
    public long getUnreadCount(Long employeeId) {
        return notificationRepository.countByRecipientIdAndStatusAndVisibleTrue(employeeId, NotificationStatus.NEW);
    }

    /**
     * Отмечает уведомление как прочитанное
     *
     * @param notificationId ID уведомления
     */
    @Transactional
    public void markAsRead(Long notificationId) {
        Optional<Notification> optional = notificationRepository.findById(notificationId);
        if (optional.isPresent()) {
            Notification notification = optional.get();
            notification.setStatus(NotificationStatus.READ);
            notificationRepository.save(notification);
        }
    }

    /**
     * Отмечает все уведомления сотрудника как прочитанные
     *
     * @param employeeId ID сотрудника
     */
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

    /**
     * Скрывает уведомление (устанавливает visible = false).
     * Уведомление остаётся в БД для расследования, но не отображается пользователю.
     *
     * @param notificationId ID уведомления
     * @return true при успешном скрытии
     */
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
