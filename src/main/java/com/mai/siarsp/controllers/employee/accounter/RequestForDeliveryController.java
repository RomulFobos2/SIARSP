package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.accounter.RequestForDeliveryService;
import com.mai.siarsp.service.employee.manager.SupplierService;
import com.mai.siarsp.service.employee.warehouseManager.ProductService;
import com.mai.siarsp.service.employee.warehouseManager.WarehouseService;
import com.mai.siarsp.service.general.ReportDocumentService;
import com.mai.siarsp.service.general.RequestForDeliveryDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Controller("accounterRequestForDeliveryController")
@Slf4j
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestForDeliveryService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final ClientOrderService clientOrderService;

    public RequestForDeliveryController(
            @Qualifier("accounterRequestForDeliveryService") RequestForDeliveryService requestForDeliveryService,
            SupplierService supplierService,
            @Qualifier("warehouseManagerProductService") ProductService productService,
            @Qualifier("warehouseManagerWarehouseService") WarehouseService warehouseService,
            ClientOrderService clientOrderService) {
        this.requestForDeliveryService = requestForDeliveryService;
        this.supplierService = supplierService;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.clientOrderService = clientOrderService;
    }

    @Transactional
    @GetMapping("/employee/accounter/requestsForDelivery/allRequestsForDelivery")
    public String allRequestsForDelivery(@RequestParam(value = "statusFilter", required = false) String statusFilter,
                                          Model model) {
        List<RequestForDeliveryDTO> requests;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            try {
                RequestStatus status = RequestStatus.valueOf(statusFilter);
                requests = requestForDeliveryService.getRequestsByStatus(status);
            } catch (IllegalArgumentException e) {
                requests = requestForDeliveryService.getAllRequests();
            }
        } else {
            requests = requestForDeliveryService.getAllRequests();
        }

        model.addAttribute("allRequests", requests);
        model.addAttribute("statuses", RequestStatus.values());
        model.addAttribute("currentFilter", statusFilter);
        return "employee/accounter/requestsForDelivery/allRequestsForDelivery";
    }

    @Transactional
    @GetMapping("/employee/accounter/requestsForDelivery/detailsRequestForDelivery/{id}")
    public String detailsRequestForDelivery(@PathVariable(value = "id") long id, Model model) {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null) {
            return "redirect:/employee/accounter/requestsForDelivery/allRequestsForDelivery";
        }

        RequestForDeliveryDTO dto = com.mai.siarsp.mapper.RequestForDeliveryMapper.INSTANCE.toDTO(request);
        model.addAttribute("requestDTO", dto);
        return "employee/accounter/requestsForDelivery/detailsRequestForDelivery";
    }

    @PostMapping("/employee/accounter/requestsForDelivery/approveRequestForDelivery/{id}")
    public String approveRequestForDelivery(@PathVariable(value = "id") long id,
                                             @RequestParam String commentText,
                                             RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.approveByAccountant(id, commentText)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при согласовании заявки.");
            return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка полностью согласована.");
        return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @PostMapping("/employee/accounter/requestsForDelivery/rejectRequestForDelivery/{id}")
    public String rejectRequestForDelivery(@PathVariable(value = "id") long id,
                                            @RequestParam String commentText,
                                            RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.rejectByAccountant(id, commentText)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при отклонении заявки.");
            return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка отклонена.");
        return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    // ========== СОЗДАНИЕ ==========

    @Transactional
    @GetMapping("/employee/accounter/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(Model model) {
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        model.addAttribute("allProducts", productService.getAllProducts());
        model.addAttribute("allWarehouses", warehouseService.getAllWarehouses());
        model.addAttribute("deficitData", clientOrderService.getProductDeficit());
        model.addAttribute("warehouseCapacityData", warehouseService.getWarehouseCapacityData());
        model.addAttribute("productVolumeData", warehouseService.getProductVolumeData());
        return "employee/accounter/requestsForDelivery/addRequestForDelivery";
    }

    @PostMapping("/employee/accounter/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(@RequestParam Long supplierId,
                                        @RequestParam Long warehouseId,
                                        @RequestParam BigDecimal deliveryCost,
                                        @RequestParam List<Long> productIds,
                                        @RequestParam List<Integer> quantities,
                                        @RequestParam List<BigDecimal> purchasePrices,
                                        @RequestParam(required = false) List<String> units,
                                        RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.createRequest(
                supplierId, warehouseId, deliveryCost, productIds, quantities, purchasePrices, units)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при создании заявки.");
            return "redirect:/employee/accounter/requestsForDelivery/addRequestForDelivery";
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка создана как черновик.");
        return "redirect:/employee/accounter/requestsForDelivery/allRequestsForDelivery";
    }

    // ========== РЕДАКТИРОВАНИЕ ==========

    @Transactional
    @GetMapping("/employee/accounter/requestsForDelivery/editRequestForDelivery/{id}")
    public String editRequestForDelivery(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null) {
            return "redirect:/employee/accounter/requestsForDelivery/allRequestsForDelivery";
        }
        if (request.getStatus() != RequestStatus.DRAFT && request.getStatus() != RequestStatus.REJECTED_BY_DIRECTOR) {
            redirectAttributes.addFlashAttribute("requestError", "Редактировать можно только черновик или отклонённую заявку.");
            return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
        }

        RequestForDeliveryDTO dto = com.mai.siarsp.mapper.RequestForDeliveryMapper.INSTANCE.toDTO(request);
        model.addAttribute("requestDTO", dto);
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        model.addAttribute("allProducts", productService.getAllProducts());
        model.addAttribute("allWarehouses", warehouseService.getAllWarehouses());
        model.addAttribute("warehouseCapacityData", warehouseService.getWarehouseCapacityData());
        model.addAttribute("productVolumeData", warehouseService.getProductVolumeData());
        return "employee/accounter/requestsForDelivery/editRequestForDelivery";
    }

    @PostMapping("/employee/accounter/requestsForDelivery/editRequestForDelivery/{id}")
    public String editRequestForDelivery(@PathVariable long id,
                                         @RequestParam Long supplierId,
                                         @RequestParam Long warehouseId,
                                         @RequestParam BigDecimal deliveryCost,
                                         @RequestParam List<Long> productIds,
                                         @RequestParam List<Integer> quantities,
                                         @RequestParam List<BigDecimal> purchasePrices,
                                         @RequestParam(required = false) List<String> units,
                                         RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.updateRequest(
                id, supplierId, warehouseId, deliveryCost, productIds, quantities, purchasePrices, units)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при сохранении заявки.");
            return "redirect:/employee/accounter/requestsForDelivery/editRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка обновлена.");
        return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    // ========== УДАЛЕНИЕ ==========

    @GetMapping("/employee/accounter/requestsForDelivery/deleteRequestForDelivery/{id}")
    public String deleteRequestForDelivery(@PathVariable long id, RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.deleteRequest(id)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при удалении заявки. Удалять можно только черновики.");
            return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка удалена.");
        return "redirect:/employee/accounter/requestsForDelivery/allRequestsForDelivery";
    }

    // ========== ОТПРАВКА ДИРЕКТОРУ ==========

    @PostMapping("/employee/accounter/requestsForDelivery/submitToDirector/{id}")
    public String submitToDirector(@PathVariable long id, RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.submitToDirector(id)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при отправке заявки директору.");
            return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка отправлена директору на согласование.");
        return "redirect:/employee/accounter/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @Transactional
    @GetMapping("/employee/accounter/requestsForDelivery/downloadContract/{id}")
    public ResponseEntity<byte[]> downloadContract(@PathVariable(value = "id") long id) throws IOException {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null ||
                (request.getStatus() != RequestStatus.APPROVED &&
                        request.getStatus() != RequestStatus.PARTIALLY_RECEIVED &&
                        request.getStatus() != RequestStatus.RECEIVED)) {
            return ResponseEntity.notFound().build();
        }
        ReportDocumentService.ReportFile file = RequestForDeliveryDocumentService.generateContract(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName()).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
