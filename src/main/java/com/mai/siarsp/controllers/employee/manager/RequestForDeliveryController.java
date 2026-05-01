package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.service.employee.ClientOrderService;
import com.mai.siarsp.service.employee.manager.RequestForDeliveryService;
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

@Controller("managerRequestForDeliveryController")
@Slf4j
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestForDeliveryService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final ClientOrderService clientOrderService;

    public RequestForDeliveryController(
            @Qualifier("managerRequestForDeliveryService") RequestForDeliveryService requestForDeliveryService,
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
    @GetMapping("/employee/manager/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(Model model) {
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        model.addAttribute("allProducts", productService.getAllProducts());
        model.addAttribute("allWarehouses", warehouseService.getAllWarehouses());
        model.addAttribute("deficitData", clientOrderService.getProductDeficit());
        model.addAttribute("warehouseCapacityData", warehouseService.getWarehouseCapacityData());
        model.addAttribute("productVolumeData", warehouseService.getProductVolumeData());
        return "employee/manager/requestsForDelivery/addRequestForDelivery";
    }

    @PostMapping("/employee/manager/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(@RequestParam Long supplierId,
                                        @RequestParam Long warehouseId,
                                        @RequestParam BigDecimal deliveryCost,
                                        @RequestParam List<Long> productIds,
                                        @RequestParam List<Integer> quantities,
                                        @RequestParam List<BigDecimal> purchasePrices,
                                        @RequestParam(required = false) List<String> units,
                                        RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.createApprovedRequest(
                supplierId, warehouseId, deliveryCost, productIds, quantities, purchasePrices, units)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при создании заявки.");
            return "redirect:/employee/manager/requestsForDelivery/addRequestForDelivery";
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка создана и автоматически согласована.");
        return "redirect:/employee/manager/requestsForDelivery/allRequestsForDelivery";
    }

    @Transactional
    @GetMapping("/employee/manager/requestsForDelivery/allRequestsForDelivery")
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
        return "employee/manager/requestsForDelivery/allRequestsForDelivery";
    }

    @Transactional
    @GetMapping("/employee/manager/requestsForDelivery/detailsRequestForDelivery/{id}")
    public String detailsRequestForDelivery(@PathVariable(value = "id") long id, Model model) {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null) {
            return "redirect:/employee/manager/requestsForDelivery/allRequestsForDelivery";
        }

        RequestForDeliveryDTO dto = com.mai.siarsp.mapper.RequestForDeliveryMapper.INSTANCE.toDTO(request);
        model.addAttribute("requestDTO", dto);
        return "employee/manager/requestsForDelivery/detailsRequestForDelivery";
    }

    @PostMapping("/employee/manager/requestsForDelivery/approveRequestForDelivery/{id}")
    public String approveRequestForDelivery(@PathVariable(value = "id") long id,
                                             @RequestParam String commentText,
                                             RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.approveByDirector(id, commentText)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при согласовании заявки.");
            return "redirect:/employee/manager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка согласована и передана бухгалтеру.");
        return "redirect:/employee/manager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @PostMapping("/employee/manager/requestsForDelivery/rejectRequestForDelivery/{id}")
    public String rejectRequestForDelivery(@PathVariable(value = "id") long id,
                                            @RequestParam String commentText,
                                            RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.rejectByDirector(id, commentText)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при отклонении заявки.");
            return "redirect:/employee/manager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка отклонена.");
        return "redirect:/employee/manager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @Transactional
    @GetMapping("/employee/manager/requestsForDelivery/downloadContract/{id}")
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
