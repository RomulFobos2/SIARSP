package com.mai.siarsp.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO для приёма данных из формы приёмки поставки.
 *
 * Содержит информацию о фактически полученном товаре:
 * - productId — идентификатор товара
 * - quantity — фактически принятое количество
 * - purchasePrice — закупочная цена за единицу
 * - deficitQuantity — количество недопоставленного товара (рассчитывается на бэке)
 * - deficitReason — причина недопоставки (обязательно при deficitQuantity > 0)
 */
@Data
public class SupplyInputDTO {
    private Long productId;
    private int quantity;
    private BigDecimal purchasePrice;
    private int deficitQuantity;
    private String deficitReason;
}
