package com.mai.siarsp.controllers.employee.warehouseWorker;

import com.mai.siarsp.dto.DeliveryTaskDTO;
import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.models.OrderedProduct;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.ZoneProductRepository;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller("warehouseWorkerDeliveryTaskController")
@RequestMapping("/employee/warehouseWorker/deliveryTasks")
@Slf4j
public class DeliveryTaskController {

    private final DeliveryTaskService deliveryTaskService;
    private final ZoneProductRepository zoneProductRepository;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService,
                                  ZoneProductRepository zoneProductRepository) {
        this.deliveryTaskService = deliveryTaskService;
        this.zoneProductRepository = zoneProductRepository;
    }

    @GetMapping("/allDeliveryTasks")
    public String allDeliveryTasks(@RequestParam(required = false) String search,
                                   @RequestParam(required = false, defaultValue = "active") String status,
                                   Model model) {
        List<DeliveryTaskDTO> tasks;
        switch (status) {
            case "completed" -> tasks = deliveryTaskService.getTasksByStatuses(
                    List.of(DeliveryTaskStatus.DELIVERED, DeliveryTaskStatus.IN_TRANSIT, DeliveryTaskStatus.CANCELLED));
            case "all" -> tasks = deliveryTaskService.getAllTasks();
            default -> {
                tasks = deliveryTaskService.getTasksByStatuses(
                        List.of(DeliveryTaskStatus.PENDING, DeliveryTaskStatus.LOADING, DeliveryTaskStatus.LOADED));
                status = "active";
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
        return "employee/warehouseWorker/deliveryTasks/allDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDeliveryTask/{id}")
    public String detailsDeliveryTask(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/warehouseWorker/deliveryTasks/allDeliveryTasks";
        }

        DeliveryTask task = optTask.get();
        model.addAttribute("task", task);

        // Местоположение товаров на складе: productId → List<ZoneProduct>
        Map<Long, List<ZoneProduct>> productLocations = new HashMap<>();
        for (OrderedProduct op : task.getClientOrder().getOrderedProducts()) {
            List<ZoneProduct> locations = zoneProductRepository.findByProduct(op.getProduct());
            productLocations.put(op.getProduct().getId(), locations);
        }
        model.addAttribute("productLocations", productLocations);

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
