package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.service.employee.manager.RequestForDeliveryService;
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

@Controller("managerRequestForDeliveryController")
@Slf4j
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestForDeliveryService;

    public RequestForDeliveryController(
            @Qualifier("managerRequestForDeliveryService") RequestForDeliveryService requestForDeliveryService) {
        this.requestForDeliveryService = requestForDeliveryService;
    }

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
}
