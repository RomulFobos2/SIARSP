package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.RequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_requestForDelivery")
@EqualsAndHashCode(of = "id")
//Запрос поставщику на Поставку (приход)
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
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestedProduct> requestedProducts = new ArrayList<>();

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



}
