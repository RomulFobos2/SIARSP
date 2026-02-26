package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.WarehouseEquipmentDTO;
import com.mai.siarsp.enumeration.EquipmentStatus;
import com.mai.siarsp.models.EquipmentType;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.service.employee.WarehouseEquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Контроллер оборудования склада для руководителя (EMPLOYEE_MANAGER).
 * Позволяет просматривать, редактировать (включая статус) и удалять оборудование.
 */
@Controller("managerEquipmentController")
@RequestMapping("/employee/manager/equipments")
@Slf4j
public class EquipmentController {

    private final WarehouseEquipmentService equipmentService;
    private final WarehouseRepository warehouseRepository;

    public EquipmentController(WarehouseEquipmentService equipmentService,
                                WarehouseRepository warehouseRepository) {
        this.equipmentService = equipmentService;
        this.warehouseRepository = warehouseRepository;
    }

    @GetMapping("/allEquipment")
    public String allEquipment(Model model) {
        List<WarehouseEquipmentDTO> equipment = equipmentService.getAllEquipment();
        model.addAttribute("equipment", equipment);
        return "employee/manager/equipment/allEquipment";
    }

    @GetMapping("/detailsEquipment/{id}")
    public String detailsEquipment(@PathVariable Long id, Model model) {
        Optional<WarehouseEquipmentDTO> dto = equipmentService.getById(id);
        if (dto.isEmpty()) {
            return "redirect:/employee/manager/equipments/allEquipment";
        }
        model.addAttribute("equipment", dto.get());
        return "employee/manager/equipment/detailsEquipment";
    }

    @GetMapping("/editEquipment/{id}")
    public String editEquipmentForm(@PathVariable Long id, Model model) {
        Optional<WarehouseEquipmentDTO> dto = equipmentService.getById(id);
        if (dto.isEmpty()) {
            return "redirect:/employee/manager/equipments/allEquipment";
        }
        List<EquipmentType> types = equipmentService.getAllTypeEntities();
        List<Warehouse> warehouses = warehouseRepository.findAll();
        model.addAttribute("equipment", dto.get());
        model.addAttribute("types", types);
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("statuses", EquipmentStatus.values());
        return "employee/manager/equipment/editEquipment";
    }

    @PostMapping("/editEquipment/{id}")
    public String editEquipment(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam(required = false) String serialNumber,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate productionDate,
                                 @RequestParam(required = false) Integer usefulLifeYears,
                                 @RequestParam Long equipmentTypeId,
                                 @RequestParam Long warehouseId,
                                 @RequestParam EquipmentStatus status,
                                 RedirectAttributes redirectAttributes) {
        boolean success = equipmentService.updateEquipment(
                id, name, serialNumber, productionDate, usefulLifeYears,
                equipmentTypeId, warehouseId, status);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Оборудование успешно обновлено.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при обновлении оборудования. Возможно, оборудование с таким именем уже существует на складе.");
        }
        return "redirect:/employee/manager/equipments/detailsEquipment/" + id;
    }

    @GetMapping("/deleteEquipment/{id}")
    public String deleteEquipment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean success = equipmentService.deleteEquipment(id);
        if (success) {
            redirectAttributes.addFlashAttribute("successMessage", "Оборудование успешно удалено.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении оборудования.");
        }
        return "redirect:/employee/manager/equipments/allEquipment";
    }
}
