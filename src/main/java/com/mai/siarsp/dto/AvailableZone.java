package com.mai.siarsp.dto;

/**
 * Доступная зона хранения при проверке возможности размещения товара
 */
public record AvailableZone(
        Long zoneId,
        String zoneLabel,
        String shelfCode,
        String warehouseName,
        int maxQuantity,
        String orientation,
        double occupancyPercent
) {
}
