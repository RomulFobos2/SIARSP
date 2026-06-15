package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Пик-линия сборки: «позиция заказа ↔ конкретная партия в конкретной зоне ↔ количество».
 * Фиксируется складским работником при подборе товара и используется при погрузке
 * для адресного списания ZoneProduct(supply, zone).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "t_orderedProductPick")
@EqualsAndHashCode(of = "id")
public class OrderedProductPick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "ordered_product_id", nullable = false)
    private OrderedProduct orderedProduct;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "supply_id", nullable = false)
    private Supply supply;

    @ToString.Exclude
    @ManyToOne(optional = false)
    @JoinColumn(name = "zone_id", nullable = false)
    private StorageZone zone;

    @Column(nullable = false)
    private int quantity;

    public OrderedProductPick(OrderedProduct orderedProduct, Supply supply, StorageZone zone, int quantity) {
        this.orderedProduct = orderedProduct;
        this.supply = supply;
        this.zone = zone;
        this.quantity = quantity;
    }
}
