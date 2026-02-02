package com.mai.siarsp.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.mai.siarsp.enumeration.AttributeType;
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
@Table(name = "t_productAttribute")
@EqualsAndHashCode(of = "id")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
//Описание возможной характеристики категории
public class ProductAttribute {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeType dataType;

    @ToString.Exclude
    @ManyToMany(mappedBy = "attributes")
    private List<ProductCategory> categories = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========
    public ProductAttribute(String name, String unit, AttributeType dataType, List<ProductCategory> categories) {
        this.name = name;
        this.unit = unit;
        this.dataType = dataType;
        this.categories = categories;
    }
}
