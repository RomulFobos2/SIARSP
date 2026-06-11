package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Партия товара от поставщика: фактический приход с конкретной датой производства и срока годности.
 * <p>
 * Одна партия идентифицируется парой {@link #productionDate} + {@link #expirationDate} в рамках Product.
 * Уникального ключа (delivery_id, product_id) больше нет: один Delivery может содержать несколько
 * партий одного товара (с разными производственными датами).
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_supply")
@EqualsAndHashCode(of = "id")
public class Supply {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(length = 100)
    private String unit;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int deficitQuantity = 0;

    @Column(length = 500)
    private String deficitReason;

    /**
     * Дата производства партии. Nullable на уровне БД для совместимости с миграцией,
     * но обязательное на уровне сервиса при создании новой Supply.
     */
    @Column
    private LocalDate productionDate;

    /**
     * Срок годности партии = productionDate + Product.shelfLifeDays. Вычисляется в сервисе.
     */
    @Column
    private LocalDate expirationDate;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Delivery delivery;

    // ========== КОНСТРУКТОРЫ ==========

    public Supply(Product product, BigDecimal purchasePrice, int quantity) {
        this.product = product;
        this.purchasePrice = purchasePrice;
        this.quantity = quantity;
    }

    // ========== МЕТОДЫ ==========

    public BigDecimal getTotalPrice() {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }
}