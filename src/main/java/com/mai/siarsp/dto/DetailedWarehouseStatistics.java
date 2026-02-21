package com.mai.siarsp.dto;

import java.util.List;

/**
 * Расширенная статистика по складу для страницы аналитики
 */
public record DetailedWarehouseStatistics(
        String warehouseName,
        int totalShelves,
        int totalZones,
        double totalCapacityLiters,
        double usedVolumeLiters,
        double occupancyPercent,
        int totalProductUnits,
        int uniqueProducts,
        List<TopProductInfo> topProducts
) {
}
