package com.mai.siarsp.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.mai.siarsp.enumeration.AttributeType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Атрибут (характеристика) товара
 *
 * Описание возможной характеристики для категории товаров.
 * Определяет название атрибута, единицу измерения и тип данных.
 *
 * Система использует EAV (Entity-Attribute-Value) паттерн для гибкого
 * хранения характеристик товаров. Это позволяет:
 * - Добавлять новые характеристики без изменения структуры БД
 * - Иметь разные наборы характеристик для разных категорий товаров
 * - Хранить характеристики разных типов (текст, число, дата)
 *
 * Примеры атрибутов:
 *
 * Для категории "Молочная продукция":
 * - "Жирность" (NUMBER, единица "%")
 * - "Срок годности" (NUMBER, единица "дней")
 * - "Объем" (NUMBER, единица "л")
 *
 * Для всех категорий (обязательные для склада):
 * - "Длина упаковки" (NUMBER, единица "см")
 * - "Ширина упаковки" (NUMBER, единица "см")
 * - "Высота упаковки" (NUMBER, единица "см")
 *
 * Связи:
 * - Один атрибут может принадлежать множеству категорий (ProductCategory)
 * - Для каждого товара атрибут имеет конкретное значение (ProductAttributeValue)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_productAttribute")
@EqualsAndHashCode(of = "id")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ProductAttribute {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название атрибута
     * Человекочитаемое имя характеристики
     *
     * Примеры:
     * - "Жирность"
     * - "Срок годности"
     * - "Длина упаковки"
     * - "Вес нетто"
     * - "Производитель"
     *
     * Используется в:
     * - Интерфейсе для отображения характеристик товара
     * - Формах ввода данных о товаре
     * - Фильтрах и поиске по характеристикам
     */
    @Column(nullable = false)
    private String name;

    /**
     * Единица измерения атрибута
     * Опциональное поле для числовых атрибутов
     *
     * Примеры:
     * - "см" (для габаритов)
     * - "%" (для жирности)
     * - "г" (для веса)
     * - "л" (для объема)
     * - "дней" (для срока годности)
     * - null (для текстовых атрибутов, например, "Производитель")
     *
     * Используется для:
     * - Отображения характеристик с правильными единицами (например, "25 см")
     * - Валидации вводимых значений
     * - Конвертации между единицами (если потребуется)
     */
    @Column(nullable = true)
    private String unit;

    /**
     * Тип данных атрибута
     * Определяет, какого типа значения могут храниться в этом атрибуте
     *
     * Возможные типы (enum AttributeType):
     * - TEXT - текстовые значения (например, "Производитель" → "ООО 'Молочный комбинат'")
     * - NUMBER - числовые значения (например, "Жирность" → 3.2)
     * - DATE - даты (например, "Дата производства" → 2025-02-01)
     *
     * Используется для:
     * - Правильного отображения полей ввода в интерфейсе
     * - Валидации вводимых значений
     * - Типобезопасного получения значений через ProductAttributeValue.getValue(Class<T>)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttributeType dataType;

    /**
     * Список категорий, к которым применим данный атрибут
     * Связь многие-ко-многим с ProductCategory
     *
     * Примеры:
     * - Атрибут "Жирность" применим к категориям: "Молоко", "Сметана", "Творог"
     * - Атрибут "Длина упаковки" применим ко всем категориям (для складского учета)
     * - Атрибут "Сорт" применим к категории: "Яблоки", "Груши", "Картофель"
     *
     * Используется для:
     * - Автоматического формирования формы ввода товара
     *   (при выборе категории показываются только применимые атрибуты)
     * - Фильтрации товаров по характеристикам внутри категории
     * - Валидации данных (проверка, что у товара заполнены все обязательные атрибуты)
     */
    @ToString.Exclude
    @ManyToMany(mappedBy = "attributes")
    private List<ProductCategory> categories = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новый атрибут товара
     *
     * @param name название атрибута (например, "Жирность")
     * @param unit единица измерения (например, "%") или null для текстовых атрибутов
     * @param dataType тип данных (TEXT, NUMBER, DATE)
     * @param categories список категорий, к которым применим атрибут
     */
    public ProductAttribute(String name, String unit, AttributeType dataType, List<ProductCategory> categories) {
        this.name = name;
        this.unit = unit;
        this.dataType = dataType;
        this.categories = categories;
    }

    // ========== МЕТОДЫ ==========
}