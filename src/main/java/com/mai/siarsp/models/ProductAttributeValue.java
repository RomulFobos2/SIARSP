package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.AttributeType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

/**
 * Значение конкретной характеристики для товара
 * Связи:
 * - Принадлежит одному товару (Product)
 * - Ссылается на один атрибут (ProductAttribute)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_productAttributeValue",
        uniqueConstraints = @UniqueConstraint(columnNames = {"product_id", "attribute_id"}))
@EqualsAndHashCode(of = "id")
public class ProductAttributeValue {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Строковое представление значения атрибута
     * Хранится в виде строки независимо от фактического типа данных
     *
     * Формат хранения:
     * - Всегда строка (varchar), независимо от типа
     * - Преобразование к нужному типу происходит при чтении (getValue)
     * - Максимальная длина: 500 символов (достаточно для любых значений)
     *
     * Валидация:
     * - При сохранении проверяется соответствие типу атрибута
     * - NUMBER: должно парситься как число
     * - DATE: должно парситься как дата
     * - TEXT: любая строка
     *
     */
    @Column(length = 500, nullable = false)
    private String value;

    /**
     * Товар, которому принадлежит данное значение атрибута
     * Связь с основной сущностью товара
     *
     * Один товар может иметь множество значений атрибутов:
     * Product "Молоко 3.2% 1л":
     *   - Жирность: 3.2
     *   - Объем: 1.0
     *   - Срок годности: 7
     *   - Длина упаковки: 10
     *   - Ширина упаковки: 10
     *   - Высота упаковки: 23
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    /**
     * Атрибут (характеристика), значение которого хранится
     * Определяет тип данных и единицы измерения
     *
     * Содержит метаданные о характеристике:
     * - Название ("Жирность", "Вес", "Срок годности")
     * - Тип данных (NUMBER, TEXT, DATE)
     * - Единицу измерения (%, г, л, дней)
     *
     */
    @ManyToOne
    @JoinColumn(nullable = false)
    private ProductAttribute attribute;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Создает новое значение атрибута для товара
     *
     * @param product товар, которому принадлежит значение
     * @param attribute атрибут (характеристика)
     * @param value строковое представление значения
     */
    public ProductAttributeValue(Product product, ProductAttribute attribute, String value) {
        this.product = product;
        this.attribute = attribute;
        this.value = value;
    }

    // ========== МЕТОДЫ ==========

    /**
     * Типобезопасное получение значения атрибута с автоматическим преобразованием типа
     *
     * Метод анализирует тип атрибута (attribute.dataType) и запрошенный тип (Class<T>),
     * выполняет безопасное преобразование строкового значения в нужный тип.
     *
     * Поддерживаемые типы:
     *
     * 1. LocalDate (для AttributeType.DATE):
     *    - Парсит строку формата ISO (yyyy-MM-dd)
     *    - Пример: "2025-03-15" → LocalDate.of(2025, 3, 15)
     *
     * 2. Double (для AttributeType.NUMBER):
     *    - Парсит числа с плавающей точкой
     *    - Поддерживает как точку, так и запятую как разделитель
     *    - Пример: "3.2" или "3,2" → 3.2
     *
     * 3. Integer (для AttributeType.NUMBER):
     *    - Парсит как Double, затем приводит к Integer
     *    - Пример: "400" → 400, "3.7" → 3 (отбрасывается дробная часть)
     *
     * 4. String (для любого типа):
     *    - Возвращает значение как есть
     *    - Всегда работает, независимо от типа атрибута
     *
     * Безопасность:
     * - Не бросает исключений при ошибках парсинга
     * - Возвращает null при несовместимости типов или ошибке парсинга
     * - Проверяет соответствие типа атрибута и запрошенного типа
     *
     * Примеры использования:
     *
     * Пример 1 - Получение числового значения:
     * ProductAttribute fatAttr = new ProductAttribute("Жирность", AttributeType.NUMBER, "%");
     * ProductAttributeValue fatValue = new ProductAttributeValue(milk, fatAttr, "3.2");
     *
     * Double fat = fatValue.getValue(Double.class);
     * // fat = 3.2
     *
     * Пример 2 - Получение целого числа:
     * ProductAttribute weightAttr = new ProductAttribute("Вес", AttributeType.NUMBER, "г");
     * ProductAttributeValue weightValue = new ProductAttributeValue(bread, weightAttr, "400");
     *
     * Integer weight = weightValue.getValue(Integer.class);
     * // weight = 400
     *
     * Пример 3 - Получение даты:
     * ProductAttribute dateAttr = new ProductAttribute("Дата производства", AttributeType.DATE, "");
     * ProductAttributeValue dateValue = new ProductAttributeValue(milk, dateAttr, "2025-03-15");
     *
     * LocalDate date = dateValue.getValue(LocalDate.class);
     * // date = LocalDate.of(2025, 3, 15)
     *
     * Пример 4 - Получение текста:
     * ProductAttribute typeAttr = new ProductAttribute("Тип обработки", AttributeType.TEXT, "");
     * ProductAttributeValue typeValue = new ProductAttributeValue(milk, typeAttr, "Пастеризованное");
     *
     * String type = typeValue.getValue(String.class);
     * // type = "Пастеризованное"
     *
     * Пример 5 - Несовместимые типы (безопасная обработка):
     * ProductAttribute textAttr = new ProductAttribute("Описание", AttributeType.TEXT, "");
     * ProductAttributeValue textValue = new ProductAttributeValue(milk, textAttr, "Свежее молоко");
     *
     * Double number = textValue.getValue(Double.class);
     * // number = null (TEXT атрибут не может быть преобразован в Double)
     *
     * Пример 6 - Ошибка парсинга (безопасная обработка):
     * ProductAttribute numAttr = new ProductAttribute("Жирность", AttributeType.NUMBER, "%");
     * ProductAttributeValue badValue = new ProductAttributeValue(milk, numAttr, "некорректное значение");
     *
     * Double fat = badValue.getValue(Double.class);
     * // fat = null (не удалось распарсить строку как число)
     *
     * Использование в коде:
     *
     * // Получение жирности молока
     * Optional<ProductAttributeValue> fatValueOpt = product.getAttributeValues().stream()
     *     .filter(v -> v.getAttribute().getName().equals("Жирность"))
     *     .findFirst();
     *
     * if (fatValueOpt.isPresent()) {
     *     Double fat = fatValueOpt.get().getValue(Double.class);
     *     if (fat != null && fat >= 3.0) {
     *         System.out.println("Высокая жирность: " + fat + "%");
     *     }
     * }
     *
     * // Фильтрация товаров по сроку годности
     * List<Product> freshProducts = products.stream()
     *     .filter(p -> {
     *         Optional<ProductAttributeValue> shelfLife = p.getAttributeValues().stream()
     *             .filter(v -> v.getAttribute().getName().equals("Срок годности"))
     *             .findFirst();
     *         if (shelfLife.isPresent()) {
     *             Integer days = shelfLife.get().getValue(Integer.class);
     *             return days != null && days >= 7;
     *         }
     *         return false;
     *     })
     *     .toList();
     *
     * @param <T> тип, в который нужно преобразовать значение
     * @param type класс типа (LocalDate.class, Double.class, Integer.class, String.class)
     * @return значение атрибута в нужном типе, или null при ошибке/несовместимости
     */
    public <T> T getValue(Class<T> type) {
        if (attribute == null) return null;

        // DATE - Преобразование в LocalDate
        if (type == LocalDate.class && attribute.getDataType() == AttributeType.DATE) {
            try {
                return type.cast(LocalDate.parse(value.trim()));
            } catch (Exception e) {
                return null; // Некорректный формат даты
            }
        }

        // NUMBER - Преобразование в Double
        if (type == Double.class && attribute.getDataType() == AttributeType.NUMBER) {
            try {
                // Поддержка как точки, так и запятой как разделителя
                return type.cast(Double.parseDouble(value.replace(",", ".").trim()));
            } catch (NumberFormatException e) {
                return null; // Некорректное число
            }
        }

        // NUMBER - Преобразование в Integer
        if (type == Integer.class && attribute.getDataType() == AttributeType.NUMBER) {
            try {
                // Сначала парсим как Double (чтобы поддержать "3,5" → 3)
                Double d = Double.parseDouble(value.replace(",", ".").trim());
                return type.cast(d.intValue());
            } catch (NumberFormatException e) {
                return null; // Некорректное число
            }
        }

        // TEXT - Возвращаем строку как есть
        if (type == String.class) {
            return type.cast(value);
        }

        // Несовместимые типы - возвращаем null
        return null;
    }
}