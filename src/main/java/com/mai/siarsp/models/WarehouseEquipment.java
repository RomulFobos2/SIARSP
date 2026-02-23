package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.EquipmentStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Оборудование склада.
 *
 * Представляет различное оборудование и технические средства,
 * используемые на складах ИП "Левчук" для хранения и обработки товаров.
 *
 * Типы оборудования (см. EquipmentType entity):
 * - Стеллаж (металлические или деревянные конструкции для полок)
 * - Холодильная камера (холодильное оборудование)
 * - Поддон (деревянные или пластиковые поддоны для товара)
 * - Весы (для взвешивания товара при приемке и отгрузке)
 * - Погрузчик (для перемещения тяжелых грузов)
 * - Прочее (любое другое складское оборудование)
 *
 * Назначение учета оборудования:
 * - Инвентаризация основных средств
 * - Планирование технического обслуживания
 * - Контроль срока службы и амортизации
 * - Своевременная замена устаревшего оборудования
 * - Обеспечение работоспособности склада
 *
 * Связи:
 * - Принадлежит одному складу (Warehouse)
 * - Относится к одному типу оборудования (EquipmentType)
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

    /**
     * Наименование оборудования.
     * Полное название с указанием модели и производителя.
     * Уникально в рамках одного склада.
     *
     * Примеры:
     * - "Холодильная камера Polair КХН-11.75"
     * - "Стеллаж металлический MS STANDART 100/50"
     * - "Весы электронные Масса-К МК-6.2-А21"
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Серийный номер оборудования.
     * Уникальный номер, присвоенный производителем.
     */
    @Column(length = 100)
    private String serialNumber;

    /**
     * Дата производства оборудования.
     * Дата изготовления, указанная производителем.
     */
    @Column
    private LocalDate productionDate;

    /**
     * Срок полезного использования в годах.
     * Нормативный срок, в течение которого оборудование сохраняет
     * работоспособность при правильной эксплуатации.
     *
     * Типичные значения:
     * - 10 лет (холодильное оборудование)
     * - 15 лет (металлические стеллажи)
     * - 7 лет (весы электронные)
     * - 8 лет (погрузчики)
     * - 3 года (поддоны деревянные)
     *
     * null — срок службы не определён.
     */
    @Column
    private Integer usefulLifeYears;

    /**
     * Тип оборудования.
     * Ссылка на справочную сущность EquipmentType.
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private EquipmentType equipmentType;

    /**
     * Текущий статус оборудования.
     *
     * - IN_USE: в эксплуатации (по умолчанию при создании)
     * - UNDER_REPAIR: на ремонте/обслуживании
     * - WRITTEN_OFF: списано
     *
     * Изменяется руководителем (ROLE_EMPLOYEE_MANAGER).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentStatus status = EquipmentStatus.IN_USE;

    /**
     * Склад, на котором находится оборудование.
     * Одна единица оборудования принадлежит только одному складу.
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Warehouse warehouse;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создаёт новую запись об оборудовании склада.
     * Статус устанавливается в IN_USE по умолчанию.
     *
     * @param name          наименование оборудования с моделью
     * @param equipmentType тип оборудования
     * @param warehouse     склад, на котором находится оборудование
     */
    public WarehouseEquipment(String name, EquipmentType equipmentType, Warehouse warehouse) {
        this.name = name;
        this.equipmentType = equipmentType;
        this.warehouse = warehouse;
        this.status = EquipmentStatus.IN_USE;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Вычисляет дату окончания срока службы оборудования.
     * Добавляет срок полезного использования к дате производства.
     *
     * Формула: productionDate + usefulLifeYears
     *
     * @return дата окончания срока службы, или null если нет данных
     */
    @Transient
    public LocalDate getExpirationDate() {
        if (productionDate != null && usefulLifeYears != null) {
            return productionDate.plusYears(usefulLifeYears);
        }
        return null;
    }

    /**
     * Проверяет, истёк ли срок службы оборудования.
     * Сравнивает дату окончания срока службы с текущей датой.
     *
     * @return true если срок службы истёк, false если ещё в сроке или срок не определён
     */
    @Transient
    public boolean isExpired() {
        LocalDate expiration = getExpirationDate();
        return expiration != null && LocalDate.now().isAfter(expiration);
    }
}
