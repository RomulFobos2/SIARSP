package com.mai.siarsp.repo;

import com.mai.siarsp.models.Shelf;
import com.mai.siarsp.models.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShelfRepository extends JpaRepository<Shelf, Long> {

    boolean existsByWarehouseAndCode(Warehouse warehouse, String code);

    Optional<Shelf> findByWarehouseAndCode(Warehouse warehouse, String code);

    List<Shelf> findByWarehouse(Warehouse warehouse);

    boolean existsByWarehouse(Warehouse warehouse);
}
