package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.DeliveryTaskDTO;
import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер задач на доставку для руководителя (менеджера)
 *
 * Руководитель просматривает все задачи на доставку (без возможности изменения)
 */
@Controller("managerDeliveryTaskController")
@RequestMapping("/employee/manager/deliveryTasks")
@Slf4j
public class DeliveryTaskController {

    private final DeliveryTaskService deliveryTaskService;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService) {
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/allDeliveryTasks")
    public String allDeliveryTasks(@RequestParam(required = false) String search,
                                   @RequestParam(required = false, defaultValue = "all") String status,
                                   Model model) {
        List<DeliveryTaskDTO> tasks;
        switch (status) {
            case "active" -> tasks = deliveryTaskService.getTasksByStatuses(
                    List.of(DeliveryTaskStatus.PENDING, DeliveryTaskStatus.LOADING,
                            DeliveryTaskStatus.LOADED, DeliveryTaskStatus.IN_TRANSIT));
            case "completed" -> tasks = deliveryTaskService.getTasksByStatuses(
                    List.of(DeliveryTaskStatus.DELIVERED, DeliveryTaskStatus.CANCELLED));
            default -> {
                tasks = deliveryTaskService.getAllTasks();
                status = "all";
            }
        }

        // Фильтрация по номеру заказа
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            tasks = tasks.stream()
                    .filter(t -> t.getClientOrderNumber() != null
                            && t.getClientOrderNumber().toLowerCase().contains(searchLower))
                    .toList();
        }

        model.addAttribute("tasks", tasks);
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("currentStatus", status);
        return "employee/manager/deliveryTasks/allDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDeliveryTask/{id}")
    public String detailsDeliveryTask(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/manager/deliveryTasks/allDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/manager/deliveryTasks/detailsDeliveryTask";
    }
}
