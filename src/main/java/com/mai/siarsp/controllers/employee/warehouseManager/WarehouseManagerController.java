package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.enumeration.WarehouseType;
import com.mai.siarsp.models.Shelf;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.service.employee.warehouseManager.WarehouseCreationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thymeleaf-контроллер управления складами.
 * Обрабатывает страницы списка, создания и детального просмотра складов.
 */
@Controller("warehouseManagerWarehouseController")
@RequestMapping("/employee/warehouseManager/warehouses")
@Slf4j
public class WarehouseManagerController {

    private final WarehouseCreationService creationService;
    private final WarehouseRepository warehouseRepository;

    public WarehouseManagerController(
            @Qualifier("warehouseCreationService") WarehouseCreationService creationService,
            WarehouseRepository warehouseRepository) {
        this.creationService = creationService;
        this.warehouseRepository = warehouseRepository;
    }

    // ========== СПИСОК СКЛАДОВ ==========

    @Transactional(readOnly = true)
    @GetMapping("/allWarehouses")
    public String allWarehouses(Model model) {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<WarehouseStat> stats = warehouses.stream()
                .map(this::buildWarehouseStat)
                .toList();
        model.addAttribute("warehouseStats", stats);
        return "employee/warehouseManager/warehouses/allWarehouses";
    }

    // ========== ДОБАВЛЕНИЕ СКЛАДА ==========

    @GetMapping("/addWarehouse")
    public String addWarehouseForm(Model model) {
        model.addAttribute("warehouseTypes", WarehouseType.values());
        return "employee/warehouseManager/warehouses/addWarehouse";
    }

    @PostMapping("/addWarehouse")
    public String addWarehouse(
            @RequestParam String name,
            @RequestParam WarehouseType type,
            @RequestParam String address,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam int shelfCount,
            @RequestParam int zonesPerShelf,
            @RequestParam double zoneLength,
            @RequestParam double zoneWidth,
            @RequestParam double zoneHeight,
            RedirectAttributes redirectAttributes) {

        if (shelfCount < 1 || zonesPerShelf < 1) {
            redirectAttributes.addFlashAttribute("warehouseError",
                    "Количество стеллажей и зон должно быть не менее 1");
            return "redirect:/employee/warehouseManager/warehouses/addWarehouse";
        }

        Optional<Warehouse> created = creationService.createWarehouseWithStructure(
                name, type, address, shelfCount, zonesPerShelf, zoneLength, zoneWidth, zoneHeight);

        if (created.isPresent()) {
            Warehouse wh = created.get();
            wh.setLatitude(latitude);
            wh.setLongitude(longitude);
            warehouseRepository.save(wh);
            log.info("Склад '{}' создан с ID={}", name, wh.getId());
            return "redirect:/employee/warehouseManager/warehouses/detailsWarehouse/" + wh.getId();
        } else {
            redirectAttributes.addFlashAttribute("warehouseError",
                    "Не удалось создать склад. Возможно, склад с таким названием уже существует.");
            return "redirect:/employee/warehouseManager/warehouses/addWarehouse";
        }
    }

    // ========== ДЕТАЛИ СКЛАДА ==========

    @Transactional(readOnly = true)
    @GetMapping("/detailsWarehouse/{id}")
    public String detailsWarehouse(@PathVariable Long id, Model model) {
        Optional<Warehouse> opt = warehouseRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/employee/warehouseManager/warehouses/allWarehouses";
        }
        Warehouse warehouse = opt.get();

        WarehouseStat whStat = buildWarehouseStat(warehouse);

