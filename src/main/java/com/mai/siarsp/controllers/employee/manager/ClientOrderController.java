package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.mapper.ClientOrderMapper;
import com.mai.siarsp.models.Client;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.repo.ClientRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.RequestForDeliveryRepository;
import com.mai.siarsp.service.employee.ClientOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller("managerClientOrderController")
@RequestMapping("/employee/manager/clientOrders")
@Slf4j
public class ClientOrderController {

    private final ClientOrderService clientOrderService;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final RequestForDeliveryRepository requestForDeliveryRepository;

    public ClientOrderController(ClientOrderService clientOrderService,
                                 ClientRepository clientRepository,
                                 ProductRepository productRepository,
                                 RequestForDeliveryRepository requestForDeliveryRepository) {
        this.clientOrderService = clientOrderService;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.requestForDeliveryRepository = requestForDeliveryRepository;
    }

    @Transactional(readOnly = true)
    @GetMapping("/allClientOrders")
    public String allClientOrders(Model model) {
        model.addAttribute("orders", clientOrderService.getAllOrders());
        model.addAttribute("statuses", ClientOrderStatus.values());
        return "employee/manager/clientOrders/allClientOrders";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsClientOrder/{id}")
    public String detailsClientOrder(@PathVariable Long id, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/manager/clientOrders/allClientOrders";
        }

        ClientOrder order = optOrder.get();
        ClientOrderDTO orderDTO = ClientOrderMapper.INSTANCE.toDTO(order);
        model.addAttribute("order", order);
        model.addAttribute("orderDTO", orderDTO);
        return "employee/manager/clientOrders/detailsClientOrder";
    }

    @Transactional(readOnly = true)
    @GetMapping("/createClientOrder")
    public String createClientOrderPage(Model model) {
        List<Client> clients = clientRepository.findAll().stream()
                .sorted(Comparator.comparing(Client::getOrganizationName))
                .collect(Collectors.toList());

        List<Map<String, Object>> productsList = buildProductsList();

        model.addAttribute("clients", clients);
        model.addAttribute("productsList", productsList);
        return "employee/manager/clientOrders/createClientOrder";
    }

    @PostMapping("/createClientOrder")
    public String createClientOrder(@RequestParam Long clientId,
                                    @RequestParam String deliveryDate,
                                    @RequestParam(required = false) String comment,
                                    @RequestParam("productId") List<Long> productIds,
                                    @RequestParam("quantity") List<Integer> quantities,
                                    @RequestParam("price") List<BigDecimal> prices,
                                    @AuthenticationPrincipal Employee currentEmployee,
                                    RedirectAttributes redirectAttributes) {
        List<ClientOrderService.OrderItemRequest> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            items.add(new ClientOrderService.OrderItemRequest(
                    productIds.get(i), quantities.get(i), prices.get(i)));
        }

        java.time.LocalDate date = java.time.LocalDate.parse(deliveryDate);

        if (!clientOrderService.createOrder(clientId, date, comment, items, currentEmployee)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при создании заказа.");
            return "redirect:/employee/manager/clientOrders/createClientOrder";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Заказ успешно создан.");
        return "redirect:/employee/manager/clientOrders/allClientOrders";
    }

    @Transactional(readOnly = true)
    @GetMapping("/editClientOrder/{id}")
    public String editClientOrderPage(@PathVariable Long id, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/manager/clientOrders/allClientOrders";
        }

        ClientOrder order = optOrder.get();
        if (order.getStatus() != ClientOrderStatus.NEW) {
            return "redirect:/employee/manager/clientOrders/detailsClientOrder/" + id;
        }

        List<Map<String, Object>> productsList = buildProductsList();

        // Подготовить данные о существующих позициях для JS
        List<Map<String, Object>> orderProducts = new ArrayList<>();
        for (var op : order.getOrderedProducts()) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", op.getProduct().getId());
            item.put("productName", op.getProduct().getName());
            item.put("quantity", op.getQuantity());
            item.put("price", op.getPrice());
            orderProducts.add(item);
        }

        model.addAttribute("order", order);
        model.addAttribute("productsList", productsList);
        model.addAttribute("orderProducts", orderProducts);
        return "employee/manager/clientOrders/editClientOrder";
    }

    @PostMapping("/editClientOrder/{id}")
    public String editClientOrder(@PathVariable Long id,
                                  @RequestParam String deliveryDate,
                                  @RequestParam(required = false) String comment,
                                  @RequestParam("productId") List<Long> productIds,
                                  @RequestParam("quantity") List<Integer> quantities,
                                  @RequestParam("price") List<BigDecimal> prices,
                                  RedirectAttributes redirectAttributes) {
        List<ClientOrderService.OrderItemRequest> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            items.add(new ClientOrderService.OrderItemRequest(
                    productIds.get(i), quantities.get(i), prices.get(i)));
        }

        java.time.LocalDate date = java.time.LocalDate.parse(deliveryDate);

        if (!clientOrderService.updateOrder(id, date, comment, items)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении заказа.");
            return "redirect:/employee/manager/clientOrders/editClientOrder/" + id;
        }

        redirectAttributes.addFlashAttribute("successMessage", "Заказ обновлён.");
        return "redirect:/employee/manager/clientOrders/detailsClientOrder/" + id;
    }

    @PostMapping("/confirmClientOrder/{id}")
    public String confirmClientOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!clientOrderService.confirmOrder(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при подтверждении заказа.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Заказ подтверждён и отправлен на склад.");
        }
        return "redirect:/employee/manager/clientOrders/detailsClientOrder/" + id;
    }

    @PostMapping("/cancelClientOrder/{id}")
    public String cancelClientOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!clientOrderService.cancelOrder(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при отмене заказа.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Заказ отменён.");
        }
        return "redirect:/employee/manager/clientOrders/detailsClientOrder/" + id;
    }

    /**
     * Формирует список товаров для JS в форме создания/редактирования
     * Включает среднее время поставки по каждому товару (на основе завершённых заявок)
     */
    private List<Map<String, Object>> buildProductsList() {
        List<Product> products = productRepository.findAll();

        // Среднее время поставки по товарам
        Map<Long, Double> avgDeliveryMap = new HashMap<>();
        try {
            List<Object[]> avgData = requestForDeliveryRepository.findAverageDeliveryDaysByProduct();
            for (Object[] row : avgData) {
                Long productId = ((Number) row[0]).longValue();
                Double avgDays = row[1] != null ? ((Number) row[1]).doubleValue() : null;
                if (avgDays != null) {
                    avgDeliveryMap.put(productId, avgDays);
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось загрузить среднее время поставки: {}", e.getMessage());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("article", p.getArticle());
            map.put("availableQuantity", p.getAvailableQuantity());
            map.put("stockQuantity", p.getStockQuantity());
            map.put("avgDeliveryDays", avgDeliveryMap.getOrDefault(p.getId(), 0.0));
            result.add(map);
        }
        result.sort(Comparator.comparing(m -> (String) m.get("name")));
        return result;
    }
}
