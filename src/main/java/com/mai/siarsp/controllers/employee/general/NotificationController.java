package com.mai.siarsp.controllers.employee.general;

import com.mai.siarsp.dto.EmployeeDTO;
import com.mai.siarsp.dto.NotificationDTO;
import com.mai.siarsp.enumeration.NotificationStatus;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Контроллер для управления уведомлениями сотрудников
 *
 * Предоставляет:
 * - Страницу со списком всех уведомлений текущего сотрудника (с пагинацией и фильтрацией)
 * - Отметку уведомления как прочитанного
 * - Отметку всех уведомлений как прочитанных
 * - AJAX-эндпоинт для получения количества непрочитанных (badge в header)
 *
 * Доступ: все авторизованные сотрудники
 * URL-префикс: /employee/notifications/
 */
@Controller
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;
    private final EmployeeService employeeService;

    public NotificationController(NotificationService notificationService,
                                   EmployeeService employeeService) {
        this.notificationService = notificationService;
        this.employeeService = employeeService;
    }

    /**
     * Страница всех уведомлений текущего сотрудника с пагинацией и фильтрацией
     */
    @GetMapping("/employee/notifications/")
    public String allNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Model model) {
        EmployeeDTO currentEmployee = employeeService.getAuthenticationEmployeeDTO();
        if (currentEmployee == null) {
            return "redirect:/employee/login";
        }

        // Парсинг статуса
        NotificationStatus notificationStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                notificationStatus = NotificationStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
                // Некорректный статус — игнорируем
            }
        }

        // Ограничиваем размер страницы
        if (size < 1) size = 20;
        if (size > 100) size = 100;

        Page<NotificationDTO> notifications = notificationService.getNotificationsForEmployee(
                currentEmployee.getId(), notificationStatus, search, PageRequest.of(page, size));

        model.addAttribute("notifications", notifications);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("statusFilter", status != null ? status : "");
        model.addAttribute("searchFilter", search != null ? search : "");
        return "employee/general/allNotifications";
    }

    /**
     * Отмечает уведомление как прочитанное и возвращает на страницу уведомлений
     */
    @GetMapping("/employee/notifications/markAsRead/{id}")
    public String markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return "redirect:/employee/notifications/";
    }

    /**
     * Отмечает все уведомления текущего сотрудника как прочитанные
     */
    @GetMapping("/employee/notifications/markAllAsRead")
    public String markAllAsRead() {
        EmployeeDTO currentEmployee = employeeService.getAuthenticationEmployeeDTO();
        if (currentEmployee == null) {
            return "redirect:/employee/login";
        }
        notificationService.markAllAsRead(currentEmployee.getId());
        return "redirect:/employee/notifications/";
    }

    /**
     * AJAX-эндпоинт: возвращает количество непрочитанных уведомлений
     * Используется для badge в header
     */
    @GetMapping("/employee/notifications/unreadCount")
    @ResponseBody
    public Long unreadCount() {
        EmployeeDTO currentEmployee = employeeService.getAuthenticationEmployeeDTO();
        if (currentEmployee == null) {
            return 0L;
        }
        return notificationService.getUnreadCount(currentEmployee.getId());
    }
}
