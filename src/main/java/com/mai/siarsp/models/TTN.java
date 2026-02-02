package com.mai.siarsp.models;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@Entity
@Table(name = "t_ttn")
@EqualsAndHashCode(of = "id")
//Товарно-транспортная накладная
public class TTN {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String ttnNumber;

    @Column(nullable = false)
    private LocalDate issueDate;

    @Column(length = 500)
    private String cargoDescription;

    @Column
    private Double totalWeight;

    @Column
    private Double totalVolume;

    @Column(length = 500)
    private String comment;

    @ToString.Exclude
    @OneToOne
    @JoinColumn(nullable = false)
    private DeliveryTask deliveryTask;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Vehicle vehicle;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee driver;

    // ========== КОНСТРУКТОРЫ ==========
    public TTN(String ttnNumber, DeliveryTask deliveryTask, Vehicle vehicle, Employee driver) {
        this.ttnNumber = ttnNumber;
        this.deliveryTask = deliveryTask;
        this.vehicle = vehicle;
        this.driver = driver;
        this.issueDate = LocalDate.now();
    }

    // ========== МЕТОДЫ ==========

}
