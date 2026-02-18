package com.mai.siarsp.controllers.employee.accounter;

import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.mapper.ProductMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.repo.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.Optional;

/**
 * Read-only контроллер просмотра товаров для бухгалтера (ACCOUNTER).
 * Позволяет просматривать список товаров и детали, без возможности редактирования.
 */
@Controller("accounterProductViewController")
@RequestMapping("/employee/accounter/products")
@Slf4j
public class ProductViewController {

    private final ProductRepository productRepository;

    public ProductViewController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    @GetMapping("/allProducts")
    public String allProducts(Model model) {
        model.addAttribute("allProducts", productRepository.findAll().stream()
                .map(ProductMapper.INSTANCE::toDTO)
                .sorted(Comparator.comparing(ProductDTO::getName))
                .toList());
        return "employee/accounter/products/allProducts";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsProduct/{id}")
    public String detailsProduct(@PathVariable(value = "id") long id, Model model) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/employee/accounter/products/allProducts";
        }

        ProductDTO productDTO = ProductMapper.INSTANCE.toDTO(opt.get());
        model.addAttribute("productDTO", productDTO);
        return "employee/accounter/products/detailsProduct";
    }
}
