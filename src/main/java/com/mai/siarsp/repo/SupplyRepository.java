package com.mai.siarsp.repo;

import com.mai.siarsp.models.Delivery;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.Supply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SupplyRepository extends JpaRepository<Supply, Long> {

    boolean existsByDeliveryAndProduct(Delivery delivery, Product product);

    Optional<Supply> findByDeliveryAndProduct(Delivery delivery, Product product);

    long countByProductId(Long productId);

    List<Supply> findByProductIdOrderByDelivery_DeliveryDateDesc(Long productId);

    /**
     * Минимальный срок годности по партиям товара, лежащим на складе (есть хотя бы одна ZoneProduct
     * на эту партию) и не истёкшим к referenceDate.
     */
    @Query("SELECT MIN(s.expirationDate) FROM Supply s WHERE s.product.id = :productId " +
            "AND s.expirationDate IS NOT NULL AND s.expirationDate >= :referenceDate " +
            "AND EXISTS (SELECT 1 FROM ZoneProduct zp WHERE zp.supply = s)")
    LocalDate findEarliestUnexpiredOnStock(@Param("productId") Long productId,
                                           @Param("referenceDate") LocalDate referenceDate);

    /**
     * Партии, у которых есть остатки в зонах и срок годности уже истёк к указанной дате.
     */
    @Query("SELECT DISTINCT s FROM Supply s WHERE s.expirationDate IS NOT NULL " +
            "AND s.expirationDate < :today " +
            "AND EXISTS (SELECT 1 FROM ZoneProduct zp WHERE zp.supply = s)")
    List<Supply> findExpiredOnStock(@Param("today") LocalDate today);

    /**
     * Партии товара, годные на referenceDate (expirationDate >= referenceDate)
     * и имеющие хотя бы один ZoneProduct с положительным остатком.
     */
    @Query("SELECT DISTINCT zp.supply FROM ZoneProduct zp WHERE zp.supply.product.id = :productId " +
            "AND zp.supply.expirationDate >= :referenceDate AND zp.quantity > 0 " +
            "ORDER BY zp.supply.expirationDate ASC")
    List<Supply> findEligibleByProductAndDate(@Param("productId") Long productId,
                                              @Param("referenceDate") LocalDate referenceDate);
}
