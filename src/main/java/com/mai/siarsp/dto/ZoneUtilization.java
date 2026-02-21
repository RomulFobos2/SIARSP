package com.mai.siarsp.dto;

/**
 * Информация об использовании зоны хранения (полки)
 */
public record ZoneUtilization(
        Long zoneId,
        String zoneLabel,
        String shelfCode,
        double occupancyPercent,
        double availableVolumeLiters,
        String recommendation
) {
}
