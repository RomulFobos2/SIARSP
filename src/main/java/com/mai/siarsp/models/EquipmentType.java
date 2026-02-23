package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Тип складского оборудования.
 *
 * Справочная сущность, описывающая категорию оборудования.
 * Управляется администратором системы через CRUD-интерфейс.
 *
 * Примеры типов по умолчанию (инициализируются через RoleRunner):
 * - Стеллаж
 * - Холодильная камера
 * - Поддон
 * - Весы
 * - Погрузчик
 * - Прочее
 *
 * Связи:
 * - Один тип может быть присвоен множеству единиц WarehouseEquipment
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

    /**
     * Наименование типа оборудования.
     * Должно быть уникальным в системе.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создаёт тип оборудования с указанным наименованием.
     *
     * @param name наименование типа оборудования
     */
    public EquipmentType(String name) {
        this.name = name;
    }
}
