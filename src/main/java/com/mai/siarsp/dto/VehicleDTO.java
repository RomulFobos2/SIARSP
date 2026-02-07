package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.VehicleStatus;
import com.mai.siarsp.enumeration.VehicleType;
import lombok.Data;

@Data
public class VehicleDTO {
    private Long id;
    private String registrationNumber;
    private String brand;
    private String model;
    private Integer year;
    private String vin;
    private Double loadCapacity;
    private Double volumeCapacity;
    private VehicleType type;
    private VehicleStatus status;
    private String fullName;
    private boolean available;
}
