package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.WarehouseEquipmentDTO;
import com.mai.siarsp.service.employee.WarehouseEquipmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Контроллер оборудования склада для руководителя (EMPLOYEE_MANAGER).
 * Только просмотр списка и деталей оборудования.
 */
@Controller("managerEquipmentController")
@RequestMapping("/employee/manager/equipments")
@Slf4j
public class EquipmentController {

    private final WarehouseEquipmentService equipmentService;

    public EquipmentController(WarehouseEquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping("/allEquipment")
    public String allEquipment(Model model) {
        List<WarehouseEquipmentDTO> equipment = equipmentService.getAllEquipment();
        model.addAttribute("equipment", equipment);
        return "employee/manager/equipments/allEquipment";
    }

    @GetMapping("/detailsEquipment/{id}")
    public String detailsEquipment(@PathVariable Long id, Model model) {
        Optional<WarehouseEquipmentDTO> dto = equipmentService.getById(id);
        if (dto.isEmpty()) {
            return "redirect:/employee/manager/equipments/allEquipment";
        }
        model.addAttribute("equipment", dto.get());
        return "employee/manager/equipments/detailsEquipment";
    }
}
