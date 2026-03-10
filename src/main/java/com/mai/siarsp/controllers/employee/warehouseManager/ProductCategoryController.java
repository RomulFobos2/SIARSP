package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.GlobalProductCategoryDTO;
import com.mai.siarsp.dto.ProductAttributeDTO;
import com.mai.siarsp.dto.ProductCategoryDTO;
import com.mai.siarsp.mapper.GlobalProductCategoryMapper;
import com.mai.siarsp.mapper.ProductAttributeMapper;
import com.mai.siarsp.mapper.ProductCategoryMapper;
import com.mai.siarsp.models.GlobalProductCategory;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.models.ProductCategory;
import com.mai.siarsp.service.employee.warehouseManager.ProductCategoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

/**
 * Контроллер для управления категориями товаров.
 *
 * Доступен для роли ROLE_EMPLOYEE_WAREHOUSE_MANAGER.
 * Базовый URL: /employee/warehouseManager/productCategories/
 */
@Controller
@Slf4j
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    public ProductCategoryController(ProductCategoryService productCategoryService) {
        this.productCategoryService = productCategoryService;
    }

    /**
     * AJAX-проверка составной уникальности (globalProductCategory + name).
     */
    @GetMapping("/employee/warehouseManager/productCategories/check-name")
    public ResponseEntity<Map<String, Boolean>> checkName(@RequestParam String name,
                                                           @RequestParam Long globalProductCategoryId,
                                                           @RequestParam(required = false) Long id) {
        log.info("Проверка названия категории {} в глобальной категории {}.", name, globalProductCategoryId);
        boolean exists = productCategoryService.checkName(name, globalProductCategoryId, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Список всех категорий товаров.
     */
    @Transactional
    @GetMapping("/employee/warehouseManager/productCategories/allProductCategories")
    public String allProductCategories(Model model) {
        model.addAttribute("allProductCategories", productCategoryService.getAllProductCategories().stream()
                .sorted(Comparator.comparing(ProductCategoryDTO::getDisplayName))
                .toList());
        return "employee/warehouseManager/productCategories/allProductCategories";
    }

    /**
     * Форма создания категории товара.
     */
    @GetMapping("/employee/warehouseManager/productCategories/addProductCategory")
    public String addProductCategory(Model model) {
        populateDropdowns(model);
        return "employee/warehouseManager/productCategories/addProductCategory";
    }

    /**
     * Обработка создания категории товара.
     */
    @Transactional
    @PostMapping("/employee/warehouseManager/productCategories/addProductCategory")
    public String addProductCategory(@RequestParam String inputName,
                                      @RequestParam Long inputGlobalProductCategoryId,
                                      @RequestParam(required = false) List<Long> inputAttributeIds,
                                      Model model) {
        Optional<GlobalProductCategory> globalCategoryOptional =
                productCategoryService.getGlobalProductCategoryRepository().findById(inputGlobalProductCategoryId);

        if (globalCategoryOptional.isEmpty()) {
            model.addAttribute("categoryError", "Выбранная глобальная категория не найдена.");
            populateDropdowns(model);
            return "employee/warehouseManager/productCategories/addProductCategory";
        }

        ProductCategory category = new ProductCategory();
        category.setName(inputName);
        category.setGlobalProductCategory(globalCategoryOptional.get());

        if (inputAttributeIds != null && !inputAttributeIds.isEmpty()) {
            category.setAttributes(productCategoryService.getProductAttributeRepository().findAllById(inputAttributeIds));
        } else {
            category.setAttributes(new ArrayList<>());
        }

        if (!productCategoryService.saveProductCategory(category)) {
            model.addAttribute("categoryError", "Ошибка при сохранении категории товара.");
            populateDropdowns(model);
            return "employee/warehouseManager/productCategories/addProductCategory";
        }

        return "redirect:/employee/warehouseManager/productCategories/detailsProductCategory/" + category.getId();
    }

    /**
     * Детальная информация о категории товара.
     */
    @Transactional
    @GetMapping("/employee/warehouseManager/productCategories/detailsProductCategory/{id}")
    public String detailsProductCategory(@PathVariable(value = "id") long id, Model model) {
        if (!productCategoryService.getProductCategoryRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/productCategories/allProductCategories";
        }
        ProductCategory category = productCategoryService.getProductCategoryRepository().findById(id).get();
        ProductCategoryDTO categoryDTO = ProductCategoryMapper.INSTANCE.toDTO(category);

        model.addAttribute("categoryDTO", categoryDTO);
        return "employee/warehouseManager/productCategories/detailsProductCategory";
    }

    /**
     * Форма редактирования категории товара.
     */
    @Transactional
    @GetMapping("/employee/warehouseManager/productCategories/editProductCategory/{id}")
    public String editProductCategory(@PathVariable(value = "id") long id, Model model) {
        if (!productCategoryService.getProductCategoryRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/productCategories/allProductCategories";
        }
        ProductCategory category = productCategoryService.getProductCategoryRepository().findById(id).get();
        ProductCategoryDTO categoryDTO = ProductCategoryMapper.INSTANCE.toDTO(category);

        // ID текущих атрибутов для определения вновь добавленных
        List<Long> selectedAttributeIds = category.getAttributes().stream()
                .map(ProductAttribute::getId)
                .toList();

        // Типы данных всех атрибутов для модального окна
        Map<Long, String> attributeDataTypes = new HashMap<>();
        for (ProductAttribute attr : productCategoryService.getProductAttributeRepository().findAll()) {
            attributeDataTypes.put(attr.getId(), attr.getDataType().name());
        }

        model.addAttribute("categoryDTO", categoryDTO);
        model.addAttribute("selectedAttributeIds", selectedAttributeIds);
        model.addAttribute("attributeDataTypes", attributeDataTypes);
        populateDropdowns(model);
        return "employee/warehouseManager/productCategories/editProductCategory";
    }

    /**
     * Обработка редактирования категории товара.
     */
    @PostMapping("/employee/warehouseManager/productCategories/editProductCategory/{id}")
    public String editProductCategory(@PathVariable(value = "id") long id,
                                       @RequestParam String inputName,
                                       @RequestParam Long inputGlobalProductCategoryId,
                                       @RequestParam(required = false) List<Long> inputAttributeIds,
                                       HttpServletRequest request,
                                       RedirectAttributes redirectAttributes) {
        // Извлекаем значения атрибутов для существующих товаров (формат: productValue_{productId}_{attributeId})
        Map<Long, Map<Long, String>> productAttributeValues = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("productValue_") && values.length > 0 && !values[0].isBlank()) {
                String[] parts = key.substring("productValue_".length()).split("_");
                if (parts.length == 2) {
                    Long productId = Long.parseLong(parts[0]);
                    Long attributeId = Long.parseLong(parts[1]);
                    productAttributeValues
                            .computeIfAbsent(productId, k -> new HashMap<>())
                            .put(attributeId, values[0]);
                }
            }
        });

        if (!productCategoryService.editProductCategory(id, inputName, inputGlobalProductCategoryId,
                inputAttributeIds, productAttributeValues)) {
            redirectAttributes.addFlashAttribute("categoryError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/warehouseManager/productCategories/editProductCategory/" + id;
        }

        return "redirect:/employee/warehouseManager/productCategories/detailsProductCategory/" + id;
    }

    /**
     * Удаление категории товара.
     */
    @GetMapping("/employee/warehouseManager/productCategories/deleteProductCategory/{id}")
    public String deleteProductCategory(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!productCategoryService.deleteProductCategory(id)) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить категорию. Убедитесь, что у неё нет связанных товаров.");
            return "redirect:/employee/warehouseManager/productCategories/detailsProductCategory/" + id;
        }

        return "redirect:/employee/warehouseManager/productCategories/allProductCategories";
    }

    /**
     * Вспомогательный метод: заполняет model данными для dropdown и чекбоксов.
     */
    private void populateDropdowns(Model model) {
        List<GlobalProductCategoryDTO> globalCategories = GlobalProductCategoryMapper.INSTANCE
                .toDTOList(productCategoryService.getGlobalProductCategoryRepository().findAll());
        List<ProductAttributeDTO> productAttributes = ProductAttributeMapper.INSTANCE
                .toDTOList(productCategoryService.getProductAttributeRepository().findAll());

        model.addAttribute("allGlobalProductCategories", globalCategories.stream()
                .sorted(Comparator.comparing(GlobalProductCategoryDTO::getName))
                .toList());
        // Исключаем обязательные атрибуты из списка выбора (они добавляются автоматически)
        model.addAttribute("allProductAttributes", productAttributes.stream()
                .filter(attr -> !ProductCategoryService.MANDATORY_ATTRIBUTE_NAMES.contains(attr.getName()))
                .sorted(Comparator.comparing(ProductAttributeDTO::getName))
                .toList());
    }
}
