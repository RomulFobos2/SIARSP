package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.enumeration.RoutePointType;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller("warehouseManagerDeliveryTaskController")
@RequestMapping("/employee/warehouseManager/deliveryTasks")
@Slf4j
public class DeliveryTaskController {

    private final DeliveryTaskService deliveryTaskService;
    private final ClientOrderService clientOrderService;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService,
                                  ClientOrderService clientOrderService) {
        this.deliveryTaskService = deliveryTaskService;
        this.clientOrderService = clientOrderService;
    }

    @GetMapping("/allDeliveryTasks")
    public String allDeliveryTasks(Model model) {
        model.addAttribute("tasks", deliveryTaskService.getAllTasks());
        return "employee/warehouseManager/deliveryTasks/allDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/createDeliveryTask/{orderId}")
    public String createDeliveryTaskForm(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/warehouseManager/clientOrders/allClientOrders";
        }

        model.addAttribute("order", optOrder.get());
        model.addAttribute("drivers", deliveryTaskService.getAvailableDrivers());
        model.addAttribute("vehicles", deliveryTaskService.getAvailableVehicles());
        model.addAttribute("routePointTypes", RoutePointType.values());
        return "employee/warehouseManager/deliveryTasks/createDeliveryTask";
    }

    @PostMapping("/createDeliveryTask/{orderId}")
    public String createDeliveryTask(
            @PathVariable Long orderId,
            @RequestParam Long driverId,
            @RequestParam Long vehicleId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedStartTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime plannedEndTime,
            @RequestParam(required = false) List<String> rpType,
            @RequestParam(required = false) List<String> rpAddress,
            @RequestParam(required = false) List<String> rpLatitude,
            @RequestParam(required = false) List<String> rpLongitude,
            @RequestParam(required = false) List<String> rpComment,
            RedirectAttributes redirectAttributes) {

        List<DeliveryTaskService.RoutePointRequest> routePoints = new ArrayList<>();
        if (rpType != null) {
            for (int i = 0; i < rpType.size(); i++) {
                RoutePointType type = RoutePointType.valueOf(rpType.get(i));
                String address = rpAddress != null && i < rpAddress.size() ? rpAddress.get(i) : "";
                Double lat = null;
                Double lng = null;
                try {
                    if (rpLatitude != null && i < rpLatitude.size() && !rpLatitude.get(i).isBlank()) {
                        lat = Double.parseDouble(rpLatitude.get(i));
                    }
                    if (rpLongitude != null && i < rpLongitude.size() && !rpLongitude.get(i).isBlank()) {
                        lng = Double.parseDouble(rpLongitude.get(i));
                    }
                } catch (NumberFormatException ignored) {}
                String comment = rpComment != null && i < rpComment.size() ? rpComment.get(i) : "";
                routePoints.add(new DeliveryTaskService.RoutePointRequest(type, address, lat, lng, comment));
            }
        }

        if (!deliveryTaskService.createTask(orderId, driverId, vehicleId, plannedStartTime, plannedEndTime, routePoints)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при создании задачи на доставку.");
            return "redirect:/employee/warehouseManager/deliveryTasks/createDeliveryTask/" + orderId;
        }

        redirectAttributes.addFlashAttribute("successMessage", "Задача на доставку создана.");
        return "redirect:/employee/warehouseManager/deliveryTasks/allDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDeliveryTask/{id}")
    public String detailsDeliveryTask(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/warehouseManager/deliveryTasks/allDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/warehouseManager/deliveryTasks/detailsDeliveryTask";
    }

    @PostMapping("/cancelDeliveryTask/{id}")
    public String cancelDeliveryTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.cancelTask(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отмене задачи.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Задача отменена.");
        }
        return "redirect:/employee/warehouseManager/deliveryTasks/detailsDeliveryTask/" + id;
    }
}
