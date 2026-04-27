package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.ProductCategoryDTO;
import com.mai.siarsp.dto.ProductDTO;
import com.mai.siarsp.enumeration.WarehouseType;
import com.mai.siarsp.mapper.ProductMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.repo.OrderedProductRepository;
import com.mai.siarsp.repo.ProductAttributeRepository;
import com.mai.siarsp.repo.SupplyRepository;
import com.mai.siarsp.repo.WriteOffActRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import com.mai.siarsp.service.employee.warehouseManager.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller("warehouseManagerProductController")
@Slf4j
public class ProductController {

    private static final List<String> GABARITE_ATTR_NAMES = List.of(
            "Длина упаковки", "Ширина упаковки", "Высота упаковки"
    );

    private final ProductService productService;
    private final ZoneProductRepository zoneProductRepository;
    private final ProductAttributeRepository productAttributeRepository;
    private final SupplyRepository supplyRepository;
    private final OrderedProductRepository orderedProductRepository;
    private final WriteOffActRepository writeOffActRepository;

    public ProductController(ProductService productService,
                             ZoneProductRepository zoneProductRepository,
                             ProductAttributeRepository productAttributeRepository,
                             SupplyRepository supplyRepository,
                             OrderedProductRepository orderedProductRepository,
                             WriteOffActRepository writeOffActRepository) {
        this.productService = productService;
        this.zoneProductRepository = zoneProductRepository;
        this.productAttributeRepository = productAttributeRepository;
        this.supplyRepository = supplyRepository;
        this.orderedProductRepository = orderedProductRepository;
        this.writeOffActRepository = writeOffActRepository;
    }

