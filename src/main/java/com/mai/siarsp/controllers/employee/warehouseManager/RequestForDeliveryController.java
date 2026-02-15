package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.service.employee.manager.SupplierService;
import com.mai.siarsp.service.employee.warehouseManager.ProductService;
import com.mai.siarsp.service.employee.warehouseManager.RequestForDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller("warehouseManagerRequestForDeliveryController")
@Slf4j
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestForDeliveryService;
    private final SupplierService supplierService;
    private final ProductService productService;

    public RequestForDeliveryController(
            @Qualifier("warehouseManagerRequestForDeliveryService") RequestForDeliveryService requestForDeliveryService,
            SupplierService supplierService,
            @Qualifier("warehouseManagerProductService") ProductService productService) {
        this.requestForDeliveryService = requestForDeliveryService;
        this.supplierService = supplierService;
        this.productService = productService;
    }

    @Transactional
    @GetMapping("/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery")
    public String allRequestsForDelivery(Model model) {
        model.addAttribute("allRequests", requestForDeliveryService.getAllRequests());
        return "employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
    }

    @Transactional
    @GetMapping("/employee/warehouseManager/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(Model model) {
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        model.addAttribute("allProducts", productService.getAllProducts());
        return "employee/warehouseManager/requestsForDelivery/addRequestForDelivery";
    }

    @PostMapping("/employee/warehouseManager/requestsForDelivery/addRequestForDelivery")
    public String addRequestForDelivery(@RequestParam Long supplierId,
                                         @RequestParam List<Long> productIds,
                                         @RequestParam List<Integer> quantities,
                                         RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.createRequest(supplierId, productIds, quantities)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при создании заявки.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/addRequestForDelivery";
        }
        return "redirect:/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
    }

    @Transactional
    @GetMapping("/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/{id}")
    public String detailsRequestForDelivery(@PathVariable(value = "id") long id, Model model) {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null) {
            return "redirect:/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
        }

        RequestForDeliveryDTO dto = com.mai.siarsp.mapper.RequestForDeliveryMapper.INSTANCE.toDTO(request);
        model.addAttribute("requestDTO", dto);
        return "employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery";
    }

    @Transactional
    @GetMapping("/employee/warehouseManager/requestsForDelivery/editRequestForDelivery/{id}")
    public String editRequestForDelivery(@PathVariable(value = "id") long id, Model model,
                                          RedirectAttributes redirectAttributes) {
        RequestForDelivery request = requestForDeliveryService.getRequestEntity(id);
        if (request == null) {
            return "redirect:/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
        }

        if (request.getStatus() != com.mai.siarsp.enumeration.RequestStatus.DRAFT
                && request.getStatus() != com.mai.siarsp.enumeration.RequestStatus.REJECTED_BY_DIRECTOR
                && request.getStatus() != com.mai.siarsp.enumeration.RequestStatus.REJECTED_BY_ACCOUNTANT) {
            redirectAttributes.addFlashAttribute("requestError", "Заявку в статусе «" + request.getStatus().getDisplayName() + "» нельзя редактировать.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }

        RequestForDeliveryDTO dto = com.mai.siarsp.mapper.RequestForDeliveryMapper.INSTANCE.toDTO(request);
        model.addAttribute("requestDTO", dto);
        model.addAttribute("allSuppliers", supplierService.getAllSuppliers());
        model.addAttribute("allProducts", productService.getAllProducts());
        return "employee/warehouseManager/requestsForDelivery/editRequestForDelivery";
    }

    @PostMapping("/employee/warehouseManager/requestsForDelivery/editRequestForDelivery/{id}")
    public String editRequestForDelivery(@PathVariable(value = "id") long id,
                                          @RequestParam Long supplierId,
                                          @RequestParam List<Long> productIds,
                                          @RequestParam List<Integer> quantities,
                                          RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.updateRequest(id, supplierId, productIds, quantities)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при сохранении изменений заявки.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/editRequestForDelivery/" + id;
        }
        return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @GetMapping("/employee/warehouseManager/requestsForDelivery/deleteRequestForDelivery/{id}")
    public String deleteRequestForDelivery(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.deleteRequest(id)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при удалении заявки. Удалить можно только черновик.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        return "redirect:/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
    }

    @PostMapping("/employee/warehouseManager/requestsForDelivery/submitRequestForDelivery/{id}")
    public String submitRequestForDelivery(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.submitForApproval(id)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при отправке заявки на согласование.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка отправлена на согласование директору.");
        return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @PostMapping("/employee/warehouseManager/requestsForDelivery/resubmitRequestForDelivery/{id}")
    public String resubmitRequestForDelivery(@PathVariable(value = "id") long id,
                                              @RequestParam String commentText,
                                              RedirectAttributes redirectAttributes) {
        if (!requestForDeliveryService.resubmitForApproval(id, commentText)) {
            redirectAttributes.addFlashAttribute("requestError", "Ошибка при повторной отправке заявки.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        redirectAttributes.addFlashAttribute("requestSuccess", "Заявка повторно отправлена на согласование.");
        return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }
}
