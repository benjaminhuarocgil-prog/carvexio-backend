package com.saas.automotriz.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PlatformNotificationDTO {
    private Long id;
    private String message;
    private Integer commissionRate;
    private LocalDateTime createdAt;
    private Boolean dismissed;
}
