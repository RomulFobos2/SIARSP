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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate requestDate = LocalDate.now();

    private LocalDate receivedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING; // PENDING, SENT, RECEIVED, CANCELLED

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Supplier supplier;

    @ToString.Exclude
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL)
    private List<RequestedProduct> requestedProducts = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;  // Поставка, которая выполнила этот запрос

    public RequestForDelivery(Supplier supplier) {
        this.supplier = supplier;
        this.requestDate = LocalDate.now();
        this.status = RequestStatus.PENDING;
        this.requestedProducts = new ArrayList<>();
    }

    public void addRequestedProduct(RequestedProduct requestedProduct) {
        this.requestedProducts.add(requestedProduct);
        requestedProduct.setRequest(this);
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    public boolean isSent() {
        return status == RequestStatus.SENT;
    }

    public boolean isReceived() {
        return status == RequestStatus.RECEIVED;
    }

    public boolean isCancelled() {
        return status == RequestStatus.CANCELLED;
    }

}
