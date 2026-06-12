package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.dto.MoveInfo;
import com.mai.siarsp.dto.PlacementInfo;
import com.mai.siarsp.dto.RemovalInfo;
import com.mai.siarsp.enumeration.BoxOrientation;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.StorageZoneRepository;
import com.mai.siarsp.repo.SupplyRepository;
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
 * Сервис адресного размещения: подбирает место хранения и контролирует корректность посадки товара.
 */

@Service("storagePlacementService")
@Slf4j
public class StoragePlacementService {

    private final WarehouseRepository warehouseRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final ZoneProductRepository zoneProductRepository;
    private final ProductRepository productRepository;
    private final SupplyRepository supplyRepository;

    public StoragePlacementService(WarehouseRepository warehouseRepository,
                                   StorageZoneRepository storageZoneRepository,
                                   ZoneProductRepository zoneProductRepository,
                                   ProductRepository productRepository,
                                   SupplyRepository supplyRepository) {
        this.warehouseRepository = warehouseRepository;
        this.storageZoneRepository = storageZoneRepository;
        this.zoneProductRepository = zoneProductRepository;
        this.productRepository = productRepository;
        this.supplyRepository = supplyRepository;
    }

    /**
     * Возвращает последнюю партию указанного товара (для совместимости со старым UI размещения,
     * где пользователь оперирует «товаром» без явного выбора партии). Если партий нет — null.
     */
    private Supply resolveSupply(Product product) {
        if (product == null || product.getId() == null) return null;
        return supplyRepository.findByProductIdOrderByDelivery_DeliveryDateDesc(product.getId())
                .stream().findFirst().orElse(null);
    }

    /**
     * Можно ли положить указанную партию (incomingSupply) в зону без смешивания с уже лежащими
     * там «разными» партиями того же товара.
     * <p>
     * Правило: в зоне можно держать несколько партий одного товара только если у них совпадают
     * productionDate и expirationDate (фактически — идентичны по срокам). Если в зоне уже лежит
     * партия того же товара с другой productionDate/expirationDate — возвращаем false.
     */
    private boolean canPlaceBatchInZone(StorageZone zone, Supply incomingSupply) {
        if (zone == null || incomingSupply == null || incomingSupply.getProduct() == null) {
            return false;
        }
        Long productId = incomingSupply.getProduct().getId();
        String productName = incomingSupply.getProduct().getName();
        log.info("🔍 Проверка зоны '{}' для партии #{} (товар: '{}' [id={}], дата пр-ва: {}, срок годности: {})",
                zone.getLabel(), incomingSupply.getId(), productName, productId,
                incomingSupply.getProductionDate(), incomingSupply.getExpirationDate());

        for (ZoneProduct existing : zone.getProducts()) {
            Supply otherSupply = existing.getSupply();
            if (otherSupply == null || otherSupply.getProduct() == null) continue;
            if (!otherSupply.getProduct().getId().equals(productId)) continue;

            log.info("   ↳ В зоне уже есть товар '{}' [id={}] из партии #{}: дата пр-ва: {}, срок годности: {}",
                    otherSupply.getProduct().getName(), otherSupply.getProduct().getId(),
                    otherSupply.getId(), otherSupply.getProductionDate(), otherSupply.getExpirationDate());

            if (otherSupply.getId() != null && otherSupply.getId().equals(incomingSupply.getId())) {
                log.info("   ↳ Та же партия — разрешено");
                continue;
            }
            boolean sameProdDate = java.util.Objects.equals(
                    otherSupply.getProductionDate(), incomingSupply.getProductionDate());
            boolean sameExpDate = java.util.Objects.equals(
                    otherSupply.getExpirationDate(), incomingSupply.getExpirationDate());
            log.info("   ↳ Сравнение дат: дата пр-ва совпадает={}, срок годности совпадает={}",
                    sameProdDate, sameExpDate);
            if (!sameProdDate || !sameExpDate) {
                log.warn("   ❌ Запрещено: в зоне '{}' другая партия товара '{}' с отличающимися датами",
                        zone.getLabel(), productName);
                return false;
            }
        }
        log.info("   ✅ Зона '{}' разрешена для размещения товара '{}'", zone.getLabel(), productName);
        return true;
    }

