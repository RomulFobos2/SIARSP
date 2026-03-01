package com.mai.siarsp.controllers.employee.warehouseWorker;

import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller("warehouseWorkerDeliveryTaskController")
@RequestMapping("/employee/warehouseWorker/deliveryTasks")
@Slf4j
public class DeliveryTaskController {

    private final DeliveryTaskService deliveryTaskService;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService) {
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/allDeliveryTasks")
    public String allDeliveryTasks(Model model) {
        model.addAttribute("tasks", deliveryTaskService.getTasksByStatuses(
                List.of(DeliveryTaskStatus.PENDING, DeliveryTaskStatus.LOADING)));
        return "employee/warehouseWorker/deliveryTasks/allDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDeliveryTask/{id}")
    public String detailsDeliveryTask(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/warehouseWorker/deliveryTasks/allDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/warehouseWorker/deliveryTasks/detailsDeliveryTask";
    }

    @PostMapping("/startLoading/{id}")
    public String startLoading(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.startLoading(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при начале погрузки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Погрузка начата.");
        }
        return "redirect:/employee/warehouseWorker/deliveryTasks/detailsDeliveryTask/" + id;
    }

    @PostMapping("/completeLoading/{id}")
    public String completeLoading(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.completeLoading(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при завершении погрузки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Погрузка завершена. Товар отгружен.");
        }
        return "redirect:/employee/warehouseWorker/deliveryTasks/detailsDeliveryTask/" + id;
    }
}
