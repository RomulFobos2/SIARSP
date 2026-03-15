package com.mai.siarsp.models;

import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Оперативная задача водителю-экспедитору: маршрут, временные окна, ответственные и текущий статус выполнения.
 */

@Data
@NoArgsConstructor
@Entity
@Table(name = "t_deliveryTask")
@EqualsAndHashCode(of = "id")
public class DeliveryTask {

    // ========== ПОЛЯ ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryTaskStatus status;

    @Column
    private LocalDateTime plannedStartTime;

    @Column
    private LocalDateTime actualStartTime;

    @Column
    private LocalDateTime plannedEndTime;

    @Column
    private LocalDateTime actualEndTime;

    @Column
    private Integer startMileage;

    @Column
    private Integer endMileage;

    @Column
    private Double currentLatitude;

    @Column
    private Double currentLongitude;

    @Column(length = 50)
    private String ttnNumber;

    @ToString.Exclude
    @OneToOne
    @JoinColumn(nullable = false)
    private ClientOrder clientOrder;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Employee driver;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(nullable = false)
    private Vehicle vehicle;

    @ToString.Exclude
    @OneToMany(mappedBy = "deliveryTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutePoint> routePoints = new ArrayList<>();

    @ToString.Exclude
    @OneToOne(mappedBy = "deliveryTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private TTN ttn;

    // ========== КОНСТРУКТОРЫ ==========

    public DeliveryTask(ClientOrder clientOrder, Employee driver, Vehicle vehicle) {
        this.clientOrder = clientOrder;
        this.driver = driver;
        this.vehicle = vehicle;
        this.status = DeliveryTaskStatus.PENDING;
    }

    // ========== МЕТОДЫ ==========

    public void addRoutePoint(RoutePoint routePoint) {
        this.routePoints.add(routePoint);
        routePoint.setDeliveryTask(this);
    }

    @Transient
    public Integer getTotalMileage() {
        if (startMileage != null && endMileage != null) {
            return endMileage - startMileage;
        }
        return null;
    }
}