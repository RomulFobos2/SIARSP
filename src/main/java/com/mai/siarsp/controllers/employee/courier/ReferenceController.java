package com.mai.siarsp.controllers.employee.courier;

import com.mai.siarsp.models.Shelf;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.service.employee.manager.VehicleService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller("courierReferenceController")
@RequestMapping("/employee/courier/reference")
public class ReferenceController {

    private final VehicleService vehicleService;
    private final WarehouseRepository warehouseRepository;

    public ReferenceController(VehicleService vehicleService,
                               WarehouseRepository warehouseRepository) {
        this.vehicleService = vehicleService;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    @GetMapping("/allVehicles")
    public String allVehicles(Model model) {
        model.addAttribute("allVehicles", vehicleService.getAllVehicles());
        return "employee/courier/reference/allVehicles";
    }

    @Transactional(readOnly = true)
    @GetMapping("/allWarehouses")
    public String allWarehouses(Model model) {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        model.addAttribute("warehouses", warehouses);
        return "employee/courier/reference/allWarehouses";
    }

    @Transactional(readOnly = true)
    @GetMapping("/detailsWarehouse/{id}")
    public String detailsWarehouse(@PathVariable Long id, Model model) {
        Optional<Warehouse> optWarehouse = warehouseRepository.findById(id);
        if (optWarehouse.isEmpty()) {
            return "redirect:/employee/courier/reference/allWarehouses";
        }
        Warehouse warehouse = optWarehouse.get();
        model.addAttribute("warehouse", warehouse);

        double totalCapacity = 0;
        double usedVolume = 0;
        for (Shelf shelf : warehouse.getShelves()) {
            for (StorageZone zone : shelf.getStorageZones()) {
                double cap = zone.getCapacityVolume();
                totalCapacity += cap;
                double occ = zone.getOccupancyPercentage();
                usedVolume += cap * occ / 100.0;
            }
        }
        double occupancyPercent = totalCapacity > 0 ? (usedVolume / totalCapacity) * 100 : 0;
        model.addAttribute("totalCapacity", totalCapacity);
        model.addAttribute("usedVolume", usedVolume);
        model.addAttribute("occupancyPercent", occupancyPercent);

        return "employee/courier/reference/detailsWarehouse";
    }
}
