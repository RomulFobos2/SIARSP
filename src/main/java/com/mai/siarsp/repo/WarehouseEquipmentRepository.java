package com.mai.siarsp.repo;

import com.mai.siarsp.enumeration.EquipmentStatus;
import com.mai.siarsp.models.Warehouse;
import com.mai.siarsp.models.WarehouseEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseEquipmentRepository extends JpaRepository<WarehouseEquipment, Long> {

    boolean existsByWarehouseAndName(Warehouse warehouse, String name);

    boolean existsByWarehouseAndNameAndIdNot(Warehouse warehouse, String name, Long id);

    Optional<WarehouseEquipment> findByWarehouseAndName(Warehouse warehouse, String name);

    List<WarehouseEquipment> findAllByOrderByNameAsc();

    List<WarehouseEquipment> findByWarehouse_Id(Long warehouseId);

    List<WarehouseEquipment> findByStatus(EquipmentStatus status);

    List<WarehouseEquipment> findByStatusNot(EquipmentStatus status);
}
