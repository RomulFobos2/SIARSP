package com.mai.siarsp.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderedProductDTO {
    private Long id;
    private int quantity;
    private BigDecimal price;
    private BigDecimal totalPrice;
    private Long clientOrderId;
    private Long productId;
    private String productName;
    private String productArticle;
}
