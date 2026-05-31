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

    /**
     * Добавочная стоимость (наценка) в процентах.
     * Итоговая цена за единицу = purchasePrice × (1 + markupPercent / 100).
     */
    @Column(nullable = false)
    private int markupPercent = 0;

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

    /**
     * Итоговая цена за единицу с учётом наценки.
     * Формула: purchasePrice × (1 + markupPercent / 100)
     */
    @Transient
    public BigDecimal getFinalPricePerUnit() {
        if (markupPercent == 0) {
            return purchasePrice;
        }
        BigDecimal multiplier = BigDecimal.ONE
                .add(BigDecimal.valueOf(markupPercent).divide(BigDecimal.valueOf(100)));
        return purchasePrice.multiply(multiplier).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Итоговая стоимость позиции с учётом наценки: итоговая цена × количество.
     */
    public BigDecimal getTotalPrice() {
        return getFinalPricePerUnit().multiply(BigDecimal.valueOf(quantity));
    }
}