package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.ClientOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * Позиция в клиентском заказе: какой товар, в каком количестве и по какой цене уходит клиенту.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_orderedProduct",
        uniqueConstraints = @UniqueConstraint(columnNames = {"client_order_id", "product_id"}))
@EqualsAndHashCode(of = "id")
public class OrderedProduct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    /**
     * Цена за единицу товара без учёта скидки (в рублях)
     * Исходная цена, от которой рассчитывается скидка
     * Может быть null, если скидка не применялась
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal originalPrice;

    /**
     * Размер скидки в процентах (от 0 до 100)
     * Может быть null, если скидка не применялась
     * price = originalPrice × (1 - discountPercent / 100)
     */
    @Column
    private Integer discountPercent;

    /**
     * Товар, который заказан
     * Ссылка на справочник товаров
     * Содержит информацию о наименовании, категории, атрибутах
     */
    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private ClientOrder clientOrder;

    // ========== КОНСТРУКТОРЫ ==========

    public OrderedProduct(Product product, int quantity, BigDecimal price) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Создает новую позицию заказа с информацией о скидке
     *
     * @param product заказываемый товар
     * @param quantity количество единиц товара
     * @param price цена за единицу с учётом скидки
     * @param originalPrice цена за единицу без скидки
     * @param discountPercent размер скидки в процентах (0-100)
     */
    public OrderedProduct(Product product, int quantity, BigDecimal price,
                          BigDecimal originalPrice, Integer discountPercent) {
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.originalPrice = originalPrice;
        this.discountPercent = discountPercent;
        this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
    }

    // ========== МЕТОДЫ ==========

    public void recalculateTotalPrice() {
        this.totalPrice = price.multiply(BigDecimal.valueOf(quantity));
    }
}