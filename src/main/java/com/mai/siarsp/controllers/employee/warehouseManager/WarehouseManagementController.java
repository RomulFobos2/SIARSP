package com.mai.siarsp.controllers.employee.warehouseManager;

import com.mai.siarsp.dto.*;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.service.employee.warehouseManager.WarehouseManagementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Контроллер расширенного управления складом.
 * Обрабатывает страницы размещения товара, поиска и аналитики.
 * Базовый путь: /employee/warehouseManager/warehouse-management
 */
@Controller("warehouseManagementController")
@Slf4j
public class WarehouseManagementController {

    private final WarehouseManagementService managementService;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public WarehouseManagementController(
            @Qualifier("warehouseManagementService") WarehouseManagementService managementService,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository) {
        this.managementService = managementService;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    // ========== РАЗМЕЩЕНИЕ ТОВАРА ==========

    @Transactional(readOnly = true)
    @GetMapping("/employee/warehouseManager/warehouse-management/place-product")
    public String placeProductPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            Model model) {

        Page<Product> products;
        if (search != null && !search.isBlank()) {
            products = productRepository
                    .findByNameContainingIgnoreCaseAndQuantityForStockGreaterThan(search, 0,
                            PageRequest.of(page, size));
        } else {
            products = productRepository
                    .findByQuantityForStockGreaterThan(0, PageRequest.of(page, size));
        }

        List<Product> negativeStockProducts = productRepository.findByQuantityForStockLessThan(0);
        List<Warehouse> warehouses = warehouseRepository.findAll();

        // Pre-compute габариты упаковки пока JPA-транзакция ещё открыта
        // (attributeValues — lazy collection, недоступна после закрытия транзакции)
        Map<Long, Boolean> hasPackageDims = new HashMap<>();
        Map<Long, String>  packageDimsStr = new HashMap<>();
        for (Product p : products.getContent()) {
            try {
                Double l = p.getPackageLength();
                Double w = p.getPackageWidth();
                Double h = p.getPackageHeight();
                boolean has = (l != null && w != null && h != null);
                hasPackageDims.put(p.getId(), has);
                if (has) {
                    packageDimsStr.put(p.getId(),
                            (int) Math.round(l) + "×" + (int) Math.round(w) + "×" + (int) Math.round(h));
                }
            } catch (Exception e) {
                hasPackageDims.put(p.getId(), false);
            }
        }

        model.addAttribute("products", products);
        model.addAttribute("negativeStockProducts", negativeStockProducts);
        model.addAttribute("warehouses", warehouses);
        model.addAttribute("search", search);
        model.addAttribute("hasPackageDims", hasPackageDims);
        model.addAttribute("packageDimsStr", packageDimsStr);
        return "employee/warehouseManager/warehouses/placeProduct";
    }

    @PostMapping("/employee/warehouseManager/warehouse-management/place-product")
    public String doPlaceProduct(
            @RequestParam Long productId,
            @RequestParam int quantity,
            @RequestParam(required = false) Long zoneId,
            RedirectAttributes redirectAttributes) {

        PlacementInfo result;
        if (zoneId == null) {
            result = managementService.placeProductOptimal(productId, quantity);
        } else {
            result = managementService.placeProductInZone(productId, zoneId, quantity);
        }

        if (result.success()) {
            redirectAttributes.addFlashAttribute("successMessage",
                    "Товар успешно размещён в зоне «" + result.zoneLabel() + "» (стеллаж "
                            + result.shelfCode() + ", склад " + result.warehouseName() + ")");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Не удалось разместить товар: " + result.reason());
        }
        return "redirect:/employee/warehouseManager/warehouse-management/place-product";
    }

    // ========== ПОИСК ТОВАРА ==========

    @Transactional(readOnly = true)
    @GetMapping("/employee/warehouseManager/warehouse-management/find-product")
    public String findProductPage(
            @RequestParam(required = false) Long productId,
            Model model) {

        List<Product> allProducts = productRepository.findAll();
        model.addAttribute("allProducts", allProducts);

        if (productId != null) {
            Optional<Product> opt = productRepository.findById(productId);
            if (opt.isPresent()) {
                Product selected = opt.get();
                List<ProductLocation> locations = managementService.findProductLocations(productId);
                double totalVolume = locations.stream()
                        .mapToDouble(ProductLocation::totalVolume).sum();
                int totalQuantity = locations.stream()
                        .mapToInt(ProductLocation::quantity).sum();

                model.addAttribute("selectedProduct", selected);
                model.addAttribute("locations", locations);
                model.addAttribute("totalQuantity", totalQuantity);
                model.addAttribute("totalVolume", totalVolume);
            }
        }
        return "employee/warehouseManager/warehouses/findProduct";
    }

    // ========== АНАЛИТИКА ==========

    @Transactional(readOnly = true)
    @GetMapping("/employee/warehouseManager/warehouse-management/analytics/{warehouseId}")
    public String analyticsPage(@PathVariable Long warehouseId, Model model) {
        Optional<Warehouse> opt = warehouseRepository.findById(warehouseId);
        if (opt.isEmpty()) {
            return "redirect:/employee/warehouseManager/warehouses/allWarehouses";
        }

        Optional<DetailedWarehouseStatistics> stats = managementService.getDetailedStatistics(warehouseId);
        if (stats.isEmpty()) {
            return "redirect:/employee/warehouseManager/warehouses/allWarehouses";
        }

        List<ZoneUtilization> underutilizedZones = managementService.getUnderutilizedZones(warehouseId, 50.0);

        model.addAttribute("warehouse", opt.get());
        model.addAttribute("stats", stats.get());
        model.addAttribute("underutilizedZones", underutilizedZones);
        return "employee/warehouseManager/warehouses/analytics";
    }

    // ========== AJAX-ЭНДПОИНТЫ ==========

    @GetMapping("/employee/warehouseManager/warehouse-management/check-placement")
    @ResponseBody
    public List<AvailableZone> checkPlacement(
            @RequestParam Long productId,
            @RequestParam int quantity) {
        return managementService.checkPlacementPossibility(productId, quantity);
    }

    @GetMapping("/employee/warehouseManager/warehouse-management/zone-info/{zoneId}")
    @ResponseBody
    public ZoneInfo getZoneInfo(@PathVariable Long zoneId) {
        return managementService.getZoneInfo(zoneId).orElse(null);
    }

    @GetMapping("/employee/warehouseManager/warehouse-management/warehouse-zones/{warehouseId}")
    @ResponseBody
    public List<ZoneSelectDto> getWarehouseZones(@PathVariable Long warehouseId) {
        return managementService.getZonesByWarehouse(warehouseId);
    }
}
