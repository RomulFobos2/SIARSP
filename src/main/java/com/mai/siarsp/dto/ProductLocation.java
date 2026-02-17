package com.mai.siarsp.dto;

/**
 * Местоположение товара на складе (одна запись = одна зона хранения)
 */
public record ProductLocation(
        Long zoneId,
        String zoneLabel,
        String shelfCode,
        String warehouseName,
        int quantity,
        String orientationLabel,
        double totalVolume
) {
}
