package com.mai.siarsp.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class DeliveryDTO {
    private Long id;
    private LocalDate deliveryDate;
    private Long supplierId;
    private String supplierName;
    private BigDecimal totalCost;
    private List<SupplyDTO> supplies;
    private Long requestId;
}
