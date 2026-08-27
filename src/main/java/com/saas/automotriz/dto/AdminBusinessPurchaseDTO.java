package com.saas.automotriz.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminBusinessPurchaseDTO {
    private Long orderId;
    private Double paidAmount;
    private Integer commissionRate;
    private Double adminAmount;
    private Double businessAmount;
    private String status;
    private LocalDateTime createdAt;
}
