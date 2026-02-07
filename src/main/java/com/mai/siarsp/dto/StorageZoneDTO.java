package com.mai.siarsp.dto;

import lombok.Data;

import java.util.List;

@Data
public class StorageZoneDTO {
    private Long id;
    private String label;
    private double length;
    private double width;
    private double height;
    private Long shelfId;
    private String shelfCode;
    private double capacityVolume;
    private double occupancyPercentage;
    private List<ZoneProductDTO> products;
}
