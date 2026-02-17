package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.BoxOrientation;

/**
 * Результат операции размещения товара в зоне хранения
 */
public record PlacementInfo(
        boolean success,
        String reason,
        Long zoneId,
        String zoneLabel,
        String shelfCode,
        String warehouseName,
        BoxOrientation orientation,
        int quantity
) {
    public static PlacementInfo failure(String reason) {
        return new PlacementInfo(false, reason, null, null, null, null, null, 0);
    }
}
