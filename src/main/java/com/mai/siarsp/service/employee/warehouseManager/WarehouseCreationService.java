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
 * Сервис создания и удаления складов со структурой (стеллажи и зоны)
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

    /**
     * Атомарно создаёт склад со структурой: стеллажи и зоны хранения.
     *
     * @param name          название склада
     * @param type          тип склада (REGULAR / REFRIGERATOR)
     * @param address       физический адрес
     * @param shelfCount    количество стеллажей
     * @param zonesPerShelf количество зон (полок) на каждом стеллаже
     * @param zoneLength    длина зоны в см
     * @param zoneWidth     ширина зоны в см
     * @param zoneHeight    высота зоны в см
     * @return созданный склад или empty при ошибке
     */
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

    /**
     * Удаляет склад, только если в нём нет товаров.
     *
     * @param warehouseId ID склада
     * @return true если склад удалён, false если есть товары или склад не найден
     */
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

    /**
     * Удаляет стеллаж, только если в его зонах нет товаров.
     *
     * @param shelfId ID стеллажа
     * @return true если стеллаж удалён, false если есть товары или стеллаж не найден
     */
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

    /**
     * Рассчитывает общий объём склада в литрах.
     * Формула: (zoneL × zoneW × zoneH) / 1_000_000 × shelfCount × zonesPerShelf
     */
    private double calculateTotalVolume(int shelfCount, int zonesPerShelf,
                                        double zoneL, double zoneW, double zoneH) {
        double zoneVolumeLiters = (zoneL * zoneW * zoneH) / 1_000_000.0 * 1000.0;
        // zoneL, zoneW, zoneH в см: результат (см³) / 1000 = литры
        double zoneVolumeL = (zoneL * zoneW * zoneH) / 1000.0;
        return zoneVolumeL * shelfCount * zonesPerShelf;
    }

    /**
     * Генерирует код стеллажа по порядковому индексу (0-based).
     * 0→A, 1→B, …, 25→Z, 26→ST-27, 27→ST-28, …
     */
    private String generateShelfCode(int index) {
        if (index < 26) {
            return String.valueOf((char) ('A' + index));
        }
        return "ST-" + (index + 1);
    }
}
