package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.EquipmentType;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WarehouseEquipmentDTO {
    private Long id;
    private String name;
    private String serialNumber;
    private LocalDate productionDate;
    private Integer usefulLifeYears;
    private EquipmentType equipmentType;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate expirationDate;
    private boolean expired;
}
