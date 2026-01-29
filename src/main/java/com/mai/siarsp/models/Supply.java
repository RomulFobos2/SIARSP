package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_supply")
@EqualsAndHashCode(of = "id")
//Поставка по конкретному товару
public class Supply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Delivery delivery;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal purchasePrice;

    @Column(nullable = false)
    private int quantity;

    public Supply(Product product, BigDecimal purchasePrice, int quantity) {
        this.product = product;
        this.purchasePrice = purchasePrice;
        this.quantity = quantity;
    }

    // Общая стоимость поставки товара
    public BigDecimal getTotalPrice() {
        return purchasePrice.multiply(BigDecimal.valueOf(quantity));
    }
}
