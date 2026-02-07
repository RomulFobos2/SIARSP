package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.AttributeType;
import lombok.Data;

@Data
public class ProductAttributeDTO {
    private Long id;
    private String name;
    private String unit;
    private AttributeType dataType;
}
