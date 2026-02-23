package com.mai.siarsp.controllers.employee.admin;

import com.mai.siarsp.dto.EquipmentTypeDTO;
import com.mai.siarsp.service.employee.WarehouseEquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер справочника типов оборудования для администратора (EMPLOYEE_ADMIN).
 * Полный CRUD для управления типами оборудования склада.
 */
@Controller
@RequestMapping("/employee/admin/equipmentTypes")
@Slf4j
public class EquipmentTypeController {

    private final WarehouseEquipmentService equipmentService;

    public EquipmentTypeController(WarehouseEquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/all")
    public String allEquipmentTypes(Model model) {
        List<EquipmentTypeDTO> types = equipmentService.getAllTypes();
        model.addAttribute("types", types);
        return "employee/admin/equipmentTypes/allEquipmentTypes";
    }

    @GetMapping("/create")
    public String createEquipmentTypeForm() {
        return "employee/admin/equipmentTypes/createEquipmentType";
    }

    @PostMapping("/create")
    public String createEquipmentType(@RequestParam String name,
                                       RedirectAttributes redirectAttributes) {
        boolean success = equipmentService.createEquipmentType(name.trim());
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тип оборудования «" + name.trim() + "» успешно создан.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Тип оборудования с таким названием уже существует.");
        }
        return "redirect:/employee/admin/equipmentTypes/all";
    }

    @GetMapping("/edit/{id}")
    public String editEquipmentTypeForm(@PathVariable Long id, Model model) {
        Optional<EquipmentTypeDTO> dto = equipmentService.getTypeById(id);
        if (dto.isEmpty()) {
            return "redirect:/employee/admin/equipmentTypes/all";
        }
        model.addAttribute("type", dto.get());
        return "employee/admin/equipmentTypes/editEquipmentType";
    }

    @PostMapping("/edit/{id}")
    public String editEquipmentType(@PathVariable Long id,
                                     @RequestParam String name,
                                     RedirectAttributes redirectAttributes) {
        boolean success = equipmentService.updateEquipmentType(id, name.trim());
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Тип оборудования успешно обновлён.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка: тип оборудования с таким названием уже существует.");
        }
        return "redirect:/employee/admin/equipmentTypes/all";
    }

    @PostMapping("/delete/{id}")
    public String deleteEquipmentType(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean success = equipmentService.deleteEquipmentType(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Тип оборудования успешно удалён.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при удалении типа. Возможно, он используется оборудованием.");
        }
        return "redirect:/employee/admin/equipmentTypes/all";
    }
}
