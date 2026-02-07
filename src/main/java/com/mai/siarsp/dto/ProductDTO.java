package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.WarehouseType;
import lombok.Data;

import java.util.List;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String article;
    private int stockQuantity;
    private int quantityForStock;
    private int reservedQuantity;
    private String image;
    private WarehouseType warehouseType;
    private Long categoryId;
    private String categoryName;
    private int availableQuantity;
    private List<ProductAttributeValueDTO> attributeValues;
}
