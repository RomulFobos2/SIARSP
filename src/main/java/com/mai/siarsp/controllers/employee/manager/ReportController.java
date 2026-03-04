package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.service.general.ProductExpirationService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller("managerReportController")
@RequestMapping("/employee/manager/reports")
public class ReportController {

    private final ProductRepository productRepository;
    private final ProductExpirationService productExpirationService;

    public ReportController(ProductRepository productRepository,
                            ProductExpirationService productExpirationService) {
        this.productRepository = productRepository;
        this.productExpirationService = productExpirationService;
    }

    @Transactional(readOnly = true)
    @GetMapping("/expiring-products")
    public String expiringProducts(@RequestParam(required = false) LocalDate startDate,
                                   @RequestParam(required = false) LocalDate endDate,
                                   Model model) {
        LocalDate s = startDate != null ? startDate : LocalDate.now();
        LocalDate e = endDate != null ? endDate : s.plusDays(7);

        final LocalDate from = s.isBefore(e) ? s : e;
        final LocalDate to   = s.isBefore(e) ? e : s;

        List<ExpiringProductRow> products = productRepository.findAll().stream()
                .map(product -> productExpirationService.getExpirationDate(product)
                        .map(expDate -> new ExpiringProductRow(product, expDate))
                        .orElse(null))
                .filter(row -> row != null
                        && !row.expirationDate().isBefore(from)
                        && !row.expirationDate().isAfter(to))
                .sorted(Comparator.comparing(ExpiringProductRow::expirationDate))
                .toList();

        model.addAttribute("reportTitle", "Отчёт по срокам годности");
        model.addAttribute("products", products);
        model.addAttribute("startDate", from);
        model.addAttribute("endDate", to);
        model.addAttribute("detailsPrefix", "/employee/manager/products/detailsProduct/");
        model.addAttribute("formAction", "/employee/manager/reports/expiring-products");
        return "employee/general/reports/expiringProducts";
    }

    public record ExpiringProductRow(Product product, LocalDate expirationDate) {
    }
}
