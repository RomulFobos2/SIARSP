package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.BoxOrientation;

/**
 * Результат операции перемещения товара между зонами хранения
 */
public record MoveInfo(
        boolean success,
        String reason,
        Long fromZoneId,
        Long toZoneId,
        int quantity,
        BoxOrientation orientation
) {
    public static MoveInfo failure(String reason) {
        return new MoveInfo(false, reason, null, null, 0, null);
    }
}
