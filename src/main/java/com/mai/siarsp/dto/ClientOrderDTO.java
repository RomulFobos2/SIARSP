package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.ClientOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ClientOrderDTO {
    private Long id;
    private String orderNumber;
    private LocalDate orderDate;
    private LocalDate deliveryDate;
    private LocalDate actualDeliveryDate;
    private ClientOrderStatus status;
    private String statusDisplayName;
    private BigDecimal totalAmount;
    private String comment;
    private String contractFile;
    private Long clientId;
    private String clientOrganizationName;
    private Long responsibleEmployeeId;
    private String responsibleEmployeeFullName;
    private List<OrderedProductDTO> orderedProducts;
    private Long deliveryTaskId;
    private Long acceptanceActId;
}
