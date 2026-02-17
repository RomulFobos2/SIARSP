package com.mai.siarsp.dto;

/**
 * Результат операции изъятия товара из зоны хранения
 */
public record RemovalInfo(
        boolean success,
        String reason,
        int removedQuantity
) {
    public static RemovalInfo failure(String reason) {
        return new RemovalInfo(false, reason, 0);
    }
}
