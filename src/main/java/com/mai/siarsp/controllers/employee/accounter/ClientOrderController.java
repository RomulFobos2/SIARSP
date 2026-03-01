package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller("accounterClientOrderController")
@RequestMapping("/employee/accounter/clientOrders")
@Slf4j
public class ClientOrderController {

    private final ClientOrderService clientOrderService;
    private final DeliveryTaskService deliveryTaskService;

    public ClientOrderController(ClientOrderService clientOrderService,
                                 DeliveryTaskService deliveryTaskService) {
        this.clientOrderService = clientOrderService;
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/allClientOrders")
    public String allClientOrders(Model model) {
        model.addAttribute("orders", clientOrderService.getAllOrders());
        model.addAttribute("statuses", ClientOrderStatus.values());
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

    // ========== ПРОСМОТР ДОКУМЕНТОВ (read-only) ==========

    @Transactional(readOnly = true)
    @GetMapping("/detailsTTN/{orderId}")
    public String detailsTTN(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty() || optOrder.get().getDeliveryTask() == null
                || optOrder.get().getDeliveryTask().getTtn() == null) {
            return "redirect:/employee/accounter/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("ttn", optOrder.get().getDeliveryTask().getTtn());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/accounter/clientOrders/detailsClientOrder/" + orderId);
        return "employee/warehouseManager/documents/detailsTTN";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsAcceptanceAct/{orderId}")
    public String detailsAcceptanceAct(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/accounter/clientOrders/allClientOrders";
        }
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(orderId);
        if (optAct.isEmpty()) {
            return "redirect:/employee/accounter/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("act", optAct.get());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/accounter/clientOrders/detailsClientOrder/" + orderId);
        return "employee/warehouseManager/documents/detailsAcceptanceAct";
    }
}
