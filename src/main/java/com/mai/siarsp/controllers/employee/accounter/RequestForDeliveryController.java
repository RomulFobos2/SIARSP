package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.dto.RequestForDeliveryDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.service.employee.accounter.RequestForDeliveryService;
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

@Controller("accounterRequestForDeliveryController")
@Slf4j
public class RequestForDeliveryController {

    private final RequestForDeliveryService requestForDeliveryService;

    public RequestForDeliveryController(
            @Qualifier("accounterRequestForDeliveryService") RequestForDeliveryService requestForDeliveryService) {
        this.requestForDeliveryService = requestForDeliveryService;
    }

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
}
