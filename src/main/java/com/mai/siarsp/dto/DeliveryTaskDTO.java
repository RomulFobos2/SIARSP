package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.DeliveryTaskStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeliveryTaskDTO {
    private Long id;
    private DeliveryTaskStatus status;
    private LocalDateTime plannedStartTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime plannedEndTime;
    private LocalDateTime actualEndTime;
    private Integer startMileage;
    private Integer endMileage;
    private Double currentLatitude;
    private Double currentLongitude;
    private String ttnNumber;
    private Long clientOrderId;
    private String clientOrderNumber;
    private Long driverId;
    private String driverFullName;
    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private Integer totalMileage;
    private List<RoutePointDTO> routePoints;
    private Long ttnId;
}
