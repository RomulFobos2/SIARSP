package com.mai.siarsp.controllers.employee.warehouseWorker;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.service.employee.ClientOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер заказов клиентов для складского работника
 *
 * Складской работник выполняет сборку заказов:
 * - Просмотр заказов, ожидающих сборки (RESERVED, IN_PROGRESS)
 * - Начало сборки (RESERVED → IN_PROGRESS)
 * - Завершение сборки (IN_PROGRESS → READY)
 */
@Controller("warehouseWorkerClientOrderController")
@RequestMapping("/employee/warehouseWorker/clientOrders")
@Slf4j
public class ClientOrderController {

    private static final List<ClientOrderStatus> WORKER_STATUSES = Arrays.asList(
            ClientOrderStatus.RESERVED,
            ClientOrderStatus.IN_PROGRESS
    );

    private final ClientOrderService clientOrderService;

    public ClientOrderController(ClientOrderService clientOrderService) {
        this.clientOrderService = clientOrderService;
    }

    @GetMapping("/allClientOrders")
    public String allClientOrders(@RequestParam(required = false) String search,
                                  @RequestParam(required = false, defaultValue = "all") String status,
                                  Model model) {
        List<ClientOrderDTO> orders;
        switch (status) {
            case "reserved" -> orders = clientOrderService.getOrdersByStatus(ClientOrderStatus.RESERVED);
            case "in_progress" -> orders = clientOrderService.getOrdersByStatus(ClientOrderStatus.IN_PROGRESS);
            default -> {
                orders = clientOrderService.getOrdersByStatuses(WORKER_STATUSES);
                status = "all";
            }
        }

        // Фильтрация по номеру заказа
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            orders = orders.stream()
                    .filter(o -> o.getOrderNumber() != null
                            && o.getOrderNumber().toLowerCase().contains(searchLower))
                    .toList();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("currentStatus", status);
        return "employee/warehouseWorker/clientOrders/allClientOrders";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsClientOrder/{id}")
    public String detailsClientOrder(@PathVariable Long id, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/warehouseWorker/clientOrders/allClientOrders";
        }

        model.addAttribute("order", optOrder.get());
        return "employee/warehouseWorker/clientOrders/detailsClientOrder";
    }

    @PostMapping("/startAssembly/{id}")
    public String startAssembly(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!clientOrderService.startAssembly(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при начале сборки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Сборка заказа начата.");
        }
        return "redirect:/employee/warehouseWorker/clientOrders/detailsClientOrder/" + id;
    }

    @PostMapping("/completeAssembly/{id}")
    public String completeAssembly(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!clientOrderService.completeAssembly(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при завершении сборки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Сборка завершена. Заказ готов к отгрузке.");
        }
        return "redirect:/employee/warehouseWorker/clientOrders/detailsClientOrder/" + id;
    }
}
