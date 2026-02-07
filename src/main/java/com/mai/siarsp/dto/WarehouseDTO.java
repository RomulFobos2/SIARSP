package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.WarehouseType;
import lombok.Data;

import java.util.List;

@Data
public class WarehouseDTO {
    private Long id;
    private String name;
    private WarehouseType type;
    private double totalVolume;
    private List<ShelfDTO> shelves;
}
