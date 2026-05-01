package com.mai.siarsp.controllers.employee.admin;

import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.mapper.ProductMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.repo.OrderedProductRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.SupplyRepository;
import com.mai.siarsp.repo.WriteOffActRepository;
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
 * Read-only контроллер просмотра товаров для администратора (ADMIN).
 * Нужен для перехода к карточке товара со страниц приёмок, заявок и заказов
 * (admin не имеет права лезть в URL других ролей).
 */
@Controller("adminProductViewController")
@RequestMapping("/employee/admin/products")
@Slf4j
public class ProductViewController {

    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;
    private final OrderedProductRepository orderedProductRepository;
    private final WriteOffActRepository writeOffActRepository;

    public ProductViewController(ProductRepository productRepository,
                                 SupplyRepository supplyRepository,
                                 OrderedProductRepository orderedProductRepository,
                                 WriteOffActRepository writeOffActRepository) {
        this.productRepository = productRepository;
        this.supplyRepository = supplyRepository;
        this.orderedProductRepository = orderedProductRepository;
        this.writeOffActRepository = writeOffActRepository;
    }

    @Transactional(readOnly = true)
    @GetMapping("/allProducts")
    public String allProducts(Model model) {
        model.addAttribute("allProducts", productRepository.findAll().stream()
                .map(ProductMapper.INSTANCE::toDTO)
                .sorted(Comparator.comparing(ProductDTO::getName))
                .toList());
        return "employee/admin/products/allProducts";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsProduct/{id}")
    public String detailsProduct(@PathVariable(value = "id") long id, Model model) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/employee/admin/products/allProducts";
        }

        ProductDTO productDTO = ProductMapper.INSTANCE.toDTO(opt.get());
        model.addAttribute("productDTO", productDTO);
        model.addAttribute("supplies", supplyRepository.findByProductIdOrderByDelivery_DeliveryDateDesc(id));
        model.addAttribute("orderedProducts", orderedProductRepository.findByProductIdOrderByClientOrder_OrderDateDesc(id));
        model.addAttribute("writeOffActs", writeOffActRepository.findByProductIdOrderByActDateDesc(id));
        return "employee/admin/products/detailsProduct";
    }
}
