package com.mai.siarsp.dto;

import lombok.Data;

import java.util.List;

@Data
public class ShelfDTO {
    private Long id;
    private String code;
    private Long warehouseId;
    private String warehouseName;
    private List<StorageZoneDTO> storageZones;
}
