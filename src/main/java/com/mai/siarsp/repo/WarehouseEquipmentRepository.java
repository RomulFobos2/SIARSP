package com.mai.siarsp.repo;

import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.WarehouseEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseEquipmentRepository extends JpaRepository<WarehouseEquipment, Long> {

    boolean existsByWarehouseAndName(Warehouse warehouse, String name);

    Optional<WarehouseEquipment> findByWarehouseAndName(Warehouse warehouse, String name);
}
