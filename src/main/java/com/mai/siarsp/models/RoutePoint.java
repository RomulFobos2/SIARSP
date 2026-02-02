package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.RoutePointType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;


@Data
@NoArgsConstructor
@Entity
@Table(name = "t_routePoint")
@EqualsAndHashCode(of = "id")
//Контрольная точка маршрута
public class RoutePoint {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoutePointType pointType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false, length = 500)
    private String address;

    @Column
    private LocalDateTime plannedArrivalTime;

    @Column
    private LocalDateTime actualArrivalTime;

    @Column(nullable = false)
    private boolean isReached;

    @Column(length = 500)
    private String comment;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private DeliveryTask deliveryTask;

    // ========== КОНСТРУКТОРЫ ==========
    public RoutePoint(int orderIndex, RoutePointType pointType,
                      Double latitude, Double longitude, String address) {
        this.orderIndex = orderIndex;
        this.pointType = pointType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.isReached = false;
    }

    // ========== МЕТОДЫ ==========
    @Transient
    public String getCoordinates() {
        return String.format("%.6f, %.6f", latitude, longitude);
    }
}
