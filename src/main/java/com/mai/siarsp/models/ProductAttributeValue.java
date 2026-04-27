package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.AttributeType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Конкретное значение атрибута для товара. Позволяет гибко задавать характеристики под разные категории.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_productAttributeValue",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "attribute_id"}))
@EqualsAndHashCode(of = "id")
public class ProductAttributeValue {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500, nullable = false)
    private String value;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ProductAttribute attribute;

    // ========== КОНСТРУКТОРЫ ==========

    public ProductAttributeValue(Product product, ProductAttribute attribute, String value) {
        this.product = product;
        this.attribute = attribute;
        this.value = value;
    }

    // ========== МЕТОДЫ ==========

    public <T> T getValue(Class<T> type) {
        if (attribute == null) return null;

        // DATE - Преобразование в LocalDate
        if (type == LocalDate.class && attribute.getDataType() == AttributeType.DATE) {
            try {
                return type.cast(LocalDate.parse(value.trim()));
            } catch (Exception e) {
                return null; // Некорректный формат даты
            }
        }

        // NUMBER - Преобразование в Double
        if (type == Double.class && attribute.getDataType() == AttributeType.NUMBER) {
            try {
                // Поддержка как точки, так и запятой как разделителя
                return type.cast(Double.parseDouble(value.replace(",", ".").trim()));
            } catch (NumberFormatException e) {
                return null; // Некорректное число
            }
        }

        // NUMBER - Преобразование в Integer
        if (type == Integer.class && attribute.getDataType() == AttributeType.NUMBER) {
            try {
                // Сначала парсим как Double (чтобы поддержать "3,5" → 3)
                Double d = Double.parseDouble(value.replace(",", ".").trim());
                return type.cast(d.intValue());
            } catch (NumberFormatException e) {
                return null; // Некорректное число
            }
        }

        // TEXT - Возвращаем строку как есть
        if (type == String.class) {
            return type.cast(value);
        }

        // Несовместимые типы - возвращаем null
        return null;
    }
}