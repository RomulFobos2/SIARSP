package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WarehouseType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_product")
@EqualsAndHashCode(of = "id")
public class Product {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String article;

    @Column(nullable = false)
    private int stockQuantity; //кол-во на складе

    @Column(nullable = false)
    private int quantityForStock; //кол-во товара который не помещен на склад.

    @Column(nullable = false)
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseType warehouseType;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ProductCategory category;

    @ToString.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.PERSIST)
    private List<Supply> supplies = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========
    public Product(String name, String article, int stockQuantity,
                   WarehouseType warehouseType,
                   ProductCategory category,
                   List<ProductAttributeValue> attributeValues) {
        this.name = name;
        this.article = article;
        this.stockQuantity = stockQuantity;
        this.warehouseType = warehouseType;
        this.category = category;
        this.attributeValues = attributeValues;
    }

    // ========== МЕТОДЫ ==========
    @Transient
    public Double getPackageLength() {
        return getAttributeValueByName("Длина упаковки");
    }

    @Transient
    public Double getPackageWidth() {
        return getAttributeValueByName("Ширина упаковки");
    }

    @Transient
    public Double getPackageHeight() {
        return getAttributeValueByName("Высота упаковки");
    }

    private Double getAttributeValueByName(String attributeName) {
        return attributeValues.stream()
                .filter(av -> av.getAttribute().getName().equals(attributeName))
                .findFirst()
                .map(av -> av.getValue(Double.class))
                .orElse(null);
    }
}