        // Статистика по каждому стеллажу
        Map<Long, ShelfStat> shelfStats = new LinkedHashMap<>();
        for (Shelf shelf : warehouse.getShelves()) {
            shelfStats.put(shelf.getId(), buildShelfStat(shelf));
        }

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("whStat", whStat);
        model.addAttribute("shelfStats", shelfStats);
        return "employee/warehouseManager/warehouses/detailsWarehouse";
    }

    // ========== УДАЛЕНИЕ СКЛАДА ==========

    @GetMapping("/deleteWarehouse/{id}")
    public String deleteWarehouse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        boolean deleted = creationService.deleteWarehouseIfEmpty(id);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Склад успешно удалён");
        } else {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить склад: на нём хранятся товары. Сначала уберите все товары.");
        }
        return "redirect:/employee/warehouseManager/warehouses/allWarehouses";
    }

    // ========== УДАЛЕНИЕ СТЕЛЛАЖА ==========

    @GetMapping("/deleteShelf/{shelfId}")
    public String deleteShelf(@PathVariable Long shelfId,
                              @RequestParam Long warehouseId,
                              RedirectAttributes redirectAttributes) {
        boolean deleted = creationService.deleteShelfIfEmpty(shelfId);
        if (deleted) {
            redirectAttributes.addFlashAttribute("successMessage", "Стеллаж успешно удалён");
        } else {
            redirectAttributes.addFlashAttribute("deleteError",
                    "Невозможно удалить стеллаж: в его зонах хранятся товары.");
        }
        return "redirect:/employee/warehouseManager/warehouses/detailsWarehouse/" + warehouseId;
    }

    // ========== РЕДАКТИРОВАНИЕ СКЛАДА ==========

    @Transactional(readOnly = true)
    @GetMapping("/editWarehouse/{id}")
    public String editWarehouse(@PathVariable Long id, Model model) {
        Optional<Warehouse> opt = warehouseRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/employee/warehouseManager/warehouses/allWarehouses";
        }
        Warehouse warehouse = opt.get();
        int shelvesCount = warehouse.getShelves().size(); // принудительная инициализация LAZY-коллекции
        model.addAttribute("warehouse", warehouse);
        model.addAttribute("shelvesCount", shelvesCount);
        return "employee/warehouseManager/warehouses/editWarehouse";
    }

    @PostMapping("/editWarehouse/{id}")
    public String saveWarehouseAddress(@PathVariable Long id,
                                        @RequestParam String address,
                                        @RequestParam(required = false) Double latitude,
                                        @RequestParam(required = false) Double longitude,
                                        RedirectAttributes redirectAttributes) {
        if (!creationService.updateWarehouseAddress(id, address)) {
            redirectAttributes.addFlashAttribute("warehouseError", "Ошибка при обновлении склада.");
            return "redirect:/employee/warehouseManager/warehouses/editWarehouse/" + id;
        }
        Optional<Warehouse> opt = warehouseRepository.findById(id);
        if (opt.isPresent()) {
            Warehouse wh = opt.get();
            wh.setLatitude(latitude);
            wh.setLongitude(longitude);
            warehouseRepository.save(wh);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Склад успешно обновлён.");
        return "redirect:/employee/warehouseManager/warehouses/detailsWarehouse/" + id;
    }

    @PostMapping("/addShelf/{warehouseId}")
    public String addShelf(@PathVariable Long warehouseId,
                            @RequestParam int zonesPerShelf,
                            @RequestParam double zoneLength,
                            @RequestParam double zoneWidth,
                            @RequestParam double zoneHeight,
                            RedirectAttributes redirectAttributes) {
        if (zonesPerShelf < 1) {
            redirectAttributes.addFlashAttribute("warehouseError", "Количество зон должно быть не менее 1.");
            return "redirect:/employee/warehouseManager/warehouses/editWarehouse/" + warehouseId;
        }
        if (!creationService.addShelfToWarehouse(warehouseId, zonesPerShelf, zoneLength, zoneWidth, zoneHeight)) {
            redirectAttributes.addFlashAttribute("warehouseError", "Ошибка при добавлении стеллажа.");
            return "redirect:/employee/warehouseManager/warehouses/editWarehouse/" + warehouseId;
        }
        redirectAttributes.addFlashAttribute("successMessage", "Стеллаж успешно добавлен.");
        return "redirect:/employee/warehouseManager/warehouses/detailsWarehouse/" + warehouseId;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private WarehouseStat buildWarehouseStat(Warehouse wh) {
        int shelfCount = wh.getShelves().size();
        int zoneCount = wh.getShelves().stream()
                .mapToInt(s -> s.getStorageZones().size())
                .sum();
        double totalCapacity = wh.getShelves().stream()
                .flatMap(s -> s.getStorageZones().stream())
                .mapToDouble(StorageZone::getCapacityVolume)
                .sum();
        double usedVolume = wh.getShelves().stream()
                .flatMap(s -> s.getStorageZones().stream())
                .flatMap(z -> z.getProducts().stream())
                .mapToDouble(ZoneProduct::getTotalVolume)
                .sum();
        double occupancy = totalCapacity > 0 ? (usedVolume / totalCapacity) * 100.0 : 0.0;
        return new WarehouseStat(wh, shelfCount, zoneCount, totalCapacity, usedVolume, occupancy);
    }

    private ShelfStat buildShelfStat(Shelf shelf) {
        int zoneCount = shelf.getStorageZones().size();
        double capacity = shelf.getStorageZones().stream()
                .mapToDouble(StorageZone::getCapacityVolume)
                .sum();
        double usedVolume = shelf.getStorageZones().stream()
                .flatMap(z -> z.getProducts().stream())
                .mapToDouble(ZoneProduct::getTotalVolume)
                .sum();
        double occupancy = capacity > 0 ? (usedVolume / capacity) * 100.0 : 0.0;
        return new ShelfStat(shelf, zoneCount, capacity, usedVolume, occupancy);
    }

    // ========== ВЛОЖЕННЫЕ ЗАПИСИ ==========

    /**
     * Статистика по складу для страницы списка
     */
    public record WarehouseStat(
            Warehouse warehouse,
            int shelfCount,
            int zoneCount,
            double totalCapacity,
            double usedVolume,
            double occupancyPercent
    ) {
    }

    /**
     * Статистика по стеллажу для страницы деталей склада
     */
    public record ShelfStat(
            Shelf shelf,
            int zoneCount,
            double capacity,
            double usedVolume,
            double occupancyPercent
    ) {
    }
}
