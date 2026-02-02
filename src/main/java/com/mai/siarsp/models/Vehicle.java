package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.VehicleStatus;
import com.mai.siarsp.enumeration.VehicleType;
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
@Table(name = "t_vehicle")
@EqualsAndHashCode(of = "id")
//Автомобиль
public class Vehicle {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String registrationNumber;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column
    private Integer year;

    @Column(length = 50)
    private String vin;

    @Column(nullable = false)
    private Double loadCapacity;

    @Column(nullable = false)
    private Double volumeCapacity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column
    private LocalDate nextMaintenanceDate;

    @Column(length = 500)
    private String comment;

    @ToString.Exclude
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DeliveryTask> deliveryTasks = new ArrayList<>();

    // ========== КОНСТРУКТОРЫ ==========
    public Vehicle(String registrationNumber, String brand, String model,
                   Double loadCapacity, Double volumeCapacity, VehicleType type) {
        this.registrationNumber = registrationNumber;
        this.brand = brand;
        this.model = model;
        this.loadCapacity = loadCapacity;
        this.volumeCapacity = volumeCapacity;
        this.type = type;
        this.status = VehicleStatus.AVAILABLE;
    }

    // ========== МЕТОДЫ ==========
    @Transient
    public String getFullName() {
        return brand + " " + model + " (" + registrationNumber + ")";
    }

    @Transient
    public boolean isAvailable() {
        return status == VehicleStatus.AVAILABLE;
    }

}
