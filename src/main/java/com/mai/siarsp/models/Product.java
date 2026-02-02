package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.WarehouseType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Товар
 *
 * Центральная сущность системы складского учета. Представляет товарную позицию,
 * которая закупается у поставщиков и продается клиентам.
 *
 * Согласно ТЗ, для товара хранится следующая информация:
 * - Идентификационный номер (article)
 * - Наименование
 * - Количество на складе
 * - Категория и характеристики
 * - Габариты упаковки (для размещения на складе)
 *
 * Особенности учета:
 * - stockQuantity - общее количество на складе (оприходованное)
 * - quantityForStock - товар получен, но еще не размещен физически на полках
 * - reservedQuantity - зарезервировано под заказы клиентов
 * - availableQuantity = stockQuantity - reservedQuantity (вычисляемое поле)
 *
 * Бизнес-процессы:
 * 1. Закупка: Delivery → stockQuantity↑, quantityForStock↑
 * 2. Размещение: ZoneProduct создается → quantityForStock↓
 * 3. Резервирование: ClientOrder создается → reservedQuantity↑
 * 4. Отгрузка: ZoneProduct удаляется → stockQuantity↓, reservedQuantity↓
 *
 * Связи:
 * - Принадлежит одной категории (ProductCategory)
 * - Имеет множество значений атрибутов (ProductAttributeValue)
 * - Участвует в поставках (Supply)
 * - Размещается в зонах склада (ZoneProduct)
 * - Заказывается клиентами (OrderedProduct)
 * - Запрашивается у поставщиков (RequestedProduct)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_product")
@EqualsAndHashCode(of = "id")
public class Product {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Наименование товара
     * Полное коммерческое название
     *
     * Примеры:
     * - "Молоко пастеризованное 3.2% 1л 'Простоквашино'"
     * - "Хлеб 'Бородинский' нарезной 400г"
     * - "Яблоки 'Гренни Смит' 1 сорт"
     *
     * Используется в:
     * - Интерфейсе для отображения товара
     * - Документах (накладные, договоры, акты)
     * - Отчетах и статистике
     */
    @Column(nullable = false)
    private String name;

    /**
     * Артикул товара (внутренний идентификационный номер)
     * Уникальный код товара в системе ИП "Левчук"
     *
     * Формат: свободный (например, "MLK-001", "HLB-BRD-400")
     *
     * Используется для:
     * - Быстрого поиска товара
     * - Идентификации в документах
     */
    @Column(unique = true, nullable = false)
    private String article;

    /**
     * Общее количество товара на складе (единиц)
     * Оприходованное количество товара
     *
     * Изменяется при:
     * - Приход товара от поставщика (Delivery) → увеличивается
     * - Отгрузка товара клиенту (ClientOrder) → уменьшается
     * - Списание по акту (WriteOffAct) → уменьшается
     *
     * Согласно, система предупреждает:
     * - О нехватке товара
     * - О переполнении склада
     */
    @Column(nullable = false)
    private int stockQuantity;

    /**
     * Количество товара, не размещенного на складе
     * Товар оприходован (stockQuantity учтено), но физически еще не размещен в зонах хранения
     *
     * Бизнес-процесс:
     * 1. Товар поступил от поставщика → stockQuantity↑, quantityForStock↑
     * 2. Заведующий размещает товар на полках → создается ZoneProduct, quantityForStock↓
     *
     * Используется для:
     * - Контроля процесса размещения товара (ТЗ п.118-120)
     * - Планирования работы склада
     * - Отчетности о неразмещенных товарах
     */
    @Column(nullable = false)
    private int quantityForStock;

    /**
     * Путь к изображению товара
     * Хранится путь к файлу изображения в файловой системе
     *
     * Используется для:
     * - Визуального отображения товара в каталоге
     * - Упрощения идентификации товара сотрудниками
     * - Формирования презентационных материалов
     */
    @Column(nullable = false)
    private String image;

