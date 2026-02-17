package com.mai.siarsp.service.employee.warehouseManager;

import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.repo.WarehouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис для работы со складами в контексте заявок на поставку.
 */
@Service("warehouseManagerWarehouseService")
@Slf4j
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }
}
