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

    @Query("SELECT COUNT(zp) > 0 FROM ZoneProduct zp WHERE zp.zone = :zone AND zp.supply.product = :product")
    boolean existsByZoneAndProduct(@Param("zone") StorageZone zone, @Param("product") Product product);

    /**
     * Любая (исторически — единственная) запись ZoneProduct указанного товара в зоне.
     * После перехода на партии в одной зоне может быть несколько партий одного товара —
     * метод возвращает первую найденную для совместимости со старым размещением.
     */
    @Query("SELECT zp FROM ZoneProduct zp WHERE zp.zone = :zone AND zp.supply.product = :product")
    List<ZoneProduct> findAllByZoneAndProduct(@Param("zone") StorageZone zone, @Param("product") Product product);

    default Optional<ZoneProduct> findByZoneAndProduct(StorageZone zone, Product product) {
        return findAllByZoneAndProduct(zone, product).stream().findFirst();
    }

    @Query("SELECT zp FROM ZoneProduct zp WHERE zp.supply.product = :product")
    List<ZoneProduct> findByProduct(@Param("product") Product product);

    @Query("SELECT zp FROM ZoneProduct zp WHERE zp.supply.product.id = :productId")
    List<ZoneProduct> findByProductId(@Param("productId") Long productId);

    List<ZoneProduct> findByZone(StorageZone zone);

    boolean existsByZone(StorageZone zone);

    @Query("SELECT COUNT(zp) > 0 FROM ZoneProduct zp WHERE zp.zone.shelf.warehouse.id = :warehouseId")
    boolean existsByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(zp) > 0 FROM ZoneProduct zp WHERE zp.zone.shelf.id = :shelfId")
    boolean existsByShelfId(@Param("shelfId") Long shelfId);

    @Query("SELECT zp FROM ZoneProduct zp WHERE zp.supply.product = :product AND zp.zone.shelf.warehouse.id = :warehouseId")
    List<ZoneProduct> findByProductAndWarehouseId(@Param("product") Product product,
                                                   @Param("warehouseId") Long warehouseId);

    @Query("SELECT COALESCE(SUM(zp.quantity), 0) FROM ZoneProduct zp WHERE zp.supply.product.id = :productId AND zp.zone.shelf.warehouse.id = :warehouseId")
    int sumQuantityByProductIdAndWarehouseId(@Param("productId") Long productId,
                                              @Param("warehouseId") Long warehouseId);

    @Query("SELECT COUNT(zp) FROM ZoneProduct zp WHERE zp.supply.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    /**
     * Все ZoneProduct'ы товара отсортированы по сроку годности их партии ASC (FEFO).
     * Партии без expirationDate уходят в конец.
     */
    @Query("SELECT zp FROM ZoneProduct zp WHERE zp.supply.product.id = :productId " +
            "ORDER BY zp.supply.expirationDate ASC NULLS LAST")
    List<ZoneProduct> findByProductIdOrderByExpirationAsc(@Param("productId") Long productId);
}
