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
    /**
     * Срок хранения товара в днях (атрибут «Срок годности» теперь хранится как Integer).
     * Реальная дата годности живёт в Supply.expirationDate каждой партии.
     */
    private Integer shelfLifeDays;
    /**
     * Единица измерения товара (атрибут «Единица измерения»).
     * Используется на форме создания заявки для автоподстановки в поле «Ед. изм.».
     */
    private String unitOfMeasure;
}
