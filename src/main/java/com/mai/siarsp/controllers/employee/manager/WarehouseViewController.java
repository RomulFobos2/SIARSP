package com.mai.siarsp.controllers.employee.manager;

import com.mai.siarsp.dto.DetailedWarehouseStatistics;
import com.mai.siarsp.dto.ZoneUtilization;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.Shelf;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.ClientOrderRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.service.employee.warehouseManager.WarehouseManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    private final ClientOrderRepository clientOrderRepository;
    private final WarehouseManagementService managementService;

    public WarehouseViewController(WarehouseRepository warehouseRepository,
                                   ClientOrderRepository clientOrderRepository,
                                   @Qualifier("warehouseManagementService") WarehouseManagementService managementService) {
        this.warehouseRepository = warehouseRepository;
        this.clientOrderRepository = clientOrderRepository;
        this.managementService = managementService;
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

    // ========== АНАЛИТИКА ==========

    @Transactional(readOnly = true)
    @GetMapping("/analytics/{warehouseId}")
    public String analyticsPage(@PathVariable Long warehouseId,
                                @RequestParam(required = false) LocalDate startDate,
                                @RequestParam(required = false) LocalDate endDate,
                                Model model) {
        Optional<Warehouse> opt = warehouseRepository.findById(warehouseId);
        if (opt.isEmpty()) {
            return "redirect:/employee/manager/warehouses/allWarehouses";
        }

        Optional<DetailedWarehouseStatistics> stats = managementService.getDetailedStatistics(warehouseId);
        if (stats.isEmpty()) {
            return "redirect:/employee/manager/warehouses/allWarehouses";
        }

        LocalDate defaultEndDate = LocalDate.now();
        LocalDate defaultStartDate = defaultEndDate.minusMonths(5).withDayOfMonth(1);

        LocalDate normalizedStartDate = startDate != null ? startDate : defaultStartDate;
        LocalDate normalizedEndDate = endDate != null ? endDate : defaultEndDate;

        if (normalizedStartDate.isAfter(normalizedEndDate)) {
            LocalDate temp = normalizedStartDate;
            normalizedStartDate = normalizedEndDate;
            normalizedEndDate = temp;
        }

        List<ZoneUtilization> underutilizedZones = managementService.getUnderutilizedZones(warehouseId, 50.0);
        AnalyticsChartsData chartsData = buildAnalyticsChartsData(opt.get(), normalizedStartDate, normalizedEndDate);

        model.addAttribute("warehouse", opt.get());
        model.addAttribute("stats", stats.get());
        model.addAttribute("underutilizedZones", underutilizedZones);
        model.addAttribute("chartsData", chartsData);
        model.addAttribute("startDate", normalizedStartDate);
        model.addAttribute("endDate", normalizedEndDate);
        return "employee/manager/warehouses/analytics";
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

    private AnalyticsChartsData buildAnalyticsChartsData(Warehouse warehouse, LocalDate startDate, LocalDate endDate) {
        List<ClientOrder> allOrders = clientOrderRepository.findAllByOrderByOrderDateDesc();

        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM.yyyy");
        Map<YearMonth, Integer> ordersByMonth = new LinkedHashMap<>();
        Map<YearMonth, BigDecimal> revenueByMonth = new LinkedHashMap<>();

        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth endMonth = YearMonth.from(endDate);
        for (YearMonth month = startMonth; !month.isAfter(endMonth); month = month.plusMonths(1)) {
            ordersByMonth.put(month, 0);
            revenueByMonth.put(month, BigDecimal.ZERO);
        }

        for (ClientOrder order : allOrders) {
            LocalDate orderLocalDate = order.getOrderDate().toLocalDate();
            if (orderLocalDate.isBefore(startDate) || orderLocalDate.isAfter(endDate)) {
                continue;
            }
            YearMonth orderMonth = YearMonth.from(order.getOrderDate());
            if (ordersByMonth.containsKey(orderMonth)) {
                ordersByMonth.put(orderMonth, ordersByMonth.get(orderMonth) + 1);
                BigDecimal orderAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
                revenueByMonth.put(orderMonth, revenueByMonth.get(orderMonth).add(orderAmount));
            }
        }

        List<String> periodLabels = ordersByMonth.keySet().stream()
                .map(monthFormatter::format)
                .toList();

        List<Integer> orderDynamics = new ArrayList<>(ordersByMonth.values());
        List<BigDecimal> revenueDynamics = new ArrayList<>(revenueByMonth.values());

        List<String> shelfLabels = warehouse.getShelves().stream()
                .map(shelf -> "Стеллаж " + shelf.getCode())
                .toList();

        List<Double> shelfOccupancy = warehouse.getShelves().stream()
                .map(shelf -> {
                    double capacity = shelf.getStorageZones().stream()
                            .mapToDouble(StorageZone::getCapacityVolume)
                            .sum();
                    double usedVolume = shelf.getStorageZones().stream()
                            .flatMap(zone -> zone.getProducts().stream())
                            .mapToDouble(ZoneProduct::getTotalVolume)
                            .sum();
                    return capacity > 0 ? (usedVolume / capacity) * 100.0 : 0.0;
                })
                .toList();

        return new AnalyticsChartsData(periodLabels, orderDynamics, revenueDynamics, shelfLabels, shelfOccupancy);
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

    public record AnalyticsChartsData(
            List<String> periodLabels,
            List<Integer> orderDynamics,
            List<BigDecimal> revenueDynamics,
            List<String> shelfLabels,
            List<Double> shelfOccupancy
    ) {
    }
}
