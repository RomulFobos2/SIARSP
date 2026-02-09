package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.VehicleDTO;
import com.mai.siarsp.enumeration.VehicleStatus;
import com.mai.siarsp.enumeration.VehicleType;
import com.mai.siarsp.mapper.VehicleMapper;
import com.mai.siarsp.models.Vehicle;
import com.mai.siarsp.service.employee.manager.VehicleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Контроллер для управления транспортными средствами (Vehicle)
 *
 * Предоставляет веб-интерфейс для CRUD-операций с ТС:
 * - Просмотр списка всех ТС
 * - Добавление нового ТС
 * - Просмотр деталей ТС
 * - Редактирование ТС
 * - Удаление ТС
 * - AJAX-валидация гос. номера
 *
 * Доступ: ROLE_EMPLOYEE_MANAGER
 * URL-префикс: /employee/manager/vehicles/
 */
@Controller
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * AJAX-эндпоинт для проверки уникальности гос. номера
     *
     * @param regNumber гос. номер для проверки
     * @param id ID текущего ТС (для исключения при редактировании)
     * @return JSON {"exists": true/false}
     */
    @GetMapping("/employee/manager/vehicles/check-regNumber")
    public ResponseEntity<Map<String, Boolean>> checkRegNumber(@RequestParam String regNumber,
                                                                @RequestParam(required = false) Long id) {
        log.info("Проверка гос. номера ТС {}.", regNumber);
        boolean exists = vehicleService.checkRegistrationNumber(regNumber, id);
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }

    /**
     * Страница списка всех транспортных средств
     */
    @Transactional
    @GetMapping("/employee/manager/vehicles/allVehicles")
    public String allVehicles(Model model) {
        model.addAttribute("allVehicles", vehicleService.getAllVehicles().stream()
                .sorted(Comparator.comparing(VehicleDTO::getRegistrationNumber))
                .toList());
        return "employee/manager/vehicles/allVehicles";
    }

    /**
     * Форма добавления нового ТС (GET)
     */
    @GetMapping("/employee/manager/vehicles/addVehicle")
    public String addVehicle(Model model) {
        model.addAttribute("vehicleTypes", VehicleType.values());
        return "employee/manager/vehicles/addVehicle";
    }

    /**
     * Обработка формы добавления ТС (POST)
     * Статус устанавливается автоматически: AVAILABLE
     */
    @PostMapping("/employee/manager/vehicles/addVehicle")
    public String addVehicle(@RequestParam String inputRegistrationNumber,
                             @RequestParam String inputBrand,
                             @RequestParam String inputModel,
                             @RequestParam(required = false) Integer inputYear,
                             @RequestParam(required = false) String inputVin,
                             @RequestParam Double inputLoadCapacity,
                             @RequestParam Double inputVolumeCapacity,
                             @RequestParam VehicleType inputType,
                             Model model) {
        Vehicle vehicle = new Vehicle(inputRegistrationNumber, inputBrand, inputModel,
                inputLoadCapacity, inputVolumeCapacity, inputType);
        vehicle.setYear(inputYear);
        vehicle.setVin(inputVin != null ? inputVin : "");

        if (!vehicleService.saveVehicle(vehicle)) {
            model.addAttribute("vehicleError", "Ошибка при сохранении транспортного средства.");
            model.addAttribute("vehicleTypes", VehicleType.values());
            return "employee/manager/vehicles/addVehicle";
        }

        return "redirect:/employee/manager/vehicles/detailsVehicle/" + vehicle.getId();
    }

    /**
     * Страница деталей ТС
     */
    @Transactional
    @GetMapping("/employee/manager/vehicles/detailsVehicle/{id}")
    public String detailsVehicle(@PathVariable(value = "id") long id, Model model) {
        if (!vehicleService.getVehicleRepository().existsById(id)) {
            return "redirect:/employee/manager/vehicles/allVehicles";
        }
        Vehicle vehicle = vehicleService.getVehicleRepository().findById(id).get();
        VehicleDTO vehicleDTO = VehicleMapper.INSTANCE.toDTO(vehicle);

        model.addAttribute("vehicleDTO", vehicleDTO);
        return "employee/manager/vehicles/detailsVehicle";
    }

    /**
     * Форма редактирования ТС (GET)
     */
    @Transactional
    @GetMapping("/employee/manager/vehicles/editVehicle/{id}")
    public String editVehicle(@PathVariable(value = "id") long id, Model model) {
        if (!vehicleService.getVehicleRepository().existsById(id)) {
            return "redirect:/employee/manager/vehicles/allVehicles";
        }
        Vehicle vehicle = vehicleService.getVehicleRepository().findById(id).get();
        VehicleDTO vehicleDTO = VehicleMapper.INSTANCE.toDTO(vehicle);

        model.addAttribute("vehicleDTO", vehicleDTO);
        model.addAttribute("vehicleTypes", VehicleType.values());
        model.addAttribute("vehicleStatuses", VehicleStatus.values());
        return "employee/manager/vehicles/editVehicle";
    }

    /**
     * Обработка формы редактирования ТС (POST)
     */
    @PostMapping("/employee/manager/vehicles/editVehicle/{id}")
    public String editVehicle(@PathVariable(value = "id") long id,
                              @RequestParam String inputRegistrationNumber,
                              @RequestParam String inputBrand,
                              @RequestParam String inputModel,
                              @RequestParam(required = false) Integer inputYear,
                              @RequestParam(required = false) String inputVin,
                              @RequestParam Double inputLoadCapacity,
                              @RequestParam Double inputVolumeCapacity,
                              @RequestParam VehicleType inputType,
                              @RequestParam VehicleStatus inputStatus,
                              RedirectAttributes redirectAttributes) {
        if (!vehicleService.editVehicle(id, inputRegistrationNumber, inputBrand, inputModel,
                inputYear, inputVin, inputLoadCapacity, inputVolumeCapacity, inputType, inputStatus)) {
            redirectAttributes.addFlashAttribute("vehicleError", "Ошибка при сохранении изменений.");
            return "redirect:/employee/manager/vehicles/editVehicle/" + id;
        }

        return "redirect:/employee/manager/vehicles/detailsVehicle/" + id;
    }

    /**
     * Удаление ТС
     */
    @GetMapping("/employee/manager/vehicles/deleteVehicle/{id}")
    public String deleteVehicle(@PathVariable(value = "id") long id, RedirectAttributes redirectAttributes) {
        if (!vehicleService.deleteVehicle(id)) {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить ТС. Убедитесь, что у него нет связанных задач на доставку.");
            return "redirect:/employee/manager/vehicles/detailsVehicle/" + id;
        }

        return "redirect:/employee/manager/vehicles/allVehicles";
    }
}
