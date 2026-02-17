package com.mai.siarsp.dto;

import java.util.List;

/**
 * Детальная информация о зоне хранения (полке)
 */
public class ZoneInfo {

    private Long id;
    private String label;
    private String shelfCode;
    private String warehouseName;
    private double length;
    private double width;
    private double height;
    private double capacityVolume;
    private double usedVolume;
    private double occupancyPercent;
    private List<ZoneProductShortInfo> products;

    public ZoneInfo(Long id, String label, String shelfCode, String warehouseName,
                    double length, double width, double height,
                    double capacityVolume, double usedVolume, double occupancyPercent,
                    List<ZoneProductShortInfo> products) {
        this.id = id;
        this.label = label;
        this.shelfCode = shelfCode;
        this.warehouseName = warehouseName;
        this.length = length;
        this.width = width;
        this.height = height;
        this.capacityVolume = capacityVolume;
        this.usedVolume = usedVolume;
        this.occupancyPercent = occupancyPercent;
        this.products = products;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public String getShelfCode() { return shelfCode; }
    public String getWarehouseName() { return warehouseName; }
    public double getLength() { return length; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getCapacityVolume() { return capacityVolume; }
    public double getUsedVolume() { return usedVolume; }
    public double getOccupancyPercent() { return occupancyPercent; }
    public List<ZoneProductShortInfo> getProducts() { return products; }

    /**
     * Краткая информация о товаре в зоне хранения
     */
    public record ZoneProductShortInfo(
            Long productId,
            String productName,
            String productArticle,
            int quantity,
            String orientationLabel
    ) {
    }
}
