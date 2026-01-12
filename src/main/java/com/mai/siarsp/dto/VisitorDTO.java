package com.mai.siarsp.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class VisitorDTO {
    private Long id;
    private String lastName;
    private String firstName;
    private String patronymicName;
    private String fullName;
    private String sex;
    private String username;
    private String roleName;
    private String mobileNumber;
    private String roleDescription;
    private LocalDate dateRegistration;
    private LocalDate dateBirthday;
    private boolean needChangePass;
}
