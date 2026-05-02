package com.mai.siarsp.controllers.employee.admin;

import java.nio.charset.StandardCharsets;
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

/**
 * Контроллер заявок на закупку товара для администратора (ADMIN).
 *
 * Администратор имеет полный доступ: просмотр всех заявок, создание черновика (DRAFT),
 * отправка директору (DRAFT/REJECTED_BY_DIRECTOR → PENDING_DIRECTOR),
 * согласование/отклонение (PENDING_DIRECTOR → APPROVED/REJECTED_BY_DIRECTOR),
 * редактирование/удаление, скачивание договора.
 */
@Controller("adminRequestForDeliveryController")
@Slf4j
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestForDeliveryService;
    private final com.mai.siarsp.service.employee.warehouseManager.RequestForDeliveryService warehouseManagerRequestService;
    private final com.mai.siarsp.service.employee.accounter.RequestForDeliveryService accounterRequestService;
    private final SupplierService supplierService;
    private final ProductService productService;
    private final WarehouseService warehouseService;
    private final ClientOrderService clientOrderService;

    public RequestForDeliveryController(
            @Qualifier("managerRequestForDeliveryService") RequestForDeliveryService requestForDeliveryService,
            @Qualifier("warehouseManagerRequestForDeliveryService") com.mai.siarsp.service.employee.warehouseManager.RequestForDeliveryService warehouseManagerRequestService,
            @Qualifier("accounterRequestForDeliveryService") com.mai.siarsp.service.employee.accounter.RequestForDeliveryService accounterRequestService,
            SupplierService supplierService,
            @Qualifier("warehouseManagerProductService") ProductService productService,
            @Qualifier("warehouseManagerWarehouseService") WarehouseService warehouseService,
            ClientOrderService clientOrderService) {
        this.requestForDeliveryService = requestForDeliveryService;
        this.warehouseManagerRequestService = warehouseManagerRequestService;
        this.accounterRequestService = accounterRequestService;
        this.supplierService = supplierService;
        this.productService = productService;
        this.warehouseService = warehouseService;
        this.clientOrderService = clientOrderService;
    }

    @Transactional
    @GetMapping("/employee/admin/requestsForDelivery/allRequestsForDelivery")
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
        return "employee/admin/requestsForDelivery/allRequestsForDelivery";
    }

    @Transactional
    @GetMapping("/employee/admin/requestsForDelivery/detailsRequestForDelivery/{id}")
    public String detailsRequestForDelivery(@PathVariable(value = "id") long id, Model model) {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null) {
            return "redirect:/employee/admin/requestsForDelivery/allRequestsForDelivery";
        }

        RequestForDeliveryDTO dto = com.mai.siarsp.mapper.RequestForDeliveryMapper.INSTANCE.toDTO(request);
        model.addAttribute("requestDTO", dto);
        return "employee/admin/requestsForDelivery/detailsRequestForDelivery";
    }

    @Transactional
    @GetMapping("/employee/admin/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(Model model) {
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        model.addAttribute("allProducts", productService.getAllProducts());
        model.addAttribute("allWarehouses", warehouseService.getAllWarehouses());
        model.addAttribute("deficitData", clientOrderService.getProductDeficit());
        model.addAttribute("warehouseCapacityData", warehouseService.getWarehouseCapacityData());
        model.addAttribute("productVolumeData", warehouseService.getProductVolumeData());
        return "employee/admin/requestsForDelivery/addRequestForDelivery";
    }

    @PostMapping("/employee/admin/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(@RequestParam Long supplierId,
                                        @RequestParam Long warehouseId,
                                        @RequestParam BigDecimal deliveryCost,
                                        @RequestParam List<Long> productIds,
                                        @RequestParam List<Integer> quantities,
                                        @RequestParam List<BigDecimal> purchasePrices,
                                        @RequestParam(required = false) List<String> units,
                                        RedirectAttributes redirectAttributes) {
        if (!warehouseManagerRequestService.createRequest(
                supplierId, warehouseId, deliveryCost, productIds, quantities, purchasePrices, units)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при создании заявки.");
            return "redirect:/employee/admin/requestsForDelivery/addRequestForDelivery";
        }
        redirectAttributes.addFlashAttribute("requestSuccess",
                "Заявка создана как черновик. Отправьте её директору на согласование, когда будет готова.");
        return "redirect:/employee/admin/requestsForDelivery/allRequestsForDelivery";
    }

    @PostMapping("/employee/admin/requestsForDelivery/submitToDirector/{id}")
    public String submitToDirector(@PathVariable("id") long id, RedirectAttributes redirectAttributes) {
        if (!accounterRequestService.submitToDirector(id)) {
            redirectAttributes.addFlashAttribute("requestError",
                    "Не удалось отправить заявку директору. Проверьте, что она в статусе «Черновик» или «Отклонена директором».");
            return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка отправлена директору на согласование.");
        return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @Transactional
    @GetMapping("/employee/admin/requestsForDelivery/editRequestForDelivery/{id}")
    public String editRequestForDelivery(@PathVariable("id") long id, Model model,
                                          RedirectAttributes redirectAttributes) {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null) {
            redirectAttributes.addFlashAttribute("requestError", "Заявка не найдена.");
            return "redirect:/employee/admin/requestsForDelivery/allRequestsForDelivery";
        }
        if (request.getStatus() == RequestStatus.PARTIALLY_RECEIVED
                || request.getStatus() == RequestStatus.RECEIVED) {
            redirectAttributes.addFlashAttribute("requestError",
                    "Редактирование запрещено — заявка уже принята (" + request.getStatus().getDisplayName() + ").");
            return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
        }

        RequestForDeliveryDTO dto = com.mai.siarsp.mapper.RequestForDeliveryMapper.INSTANCE.toDTO(request);
        model.addAttribute("requestDTO", dto);
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        model.addAttribute("allProducts", productService.getAllProducts());
        model.addAttribute("allWarehouses", warehouseService.getAllWarehouses());
        model.addAttribute("warehouseCapacityData", warehouseService.getWarehouseCapacityData());
        model.addAttribute("productVolumeData", warehouseService.getProductVolumeData());
        return "employee/admin/requestsForDelivery/editRequestForDelivery";
    }

    @PostMapping("/employee/admin/requestsForDelivery/editRequestForDelivery/{id}")
    public String editRequestForDelivery(@PathVariable("id") long id,
                                          @RequestParam Long supplierId,
                                          @RequestParam Long warehouseId,
                                          @RequestParam BigDecimal deliveryCost,
                                          @RequestParam List<Long> productIds,
                                          @RequestParam List<Integer> quantities,
                                          @RequestParam List<BigDecimal> purchasePrices,
                                          @RequestParam(required = false) List<String> units,
                                          RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.updateRequestByAdmin(
                id, supplierId, warehouseId, deliveryCost, productIds, quantities, purchasePrices, units)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при обновлении заявки.");
            return "redirect:/employee/admin/requestsForDelivery/editRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка обновлена.");
        return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @PostMapping("/employee/admin/requestsForDelivery/deleteRequestForDelivery/{id}")
    public String deleteRequestForDelivery(@PathVariable("id") long id,
                                            RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.deleteRequestByAdmin(id)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при удалении заявки.");
            return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка удалена.");
        return "redirect:/employee/admin/requestsForDelivery/allRequestsForDelivery";
    }

    @PostMapping("/employee/admin/requestsForDelivery/approveRequestForDelivery/{id}")
    public String approveRequestForDelivery(@PathVariable(value = "id") long id,
                                             @RequestParam String commentText,
                                             RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.approveByDirector(id, commentText)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при согласовании заявки.");
            return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка согласована.");
        return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @PostMapping("/employee/admin/requestsForDelivery/rejectRequestForDelivery/{id}")
    public String rejectRequestForDelivery(@PathVariable(value = "id") long id,
                                            @RequestParam String commentText,
                                            RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.rejectByDirector(id, commentText)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при отклонении заявки.");
            return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка отклонена.");
        return "redirect:/employee/admin/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @Transactional
    @GetMapping("/employee/admin/requestsForDelivery/downloadContract/{id}")
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
        headers.setContentDisposition(ContentDisposition.attachment().filename(file.fileName(), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(file.content());
    }
}
