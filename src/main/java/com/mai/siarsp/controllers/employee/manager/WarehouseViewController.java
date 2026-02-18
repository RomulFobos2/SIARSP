package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.models.Shelf;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Read-only контроллер просмотра складов для директора (MANAGER).
 * Аналогичен WarehouseManagerController, но без операций создания/удаления.
 */
@Controller("managerWarehouseViewController")
@RequestMapping("/employee/manager/warehouses")
@Slf4j
public class WarehouseViewController {

    private final WarehouseRepository warehouseRepository;

    public WarehouseViewController(WarehouseRepository warehouseRepository) {
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
        return "employee/manager/warehouses/allWarehouses";
    }

    // ========== ДЕТАЛИ СКЛАДА ==========

    @Transactional(readOnly = true)
    @GetMapping("/detailsWarehouse/{id}")
    public String detailsWarehouse(@PathVariable Long id, Model model) {
        Optional<Warehouse> opt = warehouseRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/employee/manager/warehouses/allWarehouses";
        }
        Warehouse warehouse = opt.get();

        WarehouseStat whStat = buildWarehouseStat(warehouse);

        Map<Long, ShelfStat> shelfStats = new LinkedHashMap<>();
        for (Shelf shelf : warehouse.getShelves()) {
            shelfStats.put(shelf.getId(), buildShelfStat(shelf));
        }

        model.addAttribute("warehouse", warehouse);
        model.addAttribute("whStat", whStat);
        model.addAttribute("shelfStats", shelfStats);
        return "employee/manager/warehouses/detailsWarehouse";
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

    public record WarehouseStat(
            Warehouse warehouse,
            int shelfCount,
            int zoneCount,
            double totalCapacity,
            double usedVolume,
            double occupancyPercent
    ) {
    }

    public record ShelfStat(
            Shelf shelf,
            int zoneCount,
            double capacity,
            double usedVolume,
            double occupancyPercent
    ) {
    }
}
