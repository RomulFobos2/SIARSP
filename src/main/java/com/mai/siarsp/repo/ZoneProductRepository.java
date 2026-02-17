package com.mai.siarsp.repo;

import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.ZoneProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZoneProductRepository extends JpaRepository<ZoneProduct, Long> {

    boolean existsByZoneAndProduct(StorageZone zone, Product product);

    Optional<ZoneProduct> findByZoneAndProduct(StorageZone zone, Product product);

    List<ZoneProduct> findByProduct(Product product);

    List<ZoneProduct> findByZone(StorageZone zone);

    boolean existsByZone(StorageZone zone);

    @Query("SELECT COUNT(zp) > 0 FROM ZoneProduct zp WHERE zp.zone.shelf.warehouse.id = :warehouseId")
    boolean existsByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(zp) > 0 FROM ZoneProduct zp WHERE zp.zone.shelf.id = :shelfId")
    boolean existsByShelfId(@Param("shelfId") Long shelfId);
}
