package com.mai.siarsp.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AcceptanceActDTO {
    private Long id;
    private String actNumber;
    private LocalDate actDate;
    private String clientRepresentative;
    private boolean signed;
    private LocalDateTime signedAt;
    private String comment;
    private Long clientOrderId;
    private String clientOrderNumber;
    private Long clientId;
    private String clientOrganizationName;
    private Long deliveredById;
    private String deliveredByFullName;
}
