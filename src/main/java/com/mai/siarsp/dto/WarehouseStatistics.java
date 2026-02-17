package com.mai.siarsp.dto;

/**
 * Статистика использования склада
 */
public record WarehouseStatistics(
        int totalShelves,
        int totalZones,
        double totalCapacity,
        double usedVolume,
        double occupancyPercent
) {
}
