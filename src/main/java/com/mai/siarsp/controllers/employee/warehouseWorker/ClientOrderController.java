package com.mai.siarsp.controllers.employee.warehouseWorker;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.OrderedProduct;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.OrderedProductRepository;
import com.mai.siarsp.repo.SupplyRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.general.ContractService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;

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
            ClientOrderStatus.IN_PROGRESS,
            ClientOrderStatus.READY
    );

    private final ClientOrderService clientOrderService;
    private final ZoneProductRepository zoneProductRepository;
    private final OrderedProductRepository orderedProductRepository;
    private final SupplyRepository supplyRepository;

    public ClientOrderController(ClientOrderService clientOrderService,
                                 ZoneProductRepository zoneProductRepository,
                                 OrderedProductRepository orderedProductRepository,
                                 SupplyRepository supplyRepository) {
        this.clientOrderService = clientOrderService;
        this.zoneProductRepository = zoneProductRepository;
        this.orderedProductRepository = orderedProductRepository;
        this.supplyRepository = supplyRepository;
    }

    @GetMapping("/allClientOrders")
    public String allClientOrders(@RequestParam(required = false) String search,
                                  @RequestParam(required = false, defaultValue = "all") String status,
                                  @RequestParam(required = false) String dateFrom,
                                  @RequestParam(required = false) String dateTo,
                                  Model model) {
        List<ClientOrderDTO> orders;
        switch (status) {
            case "reserved" -> orders = clientOrderService.getOrdersByStatus(ClientOrderStatus.RESERVED);
            case "in_progress" -> orders = clientOrderService.getOrdersByStatus(ClientOrderStatus.IN_PROGRESS);
            case "ready" -> orders = clientOrderService.getOrdersByStatus(ClientOrderStatus.READY);
            default -> {
                orders = clientOrderService.getOrdersByStatuses(WORKER_STATUSES);
                status = "all";
            }
        }

        // Фильтрация по номеру заказа или имени клиента
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase();
            orders = orders.stream()
                    .filter(o -> (o.getOrderNumber() != null
                            && o.getOrderNumber().toLowerCase().contains(searchLower))
                            || (o.getClientOrganizationName() != null
                            && o.getClientOrganizationName().toLowerCase().contains(searchLower)))
                    .toList();
        }

        // Фильтрация по дате доставки
        if (dateFrom != null && !dateFrom.isBlank()) {
            LocalDate from = LocalDate.parse(dateFrom);
            orders = orders.stream()
                    .filter(o -> o.getDeliveryDate() != null && !o.getDeliveryDate().isBefore(from))
                    .toList();
        }
        if (dateTo != null && !dateTo.isBlank()) {
            LocalDate to = LocalDate.parse(dateTo);
            orders = orders.stream()
                    .filter(o -> o.getDeliveryDate() != null && !o.getDeliveryDate().isAfter(to))
                    .toList();
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentSearch", search != null ? search : "");
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentDateFrom", dateFrom != null ? dateFrom : "");
        model.addAttribute("currentDateTo", dateTo != null ? dateTo : "");
        return "employee/warehouseWorker/clientOrders/allClientOrders";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsClientOrder/{id}")
    public String detailsClientOrder(@PathVariable Long id, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(id);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/warehouseWorker/clientOrders/allClientOrders";
        }

        ClientOrder order = optOrder.get();
        model.addAttribute("order", order);

        // Местоположение товаров на складе: productId → List<ZoneProduct>
        Map<Long, List<ZoneProduct>> productLocations = new HashMap<>();
        for (OrderedProduct op : order.getOrderedProducts()) {
            List<ZoneProduct> locations = zoneProductRepository.findByProduct(op.getProduct());
            productLocations.put(op.getProduct().getId(), locations);
            // Принудительная инициализация LAZY-связей для рендера в Thymeleaf
            op.getPicks().forEach(p -> {
                p.getSupply().getExpirationDate();
                p.getZone().getShelf().getWarehouse().getName();
            });
        }
        model.addAttribute("productLocations", productLocations);

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

    // ========== ПИК-ЛИНИИ ==========

    @PostMapping("/addPick")
    public String addPick(@RequestParam Long orderId,
                          @RequestParam Long orderedProductId,
                          @RequestParam Long supplyId,
                          @RequestParam Long zoneId,
                          @RequestParam int quantity,
                          RedirectAttributes redirectAttributes) {
        String error = clientOrderService.addPick(orderedProductId, supplyId, zoneId, quantity);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Пик добавлен");
        }
        return "redirect:/employee/warehouseWorker/clientOrders/detailsClientOrder/" + orderId;
    }

    @PostMapping("/removePick")
    public String removePick(@RequestParam Long orderId,
                             @RequestParam Long pickId,
                             RedirectAttributes redirectAttributes) {
        String error = clientOrderService.removePick(pickId);
        if (error != null) {
            redirectAttributes.addFlashAttribute("errorMessage", error);
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Пик удалён");
        }
        return "redirect:/employee/warehouseWorker/clientOrders/detailsClientOrder/" + orderId;
    }

    @Transactional(readOnly = true)
    @GetMapping("/eligible-supplies/{orderedProductId}")
    @ResponseBody
    public List<Map<String, Object>> eligibleSupplies(@PathVariable Long orderedProductId) {
        Optional<OrderedProduct> optOp = orderedProductRepository.findById(orderedProductId);
        if (optOp.isEmpty()) return List.of();
        OrderedProduct op = optOp.get();
        LocalDate deliveryDate = op.getClientOrder().getDeliveryDate();
        LocalDate referenceDate = deliveryDate != null ? deliveryDate : LocalDate.now();
        return supplyRepository.findEligibleByProductAndDate(op.getProduct().getId(), referenceDate)
                .stream()
                .map(s -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", s.getId());
                    m.put("productionDate", s.getProductionDate());
                    m.put("expirationDate", s.getExpirationDate());
                    int total = zoneProductRepository.findBySupplyId(s.getId())
                            .stream().mapToInt(ZoneProduct::getQuantity).sum();
                    m.put("availableTotal", total);
                    return m;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    @GetMapping("/supply-zones-with-quantity/{supplyId}")
    @ResponseBody
    public List<Map<String, Object>> supplyZones(@PathVariable Long supplyId) {
        return zoneProductRepository.findBySupplyId(supplyId).stream()
                .map(zp -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("zoneId", zp.getZone().getId());
                    m.put("label", zp.getZone().getLabel());
                    m.put("shelfCode", zp.getZone().getShelf().getCode());
                    m.put("warehouseName", zp.getZone().getShelf().getWarehouse().getName());
                    m.put("quantity", zp.getQuantity());
                    return m;
                })
                .toList();
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
            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment()
                    .filename(downloadName, StandardCharsets.UTF_8)
                    .build());
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (IOException e) {
            log.error("Ошибка скачивания контракта: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
