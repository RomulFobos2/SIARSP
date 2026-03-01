package com.mai.siarsp.dto;

import lombok.Data;

import java.util.List;

@Data
public class ClientDTO {
    private Long id;
    private String organizationType;
    private String organizationName;
    private String inn;
    private String kpp;
    private String ogrn;
    private String legalAddress;
    private String deliveryAddress;
    private Double deliveryLatitude;
    private Double deliveryLongitude;
    private String contactPerson;
    private String phoneNumber;
    private String email;
    private String displayName;
    private List<ClientOrderDTO> orders;
}
