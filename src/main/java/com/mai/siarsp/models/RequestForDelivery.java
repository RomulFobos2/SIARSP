package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Заявка на поставку от поставщика на склад. По ней запускаются приемка, размещение и дальнейшее использование товара в заказах.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_requestForDelivery")
@EqualsAndHashCode(of = "id")
public class RequestForDelivery {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate requestDate = LocalDate.now();

    private LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.DRAFT;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Supplier supplier;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn
    private Warehouse warehouse;

    @Column(precision = 10, scale = 2)
    private BigDecimal deliveryCost;

    @ToString.Exclude
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestedProduct> requestedProducts = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(mappedBy = "requestForDelivery", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Comment> comments = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    // ========== КОНСТРУКТОРЫ ==========

    public RequestForDelivery(Supplier supplier) {
        this.supplier = supplier;
        this.requestDate = LocalDate.now();
        this.status = RequestStatus.DRAFT;
        this.requestedProducts = new ArrayList<>();
    }

    // ========== МЕТОДЫ ==========

    public void addRequestedProduct(RequestedProduct requestedProduct) {
        this.requestedProducts.add(requestedProduct);
        requestedProduct.setRequest(this);
    }

    @Transient
    public BigDecimal getTotalCost() {
        BigDecimal productsCost = requestedProducts.stream()
                .map(RequestedProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return productsCost.add(deliveryCost != null ? deliveryCost : BigDecimal.ZERO);
    }

}