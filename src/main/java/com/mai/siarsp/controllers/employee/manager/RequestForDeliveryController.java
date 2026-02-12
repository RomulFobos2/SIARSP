package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.repo.EmployeeRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import com.mai.siarsp.service.employee.manager.RequestForDeliveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller("managerRequestForDeliveryController")
@RequestMapping("/employee/manager/requestsForDelivery")
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestService;
    private final EmployeeRepository employeeRepository;

    public RequestForDeliveryController(RequestForDeliveryService requestService,
                                        EmployeeRepository employeeRepository) {
        this.requestService = requestService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/allRequestsForDelivery")
    public String allRequests(@RequestParam(required = false) RequestStatus status, Model model) {
        model.addAttribute("allStatuses", RequestStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("allRequests", status == null
                ? requestService.getAllRequests()
                : requestService.getRequestsByStatus(status));
        return "employee/manager/allRequestsForDelivery";
    }

    @GetMapping("/detailsRequestForDelivery/{id}")
    public String detailsRequest(@PathVariable Long id, Model model) {
        RequestForDeliveryDTO requestDTO = requestService.getRequestById(id);
        if (requestDTO == null) {
            return "redirect:/employee/manager/requestsForDelivery/allRequestsForDelivery";
        }
        model.addAttribute("request", requestDTO);
        return "employee/manager/detailsRequestForDelivery";
    }

    @PostMapping("/approveRequestForDelivery/{id}")
    public String approve(@PathVariable Long id,
                          @RequestParam String commentText,
                          RedirectAttributes ra) {
        Employee currentEmployee = getCurrentEmployee();
        if (!requestService.approveByDirector(id, commentText, currentEmployee)) {
            ra.addFlashAttribute("requestError", "Не удалось согласовать заявку. Проверьте статус и комментарий.");
        }
        return "redirect:/employee/manager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    @PostMapping("/rejectRequestForDelivery/{id}")
    public String reject(@PathVariable Long id,
                         @RequestParam String commentText,
                         RedirectAttributes ra) {
        Employee currentEmployee = getCurrentEmployee();
        if (!requestService.rejectByDirector(id, commentText, currentEmployee)) {
            ra.addFlashAttribute("requestError", "Не удалось отклонить заявку. Проверьте статус и комментарий.");
        }
        return "redirect:/employee/manager/requestsForDelivery/detailsRequestForDelivery/" + id;
    }

    private Employee getCurrentEmployee() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return employeeRepository.findByUsername(username).orElse(null);
    }
}
