package com.mai.siarsp.controllers.employee.manager;

import java.nio.charset.StandardCharsets;
import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.mapper.ClientOrderMapper;
import com.mai.siarsp.models.*;
import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.repo.ClientRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.RequestForDeliveryRepository;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import com.mai.siarsp.service.general.AcceptanceActDocumentService;
import com.mai.siarsp.service.general.ContractService;
import com.mai.siarsp.service.general.ReportDocumentService;
import com.mai.siarsp.service.general.TTNDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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
    private final DeliveryTaskService deliveryTaskService;

    public ClientOrderController(ClientOrderService clientOrderService,
                                 ClientRepository clientRepository,
                                 ProductRepository productRepository,
                                 RequestForDeliveryRepository requestForDeliveryRepository,
                                 DeliveryTaskService deliveryTaskService) {
        this.clientOrderService = clientOrderService;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.requestForDeliveryRepository = requestForDeliveryRepository;
        this.deliveryTaskService = deliveryTaskService;
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
                                    @RequestParam(value = "originalPrice", required = false) List<BigDecimal> originalPrices,
                                    @RequestParam(value = "discountPercent", required = false) List<Integer> discountPercents,
                                    @RequestParam("contractFile") MultipartFile contractFile,
                                    @AuthenticationPrincipal Employee currentEmployee,
                                    RedirectAttributes redirectAttributes) {
        List<ClientOrderService.OrderItemRequest> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            BigDecimal origPrice = (originalPrices != null && i < originalPrices.size()) ? originalPrices.get(i) : null;
            Integer discount = (discountPercents != null && i < discountPercents.size()) ? discountPercents.get(i) : null;
            items.add(new ClientOrderService.OrderItemRequest(
                    productIds.get(i), quantities.get(i), prices.get(i), origPrice, discount));
        }

        java.time.LocalDate date = java.time.LocalDate.parse(deliveryDate);

        if (!clientOrderService.createOrder(clientId, date, comment, items, currentEmployee, contractFile)) {
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
            item.put("originalPrice", op.getOriginalPrice());
            item.put("discountPercent", op.getDiscountPercent());
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
                                  @RequestParam(value = "originalPrice", required = false) List<BigDecimal> originalPrices,
                                  @RequestParam(value = "discountPercent", required = false) List<Integer> discountPercents,
                                  @RequestParam(value = "contractFile", required = false) MultipartFile contractFile,
                                  RedirectAttributes redirectAttributes) {
        List<ClientOrderService.OrderItemRequest> items = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            BigDecimal origPrice = (originalPrices != null && i < originalPrices.size()) ? originalPrices.get(i) : null;
            Integer discount = (discountPercents != null && i < discountPercents.size()) ? discountPercents.get(i) : null;
            items.add(new ClientOrderService.OrderItemRequest(
                    productIds.get(i), quantities.get(i), prices.get(i), origPrice, discount));
        }

        java.time.LocalDate date = java.time.LocalDate.parse(deliveryDate);

        if (!clientOrderService.updateOrder(id, date, comment, items, contractFile)) {
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

    // ========== СКАЧИВАНИЕ КОНТРАКТА ==========

    @GetMapping("/downloadContract/{orderId}")
    public ResponseEntity<Resource> downloadContract(@PathVariable Long orderId) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty() || optOrder.get().getContractFile() == null) {
            return ResponseEntity.notFound().build();
        }
        try {
            String contractFileName = optOrder.get().getContractFile();
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
    @GetMapping("/detailsTTN/{orderId}")
    public String detailsTTN(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty() || optOrder.get().getDeliveryTask() == null
                || optOrder.get().getDeliveryTask().getTtn() == null) {
            return "redirect:/employee/manager/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("ttn", optOrder.get().getDeliveryTask().getTtn());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/manager/clientOrders/detailsClientOrder/" + orderId);
        model.addAttribute("downloadUrl", "/employee/manager/clientOrders/downloadTTN/" + orderId);
        return "employee/warehouseManager/documents/detailsTTN";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsAcceptanceAct/{orderId}")
    public String detailsAcceptanceAct(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/manager/clientOrders/allClientOrders";
        }
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(orderId);
        if (optAct.isEmpty()) {
            return "redirect:/employee/manager/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("act", optAct.get());
        model.addAttribute("canEdit", false);
        model.addAttribute("backUrl", "/employee/manager/clientOrders/detailsClientOrder/" + orderId);
        model.addAttribute("downloadUrl", "/employee/manager/clientOrders/downloadAcceptanceAct/" + orderId);
        return "employee/warehouseManager/documents/detailsAcceptanceAct";
    }

    // ========== СКАЧИВАНИЕ ДОКУМЕНТОВ ==========

    @Transactional(readOnly = true)
    @GetMapping("/downloadTTN/{orderId}")
    public ResponseEntity<byte[]> downloadTTN(@PathVariable Long orderId) throws IOException {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty() || optOrder.get().getDeliveryTask() == null
                || optOrder.get().getDeliveryTask().getTtn() == null) {
            return ResponseEntity.notFound().build();
        }
        ReportDocumentService.ReportFile file = TTNDocumentService.generateDocument(
                optOrder.get().getDeliveryTask().getTtn());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }

    @Transactional(readOnly = true)
    @GetMapping("/downloadAcceptanceAct/{orderId}")
    public ResponseEntity<byte[]> downloadAcceptanceAct(@PathVariable Long orderId) throws IOException {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(orderId);
        if (optAct.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        ReportDocumentService.ReportFile file = AcceptanceActDocumentService.generateDocument(optAct.get());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
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
