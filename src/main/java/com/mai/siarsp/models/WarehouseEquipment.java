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

/**
 * Оборудование склада
 *
 * Представляет различное оборудование и технические средства,
 * используемые на складах ИП "Левчук" для хранения и обработки товаров.
 *
 * Типы оборудования (см. EquipmentType enum):
 * - RACK - стеллаж (металлические или деревянные конструкции для полок)
 * - FRIDGE - холодильник (холодильное оборудование)
 * - PALLET - поддон (деревянные или пластиковые поддоны для товара)
 * - SCALE - весы (для взвешивания товара при приемке и отгрузке)
 * - FORKLIFT - погрузчик (для перемещения тяжелых грузов)
 * - OTHER - прочее оборудование (любое другое складское оборудование)
 *
 * Назначение учета оборудования:
 * - Инвентаризация основных средств
 * - Планирование технического обслуживания
 * - Контроль срока службы и амортизации
 * - Своевременная замена устаревшего оборудования
 * - Обеспечение работоспособности склада
 *
 * Примеры оборудования:
 *
 * Холодильный склад:
 * - Холодильная камера "Polair КХН-11.75" (FRIDGE, 11.75 м³)
 * - Стеллажи металлические (RACK)
 * - Весы электронные "Масса-К МК-6.2-А21" (SCALE)
 * - Поддоны пластиковые (PALLET)
 *
 * Обычный склад:
 * - Стеллажи металлические (RACK)
 * - Погрузчик гидравлический (FORKLIFT)
 * - Весы платформенные (SCALE)
 * - Поддоны деревянные (PALLET)
 *
 * Связи:
 * - Принадлежит одному складу (Warehouse)
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
     * Наименование оборудования
     * Полное название с указанием модели и производителя
     *
     * Примеры:
     * - "Холодильная камера Polair КХН-11.75"
     * - "Стеллаж металлический MS STANDART 100/50"
     * - "Весы электронные Масса-К МК-6.2-А21"
     * - "Погрузчик гидравлический AC Hydraulic WJ30"
     * - "Поддон пластиковый 1200×800"
     * - "Тележка грузовая 200 кг"
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Серийный номер оборудования
     * Уникальный номер, присвоенный производителем
     */
    @Column(length = 100)
    private String serialNumber;

    /**
     * Дата производства оборудования
     * Дата изготовления, указанная производителем
     */
    @Column
    private LocalDate productionDate;

    /**
     * Срок полезного использования в годах
     * Нормативный срок, в течение которого оборудование сохраняет
     * работоспособность при правильной эксплуатации
     *
     * Типичные значения:
     * - 10 лет (холодильное оборудование)
     * - 15 лет (металлические стеллажи)
     * - 7 лет (весы электронные)
     * - 8 лет (погрузчики)
     * - 3 года (поддоны деревянные)
     *
     * Используется для:
     * - Расчета даты окончания срока службы (getExpirationDate())
     * - Планирования замены оборудования
     * - Начисления амортизации
     * - Контроля состояния оборудования
     * - Бюджетирования закупки нового оборудования
     *
     * Формула даты окончания срока службы:
     * expirationDate = productionDate + usefulLifeYears
     *
     * null - срок службы не определен
     */
    @Column
    private Integer usefulLifeYears;


    /**
     * Тип оборудования
     *
     * Возможные типы (см. EquipmentType enum):
     * - RACK - стеллаж (конструкция для полок)
     * - FRIDGE - холодильник (холодильное оборудование)
     * - PALLET - поддон (для складирования товара)
     * - SCALE - весы (для взвешивания)
     * - FORKLIFT - погрузчик (для перемещения грузов)
     * - OTHER - прочее (любое другое оборудование)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentType equipmentType;

    /**
     * Склад, на котором находится оборудование
     * Одна единица оборудования принадлежит только одному складу
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Warehouse warehouse;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новую запись об оборудовании склада
     *
     * @param name наименование оборудования с моделью
     * @param equipmentType тип оборудования (RACK, FRIDGE, PALLET и т.д.)
     * @param warehouse склад, на котором находится оборудование
     */
    public WarehouseEquipment(String name, EquipmentType equipmentType, Warehouse warehouse) {
        this.name = name;
        this.equipmentType = equipmentType;
        this.warehouse = warehouse;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Вычисляет дату окончания срока службы оборудования
     * Добавляет срок полезного использования к дате производства
     *
     * Формула: productionDate + usefulLifeYears
     *
     * Примеры:
     * - Произведено: 2020-03-15, Срок службы: 10 лет
     *   → Окончание срока: 2030-03-15
     *
     * - Произведено: 2018-11-01, Срок службы: 7 лет
     *   → Окончание срока: 2025-11-01
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
     * Проверяет, истек ли срок службы оборудования
     * Сравнивает дату окончания срока службы с текущей датой
     *
     * Логика:
     * - Если expirationDate < текущая_дата → true (истек)
     * - Если expirationDate >= текущая_дата → false (еще в сроке)
     * - Если expirationDate == null → false (срок не определен)
     * @return true если срок службы истек, false если еще в сроке или срок не определен
     */
    @Transient
    public boolean isExpired() {
        LocalDate expiration = getExpirationDate();
        return expiration != null && LocalDate.now().isAfter(expiration);
    }
}