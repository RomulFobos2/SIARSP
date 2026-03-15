package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Зона хранения со своими правилами (температура, типы товаров, нагрузка). Служит базой для адресного размещения.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_storageZone",
        uniqueConstraints = @UniqueConstraint(columnNames = {"shelf_id", "label"}))
@EqualsAndHashCode(of = "id")
public class StorageZone {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private double length;

    @Column(nullable = false)
    private double width;

    @Column(nullable = false)
    private double height;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Shelf shelf;

    @ToString.Exclude
    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ZoneProduct> products = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========

    public StorageZone(String label, double length, double width, double height, Shelf shelf) {
        this.label = label;
        this.length = length;
        this.width = width;
        this.height = height;
        this.shelf = shelf;
    }

    // ========== МЕТОДЫ ==========

    public double getCapacityVolume() {
        return (length * width * height) / 1_000_000.0;
    }

    public double getOccupancyPercentage() {
        if (products == null || products.isEmpty()) {
            return 0.0;
        }

        double used = products.stream()
                .mapToDouble(ZoneProduct::getTotalVolume)
                .sum();
        double capacity = getCapacityVolume();

        return capacity > 0 ? (used / capacity) * 100.0 : 0.0;
    }

    @Transient
    public boolean canStoreProduct(Product product) {
        if (shelf == null || shelf.getWarehouse() == null) {
            return false;
        }
        return shelf.getWarehouse().canStoreProduct(product);
    }

}