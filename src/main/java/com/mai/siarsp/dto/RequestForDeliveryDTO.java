package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.RequestStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RequestForDeliveryDTO {
    private Long id;
    private LocalDate requestDate;
    private LocalDate receivedDate;
    private RequestStatus status;
    private Long supplierId;
    private String supplierName;
    private Long warehouseId;
    private String warehouseName;
    private String warehouseAddress;
    private BigDecimal deliveryCost;
    private BigDecimal totalCost;  // вычисляемое
    private Long deliveryId;
    private List<RequestedProductDTO> requestedProducts;
    private List<CommentDTO> comments;
}
