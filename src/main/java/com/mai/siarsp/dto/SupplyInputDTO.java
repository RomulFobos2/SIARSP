package com.mai.siarsp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO для приёма данных из формы приёмки поставки.
 *
 * Содержит информацию о фактически полученной партии:
 * - productId — идентификатор товара
 * - quantity — фактически принятое количество
 * - purchasePrice — закупочная цена за единицу
 * - deficitQuantity — количество недопоставленного товара (рассчитывается на бэке)
 * - deficitReason — причина недопоставки (обязательно при deficitQuantity > 0)
 * - productionDate — дата производства партии (срок годности рассчитывается на сервере)
 */
@Data
public class SupplyInputDTO {
    private Long productId;
    private int quantity;
    private BigDecimal purchasePrice;
    private int deficitQuantity;
    private String deficitReason;
    private LocalDate productionDate;
}
