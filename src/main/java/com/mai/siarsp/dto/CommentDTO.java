package com.mai.siarsp.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentDTO {
    private Long id;
    private Long authorId;
    private String authorFullName;
    private String text;
    private LocalDateTime createdAt;
}
