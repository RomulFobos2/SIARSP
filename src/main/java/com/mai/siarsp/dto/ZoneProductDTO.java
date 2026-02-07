package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.BoxOrientation;
import lombok.Data;

@Data
public class ZoneProductDTO {
    private Long id;
    private int quantity;
    private BoxOrientation orientation;
    private Long zoneId;
    private Long productId;
    private String productName;
    private String productArticle;
    private double volumePerUnit;
    private double totalVolume;
}
