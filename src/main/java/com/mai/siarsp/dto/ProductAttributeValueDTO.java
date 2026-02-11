package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.AttributeType;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    /**
     * Возвращает отформатированное значение атрибута для отображения.
     * Для типа DATE преобразует ISO-формат (yyyy-MM-dd) в dd.MM.yyyy.
     * Для остальных типов возвращает значение как есть.
     */
    public String getFormattedValue() {
        if (attributeDataType == AttributeType.DATE && value != null && !value.isBlank()) {
            try {
                LocalDate date = LocalDate.parse(value.trim());
                return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            } catch (Exception e) {
                return value;
            }
        }
        return value;
    }
}
