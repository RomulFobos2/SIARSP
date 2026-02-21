package com.mai.siarsp.dto;

/**
 * Лёгкий DTO зоны хранения для AJAX-эндпоинта выбора зоны размещения
 */
public class ZoneSelectDto {

    private Long id;
    private String label;
    private String shelfCode;
    private double capacityLiters;
    private double occupancyPercent;
    private double availableLiters;

    public ZoneSelectDto(Long id, String label, String shelfCode,
                         double capacityLiters, double occupancyPercent, double availableLiters) {
        this.id = id;
        this.label = label;
        this.shelfCode = shelfCode;
        this.capacityLiters = capacityLiters;
        this.occupancyPercent = occupancyPercent;
        this.availableLiters = availableLiters;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public String getShelfCode() { return shelfCode; }
    public double getCapacityLiters() { return capacityLiters; }
    public double getOccupancyPercent() { return occupancyPercent; }
    public double getAvailableLiters() { return availableLiters; }
}
