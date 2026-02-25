package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.service.employee.ClientOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller("accounterClientOrderController")
@RequestMapping("/employee/accounter/clientOrders")
@Slf4j
public class ClientOrderController {

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
                orders = clientOrderService.getAllOrders();
            }
        } else {
            orders = clientOrderService.getAllOrders();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("statuses", ClientOrderStatus.values());
        model.addAttribute("currentFilter", statusFilter);
        return "employee/accounter/clientOrders/allClientOrders";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsClientOrder/{id}")
    public String detailsClientOrder(@PathVariable Long id, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/accounter/clientOrders/allClientOrders";
        }

        ClientOrder order = optOrder.get();
        model.addAttribute("order", order);
        return "employee/accounter/clientOrders/detailsClientOrder";
    }
}
