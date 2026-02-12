package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.dto.RequestedProductDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Supplier;
import com.mai.siarsp.repo.EmployeeRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.SupplierRepository;
import com.mai.siarsp.service.employee.warehouseManager.RequestForDeliveryService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/employee/warehouseManager/requestsForDelivery")
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestService;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;

    public RequestForDeliveryController(RequestForDeliveryService requestService,
                                        SupplierRepository supplierRepository,
                                        ProductRepository productRepository,
                                        EmployeeRepository employeeRepository) {
        this.requestService = requestService;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/allRequestsForDelivery")
    public String allRequests(Model model) {
        model.addAttribute("allRequests", requestService.getAllRequests());
        return "employee/warehouseManager/allRequestsForDelivery";
    }

    @GetMapping("/addRequestForDelivery")
    public String addRequest(Model model) {
        fillSelects(model);
        return "employee/warehouseManager/addRequestForDelivery";
    }

    @PostMapping("/addRequestForDelivery")
    public String addRequest(@RequestParam Long supplierId,
                             @RequestParam(name = "productIds", required = false) List<Long> productIds,
                             @RequestParam(name = "quantities", required = false) List<Integer> quantities,
                             Model model) {
        RequestForDeliveryDTO dto = new RequestForDeliveryDTO();
        dto.setSupplierId(supplierId);
        dto.setRequestedProducts(mapItems(productIds, quantities));

        RequestForDeliveryDTO saved = requestService.createRequest(dto);
        if (saved == null) {
            model.addAttribute("requestError", "Ошибка при создании заявки");
            fillSelects(model);
            return "employee/warehouseManager/addRequestForDelivery";
        }

        return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + saved.getId();
    }

    @GetMapping("/detailsRequestForDelivery/{id}")
    public String detailsRequest(@PathVariable Long id, Model model) {
        RequestForDeliveryDTO requestDTO = requestService.getRequestById(id);
        if (requestDTO == null) {
            return "redirect:/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
        }
        model.addAttribute("request", requestDTO);
        model.addAttribute("editableStatuses", List.of(RequestStatus.DRAFT, RequestStatus.REJECTED_BY_DIRECTOR, RequestStatus.REJECTED_BY_ACCOUNTANT));
        return "employee/warehouseManager/detailsRequestForDelivery";
    }

    @GetMapping("/editRequestForDelivery/{id}")
    public String editRequest(@PathVariable Long id, Model model, RedirectAttributes ra) {
        RequestForDeliveryDTO requestDTO = requestService.getRequestById(id);
        if (requestDTO == null) {
            return "redirect:/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
        }
        if (!List.of(RequestStatus.DRAFT, RequestStatus.REJECTED_BY_DIRECTOR, RequestStatus.REJECTED_BY_ACCOUNTANT).contains(requestDTO.getStatus())) {
            ra.addFlashAttribute("requestError", "Редактирование доступно только для черновика или отклонённых заявок.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }

        fillSelects(model);
        model.addAttribute("request", requestDTO);
        return "employee/warehouseManager/editRequestForDelivery";
    }

    @PostMapping("/editRequestForDelivery/{id}")
    public String editRequest(@PathVariable Long id,
                              @RequestParam Long supplierId,
                              @RequestParam(name = "productIds", required = false) List<Long> productIds,
                              @RequestParam(name = "quantities", required = false) List<Integer> quantities,
                              RedirectAttributes ra) {
        RequestForDeliveryDTO dto = new RequestForDeliveryDTO();
        dto.setSupplierId(supplierId);
        dto.setRequestedProducts(mapItems(productIds, quantities));

        if (!requestService.updateRequest(id, dto)) {
            ra.addFlashAttribute("requestError", "Ошибка при сохранении заявки");
            return "redirect:/employee/warehouseManager/requestsForDelivery/editRequestForDelivery/" + id;
        }

        return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @GetMapping("/deleteRequestForDelivery/{id}")
    public String deleteRequest(@PathVariable Long id, RedirectAttributes ra) {
        if (!requestService.deleteRequest(id)) {
            ra.addFlashAttribute("requestError", "Удаление доступно только для черновика.");
            return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
        }
        return "redirect:/employee/warehouseManager/requestsForDelivery/allRequestsForDelivery";
    }

    @PostMapping("/submitRequestForDelivery/{id}")
    public String submitRequest(@PathVariable Long id, RedirectAttributes ra) {
        if (!requestService.submitForApproval(id)) {
            ra.addFlashAttribute("requestError", "Заявку нельзя отправить на согласование в текущем статусе.");
        }
        return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @PostMapping("/resubmitRequestForDelivery/{id}")
    public String resubmitRequest(@PathVariable Long id,
                                  @RequestParam String commentText,
                                  RedirectAttributes ra) {
        Employee currentEmployee = getCurrentEmployee();
        if (!requestService.resubmitForApproval(id, commentText, currentEmployee)) {
            ra.addFlashAttribute("requestError", "Повторная отправка не выполнена. Проверьте комментарий и статус заявки.");
        }
        return "redirect:/employee/warehouseManager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    private List<RequestedProductDTO> mapItems(List<Long> productIds, List<Integer> quantities) {
        List<RequestedProductDTO> items = new ArrayList<>();
        if (productIds == null || quantities == null) {
            return items;
        }
        int size = Math.min(productIds.size(), quantities.size());
        for (int i = 0; i < size; i++) {
            RequestedProductDTO item = new RequestedProductDTO();
            item.setProductId(productIds.get(i));
            item.setQuantity(quantities.get(i) == null ? 0 : quantities.get(i));
            items.add(item);
        }
        return items;
    }

    private void fillSelects(Model model) {
        model.addAttribute("suppliers", supplierRepository.findAll().stream().sorted(Comparator.comparing(Supplier::getName)).toList());
        model.addAttribute("products", productRepository.findAll().stream().sorted(Comparator.comparing(Product::getName)).toList());
    }

    private Employee getCurrentEmployee() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepository.findByUsername(username).orElse(null);
    }
}
