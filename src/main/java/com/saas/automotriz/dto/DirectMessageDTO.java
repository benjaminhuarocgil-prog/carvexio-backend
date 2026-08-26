package com.saas.automotriz.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DirectMessageDTO {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private boolean mine;
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType;
}