    @GetMapping("/employee/warehouseManager/products/check-article")
    public ResponseEntity<Map<String, Boolean>> checkArticle(@RequestParam String article,
                                                              @RequestParam(required = false) Long id) {
        boolean exists = productService.checkArticle(article, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    @Transactional
    @GetMapping("/employee/warehouseManager/products/allProducts")
    public String allProducts(Model model) {
        model.addAttribute("allProducts", productService.getAllProducts().stream()
                .sorted(Comparator.comparing(ProductDTO::getName))
                .toList());
        return "employee/warehouseManager/products/allProducts";
    }

    @GetMapping("/employee/warehouseManager/products/addProduct")
    public String addProduct(Model model) {
        model.addAttribute("warehouseTypes", WarehouseType.values());
        populateCategories(model);
        return "employee/warehouseManager/products/addProduct";
    }

    @PostMapping("/employee/warehouseManager/products/addProduct")
    public String addProduct(@RequestParam String inputName,
                             @RequestParam String inputArticle,
                             @RequestParam int inputStockQuantity,
                             @RequestParam int inputQuantityForStock,
                             @RequestParam WarehouseType inputWarehouseType,
                             @RequestParam Long inputCategoryId,
                             @RequestParam MultipartFile inputFileField,
                             @RequestParam Map<String, String> allParams,
                             Model model) {
        Product product = new Product();
        product.setName(inputName);
        product.setArticle(inputArticle);
        product.setStockQuantity(inputStockQuantity);
        product.setQuantityForStock(inputQuantityForStock);
        product.setWarehouseType(inputWarehouseType);
        product.setReservedQuantity(0);

        Map<String, String> attributeValues = extractAttributeValues(allParams);

        Optional<Long> savedProductId = productService.saveProduct(product, inputCategoryId, inputFileField, attributeValues);
        if (savedProductId.isEmpty()) {
            model.addAttribute("productError", "Ошибка при сохранении товара.");
            model.addAttribute("warehouseTypes", WarehouseType.values());
            populateCategories(model);
            return "employee/warehouseManager/products/addProduct";
        }

        return "redirect:/employee/warehouseManager/products/detailsProduct/" + savedProductId.get();
    }

    @Transactional
    @GetMapping("/employee/warehouseManager/products/detailsProduct/{id}")
    public String detailsProduct(@PathVariable(value = "id") long id, Model model) {
        if (!productService.getProductRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/products/allProducts";
        }

        Product product = productService.getProductRepository().findById(id).get();
        ProductDTO productDTO = ProductMapper.INSTANCE.toDTO(product);
        model.addAttribute("productDTO", productDTO);
        model.addAttribute("supplies", supplyRepository.findByProductIdOrderByDelivery_DeliveryDateDesc(id));
        model.addAttribute("orderedProducts", orderedProductRepository.findByProductIdOrderByClientOrder_OrderDateDesc(id));
        model.addAttribute("writeOffActs", writeOffActRepository.findByProductIdOrderByActDateDesc(id));
        return "employee/warehouseManager/products/detailsProduct";
    }

    @Transactional
    @GetMapping("/employee/warehouseManager/products/editProduct/{id}")
    public String editProduct(@PathVariable(value = "id") long id, Model model) {
        if (!productService.getProductRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/products/allProducts";
        }

        Product product = productService.getProductRepository().findById(id).get();
        ProductDTO productDTO = ProductMapper.INSTANCE.toDTO(product);
        boolean isInWarehouse = !zoneProductRepository.findByProduct(product).isEmpty();
        model.addAttribute("productDTO", productDTO);
        model.addAttribute("isInWarehouse", isInWarehouse);
        model.addAttribute("warehouseTypes", WarehouseType.values());
        populateCategories(model);
        return "employee/warehouseManager/products/editProduct";
    }

    @Transactional
    @PostMapping("/employee/warehouseManager/products/editProduct/{id}")
    public String editProduct(@PathVariable(value = "id") long id,
                              @RequestParam String inputName,
                              @RequestParam String inputArticle,
                              @RequestParam int inputStockQuantity,
                              @RequestParam int inputQuantityForStock,
                              @RequestParam WarehouseType inputWarehouseType,
                              @RequestParam Long inputCategoryId,
                              @RequestParam(required = false) MultipartFile inputFileField,
                              @RequestParam Map<String, String> allParams,
                              RedirectAttributes redirectAttributes) {
        Map<String, String> attributeValues = extractAttributeValues(allParams);

        // Серверная защита: если товар размещён на складе — убрать габаритные атрибуты
        Optional<Product> optProduct = productService.getProductRepository().findById(id);
        if (optProduct.isPresent() && !zoneProductRepository.findByProduct(optProduct.get()).isEmpty()) {
            Set<String> gabariteAttrIds = GABARITE_ATTR_NAMES.stream()
                    .map(productAttributeRepository::findByName)
                    .filter(Optional::isPresent)
                    .map(opt -> String.valueOf(opt.get().getId()))
                    .collect(Collectors.toSet());
            attributeValues.keySet().removeAll(gabariteAttrIds);
        }

        Optional<Long> editedProductId = productService.editProduct(id, inputName, inputArticle, inputStockQuantity,
                inputQuantityForStock, inputWarehouseType, inputCategoryId, inputFileField, attributeValues);

        if (editedProductId.isEmpty()) {
            redirectAttributes.addFlashAttribute("productError", "Ошибка при сохранении изменений товара.");
            return "redirect:/employee/warehouseManager/products/editProduct/" + id;
        }

        return "redirect:/employee/warehouseManager/products/detailsProduct/" + editedProductId.get();
    }

    @GetMapping("/employee/warehouseManager/products/deleteProduct/{id}")
    public String deleteProduct(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!productService.deleteProduct(id)) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить товар. Товар используется в заказах, поставках, складских зонах или документах.");
            return "redirect:/employee/warehouseManager/products/detailsProduct/" + id;
        }

        return "redirect:/employee/warehouseManager/products/allProducts";
    }

    /**
     * Заполняет список категорий для формы без использования ProductCategoryMapper.
     *
     * Используется облегчённый ручной маппинг, чтобы не трогать LAZY-коллекцию
     * ProductCategory.attributes и не провоцировать LazyInitializationException.
     *
     * В случае merge-конфликта с вариантом через ProductCategoryMapper следует
     * оставлять именно текущую реализацию с toLightCategoryDTO(...).
     */
    private void populateCategories(Model model) {
        List<ProductCategoryDTO> categories = productService.getProductCategoryRepository().findAll().stream()
                .map(this::toLightCategoryDTO)
                .sorted(Comparator.comparing(ProductCategoryDTO::getDisplayName))
                .toList();

        model.addAttribute("allProductCategories", categories);
    }

    private Map<String, String> extractAttributeValues(Map<String, String> allParams) {
        Map<String, String> attributeValues = new HashMap<>();
        String prefix = "attributeValues[";
        for (Map.Entry<String, String> entry : allParams.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getKey().endsWith("]")) {
                String attrId = entry.getKey().substring(prefix.length(), entry.getKey().length() - 1);
                attributeValues.put(attrId, entry.getValue());
            }
        }
        return attributeValues;
    }

    private ProductCategoryDTO toLightCategoryDTO(ProductCategory category) {
        ProductCategoryDTO dto = new ProductCategoryDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setGlobalProductCategoryId(category.getGlobalProductCategory().getId());
        dto.setGlobalProductCategoryName(category.getGlobalProductCategory().getName());
        dto.setDisplayName(category.getDisplayName());
        return dto;
    }
}
