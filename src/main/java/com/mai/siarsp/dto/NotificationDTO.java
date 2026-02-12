package com.mai.siarsp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private Long recipientId;
    private String text;
    private LocalDateTime createdAt;
    private String status;
}
