package com.mai.siarsp.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
//Продукт
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String article; //Уникальное

    @Column(nullable = false)
    private int stockQuantity; //кол-во на складе

    @Column(nullable = false)
    private int quantityForStock; //кол-во товара который не помещен на склад.

    @Column(nullable = false)
    private String image;

    @ManyToOne
    @JoinColumn(nullable = false)
    private ProductCategory category;

    @ToString.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.PERSIST)
    private List<Supply> supplies = new ArrayList<>();

    public Product(String name, String article, int stockQuantity, ProductCategory category, List<ProductAttributeValue> attributeValues) {
        this.name = name;
        this.article = article;
        this.stockQuantity = stockQuantity;
        this.category = category;
        this.attributeValues = attributeValues;
    }

}