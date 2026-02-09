package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.GlobalProductCategoryDTO;
import com.mai.siarsp.mapper.GlobalProductCategoryMapper;
import com.mai.siarsp.models.GlobalProductCategory;
import com.mai.siarsp.service.employee.warehouseManager.GlobalProductCategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления глобальными категориями товаров.
 *
 * Доступен для роли ROLE_EMPLOYEE_WAREHOUSE_MANAGER.
 * Базовый URL: /employee/warehouseManager/globalProductCategories/
 */
@Controller
@Slf4j
public class GlobalProductCategoryController {

    private final GlobalProductCategoryService globalProductCategoryService;

    public GlobalProductCategoryController(GlobalProductCategoryService globalProductCategoryService) {
        this.globalProductCategoryService = globalProductCategoryService;
    }

    /**
     * AJAX-проверка уникальности названия глобальной категории.
     */
    @GetMapping("/employee/warehouseManager/globalProductCategories/check-name")
    public ResponseEntity<Map<String, Boolean>> checkName(@RequestParam String name,
                                                           @RequestParam(required = false) Long id) {
        log.info("Проверка названия глобальной категории {}.", name);
        boolean exists = globalProductCategoryService.checkName(name, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Список всех глобальных категорий.
     */
    @GetMapping("/employee/warehouseManager/globalProductCategories/allGlobalProductCategories")
    public String allGlobalProductCategories(Model model) {
        model.addAttribute("allGlobalProductCategories", globalProductCategoryService.getAllGlobalProductCategories().stream()
                .sorted(Comparator.comparing(GlobalProductCategoryDTO::getName))
                .toList());
        return "employee/warehouseManager/globalProductCategories/allGlobalProductCategories";
    }

    /**
     * Форма создания глобальной категории.
     */
    @GetMapping("/employee/warehouseManager/globalProductCategories/addGlobalProductCategory")
    public String addGlobalProductCategory() {
        return "employee/warehouseManager/globalProductCategories/addGlobalProductCategory";
    }

    /**
     * Обработка создания глобальной категории.
     */
    @PostMapping("/employee/warehouseManager/globalProductCategories/addGlobalProductCategory")
    public String addGlobalProductCategory(@RequestParam String inputName,
                                            Model model) {
        GlobalProductCategory category = new GlobalProductCategory();
        category.setName(inputName);

        if (!globalProductCategoryService.saveGlobalProductCategory(category)) {
            model.addAttribute("categoryError", "Ошибка при сохранении глобальной категории.");
            return "employee/warehouseManager/globalProductCategories/addGlobalProductCategory";
        }

        return "redirect:/employee/warehouseManager/globalProductCategories/detailsGlobalProductCategory/" + category.getId();
    }

    /**
     * Детальная информация о глобальной категории.
     */
    @GetMapping("/employee/warehouseManager/globalProductCategories/detailsGlobalProductCategory/{id}")
    public String detailsGlobalProductCategory(@PathVariable(value = "id") long id, Model model) {
        if (!globalProductCategoryService.getGlobalProductCategoryRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/globalProductCategories/allGlobalProductCategories";
        }
        GlobalProductCategory category = globalProductCategoryService.getGlobalProductCategoryRepository().findById(id).get();
        GlobalProductCategoryDTO categoryDTO = GlobalProductCategoryMapper.INSTANCE.toDTO(category);

        model.addAttribute("categoryDTO", categoryDTO);
        return "employee/warehouseManager/globalProductCategories/detailsGlobalProductCategory";
    }

    /**
     * Форма редактирования глобальной категории.
     */
    @GetMapping("/employee/warehouseManager/globalProductCategories/editGlobalProductCategory/{id}")
    public String editGlobalProductCategory(@PathVariable(value = "id") long id, Model model) {
        if (!globalProductCategoryService.getGlobalProductCategoryRepository().existsById(id)) {
            return "redirect:/employee/warehouseManager/globalProductCategories/allGlobalProductCategories";
        }
        GlobalProductCategory category = globalProductCategoryService.getGlobalProductCategoryRepository().findById(id).get();
        GlobalProductCategoryDTO categoryDTO = GlobalProductCategoryMapper.INSTANCE.toDTO(category);

        model.addAttribute("categoryDTO", categoryDTO);
        return "employee/warehouseManager/globalProductCategories/editGlobalProductCategory";
    }

    /**
     * Обработка редактирования глобальной категории.
     */
    @PostMapping("/employee/warehouseManager/globalProductCategories/editGlobalProductCategory/{id}")
    public String editGlobalProductCategory(@PathVariable(value = "id") long id,
                                             @RequestParam String inputName,
                                             RedirectAttributes redirectAttributes) {
        if (!globalProductCategoryService.editGlobalProductCategory(id, inputName)) {
            redirectAttributes.addFlashAttribute("categoryError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/warehouseManager/globalProductCategories/editGlobalProductCategory/" + id;
        }

        return "redirect:/employee/warehouseManager/globalProductCategories/detailsGlobalProductCategory/" + id;
    }

    /**
     * Удаление глобальной категории.
     */
    @GetMapping("/employee/warehouseManager/globalProductCategories/deleteGlobalProductCategory/{id}")
    public String deleteGlobalProductCategory(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!globalProductCategoryService.deleteGlobalProductCategory(id)) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить глобальную категорию. Убедитесь, что у неё нет связанных подкатегорий.");
            return "redirect:/employee/warehouseManager/globalProductCategories/detailsGlobalProductCategory/" + id;
        }

        return "redirect:/employee/warehouseManager/globalProductCategories/allGlobalProductCategories";
    }
}
