package com.mai.siarsp.controllers.employee.courier;

import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller("courierDeliveryTaskController")
@RequestMapping("/employee/courier/deliveryTasks")
@Slf4j
public class DeliveryTaskController {

    private final DeliveryTaskService deliveryTaskService;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService) {
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/myDeliveryTasks")
    public String myDeliveryTasks(@AuthenticationPrincipal Employee currentUser, Model model) {
        model.addAttribute("tasks", deliveryTaskService.getTasksByDriver(currentUser.getId()));
        return "employee/courier/deliveryTasks/myDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDeliveryTask/{id}")
    public String detailsDeliveryTask(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/courier/deliveryTasks/myDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/courier/deliveryTasks/detailsDeliveryTask";
    }

    @PostMapping("/startDelivery/{id}")
    public String startDelivery(@PathVariable Long id,
                                @RequestParam Integer startMileage,
                                RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.startDelivery(id, startMileage)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при начале доставки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Доставка начата.");
        }
        return "redirect:/employee/courier/deliveryTasks/detailsDeliveryTask/" + id;
    }

    @PostMapping("/updateLocation/{id}")
    public String updateLocation(@PathVariable Long id,
                                 @RequestParam Double latitude,
                                 @RequestParam Double longitude,
                                 RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.updateLocation(id, latitude, longitude)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении местоположения.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Местоположение обновлено.");
        }
        return "redirect:/employee/courier/deliveryTasks/detailsDeliveryTask/" + id;
    }

    @PostMapping("/markRoutePoint/{id}/{pointId}")
    public String markRoutePoint(@PathVariable Long id,
                                 @PathVariable Long pointId,
                                 RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.markRoutePointReached(id, pointId)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отметке маршрутной точки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Маршрутная точка отмечена.");
        }
        return "redirect:/employee/courier/deliveryTasks/detailsDeliveryTask/" + id;
    }

    @Transactional(readOnly = true)
    @GetMapping("/editDocuments/{id}")
    public String editDocuments(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/courier/deliveryTasks/myDeliveryTasks";
        }

        DeliveryTask task = optTask.get();
        model.addAttribute("task", task);

        // Предзаполнить акт если есть
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByTask(id);
        optAct.ifPresent(act -> model.addAttribute("acceptanceAct", act));

        return "employee/courier/deliveryTasks/editDocuments";
    }

    @PostMapping("/saveDocuments/{id}")
    public String saveDocuments(@PathVariable Long id,
                                @RequestParam(required = false) String cargoDescription,
                                @RequestParam(required = false) Double totalWeight,
                                @RequestParam(required = false) Double totalVolume,
                                @RequestParam(required = false) String ttnComment,
                                @RequestParam(required = false) String actComment,
                                RedirectAttributes redirectAttributes) {
        boolean ttnOk = deliveryTaskService.createOrUpdateTTN(id, cargoDescription, totalWeight, totalVolume, ttnComment);
        boolean actOk = deliveryTaskService.createOrUpdateAcceptanceAct(id, actComment);

        if (!ttnOk || !actOk) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при сохранении документов.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Документы сохранены.");
        }
        return "redirect:/employee/courier/deliveryTasks/detailsDeliveryTask/" + id;
    }

    @PostMapping("/completeDelivery/{id}")
    public String completeDelivery(@PathVariable Long id,
                                   @RequestParam Integer endMileage,
                                   @RequestParam String clientRepresentative,
                                   @RequestParam(required = false) String actComment,
                                   RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.completeDelivery(id, endMileage, clientRepresentative, actComment)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при завершении доставки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Доставка завершена. Заказ доставлен.");
        }
        return "redirect:/employee/courier/deliveryTasks/detailsDeliveryTask/" + id;
    }
}
