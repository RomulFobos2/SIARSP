package com.mai.siarsp.repo;

import com.mai.siarsp.models.OrderedProductPick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderedProductPickRepository extends JpaRepository<OrderedProductPick, Long> {

    /**
     * Сколько единиц партии в зоне уже зарезервировано существующими пиками
     * (всеми, независимо от статуса заказа). Используется для защиты от двойного резерва
     * одной (supply, zone) разными заказами на этапе сборки.
     */
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM OrderedProductPick p " +
            "WHERE p.zone.id = :zoneId AND p.supply.id = :supplyId")
    int sumQuantityByZoneAndSupply(@Param("zoneId") Long zoneId,
                                    @Param("supplyId") Long supplyId);
}
