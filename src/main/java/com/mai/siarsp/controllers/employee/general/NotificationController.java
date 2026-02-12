package com.mai.siarsp.controllers.employee.general;

import com.mai.siarsp.dto.EmployeeDTO;
import com.mai.siarsp.service.employee.EmployeeService;
import com.mai.siarsp.service.employee.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Контроллер для управления уведомлениями сотрудников
 *
 * Предоставляет:
 * - Страницу со списком всех уведомлений текущего сотрудника
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
     * Страница всех уведомлений текущего сотрудника
     */
    @GetMapping("/employee/notifications/")
    public String allNotifications(Model model) {
        EmployeeDTO currentEmployee = employeeService.getAuthenticationEmployeeDTO();
        if (currentEmployee == null) {
            return "redirect:/employee/login";
        }
        model.addAttribute("notifications", notificationService.getNotificationsForEmployee(currentEmployee.getId()));
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
