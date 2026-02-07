package com.mai.siarsp.repo;

import com.mai.siarsp.models.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    boolean existsByName(String name);

    Optional<Warehouse> findByName(String name);
}
