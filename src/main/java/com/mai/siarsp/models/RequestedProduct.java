package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Позиция внутри заявки на поставку: товар, объем и ожидаемые параметры, с которыми склад готовится к приемке.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_requestedProduct",
        uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "product_id"}))
@EqualsAndHashCode(of = "id")
public class RequestedProduct {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(length = 100)
    private String unit;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private RequestForDelivery request;

    // ========== КОНСТРУКТОРЫ ==========

    public RequestedProduct(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

    public RequestedProduct(Product product, int quantity, BigDecimal purchasePrice) {
        this.product = product;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
    }

    // ========== МЕТОДЫ ==========

    @Transient
    public BigDecimal getTotalPrice() {
        if (purchasePrice == null) return BigDecimal.ZERO;
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }

}