package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.BoxOrientation;

/**
 * Результат проверки возможности размещения товара (без изменения данных)
 */
public record PlacementCheck(
        boolean possible,
        String reason,
        int maxPossible,
        BoxOrientation bestOrientation
) {
    public static PlacementCheck impossible(String reason) {
        return new PlacementCheck(false, reason, 0, null);
    }
}
