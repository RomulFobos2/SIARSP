package com.mai.siarsp.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RequestedProductDTO {
    private Long id;
    private int quantity;
    private Long requestId;
    private Long productId;
    private String productName;
    private String productArticle;
    private BigDecimal purchasePrice;  // цена за единицу
    private BigDecimal totalPrice;     // вычисляемое (price × quantity)
}
