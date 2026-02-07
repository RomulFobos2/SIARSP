package com.mai.siarsp.dto;

import com.mai.siarsp.enumeration.WriteOffReason;
import lombok.Data;

import java.time.LocalDate;

@Data
public class WriteOffActDTO {
    private Long id;
    private String actNumber;
    private LocalDate actDate;
    private WriteOffReason reason;
    private int quantity;
    private String comment;
    private Long productId;
    private String productName;
    private String productArticle;
    private Long responsibleEmployeeId;
    private String responsibleEmployeeFullName;
}