    /**
     * Тип склада, на котором должен храниться товар
     * Определяет требования к условиям хранения
     *
     * Значения:
     * - STANDARD - обычный склад (для непродовольственных товаров)
     * - REFRIGERATED - холодильный склад (для скоропортящихся продуктов)
     *
     * Согласно ТЗ ИП "Левчук" поставляет продукты в детские сады,
     * школы и больницы, поэтому многие товары требуют холодильного хранения
     *
     * Используется при:
     * - Размещении товара на складе (проверка совместимости с типом склада)
     * - Планировании закупок (учет доступной емкости холодильников)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WarehouseType warehouseType;

    /**
     * Категория товара
     * Классификация товара для организации каталога и отчетности
     *
     * Примеры:
     * - Молоко (→ Молочная продукция)
     * - Хлеб белый (→ Хлебобулочные изделия)
     * - Яблоки (→ Овощи и фрукты)
     *
     * Используется для:
     * - Группировки товаров в интерфейсе
     * - Статистики по группам товаров (ТЗ п.169-170)
     * - Применения атрибутов (категория определяет набор характеристик)
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private ProductCategory category;

    /**
     * Список значений атрибутов товара
     * Характеристики товара (габариты упаковки, срок годности, вес и т.п.)
     *
     * Примеры атрибутов:
     * - Длина упаковки: 20 см
     * - Ширина упаковки: 10 см
     * - Высота упаковки: 25 см
     * - Срок годности: 5 дней
     * - Вес нетто: 1000 г
     *
     * Габариты упаковки используются для:
     * - Расчета объема товара
     * - Автоматического размещения на складе (подбор подходящей зоны)
     * - Оптимизации загрузки транспорта
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductAttributeValue> attributeValues = new ArrayList<>();

    /**
     * История поставок данного товара
     * Список всех поставок, в которых участвовал товар
     *
     * Используется для:
     * - Анализа закупочных цен
     * - Выбора оптимального поставщика
     * - Расчета средней закупочной цены
     * - Планирования заказов поставщикам
     */
    @ToString.Exclude
    @OneToMany(mappedBy = "product", cascade = CascadeType.PERSIST)
    private List<Supply> supplies = new ArrayList<>();

    /**
     * Количество товара, зарезервированного под заказы клиентов
     * Товар физически на складе (учтен в stockQuantity), но уже зарезервирован
     * для конкретных заказов и не может быть продан другим клиентам
     *
     * Бизнес-процесс:
     * 1. Создан заказ клиента → reservedQuantity↑
     * 2. Заказ отменен → reservedQuantity↓
     * 3. Товар отгружен → stockQuantity↓, reservedQuantity↓
     *
     * Используется для:
     * - Контроля доступности товара (availableQuantity = stockQuantity - reservedQuantity)
     * - Предотвращения продажи уже зарезервированного товара
     * - Планирования закупок с учетом обязательств перед клиентами
     */
    @Column(nullable = false)
    private int reservedQuantity;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новый товар с базовыми характеристиками
     *
     * @param name наименование товара
     * @param article уникальный артикул
     * @param stockQuantity начальное количество на складе
     * @param warehouseType тип склада (обычный/холодильный)
     * @param category категория товара
     * @param attributeValues список характеристик товара
     */
    public Product(String name, String article, int stockQuantity,
                   WarehouseType warehouseType,
                   ProductCategory category,
                   List<ProductAttributeValue> attributeValues) {
        this.name = name;
        this.article = article;
        this.stockQuantity = stockQuantity;
        this.warehouseType = warehouseType;
        this.category = category;
        this.attributeValues = attributeValues;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Получает длину упаковки товара из атрибутов
     * Используется для расчета объема и размещения на складе
     *
     * @return длина упаковки в см или null, если атрибут не задан
     */
    @Transient
    public Double getPackageLength() {
        return getAttributeValueByName("Длина упаковки");
    }

    /**
     * Получает ширину упаковки товара из атрибутов
     * Используется для расчета объема и размещения на складе
     *
     * @return ширина упаковки в см или null, если атрибут не задан
     */
    @Transient
    public Double getPackageWidth() {
        return getAttributeValueByName("Ширина упаковки");
    }

    /**
     * Получает высоту упаковки товара из атрибутов
     * Используется для расчета объема и размещения на складе
     *
     * @return высота упаковки в см или null, если атрибут не задан
     */
    @Transient
    public Double getPackageHeight() {
        return getAttributeValueByName("Высота упаковки");
    }

    /**
     * Получает значение атрибута товара по имени
     * Вспомогательный метод для извлечения числовых значений атрибутов
     *
     * @param attributeName название атрибута (например, "Длина упаковки")
     * @return числовое значение атрибута или null, если атрибут не найден
     */
    private Double getAttributeValueByName(String attributeName) {
        return attributeValues.stream()
                .filter(av -> av.getAttribute().getName().equals(attributeName))
                .findFirst()
                .map(av -> av.getValue(Double.class))
                .orElse(null);
    }

    /**
     * Вычисляет доступное для продажи количество товара
     * Учитывает резервирование под заказы клиентов
     *
     * Формула: availableQuantity = stockQuantity - reservedQuantity
     *
     * Используется для:
     * - Проверки возможности создания нового заказа
     * - Отображения в интерфейсе "в наличии"
     * - Формирования предупреждений о нехватке товара (ТЗ п.177)
     *
     * @return количество товара, доступное для новых заказов
     */
    @Transient
    public int getAvailableQuantity() {
        return stockQuantity - reservedQuantity;
    }
}