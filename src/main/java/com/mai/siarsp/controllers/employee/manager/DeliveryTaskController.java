package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.DeliveryTaskDTO;
import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import com.mai.siarsp.service.general.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    // ========== СКАЧИВАНИЕ КОНТРАКТА ==========

    @GetMapping("/downloadContract/{taskId}")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long taskId) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getClientOrder() == null
                || optTask.get().getClientOrder().getContractFile() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contractFileName = optTask.get().getClientOrder().getContractFile();
            Resource resource = ContractService.getContractData(contractFileName);
            String downloadName = contractFileName.contains("_")
                    ? contractFileName.substring(contractFileName.indexOf("_") + 1)
                    : contractFileName;
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + downloadName + "\"")
                    .body(resource);
        } catch (IOException e) {
            log.error("Ошибка скачивания контракта: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ========== ПРОСМОТР ДОКУМЕНТОВ (read-only) ==========

    @Transactional(readOnly = true)
    @GetMapping("/detailsTTN/{taskId}")
    public String detailsTTN(@PathVariable Long taskId, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getTtn() == null) {
            return "redirect:/employee/manager/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        DeliveryTask task = optTask.get();
        model.addAttribute("order", task.getClientOrder());
        model.addAttribute("ttn", task.getTtn());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/manager/deliveryTasks/detailsDeliveryTask/" + taskId);
        return "employee/warehouseManager/documents/detailsTTN";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsAcceptanceAct/{taskId}")
    public String detailsAcceptanceAct(@PathVariable Long taskId, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getClientOrder() == null) {
            return "redirect:/employee/manager/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        DeliveryTask task = optTask.get();
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(task.getClientOrder().getId());
        if (optAct.isEmpty()) {
            return "redirect:/employee/manager/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        model.addAttribute("order", task.getClientOrder());
        model.addAttribute("act", optAct.get());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/manager/deliveryTasks/detailsDeliveryTask/" + taskId);
        return "employee/warehouseManager/documents/detailsAcceptanceAct";
    }
}
