package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.MoveInfo;
import com.mai.siarsp.dto.PlacementInfo;
import com.mai.siarsp.dto.RemovalInfo;
import com.mai.siarsp.enumeration.BoxOrientation;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.StorageZoneRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Сервис размещения, изъятия и перемещения товаров в зонах хранения.
 * Переиспользует логику ZoneProduct.findBestOrientation().
 */
@Service("storagePlacementService")
@Slf4j
public class StoragePlacementService {

    private final WarehouseRepository warehouseRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final ZoneProductRepository zoneProductRepository;
    private final ProductRepository productRepository;

    public StoragePlacementService(WarehouseRepository warehouseRepository,
                                   StorageZoneRepository storageZoneRepository,
                                   ZoneProductRepository zoneProductRepository,
                                   ProductRepository productRepository) {
        this.warehouseRepository = warehouseRepository;
        this.storageZoneRepository = storageZoneRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.productRepository = productRepository;
    }

    /**
     * Находит оптимальную зону и размещает товар.
     * Выбирает зону с минимальным заполнением среди совместимых складов.
     */
    @Transactional
    public PlacementInfo placeOptimal(Product product, int quantity) {
        try {
            List<Warehouse> warehouses = warehouseRepository.findAll();
            ZoneProduct helper = new ZoneProduct();

            ZoneCandidate best = null;
            double bestOccupancy = Double.MAX_VALUE;

            for (Warehouse wh : warehouses) {
                if (!wh.canStoreProduct(product)) continue;

                for (var shelf : wh.getShelves()) {
                    for (StorageZone zone : shelf.getStorageZones()) {
                        BoxOrientation orientation = helper.findBestOrientation(product, zone, quantity);
                        if (orientation != null && zone.getOccupancyPercentage() < bestOccupancy) {
                            bestOccupancy = zone.getOccupancyPercentage();
                            best = new ZoneCandidate(zone, orientation);
                        }
                    }
                }
            }

            if (best == null) {
                return PlacementInfo.failure("Нет подходящей зоны хранения для товара");
            }

            return placeInZone(product, best.zone(), quantity, best.orientation());

        } catch (Exception e) {
            log.error("❌ placeOptimal: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return PlacementInfo.failure("Внутренняя ошибка при размещении");
        }
    }

    /**
     * Размещает товар в конкретной зоне (автоподбор ориентации).
     */
    @Transactional
    public PlacementInfo placeInZone(Product product, StorageZone zone, int quantity) {
        ZoneProduct helper = new ZoneProduct();
        BoxOrientation orientation = helper.findBestOrientation(product, zone, quantity);
        if (orientation == null) {
            return PlacementInfo.failure("Товар не помещается в указанную зону");
        }
        return placeInZone(product, zone, quantity, orientation);
    }

    /**
     * Размещает товар в конкретной зоне с заданной ориентацией.
     * Создаёт или обновляет ZoneProduct. Уменьшает product.quantityForStock.
     */
    @Transactional
    public PlacementInfo placeInZone(Product product, StorageZone zone, int quantity, BoxOrientation orientation) {
        try {
            Optional<ZoneProduct> existing = zoneProductRepository.findByZoneAndProduct(zone, product);
            ZoneProduct zp;
            if (existing.isPresent()) {
                zp = existing.get();
                zp.setQuantity(zp.getQuantity() + quantity);
                zp.setOrientation(orientation);
            } else {
                zp = new ZoneProduct(product, quantity);
                zp.setZone(zone);
                zp.setOrientation(orientation);
            }
            zoneProductRepository.save(zp);

            product.setQuantityForStock(product.getQuantityForStock() - quantity);
            productRepository.save(product);

            log.info("✅ Размещено {} ед. '{}' в зоне {} ({})",
                    quantity, product.getName(), zone.getLabel(), orientation);

            return new PlacementInfo(true, null, zone.getId(), zone.getLabel(),
                    zone.getShelf().getCode(), zone.getShelf().getWarehouse().getName(),
                    orientation, quantity);

        } catch (Exception e) {
            log.error("❌ placeInZone: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return PlacementInfo.failure("Ошибка при размещении товара");
        }
    }

    /**
     * Убирает товар из зоны хранения.
     * Уменьшает ZoneProduct.quantity или удаляет запись. Увеличивает quantityForStock.
     */
    @Transactional
    public RemovalInfo removeFromZone(Product product, StorageZone zone, int quantity) {
        try {
            Optional<ZoneProduct> opt = zoneProductRepository.findByZoneAndProduct(zone, product);
            if (opt.isEmpty()) {
                return RemovalInfo.failure("Товар не найден в данной зоне");
            }
            ZoneProduct zp = opt.get();
            if (zp.getQuantity() < quantity) {
                return RemovalInfo.failure("Недостаточно товара в зоне: " + zp.getQuantity() + " ед.");
            }

            if (zp.getQuantity() == quantity) {
                zoneProductRepository.delete(zp);
            } else {
                zp.setQuantity(zp.getQuantity() - quantity);
                zoneProductRepository.save(zp);
            }

            product.setQuantityForStock(product.getQuantityForStock() + quantity);
            productRepository.save(product);

            log.info("✅ Изъято {} ед. '{}' из зоны {}", quantity, product.getName(), zone.getLabel());
            return new RemovalInfo(true, null, quantity);

        } catch (Exception e) {
            log.error("❌ removeFromZone: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return RemovalInfo.failure("Ошибка при изъятии товара");
        }
    }

    /**
     * Перемещает товар из одной зоны в другую.
     */
    @Transactional
    public MoveInfo moveProduct(Product product, StorageZone from, StorageZone to, int quantity) {
        try {
            RemovalInfo removal = removeFromZone(product, from, quantity);
            if (!removal.success()) {
                return MoveInfo.failure(removal.reason());
            }

            PlacementInfo placement = placeInZone(product, to, quantity);
            if (!placement.success()) {
                // Откатить изъятие
                placeInZone(product, from, quantity);
                return MoveInfo.failure("Не удалось разместить в целевой зоне: " + placement.reason());
            }

            log.info("✅ Перемещено {} ед. '{}': {} → {}", quantity, product.getName(),
                    from.getLabel(), to.getLabel());

            return new MoveInfo(true, null, from.getId(), to.getId(), quantity, placement.orientation());

        } catch (Exception e) {
            log.error("❌ moveProduct: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return MoveInfo.failure("Ошибка при перемещении товара");
        }
    }

    // Вспомогательная запись для поиска оптимального кандидата
    private record ZoneCandidate(StorageZone zone, BoxOrientation orientation) {
    }
}
