package com.mai.siarsp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeDTO {
    private Long id;
    private String lastName;
    private String firstName;
    private String patronymicName;
    private String fullName;
    private String username;
    private String roleName;
    private String roleDescription;
    private LocalDate dateOfRegistration;
    private boolean needChangePass;
    private boolean isActive;
}
