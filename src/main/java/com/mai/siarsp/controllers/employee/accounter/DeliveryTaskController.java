package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.models.DeliveryTask;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller("accounterDeliveryTaskController")
@RequestMapping("/employee/accounter/deliveryTasks")
@Slf4j
public class DeliveryTaskController {

    private final DeliveryTaskService deliveryTaskService;

    public DeliveryTaskController(DeliveryTaskService deliveryTaskService) {
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/allDeliveryTasks")
    public String allDeliveryTasks(Model model) {
        model.addAttribute("tasks", deliveryTaskService.getAllTasks());
        return "employee/accounter/deliveryTasks/allDeliveryTasks";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDeliveryTask/{id}")
    public String detailsDeliveryTask(@PathVariable Long id, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(id);
        if (optTask.isEmpty()) {
            return "redirect:/employee/accounter/deliveryTasks/allDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/accounter/deliveryTasks/detailsDeliveryTask";
    }

    @Transactional(readOnly = true)
    @GetMapping("/createTTN/{taskId}")
    public String createTTNForm(@PathVariable Long taskId, Model model) {
        Optional<DeliveryTask> optTask = deliveryTaskService.getTaskById(taskId);
        if (optTask.isEmpty()) {
            return "redirect:/employee/accounter/deliveryTasks/allDeliveryTasks";
        }

        model.addAttribute("task", optTask.get());
        return "employee/accounter/deliveryTasks/createTTN";
    }

    @PostMapping("/createTTN/{taskId}")
    public String createTTN(@PathVariable Long taskId,
                            @RequestParam(required = false) String cargoDescription,
                            @RequestParam(required = false) Double totalWeight,
                            @RequestParam(required = false) Double totalVolume,
                            @RequestParam(required = false) String comment,
                            RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.createTTN(taskId, cargoDescription, totalWeight, totalVolume, comment)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при оформлении ТТН.");
            return "redirect:/employee/accounter/deliveryTasks/createTTN/" + taskId;
        }

        redirectAttributes.addFlashAttribute("successMessage", "ТТН успешно оформлена.");
        return "redirect:/employee/accounter/deliveryTasks/detailsDeliveryTask/" + taskId;
    }
}
