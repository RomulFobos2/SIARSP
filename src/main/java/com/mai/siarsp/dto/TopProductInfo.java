package com.mai.siarsp.dto;

/**
 * Краткая информация о топ-товаре по объёму хранения
 */
public record TopProductInfo(
        String productName,
        int totalQuantity,
        double volumeUsedM3
) {
}
