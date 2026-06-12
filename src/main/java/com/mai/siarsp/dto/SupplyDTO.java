package com.mai.siarsp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SupplyDTO {
    private Long id;
    private BigDecimal purchasePrice;
    private String unit;
    private int quantity;
    private BigDecimal totalPrice;
    private Long deliveryId;
    private Long productId;
    private String productName;
    private String productArticle;
    private int deficitQuantity;
    private String deficitReason;
    private int orderedQuantity;
    private LocalDate productionDate;
    private LocalDate expirationDate;
}
