package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.EquipmentType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_warehouseEquipment")
@EqualsAndHashCode(of = "id")
//Оборудование склада
public class WarehouseEquipment {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String serialNumber;

    @Column
    private LocalDate productionDate;

    @Column
    private Integer usefulLifeYears;

    @Column
    private Double volumeCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType equipmentType;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Warehouse warehouse;

    @Column(length = 500)
    private String comment;

    // ========== КОНСТРУКТОРЫ ==========
    public WarehouseEquipment(String name, EquipmentType equipmentType, Warehouse warehouse) {
        this.name = name;
        this.equipmentType = equipmentType;
        this.warehouse = warehouse;
    }

    // ========== МЕТОДЫ ==========
    @Transient
    public LocalDate getExpirationDate() {
        if (productionDate != null && usefulLifeYears != null) {
            return productionDate.plusYears(usefulLifeYears);
        }
        return null;
    }

    @Transient
    public boolean isExpired() {
        LocalDate expiration = getExpirationDate();
        return expiration != null && LocalDate.now().isAfter(expiration);
    }
}
