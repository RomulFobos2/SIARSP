package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.ClientOrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Главная сущность процесса продаж: от создания заказа до фактической доставки. На заказ завязаны позиции, доставка и закрывающие документы.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_clientOrder")
@EqualsAndHashCode(of = "id")
public class ClientOrder {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private LocalDate deliveryDate;

    @Column
    private LocalDate actualDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClientOrderStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(length = 500)
    private String comment;

    @Column(length = 500)
    private String contractFile;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Client client;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee responsibleEmployee;

    @ToString.Exclude
    @OneToMany(mappedBy = "clientOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderedProduct> orderedProducts = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(mappedBy = "clientOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private DeliveryTask deliveryTask;

    @ToString.Exclude
    @OneToOne(mappedBy = "clientOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private AcceptanceAct acceptanceAct;

    // ========== КОНСТРУКТОРЫ ==========

    public ClientOrder(String orderNumber, Client client, Employee responsibleEmployee, LocalDate deliveryDate) {
        this.orderNumber = orderNumber;
        this.client = client;
        this.responsibleEmployee = responsibleEmployee;
        this.deliveryDate = deliveryDate;
        this.orderDate = LocalDateTime.now();
        this.status = ClientOrderStatus.NEW;
        this.totalAmount = BigDecimal.ZERO;
    }

    // ========== МЕТОДЫ ==========

    public void addOrderedProduct(OrderedProduct orderedProduct) {
        this.orderedProducts.add(orderedProduct);
        orderedProduct.setClientOrder(this);
    }

    public void calculateTotalAmount() {
        this.totalAmount = orderedProducts.stream()
                .map(OrderedProduct::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}