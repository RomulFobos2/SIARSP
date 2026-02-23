package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.EquipmentStatus;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO для отображения данных об оборудовании склада.
 */
@Data
public class WarehouseEquipmentDTO {
    private Long id;
    private String name;
    private String serialNumber;
    private LocalDate productionDate;
    private Integer usefulLifeYears;
    private Long equipmentTypeId;
    private String equipmentTypeName;
    private Long warehouseId;
    private String warehouseName;
    private EquipmentStatus status;
    private String statusDisplayName;
    private LocalDate expirationDate;
    private boolean expired;
}
