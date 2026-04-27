package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.mapper.ClientOrderMapper;
import com.mai.siarsp.models.AcceptanceAct;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.TTN;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.DeliveryTaskService;
import com.mai.siarsp.service.general.AcceptanceActDocumentService;
import com.mai.siarsp.service.general.ReportDocumentService;
import com.mai.siarsp.service.general.ContractService;
import com.mai.siarsp.service.general.TTNDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
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
            ClientOrderStatus.READY,
            ClientOrderStatus.SHIPPED,
            ClientOrderStatus.DELIVERED
    );

    private final ClientOrderService clientOrderService;
    private final DeliveryTaskService deliveryTaskService;

    public ClientOrderController(ClientOrderService clientOrderService,
                                 DeliveryTaskService deliveryTaskService) {
        this.clientOrderService = clientOrderService;
        this.deliveryTaskService = deliveryTaskService;
    }

    @GetMapping("/allClientOrders")
    public String allClientOrders(Model model) {
        model.addAttribute("orders", clientOrderService.getOrdersByStatuses(WAREHOUSE_STATUSES));
        model.addAttribute("statuses", WAREHOUSE_STATUSES);
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

    // ========== ДОКУМЕНТЫ (ТТН + AcceptanceAct) ==========

    @PostMapping("/createDocuments/{id}")
    public String createDocuments(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.createDocumentsForOrder(id)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при создании документов.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Документы (ТТН и акт) подготовлены.");
        }
        return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + id;
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsTTN/{orderId}")
    public String detailsTTN(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty() || optOrder.get().getDeliveryTask() == null
                || optOrder.get().getDeliveryTask().getTtn() == null) {
            return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("ttn", optOrder.get().getDeliveryTask().getTtn());
        model.addAttribute("canEdit", true);
        model.addAttribute("backUrl", "/employee/warehouseManager/clientOrders/detailsClientOrder/" + orderId);
        model.addAttribute("editUrl", "/employee/warehouseManager/clientOrders/editTTN/" + orderId);
        model.addAttribute("downloadUrl", "/employee/warehouseManager/clientOrders/downloadTTN/" + orderId);
        return "employee/warehouseManager/documents/detailsTTN";
    }

    @Transactional(readOnly = true)
    @GetMapping("/editTTN/{orderId}")
    public String editTTN(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty() || optOrder.get().getDeliveryTask() == null
                || optOrder.get().getDeliveryTask().getTtn() == null) {
            return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("ttn", optOrder.get().getDeliveryTask().getTtn());
        return "employee/warehouseManager/documents/editTTN";
    }

    @PostMapping("/saveTTN/{orderId}")
    public String saveTTN(@PathVariable Long orderId,
                          @RequestParam Long ttnId,
                          @RequestParam(required = false) String cargoDescription,
                          @RequestParam(required = false) Double totalWeight,
                          @RequestParam(required = false) Double totalVolume,
                          @RequestParam(required = false) String comment,
                          RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.updateTTN(ttnId, cargoDescription, totalWeight, totalVolume, comment)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при сохранении ТТН.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "ТТН сохранена.");
        }
        return "redirect:/employee/warehouseManager/clientOrders/detailsTTN/" + orderId;
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsAcceptanceAct/{orderId}")
    public String detailsAcceptanceAct(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/warehouseManager/clientOrders/allClientOrders";
        }
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(orderId);
        if (optAct.isEmpty()) {
            return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("act", optAct.get());
        model.addAttribute("canEdit", true);
        model.addAttribute("backUrl", "/employee/warehouseManager/clientOrders/detailsClientOrder/" + orderId);
        model.addAttribute("editUrl", "/employee/warehouseManager/clientOrders/editAcceptanceAct/" + orderId);
        model.addAttribute("downloadUrl", "/employee/warehouseManager/clientOrders/downloadAcceptanceAct/" + orderId);
        return "employee/warehouseManager/documents/detailsAcceptanceAct";
    }

    @Transactional(readOnly = true)
    @GetMapping("/editAcceptanceAct/{orderId}")
    public String editAcceptanceAct(@PathVariable Long orderId, Model model) {
        Optional<ClientOrder> optOrder = clientOrderService.getOrderById(orderId);
        if (optOrder.isEmpty()) {
            return "redirect:/employee/warehouseManager/clientOrders/allClientOrders";
        }
        Optional<AcceptanceAct> optAct = deliveryTaskService.getAcceptanceActByOrder(orderId);
        if (optAct.isEmpty()) {
            return "redirect:/employee/warehouseManager/clientOrders/detailsClientOrder/" + orderId;
        }
        model.addAttribute("order", optOrder.get());
        model.addAttribute("act", optAct.get());
        return "employee/warehouseManager/documents/editAcceptanceAct";
    }

    @PostMapping("/saveAcceptanceAct/{orderId}")
    public String saveAcceptanceAct(@PathVariable Long orderId,
                                    @RequestParam Long actId,
                                    @RequestParam(required = false) String clientRepresentative,
                                    @RequestParam(required = false, defaultValue = "false") boolean signed,
                                    @RequestParam(required = false) String comment,
                                    RedirectAttributes redirectAttributes) {
        if (!deliveryTaskService.updateAcceptanceAct(actId, clientRepresentative, signed, comment)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при сохранении акта.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Акт сохранён.");
        }
        return "redirect:/employee/warehouseManager/clientOrders/detailsAcceptanceAct/" + orderId;
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
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
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
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
