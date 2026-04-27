package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Базовый сервис по складам: CRUD-операции и общие проверки, которые переиспользуют остальные модули.
 */

@Service("warehouseManagerWarehouseService")
@Slf4j
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public WarehouseService(WarehouseRepository warehouseRepository, ProductRepository productRepository) {
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    /**
     * Возвращает данные о вместимости каждого склада.
     * Ключ — id склада, значение — массив [totalVolume, usedVolume] (в м³).
     */
    @Transactional(readOnly = true)
    public Map<Long, double[]> getWarehouseCapacityData() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        Map<Long, double[]> result = new HashMap<>();
        for (Warehouse wh : warehouses) {
            double usedVolume = wh.getShelves().stream()
                    .flatMap(shelf -> shelf.getStorageZones().stream())
                    .flatMap(zone -> zone.getProducts().stream())
                    .mapToDouble(ZoneProduct::getTotalVolume)
                    .sum();
            result.put(wh.getId(), new double[]{wh.getTotalVolume(), usedVolume});
        }
        return result;
    }

    /**
     * Возвращает объём одной единицы каждого товара (в м³).
     * Ключ — id товара, значение — объём (L*W*H / 1_000_000) или 0.0.
     */
    @Transactional(readOnly = true)
    public Map<Long, Double> getProductVolumeData() {
        List<Product> products = productRepository.findAll();
        Map<Long, Double> result = new HashMap<>();
        for (Product p : products) {
            Double l = p.getPackageLength();
            Double w = p.getPackageWidth();
            Double h = p.getPackageHeight();
            double volume = (l != null && w != null && h != null)
                    ? (l * w * h) / 1_000_000.0 : 0.0;
            result.put(p.getId(), volume);
        }
        return result;
    }
}
