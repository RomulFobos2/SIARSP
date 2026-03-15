package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Справочник типов складского оборудования (стеллаж, холодильник и т.п.), чтобы единообразно описывать инфраструктуру склада.
 */

@Entity
@Table(name = "t_equipmentType")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class EquipmentType {

    // ========== ПОЛЯ ==========

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // ========== КОНСТРУКТОРЫ ==========

    public EquipmentType(String name) {
        this.name = name;
    }
}
