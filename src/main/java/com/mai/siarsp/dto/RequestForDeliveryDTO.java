package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.RequestStatus;
import lombok.Data;

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
    private Long deliveryId;
    private List<RequestedProductDTO> requestedProducts;
}
