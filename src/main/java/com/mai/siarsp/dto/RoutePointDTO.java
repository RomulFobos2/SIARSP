package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.RoutePointType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RoutePointDTO {
    private Long id;
    private int orderIndex;
    private RoutePointType pointType;
    private Double latitude;
    private Double longitude;
    private String address;
    private LocalDateTime plannedArrivalTime;
    private LocalDateTime actualArrivalTime;
    private boolean reached;
    private String comment;
    private Long deliveryTaskId;
    private String coordinates;
}
