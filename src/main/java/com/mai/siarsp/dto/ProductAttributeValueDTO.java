package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.AttributeType;
import lombok.Data;

@Data
public class ProductAttributeValueDTO {
    private Long id;
    private String value;
    private Long productId;
    private String productArticle;
    private Long attributeId;
    private String attributeName;
    private String attributeUnit;
    private AttributeType attributeDataType;
}
