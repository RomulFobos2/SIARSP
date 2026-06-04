package com.mai.siarsp.controllers.employee.admin;

import com.mai.siarsp.dto.DeliveryDTO;
import com.mai.siarsp.dto.SupplyDTO;
import com.mai.siarsp.dto.SupplyInputDTO;
import com.mai.siarsp.enumeration.RequestStatus;
import com.mai.siarsp.mapper.DeliveryMapper;
import com.mai.siarsp.mapper.RequestForDeliveryMapper;
import com.mai.siarsp.models.Delivery;
import com.mai.siarsp.models.RequestForDelivery;
import com.mai.siarsp.models.RequestedProduct;
import com.mai.siarsp.service.employee.warehouseManager.DeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Контроллер приёмки поставок от поставщиков для администратора (ADMIN).
 *
 * Администратор имеет полный доступ к приёмкам: просмотр, оформление.
 */
@Controller("adminDeliveryController")
@RequestMapping("/employee/admin/deliveries")
@Slf4j
public class DeliveryController {

    private final DeliveryService deliveryService;

    public DeliveryController(
            @Qualifier("warehouseManagerDeliveryService") DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @GetMapping("/allDeliveries")
    public String allDeliveries(Model model) {
        model.addAttribute("allDeliveries", deliveryService.getAllDeliveries());
        return "employee/admin/deliveries/allDeliveries";
    }

    @Transactional(readOnly = true)
    @GetMapping("/addDelivery")
    public String addDelivery(Model model) {
        List<RequestForDelivery> approvedRequests = deliveryService.getApprovedRequests();
        model.addAttribute("approvedRequests",
                RequestForDeliveryMapper.INSTANCE.toDTOList(approvedRequests));
        return "employee/admin/deliveries/addDelivery";
    }

    @Transactional(readOnly = true)
    @GetMapping("/addDeliveryFromRequest/{requestId}")
    public String addDeliveryFromRequest(@PathVariable Long requestId, Model model,
                                          RedirectAttributes redirectAttributes) {
        Optional<RequestForDelivery> optRequest = deliveryService.getRequestById(requestId);
        if (optRequest.isEmpty()) {
            redirectAttributes.addFlashAttribute("deliveryError", "Заявка не найдена.");
            return "redirect:/employee/admin/deliveries/addDelivery";
        }

        RequestForDelivery request = optRequest.get();
        if (request.getStatus() != RequestStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("deliveryError",
                    "Заявка не в статусе «Согласовано». Текущий статус: " + request.getStatus().getDisplayName());
            return "redirect:/employee/admin/deliveries/addDelivery";
        }

        model.addAttribute("request", RequestForDeliveryMapper.INSTANCE.toDTO(request));
        model.addAttribute("requestedProducts", request.getRequestedProducts());
        model.addAttribute("deliveryDate", LocalDate.now());
        return "employee/admin/deliveries/addDeliveryFromRequest";
    }

    @PostMapping("/addDelivery")
    public String saveDelivery(@RequestParam Long requestId,
                                @RequestParam LocalDate deliveryDate,
                                @RequestParam List<Long> productIds,
                                @RequestParam List<Integer> quantities,
                                @RequestParam List<BigDecimal> purchasePrices,
                                @RequestParam(required = false) List<String> deficitReasons,
                                RedirectAttributes redirectAttributes) {
        List<SupplyInputDTO> supplyInputs = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            SupplyInputDTO input = new SupplyInputDTO();
            input.setProductId(productIds.get(i));
            input.setQuantity(quantities.get(i));
            input.setPurchasePrice(purchasePrices.get(i));
            input.setDeficitReason(deficitReasons != null && i < deficitReasons.size()
                    ? deficitReasons.get(i) : null);
            supplyInputs.add(input);
        }

        boolean success = deliveryService.createDeliveryFromRequest(requestId, supplyInputs, deliveryDate);

        if (!success) {
            redirectAttributes.addFlashAttribute("deliveryError",
                    "Ошибка при создании поставки. Проверьте данные и попробуйте снова.");
            return "redirect:/employee/admin/deliveries/addDeliveryFromRequest/" + requestId;
        }

        redirectAttributes.addFlashAttribute("successMessage", "Поставка успешно оформлена.");
        return "redirect:/employee/admin/deliveries/allDeliveries";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsDelivery/{id}")
    public String detailsDelivery(@PathVariable Long id, Model model,
                                   RedirectAttributes redirectAttributes) {
        Optional<Delivery> optDelivery = deliveryService.getDeliveryById(id);
        if (optDelivery.isEmpty()) {
            redirectAttributes.addFlashAttribute("deliveryError", "Поставка не найдена.");
            return "redirect:/employee/admin/deliveries/allDeliveries";
        }

        Delivery delivery = optDelivery.get();
        DeliveryDTO deliveryDTO = DeliveryMapper.INSTANCE.toDTO(delivery);

        if (delivery.getRequest() != null) {
            Map<Long, RequestedProduct> requestedMap = delivery.getRequest().getRequestedProducts().stream()
                    .collect(Collectors.toMap(rp -> rp.getProduct().getId(), Function.identity()));

            for (SupplyDTO supplyDTO : deliveryDTO.getSupplies()) {
                RequestedProduct rp = requestedMap.get(supplyDTO.getProductId());
                if (rp != null) {
                    supplyDTO.setOrderedQuantity(rp.getQuantity());
                }
            }

            model.addAttribute("requestDTO",
                    RequestForDeliveryMapper.INSTANCE.toDTO(delivery.getRequest()));
        }

        boolean hasDeficit = deliveryDTO.getSupplies().stream()
                .anyMatch(s -> s.getDeficitQuantity() > 0);
        model.addAttribute("delivery", deliveryDTO);
        model.addAttribute("hasDeficit", hasDeficit);

        if (delivery.getRequest() != null && delivery.getRequest().getDeliveryCost() != null) {
            model.addAttribute("deliveryCost", delivery.getRequest().getDeliveryCost());
            model.addAttribute("grandTotal", deliveryDTO.getTotalCost().add(delivery.getRequest().getDeliveryCost()));
        } else {
            model.addAttribute("deliveryCost", null);
            model.addAttribute("grandTotal", deliveryDTO.getTotalCost());
        }

        return "employee/admin/deliveries/detailsDelivery";
    }
}
