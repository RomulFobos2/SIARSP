package com.mai.siarsp.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Категория товара
 *
 * Средний уровень классификации товаров в системе СИАРСП.
 * Объединяет товары схожего типа и определяет набор характеристик (атрибутов),
 * которые должны быть заполнены для товаров данной категории.
 *
 * Иерархия классификации:
 * GlobalProductCategory (Молочная продукция)
 *   → ProductCategory (Молоко пастеризованное)
 *     → Product (Молоко 3.2% 1л "Простоквашино")
 *   → ProductCategory (Творог)
 *     → Product (Творог 9% 200г "Домик в деревне")
 *
 * Каждая категория связана с набором атрибутов (ProductAttribute),
 * которые определяют, какие характеристики должны быть у товаров:
 * - Для "Молоко": жирность (%), объем (л), срок годности (дней)
 * - Для "Хлеб": вес (г), срок годности (дней)
 * - Для всех: длина, ширина, высота упаковки (см) - для складского учета
 *
 * Связи:
 * - Принадлежит одной глобальной категории (GlobalProductCategory)
 * - Имеет множество товаров (Product)
 * - Связана со множеством атрибутов (ProductAttribute) через many-to-many
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_productCategory",
        uniqueConstraints = @UniqueConstraint(columnNames = {"global_product_category_id", "name"}))
@EqualsAndHashCode(of = "id")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ProductCategory {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название категории
     * Конкретный тип товара в рамках глобальной категории
     */
    @Column(nullable = false)
    private String name;

    /**
     * Глобальная категория, к которой относится данная категория
     * Верхний уровень иерархии классификации
     * EAGER загрузка используется потому что:
     * - Глобальная категория практически всегда нужна при работе с категорией
     * - Размер справочника глобальных категорий небольшой (10-20 записей)
     * - Избегаем проблем с lazy loading в некоторых контекстах
     */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(nullable = false)
    private GlobalProductCategory globalProductCategory;

    /**
     * Список атрибутов (характеристик), применимых к товарам данной категории
     * Связь многие-ко-многим: один атрибут может применяться к нескольким категориям
     *
     * Примеры наборов атрибутов:
     *
     * Для категории "Молоко":
     * - "Жирность" (NUMBER, %)
     * - "Объем" (NUMBER, л)
     * - "Срок годности" (NUMBER, дней)
     * - "Длина упаковки" (NUMBER, см)
     * - "Ширина упаковки" (NUMBER, см)
     * - "Высота упаковки" (NUMBER, см)
     *
     * Для категории "Хлеб":
     * - "Вес" (NUMBER, г)
     * - "Срок годности" (NUMBER, дней)
     * - "Длина упаковки" (NUMBER, см)
     * - "Ширина упаковки" (NUMBER, см)
     * - "Высота упаковки" (NUMBER, см)
     */
    @ToString.Exclude
    @ManyToMany
    @JoinTable(
            name = "category_attribute",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "attribute_id")
    )
    private List<ProductAttribute> attributes = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========
    // Lombok @NoArgsConstructor создает конструктор без параметров

    // ========== МЕТОДЫ ==========

    /**
     * Возвращает название глобальной категории
     * Вспомогательный метод для удобства доступа к вложенному объекту
     *
     * @return название глобальной категории (например, "Молочная продукция")
     */
    @Transient
    public String getGlobalProductCategoryName() {
        return globalProductCategory.getName();
    }

    /**
     * Возвращает полное отображаемое название категории
     * Комбинирует название категории и глобальной категории для наглядности
     * @return строка вида "Категория (Глобальная категория)"
     */
    @Transient
    public String getDisplayName() {
        return name + " (" + getGlobalProductCategoryName() + ")";
    }

}