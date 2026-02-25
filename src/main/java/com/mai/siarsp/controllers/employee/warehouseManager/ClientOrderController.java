package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.mapper.ClientOrderMapper;
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

@Controller("warehouseManagerClientOrderController")
@RequestMapping("/employee/warehouseManager/clientOrders")
@Slf4j
public class ClientOrderController {

    private static final List<ClientOrderStatus> WAREHOUSE_STATUSES = Arrays.asList(
            ClientOrderStatus.CONFIRMED,
            ClientOrderStatus.RESERVED,
            ClientOrderStatus.IN_PROGRESS,
            ClientOrderStatus.READY
    );

    private final ClientOrderService clientOrderService;

    public ClientOrderController(ClientOrderService clientOrderService) {
        this.clientOrderService = clientOrderService;
    }

    @GetMapping("/allClientOrders")
    public String allClientOrders(@RequestParam(value = "statusFilter", required = false) String statusFilter,
                                  Model model) {
        List<ClientOrderDTO> orders;
        if (statusFilter != null && !statusFilter.isBlank()) {
            try {
                ClientOrderStatus status = ClientOrderStatus.valueOf(statusFilter);
                orders = clientOrderService.getOrdersByStatus(status);
            } catch (IllegalArgumentException e) {
                orders = clientOrderService.getOrdersByStatuses(WAREHOUSE_STATUSES);
            }
        } else {
            orders = clientOrderService.getOrdersByStatuses(WAREHOUSE_STATUSES);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("statuses", WAREHOUSE_STATUSES);
        model.addAttribute("currentFilter", statusFilter);
        return "employee/warehouseManager/clientOrders/allClientOrders";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsClientOrder/{id}")
    public String detailsClientOrder(@PathVariable Long id, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/warehouseManager/clientOrders/allClientOrders";
        }

        ClientOrder order = optOrder.get();
        model.addAttribute("order", order);
        return "employee/warehouseManager/clientOrders/detailsClientOrder";
    }

    @PostMapping("/reserveClientOrder/{id}")
    public String reserveProducts(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!clientOrderService.reserveProducts(id)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка резервирования. Недостаточно товара на складе.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Товар по заказу успешно зарезервирован.");
        }
        return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + id;
    }

    @PostMapping("/startAssembly/{id}")
    public String startAssembly(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!clientOrderService.startAssembly(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при начале сборки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Сборка заказа начата.");
        }
        return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + id;
    }

    @PostMapping("/completeAssembly/{id}")
    public String completeAssembly(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!clientOrderService.completeAssembly(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при завершении сборки.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Сборка завершена. Заказ готов к отгрузке.");
        }
        return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + id;
    }
}
