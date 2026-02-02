package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Глобальная категория товара
 *
 * Верхний уровень классификации товаров в системе СИАРСП.
 * Представляет крупные группы товаров, общие для всех видов продукции.
 *
 * Примеры глобальных категорий:
 * - "Молочная продукция"
 * - "Мясная продукция"
 * - "Овощи и фрукты"
 * - "Хлебобулочные изделия"
 * - "Бакалея"
 *
 * Используется для:
 * - Верхнеуровневой классификации товаров
 *
 * Иерархия классификации:
 * GlobalProductCategory (Молочная продукция)
 *   → ProductCategory (Молоко)
 *     → Product (Молоко 3.2% 1л "Простоквашино")
 *
 * Связи:
 * - Одна глобальная категория может содержать множество категорий (ProductCategory)
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_globalProductCategory")
@EqualsAndHashCode(of = "id")
public class GlobalProductCategory {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название глобальной категории
     * Уникальное для всей системы
     */
    @Column(nullable = false, unique = true)
    private String name;

    // ========== КОНСТРУКТОРЫ ==========

    // ========== МЕТОДЫ ==========

}