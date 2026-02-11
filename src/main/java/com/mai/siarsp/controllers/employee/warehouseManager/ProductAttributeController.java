package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.ProductAttributeDTO;
import com.mai.siarsp.dto.ProductCategoryDTO;
import com.mai.siarsp.enumeration.AttributeType;
import com.mai.siarsp.mapper.ProductAttributeMapper;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.ProductAttribute;
import com.mai.siarsp.service.employee.warehouseManager.ProductAttributeService;
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
 * Контроллер для управления атрибутами (характеристиками) товаров
 *
 * Предоставляет веб-интерфейс для CRUD-операций с атрибутами:
 * - Просмотр списка всех атрибутов
 * - Добавление нового атрибута с выбором категорий (чекбоксы)
 * - Просмотр деталей атрибута
 * - Редактирование атрибута с пересинхронизацией категорий
 * - Удаление атрибута
 * - AJAX-валидация названия
 *
 * Доступ: ROLE_EMPLOYEE_WAREHOUSE_MANAGER
 * URL-префикс: /employee/warehouseManager/productAttributes/
 */
@Controller
@Slf4j
public class ProductAttributeController {

    private final ProductAttributeService productAttributeService;

    public ProductAttributeController(ProductAttributeService productAttributeService) {
        this.productAttributeService = productAttributeService;
    }

    /**
     * AJAX-эндпоинт для проверки уникальности названия атрибута
     */
    @GetMapping("/employee/warehouseManager/productAttributes/check-name")
    public ResponseEntity<Map<String, Boolean>> checkName(@RequestParam String name,
                                                           @RequestParam(required = false) Long id) {
        log.info("Проверка названия атрибута '{}'.", name);
        boolean exists = productAttributeService.checkName(name, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * AJAX-эндпоинт: возвращает список товаров в указанных категориях.
     * Используется для модального окна при создании атрибута.
     */
    @GetMapping("/employee/warehouseManager/productAttributes/products-by-categories")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getProductsByCategories(
            @RequestParam List<Long> categoryIds) {
        List<Product> products = productAttributeService.getProductRepository()
                .findAllByCategoryIdIn(categoryIds);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("article", p.getArticle());
            item.put("categoryName", p.getCategory().getName());
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Страница списка всех атрибутов
     */
    @Transactional
    @GetMapping("/employee/warehouseManager/productAttributes/allProductAttributes")
    public String allProductAttributes(Model model) {
        model.addAttribute("allProductAttributes", productAttributeService.getAllProductAttributes().stream()
                .sorted(Comparator.comparing(ProductAttributeDTO::getName))
                .toList());

        // Для отображения количества категорий нужно обратиться к entity напрямую
        List<ProductAttribute> attributes = productAttributeService.getProductAttributeRepository().findAll();
        Map<Long, Integer> categoryCounts = new HashMap<>();
        for (ProductAttribute attr : attributes) {
            categoryCounts.put(attr.getId(), attr.getCategories().size());
        }
        model.addAttribute("categoryCounts", categoryCounts);

        return "employee/warehouseManager/productAttributes/allProductAttributes";
    }

    /**
     * Форма добавления нового атрибута (GET)
     */
    @GetMapping("/employee/warehouseManager/productAttributes/addProductAttribute")
    public String addProductAttribute(Model model) {
        populateDropdowns(model);
        return "employee/warehouseManager/productAttributes/addProductAttribute";
    }

    /**
     * Обработка формы добавления атрибута (POST).
     *
     * Если в выбранных категориях есть существующие товары, из формы также приходят
     * параметры productValue_{productId} с значениями нового атрибута для каждого товара.
     */
    @PostMapping("/employee/warehouseManager/productAttributes/addProductAttribute")
    public String addProductAttribute(@RequestParam String inputName,
                                       @RequestParam(required = false) String inputUnit,
                                       @RequestParam AttributeType inputDataType,
                                       @RequestParam(required = false) List<Long> inputCategoryIds,
                                       HttpServletRequest request,
                                       Model model) {
        ProductAttribute attribute = new ProductAttribute(inputName,
                inputUnit != null ? inputUnit : "",
                inputDataType,
                new java.util.ArrayList<>());

        // Извлекаем значения атрибутов для существующих товаров из параметров формы
        Map<Long, String> productValues = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("productValue_") && values.length > 0 && !values[0].isBlank()) {
                Long productId = Long.parseLong(key.substring("productValue_".length()));
                productValues.put(productId, values[0]);
            }
        });

        boolean success;
        if (!productValues.isEmpty()) {
            success = productAttributeService.saveProductAttributeWithValues(attribute, inputCategoryIds, productValues);
        } else {
            success = productAttributeService.saveProductAttribute(attribute, inputCategoryIds);
        }

        if (!success) {
            model.addAttribute("attributeError", "Ошибка при сохранении атрибута.");
            populateDropdowns(model);
            return "employee/warehouseManager/productAttributes/addProductAttribute";
        }

        return "redirect:/employee/warehouseManager/productAttributes/detailsProductAttribute/" + attribute.getId();
    }

