package com.mai.siarsp.controllers.employee.general;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.models.ProductAttributeValue;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.ProductAttributeValueRepository;
import com.mai.siarsp.repo.ProductCategoryRepository;
import com.mai.siarsp.repo.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

/**
 * REST-контроллер для AJAX-запросов значений атрибутов товаров
 *
 * Используется на страницах товаров для:
 * - Модального окна просмотра характеристик в списке товаров
 * - Динамической загрузки полей атрибутов при выборе категории (add/edit)
 *
 * Доступен для ROLE_EMPLOYEE_MANAGER и ROLE_EMPLOYEE_WAREHOUSE_MANAGER
 */
@Controller
public class ProductAttributeValueController {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductAttributeValueRepository productAttributeValueRepository;

    public ProductAttributeValueController(ProductRepository productRepository,
                                           ProductCategoryRepository productCategoryRepository,
                                           ProductAttributeValueRepository productAttributeValueRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productAttributeValueRepository = productAttributeValueRepository;
    }

    /**
     * Возвращает значения атрибутов для конкретного товара
     * Используется в модальном окне на странице allProducts
     *
     * @param productId ID товара
     * @return JSON список {name, value, unit}
     */
    @Transactional
    @GetMapping("/employee/general/productAttributeValues/values-by-product")
    public ResponseEntity<List<Map<String, String>>> getValuesByProduct(@RequestParam Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        Product product = productOpt.get();
        List<Map<String, String>> result = new ArrayList<>();

        for (ProductAttributeValue pav : product.getAttributeValues()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("name", pav.getAttribute().getName());
            item.put("value", pav.getValue());
            item.put("unit", pav.getAttribute().getUnit() != null ? pav.getAttribute().getUnit() : "");
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Возвращает список атрибутов для выбранной категории
     * Если productId > 0, также возвращает текущие значения для предзаполнения
     * Используется на страницах добавления/редактирования товара
     *
     * @param categoryId ID категории
     * @param productId  ID товара (0 при создании нового)
     * @return JSON список {id, name, unit, value}
     */
    @Transactional
    @GetMapping("/employee/general/productAttributeValues/attributes-by-category")
    public ResponseEntity<List<Map<String, Object>>> getAttributesByCategory(
            @RequestParam Long categoryId,
            @RequestParam(defaultValue = "0") Long productId) {

        Optional<ProductCategory> categoryOpt = productCategoryRepository.findById(categoryId);
        if (categoryOpt.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        ProductCategory category = categoryOpt.get();
        List<ProductAttribute> attributes = category.getAttributes();

        Product product = null;
        if (productId > 0) {
            product = productRepository.findById(productId).orElse(null);
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (ProductAttribute attr : attributes) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", attr.getId());
            item.put("name", attr.getName());
            item.put("unit", attr.getUnit() != null ? attr.getUnit() : "");

            String value = "";
            if (product != null) {
                Optional<ProductAttributeValue> pavOpt = productAttributeValueRepository
                        .findByProductAndAttribute(product, attr);
                if (pavOpt.isPresent()) {
                    value = pavOpt.get().getValue();
                }
            }
            item.put("value", value);

            result.add(item);
        }

        return ResponseEntity.ok(result);
    }
}
