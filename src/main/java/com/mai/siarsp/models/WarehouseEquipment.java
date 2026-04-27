package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.EquipmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Экземпляр оборудования на складе с привязкой к зоне и техническим состоянием.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_warehouseEquipment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"warehouse_id", "name"}))
@EqualsAndHashCode(of = "id")
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

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private EquipmentType equipmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status = EquipmentStatus.IN_USE;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Warehouse warehouse;

    // ========== КОНСТРУКТОРЫ ==========

    public WarehouseEquipment(String name, EquipmentType equipmentType, Warehouse warehouse) {
        this.name = name;
        this.equipmentType = equipmentType;
        this.warehouse = warehouse;
        this.status = EquipmentStatus.IN_USE;
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