    /**
     * Размещает конкретную партию (Supply) в зонах оптимально — используется при приёмке.
     * В отличие от {@link #placeOptimal(Product, int)} не пытается найти партию по товару,
     * а оперирует уже известной partyе.
     */
    @Transactional
    public PlacementInfo placeOptimalForSupply(Supply supply, int quantity) {
        if (supply == null || supply.getProduct() == null) {
            return PlacementInfo.failure("Партия не задана");
        }
        // Используем имеющийся алгоритм placeOptimal, но создаём ZoneProduct под конкретную партию
        Product product = supply.getProduct();
        try {
            ZoneProduct helper = new ZoneProduct();
            ZoneCandidate best = null;
            double bestOccupancy = Double.MAX_VALUE;
            for (Warehouse wh : warehouseRepository.findAll()) {
                if (!wh.canStoreProduct(product)) continue;
                for (var shelf : wh.getShelves()) {
                    for (StorageZone zone : shelf.getStorageZones()) {
                        // Пропускаем зоны, где уже лежит другая партия этого товара
                        if (!canPlaceBatchInZone(zone, supply)) continue;
                        Double boxL = product.getPackageLength();
                        Double boxW = product.getPackageWidth();
                        Double boxH = product.getPackageHeight();
                        if (boxL != null && boxW != null && boxH != null) {
                            double itemVolume = (boxL * boxW * boxH) / 1_000_000.0;
                            double newVolume = itemVolume * quantity;
                            double usedVolume = zone.getProducts().stream()
                                    .mapToDouble(ZoneProduct::getTotalVolume).sum();
                            if (usedVolume + newVolume > zone.getCapacityVolume()) continue;
                        }
                        BoxOrientation orientation = helper.findBestOrientation(product, zone, quantity);
                        if (orientation != null && zone.getOccupancyPercentage() < bestOccupancy) {
                            bestOccupancy = zone.getOccupancyPercentage();
                            best = new ZoneCandidate(zone, orientation);
                        }
                    }
                }
            }
            if (best == null) {
                return PlacementInfo.failure("Нет подходящей зоны хранения для партии " +
                        "(зоны с другими партиями этого товара пропускаются)");
            }
            ZoneProduct zp = new ZoneProduct(supply, quantity);
            zp.setZone(best.zone());
            zp.setOrientation(best.orientation());
            zoneProductRepository.save(zp);
            supply.setQuantityForStock(Math.max(0, supply.getQuantityForStock() - quantity));
            supplyRepository.save(supply);
            product.setQuantityForStock(Math.max(0, product.getQuantityForStock() - quantity));
            productRepository.save(product);
            log.info("✅ Партия #{} ({} ед.) размещена в зоне {} ({})",
                    supply.getId(), quantity, best.zone().getLabel(), best.orientation());
            return new PlacementInfo(true, null,
                    best.zone().getId(), best.zone().getLabel(),
                    best.zone().getShelf().getCode(),
                    best.zone().getShelf().getWarehouse().getName(),
                    best.orientation(), quantity);
        } catch (Exception e) {
            log.error("❌ placeOptimalForSupply: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return PlacementInfo.failure("Ошибка размещения партии: " + e.getMessage());
        }
    }

    @Transactional
    public PlacementInfo placeOptimal(Product product, int quantity) {
        try {
            List<Warehouse> warehouses = warehouseRepository.findAll();
            ZoneProduct helper = new ZoneProduct();

            ZoneCandidate best = null;
            double bestOccupancy = Double.MAX_VALUE;

            Supply supplyForPlacement = resolveSupply(product);
            for (Warehouse wh : warehouses) {
                if (!wh.canStoreProduct(product)) continue;

                for (var shelf : wh.getShelves()) {
                    for (StorageZone zone : shelf.getStorageZones()) {
                        // Пропускаем зоны, где уже лежит другая партия этого товара
                        if (supplyForPlacement != null
                                && !canPlaceBatchInZone(zone, supplyForPlacement)) continue;
                        // Проверка свободного объёма
                        Double boxL = product.getPackageLength();
                        Double boxW = product.getPackageWidth();
                        Double boxH = product.getPackageHeight();
                        if (boxL != null && boxW != null && boxH != null) {
                            double itemVolume = (boxL * boxW * boxH) / 1_000_000.0;
                            double newVolume = itemVolume * quantity;
                            double usedVolume = zone.getProducts().stream()
                                    .mapToDouble(ZoneProduct::getTotalVolume).sum();
                            if (usedVolume + newVolume > zone.getCapacityVolume()) continue;
                        }

                        BoxOrientation orientation = helper.findBestOrientation(product, zone, quantity);
                        if (orientation != null && zone.getOccupancyPercentage() < bestOccupancy) {
                            bestOccupancy = zone.getOccupancyPercentage();
                            best = new ZoneCandidate(zone, orientation);
                        }
                    }
                }
            }

            if (best == null) {
                return PlacementInfo.failure("Нет подходящей зоны хранения для товара " +
                        "(зоны с другими партиями этого товара пропускаются)");
            }

            return placeInZone(product, best.zone(), quantity, best.orientation());

        } catch (Exception e) {
            log.error("❌ placeOptimal: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return PlacementInfo.failure("Внутренняя ошибка при размещении");
        }
    }

    @Transactional
    public PlacementInfo placeInZoneForSupply(Supply supply, StorageZone zone, int quantity) {
        if (supply == null || supply.getProduct() == null) {
            return PlacementInfo.failure("Партия не задана");
        }
        Product product = supply.getProduct();
        Double boxL = product.getPackageLength();
        Double boxW = product.getPackageWidth();
        Double boxH = product.getPackageHeight();
        if (boxL != null && boxW != null && boxH != null) {
            double itemVolume = (boxL * boxW * boxH) / 1_000_000.0;
            double newVolume = itemVolume * quantity;
            double usedVolume = zone.getProducts().stream()
                    .mapToDouble(ZoneProduct::getTotalVolume).sum();
            double capacity = zone.getCapacityVolume();
            if (usedVolume + newVolume > capacity) {
                return PlacementInfo.failure("Недостаточно свободного объёма в зоне (свободно: "
                        + String.format("%.4f", capacity - usedVolume) + " м³, требуется: "
                        + String.format("%.4f", newVolume) + " м³)");
            }
        }
        if (!canPlaceBatchInZone(zone, supply)) {
            return PlacementInfo.failure("В зоне уже лежит другая партия этого товара " +
                    "(с другими датами производства/срока годности). Выберите другую зону.");
        }
        ZoneProduct helper = new ZoneProduct();
        BoxOrientation orientation = helper.findBestOrientation(product, zone, quantity);
        if (orientation == null) {
            return PlacementInfo.failure("Товар не помещается в указанную зону");
        }
        try {
            Optional<ZoneProduct> existing = zoneProductRepository.findByZoneAndProduct(zone, product);
            ZoneProduct zp;
            if (existing.isPresent()) {
                zp = existing.get();
                zp.setQuantity(zp.getQuantity() + quantity);
                zp.setOrientation(orientation);
            } else {
                zp = new ZoneProduct(supply, quantity);
                zp.setZone(zone);
                zp.setOrientation(orientation);
            }
            zoneProductRepository.save(zp);
            supply.setQuantityForStock(Math.max(0, supply.getQuantityForStock() - quantity));
            supplyRepository.save(supply);
            product.setQuantityForStock(Math.max(0, product.getQuantityForStock() - quantity));
            productRepository.save(product);
            log.info("✅ Размещено {} ед. '{}' (партия #{}) в зоне {} ({})",
                    quantity, product.getName(), supply.getId(), zone.getLabel(), orientation);
            return new PlacementInfo(true, null, zone.getId(), zone.getLabel(),
                    zone.getShelf().getCode(), zone.getShelf().getWarehouse().getName(),
                    orientation, quantity);
        } catch (Exception e) {
            log.error("❌ placeInZoneForSupply: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return PlacementInfo.failure("Ошибка при размещении партии");
        }
    }

    @Transactional
    public PlacementInfo placeInZone(Product product, StorageZone zone, int quantity) {
        // Проверка: хватает ли свободного объёма в зоне
        Double boxL = product.getPackageLength();
        Double boxW = product.getPackageWidth();
        Double boxH = product.getPackageHeight();
        if (boxL != null && boxW != null && boxH != null) {
            double itemVolume = (boxL * boxW * boxH) / 1_000_000.0; // см³ → м³
            double newVolume = itemVolume * quantity;
            double usedVolume = zone.getProducts().stream()
                    .mapToDouble(ZoneProduct::getTotalVolume)
                    .sum();
            double capacity = zone.getCapacityVolume();
            if (usedVolume + newVolume > capacity) {
                return PlacementInfo.failure("Недостаточно свободного объёма в зоне (свободно: "
                        + String.format("%.4f", capacity - usedVolume) + " м³, требуется: "
                        + String.format("%.4f", newVolume) + " м³)");
            }
        }

        ZoneProduct helper = new ZoneProduct();
        BoxOrientation orientation = helper.findBestOrientation(product, zone, quantity);
        if (orientation == null) {
            return PlacementInfo.failure("Товар не помещается в указанную зону");
        }
        return placeInZone(product, zone, quantity, orientation);
    }

    @Transactional
    public PlacementInfo placeInZone(Product product, StorageZone zone, int quantity, BoxOrientation orientation) {
        try {
            Supply supply = resolveSupply(product);
            if (supply == null) {
                return PlacementInfo.failure("У товара нет принятых партий — невозможно разместить");
            }
            if (!canPlaceBatchInZone(zone, supply)) {
                return PlacementInfo.failure("В зоне уже лежит другая партия этого товара " +
                        "(с другими датами производства/срока годности). Выберите другую зону.");
            }
            Optional<ZoneProduct> existing = zoneProductRepository.findByZoneAndProduct(zone, product);
            ZoneProduct zp;
            if (existing.isPresent()) {
                zp = existing.get();
                zp.setQuantity(zp.getQuantity() + quantity);
                zp.setOrientation(orientation);
            } else {
                zp = new ZoneProduct(supply, quantity);
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
