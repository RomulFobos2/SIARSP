package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WarehouseType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Структура:
// Warehouse → Shelf → StorageZone (полка) → ZoneProduct (товар на полке + количество)
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_warehouse")
@EqualsAndHashCode(of = "id")
public class Warehouse {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseType type;

    @Column(nullable = false)
    private double totalVolume;

    @ToString.Exclude
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Shelf> shelves = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========
    public Warehouse(String name, WarehouseType type, double totalVolume) {
        this.name = name;
        this.type = type;
        this.totalVolume = totalVolume;
    }

    // ========== МЕТОДЫ ==========
    // TODO: Перенести в WarehouseService - бизнес-правило совместимости
    @Transient
    public boolean canStoreProduct(Product product) {
        return this.type == product.getWarehouseType();
    }
}
