package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Факт поставки от поставщика, который привязан к заявке и документам приемки.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_supply",
        uniqueConstraints = @UniqueConstraint(columnNames = {"delivery_id", "product_id"}))
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