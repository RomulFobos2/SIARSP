package com.mai.siarsp.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
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
@Table(name = "t_productCategory")
@EqualsAndHashCode(of = "id")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
//Категория товара
public class ProductCategory {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private GlobalProductCategory globalProductCategory;

    @ToString.Exclude
    @ManyToMany
    @JoinTable(
            name = "category_attribute",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "attribute_id")
    )
    private List<ProductAttribute> attributes = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========


    // ========== МЕТОДЫ ==========
    @Transient
    public String getGlobalProductCategoryName() {
        return globalProductCategory.getName();
    }

    @Transient
    public String getDisplayName() {
        return name + " (" + getGlobalProductCategoryName() + ")";
    }

}
