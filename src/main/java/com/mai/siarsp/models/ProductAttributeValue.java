package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.AttributeType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_productAttributeValue")
@EqualsAndHashCode(of = "id")
//Значение конкретной характеристики для товара
public class ProductAttributeValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ProductAttribute attribute;

    @Column(length = 500, nullable = false)
    private String value;

    public ProductAttributeValue(Product product, ProductAttribute attribute, String value) {
        this.product = product;
        this.attribute = attribute;
        this.value = value;
    }

    /*Использовать
    LocalDate date = attrValue.getValue(LocalDate.class);
    Double number = attrValue.getValue(Double.class);
    String text = attrValue.getValue(String.class);

    //Если типы не совпадают - вернет null (не упадет!)
    String wrong = attrValue.getValue(String.class); // для DATE вернет null
     */
    public <T> T getValue(Class<T> type) {
        if (attribute == null) return null;

        // DATE
        if (type == LocalDate.class && attribute.getDataType() == AttributeType.DATE) {
            try {
                return type.cast(LocalDate.parse(value.trim()));
            } catch (Exception e) {
                return null;
            }
        }

        // NUMBER - Double
        if (type == Double.class && attribute.getDataType() == AttributeType.NUMBER) {
            try {
                return type.cast(Double.parseDouble(value.replace(",", ".").trim()));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // NUMBER - Integer
        if (type == Integer.class && attribute.getDataType() == AttributeType.NUMBER) {
            try {
                Double d = Double.parseDouble(value.replace(",", ".").trim());
                return type.cast(d.intValue());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // TEXT
        if (type == String.class) {
            return type.cast(value);
        }

        // Несовместимые типы
        return null;
    }
}
