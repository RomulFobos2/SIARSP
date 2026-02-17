package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.*;
import com.mai.siarsp.enumeration.BoxOrientation;
import com.mai.siarsp.models.*;
import com.mai.siarsp.repo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Фасадный сервис для REST API управления складом.
 * Принимает Long ID, разрешает сущности, делегирует StoragePlacementService.
 */
@Service("warehouseApiService")
@Slf4j
public class WarehouseApiService {

    private final StoragePlacementService placementService;
    private final ProductRepository productRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final WarehouseRepository warehouseRepository;
    private final ShelfRepository shelfRepository;
    private final ZoneProductRepository zoneProductRepository;

    public WarehouseApiService(
            @Qualifier("storagePlacementService") StoragePlacementService placementService,
            ProductRepository productRepository,
            StorageZoneRepository storageZoneRepository,
            WarehouseRepository warehouseRepository,
            ShelfRepository shelfRepository,
            ZoneProductRepository zoneProductRepository) {
        this.placementService = placementService;
        this.productRepository = productRepository;
        this.storageZoneRepository = storageZoneRepository;
        this.warehouseRepository = warehouseRepository;
        this.shelfRepository = shelfRepository;
        this.zoneProductRepository = zoneProductRepository;
    }

    /**
     * Автоматически размещает товар в оптимальную зону.
     */
    @Transactional
    public PlacementInfo placeProductOptimal(Long productId, int quantity) {
        Optional<Product> opt = productRepository.findById(productId);
        if (opt.isEmpty()) return PlacementInfo.failure("Товар не найден");
        Product product = opt.get();
        if (product.getQuantityForStock() < quantity) {
            return PlacementInfo.failure("Недостаточно товара для размещения (доступно: " + product.getQuantityForStock() + ")");
        }
        return placementService.placeOptimal(product, quantity);
    }

    /**
     * Размещает товар в конкретную зону.
     */
    @Transactional
    public PlacementInfo placeProductInSpecificZone(Long productId, Long zoneId, int quantity) {
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
            return PlacementInfo.failure("Недостаточно товара для размещения (доступно: " + product.getQuantityForStock() + ")");
        }

        return placementService.placeInZone(product, zone, quantity);
    }

    /**
     * Убирает товар из зоны хранения.
     */
    @Transactional
    public RemovalInfo removeProduct(Long productId, Long zoneId, int quantity) {
        Optional<Product> optP = productRepository.findById(productId);
        if (optP.isEmpty()) return RemovalInfo.failure("Товар не найден");
        Optional<StorageZone> optZ = storageZoneRepository.findById(zoneId);
        if (optZ.isEmpty()) return RemovalInfo.failure("Зона хранения не найдена");

        return placementService.removeFromZone(optP.get(), optZ.get(), quantity);
    }

    /**
     * Перемещает товар между зонами хранения.
     */
    @Transactional
    public MoveInfo moveProduct(Long productId, Long fromZoneId, Long toZoneId, int quantity) {
        Optional<Product> optP = productRepository.findById(productId);
        if (optP.isEmpty()) return MoveInfo.failure("Товар не найден");
        Optional<StorageZone> optFrom = storageZoneRepository.findById(fromZoneId);
        if (optFrom.isEmpty()) return MoveInfo.failure("Исходная зона не найдена");
        Optional<StorageZone> optTo = storageZoneRepository.findById(toZoneId);
        if (optTo.isEmpty()) return MoveInfo.failure("Целевая зона не найдена");

        Product product = optP.get();
        StorageZone to = optTo.get();
        if (!to.canStoreProduct(product)) {
            return MoveInfo.failure("Тип склада целевой зоны не совместим с типом хранения товара");
        }

        return placementService.moveProduct(product, optFrom.get(), to, quantity);
    }

    /**
     * Проверяет возможность размещения без изменения данных.
     */
    @Transactional(readOnly = true)
    public PlacementCheck checkPlacementPossibility(Long productId, int quantity) {
        Optional<Product> optP = productRepository.findById(productId);
        if (optP.isEmpty()) return PlacementCheck.impossible("Товар не найден");
        Product product = optP.get();

        ZoneProduct helper = new ZoneProduct();
        BoxOrientation bestOrientation = null;
        int maxPossible = 0;

        List<Warehouse> warehouses = warehouseRepository.findAll();
        for (Warehouse wh : warehouses) {
            if (!wh.canStoreProduct(product)) continue;
            for (var shelf : wh.getShelves()) {
                for (StorageZone zone : shelf.getStorageZones()) {
                    BoxOrientation o = helper.findBestOrientation(product, zone, quantity);
                    if (o != null) {
                        bestOrientation = o;
                        // Вычисляем максимальное количество, которое помещается
                        ZoneProduct tmp = new ZoneProduct(product, Integer.MAX_VALUE);
                        tmp.setZone(zone);
                        BoxOrientation any = helper.findBestOrientation(product, zone, 1);
                        if (any != null) maxPossible = Math.max(maxPossible, quantity);
                    }
                }
            }
        }

        if (bestOrientation == null) {
            return PlacementCheck.impossible("Нет подходящей зоны хранения");
        }
        return new PlacementCheck(true, null, maxPossible, bestOrientation);
    }

    /**
     * Возвращает все места хранения товара.
     */
    @Transactional(readOnly = true)
    public List<ProductLocation> findProductLocations(Long productId) {
        Optional<Product> optP = productRepository.findById(productId);
        if (optP.isEmpty()) return List.of();

        return zoneProductRepository.findByProduct(optP.get()).stream()
                .map(zp -> new ProductLocation(
                        zp.getZone().getId(),
                        zp.getZone().getLabel(),
                        zp.getZone().getShelf().getCode(),
                        zp.getZone().getShelf().getWarehouse().getName(),
                        zp.getQuantity(),
                        orientationLabel(zp.getOrientation()),
                        zp.getTotalVolume()
                ))
                .toList();
    }

    /**
     * Возвращает детальную информацию о зоне хранения.
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
                            orientationLabel(zp.getOrientation())
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

    /**
     * Возвращает статистику по складу.
     */
    @Transactional(readOnly = true)
    public Optional<WarehouseStatistics> getWarehouseStatistics(Long warehouseId) {
        return warehouseRepository.findById(warehouseId).map(wh -> {
            int totalShelves = wh.getShelves().size();
            int totalZones = wh.getShelves().stream()
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
            return new WarehouseStatistics(totalShelves, totalZones, totalCapacity, usedVolume, occupancy);
        });
    }

    /**
     * Возвращает русское название ориентации размещения.
     */
    public static String orientationLabel(BoxOrientation o) {
        if (o == null) return "";
        return switch (o) {
            case STANDARD -> "Стандартно";
            case ROTATED_90 -> "Повернуто 90°";
            case LAY_ON_SIDE -> "На боку";
            case ROTATE_AND_LAY -> "Повернуто и на боку";
        };
    }
}
