package com.mai.siarsp.dto;

import lombok.Data;

@Data
public class RequestedProductDTO {
    private Long id;
    private int quantity;
    private Long requestId;
    private Long productId;
    private String productName;
    private String productArticle;
}
