package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_requestedProduct")
@EqualsAndHashCode(of = "id")
public class RequestedProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private RequestForDelivery request;

    public RequestedProduct(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
    }

}
