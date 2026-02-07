package com.mai.siarsp.dto;

import lombok.Data;

import java.util.List;

@Data
public class SupplierDTO {
    private Long id;
    private String name;
    private String contactInfo;
    private String address;
    private String inn;
    private String kpp;
    private String ogrn;
    private String paymentAccount;
    private String bik;
    private String bank;
    private String directorLastName;
    private String directorFirstName;
    private String directorPatronymicName;
    private String fullName;
    private String directorShortName;
    private List<DeliveryDTO> deliveries;
}
