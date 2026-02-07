package com.mai.siarsp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TTNDTO {
    private Long id;
    private String ttnNumber;
    private LocalDate issueDate;
    private String cargoDescription;
    private Double totalWeight;
    private Double totalVolume;
    private String comment;
    private Long deliveryTaskId;
    private Long vehicleId;
    private String vehicleRegistrationNumber;
    private Long driverId;
    private String driverFullName;
}
