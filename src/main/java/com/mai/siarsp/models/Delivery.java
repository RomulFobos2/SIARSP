package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_delivery")
@EqualsAndHashCode(of = "id")
//Поставка общая
public class Delivery {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate deliveryDate;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Supplier supplier;

    @ToString.Exclude
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Supply> supplies = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(mappedBy = "delivery")
    private RequestForDelivery request;

    // ========== КОНСТРУКТОРЫ ==========
    public Delivery(Supplier supplier, LocalDate deliveryDate) {
        this.supplier = supplier;
        this.deliveryDate = deliveryDate;
        this.supplies = new ArrayList<>();
    }

    // ========== МЕТОДЫ ==========
    public void addSupply(Supply supply) {
        this.supplies.add(supply);
        supply.setDelivery(this);
    }

    public BigDecimal getTotalCost() {
        return supplies.stream()
                .map(Supply::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