    /**
     * Страница деталей атрибута
     */
    @Transactional
    @GetMapping("/employee/warehouseManager/productAttributes/detailsProductAttribute/{id}")
    public String detailsProductAttribute(@PathVariable(value = "id") long id, Model model) {
        if (!productAttributeService.getProductAttributeRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/productAttributes/allProductAttributes";
        }
        ProductAttribute attribute = productAttributeService.getProductAttributeRepository().findById(id).get();
        ProductAttributeDTO attributeDTO = ProductAttributeMapper.INSTANCE.toDTO(attribute);

        // Передаём список связанных категорий отдельно (DTO не содержит categories)
        // Ручной маппинг — избегаем ProductCategoryMapper, который тянет lazy attributes
        List<ProductCategoryDTO> linkedCategories = attribute.getCategories().stream()
                .map(cat -> {
                    ProductCategoryDTO dto = new ProductCategoryDTO();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    return dto;
                })
                .sorted(Comparator.comparing(ProductCategoryDTO::getName))
                .toList();

        model.addAttribute("attributeDTO", attributeDTO);
        model.addAttribute("linkedCategories", linkedCategories);
        return "employee/warehouseManager/productAttributes/detailsProductAttribute";
    }

    /**
     * Форма редактирования атрибута (GET)
     */
    @Transactional
    @GetMapping("/employee/warehouseManager/productAttributes/editProductAttribute/{id}")
    public String editProductAttribute(@PathVariable(value = "id") long id, Model model) {
        if (!productAttributeService.getProductAttributeRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/productAttributes/allProductAttributes";
        }
        ProductAttribute attribute = productAttributeService.getProductAttributeRepository().findById(id).get();
        ProductAttributeDTO attributeDTO = ProductAttributeMapper.INSTANCE.toDTO(attribute);

        // ID текущих категорий для предотмечивания чекбоксов
        List<Long> selectedCategoryIds = attribute.getCategories().stream()
                .map(cat -> cat.getId())
                .toList();

        model.addAttribute("attributeDTO", attributeDTO);
        model.addAttribute("selectedCategoryIds", selectedCategoryIds);
        populateDropdowns(model);
        return "employee/warehouseManager/productAttributes/editProductAttribute";
    }

    /**
     * Обработка формы редактирования атрибута (POST).
     *
     * Если при редактировании добавлены новые категории, содержащие товары,
     * из формы также приходят параметры productValue_{productId} с значениями
     * атрибута для каждого товара из новых категорий.
     */
    @PostMapping("/employee/warehouseManager/productAttributes/editProductAttribute/{id}")
    public String editProductAttribute(@PathVariable(value = "id") long id,
                                        @RequestParam String inputName,
                                        @RequestParam(required = false) String inputUnit,
                                        @RequestParam AttributeType inputDataType,
                                        @RequestParam(required = false) List<Long> inputCategoryIds,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        // Извлекаем значения атрибутов для товаров из новых категорий
        Map<Long, String> productValues = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (key.startsWith("productValue_") && values.length > 0 && !values[0].isBlank()) {
                Long productId = Long.parseLong(key.substring("productValue_".length()));
                productValues.put(productId, values[0]);
            }
        });

        if (!productAttributeService.editProductAttribute(id, inputName, inputUnit, inputDataType, inputCategoryIds, productValues)) {
            redirectAttributes.addFlashAttribute("attributeError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/warehouseManager/productAttributes/editProductAttribute/" + id;
        }

        return "redirect:/employee/warehouseManager/productAttributes/detailsProductAttribute/" + id;
    }

    /**
     * Удаление атрибута
     */
    @GetMapping("/employee/warehouseManager/productAttributes/deleteProductAttribute/{id}")
    public String deleteProductAttribute(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!productAttributeService.deleteProductAttribute(id)) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить атрибут. Убедитесь, что у него нет связанных значений товаров.");
            return "redirect:/employee/warehouseManager/productAttributes/detailsProductAttribute/" + id;
        }

        return "redirect:/employee/warehouseManager/productAttributes/allProductAttributes";
    }

    /**
     * Заполняет справочники для dropdown и чекбоксов
     */
    private void populateDropdowns(Model model) {
        List<ProductCategoryDTO> allCategories = productAttributeService.getProductCategoryRepository().findAll()
                .stream()
                .map(cat -> {
                    ProductCategoryDTO dto = new ProductCategoryDTO();
                    dto.setId(cat.getId());
                    dto.setName(cat.getName());
                    return dto;
                })
                .sorted(Comparator.comparing(ProductCategoryDTO::getName))
                .toList();
        model.addAttribute("allProductCategories", allCategories);
        model.addAttribute("attributeTypes", AttributeType.values());
    }
}
