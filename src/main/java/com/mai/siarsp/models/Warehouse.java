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

/**
 * Склад для хранения товаров
 *
 * Основная единица складского учета в системе СИАРСП.
 * Представляет физическое помещение для хранения товаров ИП "Левчук".
 * Содержит иерархическую структуру: Склад → Стеллаж → Полка → Товар.
 *
 * Иерархия складского учета:
 * Warehouse (Основной склад, 5000 л)
 *   → Shelf (Стеллаж A)
 *     → StorageZone (Полка A1: 200×80×40 см = 640 л)
 *       → ZoneProduct (Молоко 3.2% - 50 шт., 115 л)
 *       → ZoneProduct (Хлеб белый - 30 шт., 86 л)
 *     → StorageZone (Полка A2: 200×80×40 см = 640 л)
 *       → ZoneProduct (Сметана 20% - 100 шт., 120 л)
 *   → Shelf (Стеллаж B)
 *     → ...
 *
 * Типы складов:
 * 1. STANDARD - обычный склад (для товаров стандартного хранения)
 *    - Хлебобулочные изделия
 *    - Бакалея
 *    - Овощи и фрукты (нескоропортящиеся)
 *
 * 2. REFRIGERATED - холодильный склад (для скоропортящихся товаров)
 *    - Молочная продукция
 *    - Мясная продукция
 *    - Рыбная продукция
 *    - Скоропортящиеся овощи и фрукты
 * Бизнес-правила:
 * - На холодильном складе могут храниться ТОЛЬКО товары с warehouseType = REFRIGERATED
 * - На обычном складе могут храниться ТОЛЬКО товары с warehouseType = STANDARD
 * - Нельзя разместить скоропортящийся товар на обычном складе
 * - Нельзя разместить обычный товар на холодильном складе (неэффективное использование)
 *
 * Связи:
 * - Содержит множество стеллажей (Shelf)
 * - Имеет оборудование (WarehouseEquipment)
 */
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

    /**
     * Название склада
     * Понятное наименование для идентификации склада
     *
     * Примеры:
     * - "Основной склад"
     * - "Холодильный склад №1"
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Тип склада
     *
     * Возможные типы (см. WarehouseType enum):
     * - STANDARD - обычный склад (комнатная температура, ~18-25°C)
     * - REFRIGERATED - холодильный склад (пониженная температура, +2-+6°C)
     *
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseType type;

    /**
     * Общий объем склада в литрах
     * Максимальная вместимость всего складского помещения
     *
     * Примеры:
     * - 5000.0 л (небольшой склад, 5 м³)
     * - 10000.0 л (средний склад, 10 м³)
     * - 50000.0 л (большой склад, 50 м³)
     * - 100000.0 л (очень большой склад, 100 м³)
     *
     * Расчет на основе габаритов помещения:
     * totalVolume = (длина × ширина × высота) / 1000
     * Пример: помещение 10м × 5м × 3м = 150 м³ = 150 000 л  <--- Почему в литрах??
     *
     */
    @Column(nullable = false)
    private double totalVolume;

    /**
     * Адрес склада
     * Физический адрес местонахождения складского помещения
     *
     * Используется для:
     * - Формирования договоров поставки (адрес доставки товара)
     * - Координации логистики
     * - Документации и отчетности
     *
     * Примеры:
     * - "г. Минск, ул. Складская, д. 15"
     * - "г. Гродно, пр-т Космонавтов, д. 42, корп. 2"
     */
    @Column
    private String address;

    /**
     * Широта местоположения склада (GPS)
     * Используется для автоматического заполнения маршрута доставки
     */
    @Column
    private Double latitude;

    /**
     * Долгота местоположения склада (GPS)
     * Используется для автоматического заполнения маршрута доставки
     */
    @Column
    private Double longitude;

    /**
     * Список стеллажей на складе
     * Физические конструкции для размещения полок с товаром
     *
     * Структура:
     * Склад "Основной склад":
     *   - Стеллаж A (молочная продукция)
     *     - Полка A1, A2, A3...
     *   - Стеллаж B (хлебобулочные изделия)
     *     - Полка B1, B2, B3...
     *   - Стеллаж C (овощи и фрукты)
     *     - Полка C1, C2, C3...
     *
     * Каждый стеллаж (Shelf):
     * - Имеет уникальный код (A, B, C...)
     * - Содержит полки (StorageZone)
     * - Полки содержат товары (ZoneProduct)
     *
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Shelf> shelves = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новый склад
     * Инициализирует пустой список стеллажей (будут добавлены позже)
     *
     * @param name название склада (например, "Основной склад")
     * @param type тип склада (STANDARD или REFRIGERATED)
     * @param totalVolume общий объем склада в литрах
     */
    public Warehouse(String name, WarehouseType type, double totalVolume) {
        this.name = name;
        this.type = type;
        this.totalVolume = totalVolume;
    }

    /**
     * Создает новый склад с адресом
     * Инициализирует пустой список стеллажей (будут добавлены позже)
     *
     * @param name название склада (например, "Основной склад")
     * @param type тип склада (STANDARD или REFRIGERATED)
     * @param totalVolume общий объем склада в литрах
     * @param address физический адрес склада
     */
    public Warehouse(String name, WarehouseType type, double totalVolume, String address) {
        this.name = name;
        this.type = type;
        this.totalVolume = totalVolume;
        this.address = address;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Проверяет возможность размещения товара на данном складе
     * Проверяет совместимость типа склада с требованиями товара к хранению
     *
     * Бизнес-правило:
     * Товар можно разместить на складе, если:
     * warehouse.type == product.warehouseType
     *
     * Логика:
     * - REFRIGERATED склад + REFRIGERATED товар → true ✓
     * - STANDARD склад + STANDARD товар → true ✓
     * - REFRIGERATED склад + STANDARD товар → false ✗ (неэффективно)
     * - STANDARD склад + REFRIGERATED товар → false ✗ (испортится!)
     * TODO: Перенести в WarehouseService - бизнес-правило совместимости
     * Этот метод содержит бизнес-логику и должен быть в сервисном слое,
     * а не в entity-классе
     *
     * @param product товар, который планируется разместить на складе
     * @return true если товар можно разместить (типы совместимы), false иначе
     */
    @Transient
    public boolean canStoreProduct(Product product) {
        return this.type == product.getWarehouseType();
    }
}