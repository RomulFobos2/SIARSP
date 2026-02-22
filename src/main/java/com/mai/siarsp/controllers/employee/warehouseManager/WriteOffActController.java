package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.WriteOffActDTO;
import com.mai.siarsp.enumeration.WriteOffReason;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.WriteOffAct;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.service.employee.WriteOffActService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер актов списания для заведующего складом (WAREHOUSE_MANAGER).
 * Позволяет создавать акты и просматривать их статус.
 */
@Controller("warehouseManagerWriteOffActController")
@RequestMapping("/employee/warehouseManager/writeOffActs")
@Slf4j
public class WriteOffActController {

    private final WriteOffActService writeOffActService;
    private final ProductRepository productRepository;

    public WriteOffActController(WriteOffActService writeOffActService,
                                 ProductRepository productRepository) {
        this.writeOffActService = writeOffActService;
        this.productRepository = productRepository;
    }

    @GetMapping("/allWriteOffActs")
    public String allWriteOffActs(Model model) {
        List<WriteOffActDTO> acts = writeOffActService.getAllActs();
        model.addAttribute("acts", acts);
        return "employee/warehouseManager/writeOffActs/allWriteOffActs";
    }

    @Transactional(readOnly = true)
    @GetMapping("/createWriteOffAct")
    public String createWriteOffActPage(Model model) {
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> p.getStockQuantity() > 0)
                .toList();
        model.addAttribute("products", products);
        model.addAttribute("reasons", WriteOffReason.values());
        return "employee/warehouseManager/writeOffActs/createWriteOffAct";
    }

    @PostMapping("/createWriteOffAct")
    public String createWriteOffAct(@RequestParam Long productId,
                                    @RequestParam int quantity,
                                    @RequestParam WriteOffReason reason,
                                    @RequestParam(required = false) String comment,
                                    @AuthenticationPrincipal Employee currentEmployee,
                                    RedirectAttributes redirectAttributes) {
        boolean success = writeOffActService.createAct(productId, quantity, reason, comment, currentEmployee);

        if (success) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Акт списания успешно создан и отправлен на подпись директору.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при создании акта списания. Проверьте доступное количество товара.");
        }
        return "redirect:/employee/warehouseManager/writeOffActs/allWriteOffActs";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsWriteOffAct/{id}")
    public String detailsWriteOffAct(@PathVariable Long id, Model model) {
        Optional<WriteOffAct> optAct = writeOffActService.getActById(id);
        if (optAct.isEmpty()) {
            return "redirect:/employee/warehouseManager/writeOffActs/allWriteOffActs";
        }
        model.addAttribute("act", optAct.get());
        return "employee/warehouseManager/writeOffActs/detailsWriteOffAct";
    }
}
