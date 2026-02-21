package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.*;
import com.mai.siarsp.enumeration.BoxOrientation;
import com.mai.siarsp.models.*;
import com.mai.siarsp.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис расширенного управления складом: аналитика, поиск товаров, проверка размещения.
 * Делегирует операции размещения StoragePlacementService.
 */
@Service("warehouseManagementService")
@Slf4j
public class WarehouseManagementService {

    private final StoragePlacementService placementService;
    private final WarehouseRepository warehouseRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final ZoneProductRepository zoneProductRepository;
    private final ProductRepository productRepository;

    public WarehouseManagementService(
            @Qualifier("storagePlacementService") StoragePlacementService placementService,
            WarehouseRepository warehouseRepository,
            StorageZoneRepository storageZoneRepository,
            ZoneProductRepository zoneProductRepository,
            ProductRepository productRepository) {
        this.placementService = placementService;
        this.warehouseRepository = warehouseRepository;
        this.storageZoneRepository = storageZoneRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.productRepository = productRepository;
    }

    // ========== РАЗМЕЩЕНИЕ ==========

    /**
     * Автоматически размещает товар в оптимальную зону.
     */
    @Transactional
    public PlacementInfo placeProductOptimal(Long productId, int quantity) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) return PlacementInfo.failure("Товар не найден");
        Product product = opt.get();
        if (product.getQuantityForStock() < quantity) {
            return PlacementInfo.failure("Недостаточно товара для размещения (доступно: "
                    + product.getQuantityForStock() + ")");
        }
        return placementService.placeOptimal(product, quantity);
    }

    /**
     * Размещает товар в конкретную зону.
     */
    @Transactional
    public PlacementInfo placeProductInZone(Long productId, Long zoneId, int quantity) {
        Optional<Product> optP = productRepository.findById(productId);
        if (optP.isEmpty()) return PlacementInfo.failure("Товар не найден");
        Optional<StorageZone> optZ = storageZoneRepository.findById(zoneId);
        if (optZ.isEmpty()) return PlacementInfo.failure("Зона хранения не найдена");

        Product product = optP.get();
        StorageZone zone = optZ.get();

        if (!zone.canStoreProduct(product)) {
            return PlacementInfo.failure("Тип склада не совместим с типом хранения товара");
        }
        if (product.getQuantityForStock() < quantity) {
            return PlacementInfo.failure("Недостаточно товара для размещения (доступно: "
                    + product.getQuantityForStock() + ")");
        }
        return placementService.placeInZone(product, zone, quantity);
    }

    // ========== ПОИСК ТОВАРА ==========

    /**
     * Возвращает список мест хранения товара на всех складах.
     */
    @Transactional(readOnly = true)
    public List<ProductLocation> findProductLocations(Long productId) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) return List.of();
        return zoneProductRepository.findByProduct(opt.get()).stream()
                .map(zp -> new ProductLocation(
                        zp.getZone().getId(),
                        zp.getZone().getLabel(),
                        zp.getZone().getShelf().getCode(),
                        zp.getZone().getShelf().getWarehouse().getName(),
                        zp.getQuantity(),
                        WarehouseApiService.orientationLabel(zp.getOrientation()),
                        zp.getTotalVolume()
                ))
                .toList();
    }

    // ========== АНАЛИТИКА ==========

    /**
     * Возвращает расширенную статистику по складу.
     */
    @Transactional(readOnly = true)
    public Optional<DetailedWarehouseStatistics> getDetailedStatistics(Long warehouseId) {
        return warehouseRepository.findById(warehouseId).map(wh -> {
            List<StorageZone> allZones = wh.getShelves().stream()
                    .flatMap(s -> s.getStorageZones().stream())
                    .toList();

            double totalCapacity = allZones.stream()
                    .mapToDouble(StorageZone::getCapacityVolume)
                    .sum();

            List<ZoneProduct> allZoneProducts = allZones.stream()
                    .flatMap(z -> z.getProducts().stream())
                    .toList();

            double usedVolume = allZoneProducts.stream()
                    .mapToDouble(ZoneProduct::getTotalVolume)
                    .sum();

            double occupancy = totalCapacity > 0 ? (usedVolume / totalCapacity) * 100.0 : 0.0;
            int totalUnits = allZoneProducts.stream().mapToInt(ZoneProduct::getQuantity).sum();
            long uniqueProducts = allZoneProducts.stream()
                    .map(zp -> zp.getProduct().getId())
                    .distinct()
                    .count();

            // Топ-5 товаров по занятому объёму
            List<TopProductInfo> topProducts = allZoneProducts.stream()
                    .collect(Collectors.groupingBy(
                            zp -> zp.getProduct().getName(),
                            Collectors.summarizingDouble(ZoneProduct::getTotalVolume)
                    ))
                    .entrySet().stream()
                    .sorted(Comparator.comparingDouble(e -> -e.getValue().getSum()))
                    .limit(5)
                    .map(e -> {
                        int qty = allZoneProducts.stream()
                                .filter(zp -> zp.getProduct().getName().equals(e.getKey()))
                                .mapToInt(ZoneProduct::getQuantity)
                                .sum();
                        return new TopProductInfo(e.getKey(), qty, e.getValue().getSum());
                    })
                    .toList();

            return new DetailedWarehouseStatistics(
                    wh.getName(),
                    wh.getShelves().size(),
                    allZones.size(),
                    totalCapacity,
                    usedVolume,
                    occupancy,
                    totalUnits,
                    (int) uniqueProducts,
                    topProducts
            );
        });
    }

    /**
     * Возвращает зоны с заполненностью ниже указанного порога, отсортированные по заполненности.
     */
    @Transactional(readOnly = true)
    public List<ZoneUtilization> getUnderutilizedZones(Long warehouseId, double maxOccupancyPercent) {
        return warehouseRepository.findById(warehouseId)
                .map(wh -> wh.getShelves().stream()
                        .flatMap(s -> s.getStorageZones().stream())
                        .filter(z -> z.getOccupancyPercentage() <= maxOccupancyPercent)
                        .sorted(Comparator.comparingDouble(StorageZone::getOccupancyPercentage))
                        .map(z -> {
                            double occ = z.getOccupancyPercentage();
                            double capacity = z.getCapacityVolume();
                            double used = z.getProducts().stream()
                                    .mapToDouble(ZoneProduct::getTotalVolume).sum();
                            double available = capacity - used;
                            String rec = buildRecommendation(occ);
                            return new ZoneUtilization(
                                    z.getId(), z.getLabel(),
                                    z.getShelf().getCode(),
                                    occ, available, rec
                            );
                        })
                        .toList()
                )
                .orElse(List.of());
    }

    private String buildRecommendation(double occ) {
        if (occ < 20.0) return "Много свободного места";
        if (occ < 60.0) return "Оптимальная загрузка";
        if (occ < 85.0) return "Хорошо заполнена";
        return "Почти заполнена — добавьте осторожно";
    }

    // ========== ПРОВЕРКА ВОЗМОЖНОСТИ РАЗМЕЩЕНИЯ ==========

    /**
     * Возвращает список доступных зон для размещения товара, отсортированных по заполненности.
     */
    @Transactional(readOnly = true)
    public List<AvailableZone> checkPlacementPossibility(Long productId, int quantity) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) return List.of();

        Product product = opt.get();
        ZoneProduct helper = new ZoneProduct();
        List<AvailableZone> result = new ArrayList<>();

        for (Warehouse wh : warehouseRepository.findAll()) {
            if (!wh.canStoreProduct(product)) continue;
            for (Shelf shelf : wh.getShelves()) {
                for (StorageZone zone : shelf.getStorageZones()) {
                    BoxOrientation orientation = helper.findBestOrientation(product, zone, quantity);
                    if (orientation != null) {
                        // Вычислить максимально возможное кол-во
                        int maxQty = calcMaxQuantity(helper, product, zone);
                        result.add(new AvailableZone(
                                zone.getId(),
                                zone.getLabel(),
                                shelf.getCode(),
                                wh.getName(),
                                maxQty,
                                WarehouseApiService.orientationLabel(orientation),
                                zone.getOccupancyPercentage()
                        ));
                    }
                }
            }
        }

        result.sort(Comparator.comparingDouble(AvailableZone::occupancyPercent));
        return result;
    }

    private int calcMaxQuantity(ZoneProduct helper, Product product, StorageZone zone) {
        int lo = 1, hi = 10_000, best = 0;
        while (lo <= hi) {
            int mid = (lo + hi) / 2;
            if (helper.findBestOrientation(product, zone, mid) != null) {
                best = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return best;
    }

    // ========== AJAX: ЗОНЫ СКЛАДА ==========

    /**
     * Возвращает список зон конкретного склада для AJAX-выбора.
     */
    @Transactional(readOnly = true)
    public List<ZoneSelectDto> getZonesByWarehouse(Long warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .map(wh -> wh.getShelves().stream()
                        .flatMap(s -> s.getStorageZones().stream())
                        .map(z -> {
                            double cap = z.getCapacityVolume();
                            double used = z.getProducts().stream()
                                    .mapToDouble(ZoneProduct::getTotalVolume).sum();
                            double occ = cap > 0 ? (used / cap) * 100.0 : 0.0;
                            double avail = Math.max(0.0, cap - used);
                            return new ZoneSelectDto(
                                    z.getId(), z.getLabel(),
                                    z.getShelf().getCode(),
                                    cap, occ, avail
                            );
                        })
                        .toList()
                )
                .orElse(List.of());
    }

    /**
     * Возвращает детальную информацию о зоне для AJAX.
     * Делегирует WarehouseApiService.
     */
    @Transactional(readOnly = true)
    public Optional<ZoneInfo> getZoneInfo(Long zoneId) {
        return storageZoneRepository.findById(zoneId).map(zone -> {
            double usedVolume = zone.getProducts().stream()
                    .mapToDouble(ZoneProduct::getTotalVolume)
                    .sum();
            List<ZoneInfo.ZoneProductShortInfo> products = zone.getProducts().stream()
                    .map(zp -> new ZoneInfo.ZoneProductShortInfo(
                            zp.getProduct().getId(),
                            zp.getProduct().getName(),
                            zp.getProduct().getArticle(),
                            zp.getQuantity(),
                            WarehouseApiService.orientationLabel(zp.getOrientation()),
                            zp.getTotalVolume()
                    ))
                    .toList();
            return new ZoneInfo(
                    zone.getId(),
                    zone.getLabel(),
                    zone.getShelf().getCode(),
                    zone.getShelf().getWarehouse().getName(),
                    zone.getLength(),
                    zone.getWidth(),
                    zone.getHeight(),
                    zone.getCapacityVolume(),
                    usedVolume,
                    zone.getOccupancyPercentage(),
                    products
            );
        });
    }
}
