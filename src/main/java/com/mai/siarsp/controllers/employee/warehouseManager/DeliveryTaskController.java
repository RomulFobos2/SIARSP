package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.enumeration.RoutePointType;
import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import com.mai.siarsp.service.general.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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
    private final WarehouseRepository warehouseRepository;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService,
                                  ClientOrderService clientOrderService,
                                  WarehouseRepository warehouseRepository) {
        this.deliveryTaskService = deliveryTaskService;
        this.clientOrderService = clientOrderService;
        this.warehouseRepository = warehouseRepository;
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

        ClientOrder order = optOrder.get();
        boolean needsRefrigeration = deliveryTaskService.orderNeedsRefrigeration(order);

        model.addAttribute("order", order);
        model.addAttribute("drivers", deliveryTaskService.getAvailableDrivers());
        model.addAttribute("vehicles", deliveryTaskService.getAvailableVehicles(needsRefrigeration));
        model.addAttribute("warehouses", warehouseRepository.findAll());
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
            return "redirect:/employee/warehouseManager/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        DeliveryTask task = optTask.get();
        model.addAttribute("order", task.getClientOrder());
        model.addAttribute("ttn", task.getTtn());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/warehouseManager/deliveryTasks/detailsDeliveryTask/" + taskId);
        return "employee/warehouseManager/documents/detailsTTN";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsAcceptanceAct/{taskId}")
    public String detailsAcceptanceAct(@PathVariable Long taskId, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty() || optTask.get().getClientOrder() == null) {
            return "redirect:/employee/warehouseManager/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        DeliveryTask task = optTask.get();
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(task.getClientOrder().getId());
        if (optAct.isEmpty()) {
            return "redirect:/employee/warehouseManager/deliveryTasks/detailsDeliveryTask/" + taskId;
        }
        model.addAttribute("order", task.getClientOrder());
        model.addAttribute("act", optAct.get());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/warehouseManager/deliveryTasks/detailsDeliveryTask/" + taskId);
        return "employee/warehouseManager/documents/detailsAcceptanceAct";
    }
}
