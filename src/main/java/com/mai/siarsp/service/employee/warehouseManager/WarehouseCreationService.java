package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.enumeration.WarehouseType;
import com.mai.siarsp.models.Shelf;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.repo.ShelfRepository;
import com.mai.siarsp.repo.StorageZoneRepository;
import com.mai.siarsp.repo.WarehouseRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.Optional;

/**
 * Сервис первичной настройки склада: создание зон, полок и стартовой инфраструктуры.
 */

@Service("warehouseCreationService")
@Slf4j
public class WarehouseCreationService {

    private final WarehouseRepository warehouseRepository;
    private final ShelfRepository shelfRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final ZoneProductRepository zoneProductRepository;

    public WarehouseCreationService(WarehouseRepository warehouseRepository,
                                    ShelfRepository shelfRepository,
                                    StorageZoneRepository storageZoneRepository,
                                    ZoneProductRepository zoneProductRepository) {
        this.warehouseRepository = warehouseRepository;
        this.shelfRepository = shelfRepository;
        this.storageZoneRepository = storageZoneRepository;
        this.zoneProductRepository = zoneProductRepository;
    }

    @Transactional
    public Optional<Warehouse> createWarehouseWithStructure(String name, WarehouseType type,
                                                            String address,
                                                            int shelfCount, int zonesPerShelf,
                                                            double zoneLength, double zoneWidth,
                                                            double zoneHeight) {
        try {
            if (warehouseRepository.existsByName(name)) {
                log.error("❌ Склад с названием '{}' уже существует", name);
                return Optional.empty();
            }

            double totalVolume = calculateTotalVolume(shelfCount, zonesPerShelf, zoneLength, zoneWidth, zoneHeight);
            Warehouse warehouse = new Warehouse(name, type, totalVolume, address);
            warehouse = warehouseRepository.save(warehouse);

            for (int i = 0; i < shelfCount; i++) {
                String shelfCode = generateShelfCode(i);
                Shelf shelf = new Shelf(shelfCode, warehouse);
                shelf = shelfRepository.save(shelf);

                for (int j = 1; j <= zonesPerShelf; j++) {
                    String zoneLabel = shelfCode + "-" + j;
                    StorageZone zone = new StorageZone(zoneLabel, zoneLength, zoneWidth, zoneHeight, shelf);
                    storageZoneRepository.save(zone);
                }
            }

            log.info("✅ Создан склад '{}': {} стеллажей × {} зон, объём {} л",
                    name, shelfCount, zonesPerShelf, totalVolume);
            return Optional.of(warehouse);

        } catch (Exception e) {
            log.error("❌ Ошибка при создании склада '{}': {}", name, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Optional.empty();
        }
    }

    @Transactional
    public boolean deleteWarehouseIfEmpty(Long warehouseId) {
        try {
            Optional<Warehouse> opt = warehouseRepository.findById(warehouseId);
            if (opt.isEmpty()) {
                log.error("❌ Склад с ID {} не найден", warehouseId);
                return false;
            }
            if (zoneProductRepository.existsByWarehouseId(warehouseId)) {
                log.error("❌ Нельзя удалить склад ID {} — есть товары", warehouseId);
                return false;
            }
            warehouseRepository.deleteById(warehouseId);
            log.info("✅ Склад ID {} удалён", warehouseId);
            return true;
        } catch (Exception e) {
            log.error("❌ Ошибка при удалении склада ID {}: {}", warehouseId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Transactional
    public boolean deleteShelfIfEmpty(Long shelfId) {
        try {
            Optional<Shelf> opt = shelfRepository.findById(shelfId);
            if (opt.isEmpty()) {
                log.error("❌ Стеллаж с ID {} не найден", shelfId);
                return false;
            }
            if (zoneProductRepository.existsByShelfId(shelfId)) {
                log.error("❌ Нельзя удалить стеллаж ID {} — есть товары", shelfId);
                return false;
            }
            shelfRepository.deleteById(shelfId);
            log.info("✅ Стеллаж ID {} удалён", shelfId);
            return true;
        } catch (Exception e) {
            log.error("❌ Ошибка при удалении стеллажа ID {}: {}", shelfId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Transactional
    public boolean updateWarehouseAddress(Long warehouseId, String newAddress) {
        try {
            Optional<Warehouse> opt = warehouseRepository.findById(warehouseId);
            if (opt.isEmpty()) {
                log.error("Склад с ID {} не найден", warehouseId);
                return false;
            }
            Warehouse warehouse = opt.get();
            warehouse.setAddress(newAddress);
            warehouseRepository.save(warehouse);
            log.info("Адрес склада '{}' (ID {}) обновлён: {}", warehouse.getName(), warehouseId, newAddress);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при обновлении адреса склада ID {}: {}", warehouseId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    @Transactional
    public boolean addShelfToWarehouse(Long warehouseId, int zonesPerShelf,
                                        double zoneLength, double zoneWidth, double zoneHeight) {
        try {
            Optional<Warehouse> opt = warehouseRepository.findById(warehouseId);
            if (opt.isEmpty()) {
                log.error("Склад с ID {} не найден", warehouseId);
                return false;
            }
            Warehouse warehouse = opt.get();

            // Определить код нового стеллажа
            int nextIndex = warehouse.getShelves().size();
            String shelfCode = generateShelfCode(nextIndex);
            while (shelfRepository.existsByWarehouseAndCode(warehouse, shelfCode)) {
                nextIndex++;
                shelfCode = generateShelfCode(nextIndex);
            }

            Shelf shelf = new Shelf(shelfCode, warehouse);
            shelf = shelfRepository.save(shelf);

            for (int j = 1; j <= zonesPerShelf; j++) {
                String zoneLabel = shelfCode + "-" + j;
                StorageZone zone = new StorageZone(zoneLabel, zoneLength, zoneWidth, zoneHeight, shelf);
                storageZoneRepository.save(zone);
            }

            // Пересчитать общий объём
            double newVolume = (zoneLength * zoneWidth * zoneHeight) / 1000.0 * zonesPerShelf;
            warehouse.setTotalVolume(warehouse.getTotalVolume() + newVolume);
            warehouseRepository.save(warehouse);

            log.info("Стеллаж '{}' добавлен к складу '{}' (ID {}): {} зон, доп. объём {} л",
                    shelfCode, warehouse.getName(), warehouseId, zonesPerShelf, newVolume);
            return true;
        } catch (Exception e) {
            log.error("Ошибка при добавлении стеллажа к складу ID {}: {}", warehouseId, e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    private double calculateTotalVolume(int shelfCount, int zonesPerShelf,
                                        double zoneL, double zoneW, double zoneH) {
        double zoneVolumeLiters = (zoneL * zoneW * zoneH) / 1_000_000.0 * 1000.0;
        // zoneL, zoneW, zoneH в см: результат (см³) / 1000 = литры
        double zoneVolumeL = (zoneL * zoneW * zoneH) / 1000.0;
        return zoneVolumeL * shelfCount * zonesPerShelf;
    }

    private String generateShelfCode(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "ST-" + (index + 1);
    }
}
