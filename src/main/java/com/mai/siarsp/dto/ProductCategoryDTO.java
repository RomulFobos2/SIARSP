package com.mai.siarsp.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductCategoryDTO {
    private Long id;
    private String name;
    private Long globalProductCategoryId;
    private String globalProductCategoryName;
    private String displayName;
    private List<ProductAttributeDTO> attributes;
}
